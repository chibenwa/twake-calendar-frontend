package com.linagora.calendar.e2e.docker;

import java.io.File;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * The whole Twake Calendar stack (frontend + backend + OIDC provider) running in docker.
 *
 * <p>Started once per JVM and shared by every test: booting it costs a couple of minutes,
 * paying that per test class would make the suite unusable.
 *
 * <p><b>Addressing.</b> Docker publishes the services on random host ports, yet the SPA is
 * configured at image build time with fixed URLs ({@code http://frontend}, {@code http://api},
 * {@code http://sso:5556}...). The two are reconciled by {@link #hostResolverRules()}, fed to
 * Chromium so that the browser resolves those hostnames to the ports docker actually picked.
 * The upside is that browser and containers share one single set of URLs: no {@code /etc/hosts}
 * entry, no fixed port, and the cross origin topology of a real deployment is preserved.
 */
public class TwakeCalendarStack {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwakeCalendarStack.class);

    public enum Service {
        FRONTEND("frontend", 80),
        /** Reverse proxy exposing the backend as `api` and `dav` */
        PROXY("proxy", 80),
        SSO("sso", 5554),
        SIDE_SERVICE("twake-calendar-side-service", 8080),
        SIDE_SERVICE_ADMIN("twake-calendar-side-service", 8000),
        SABRE("sabre_dav", 80),
        MONGO("mongo", 27017),
        /** Dex authenticates against it, and E2EUserFactory writes the test accounts to it */
        LDAP("ldap", 389);

        private final String serviceName;
        private final int port;

        Service(String serviceName, int port) {
            this.serviceName = serviceName;
            this.port = port;
        }

        public String serviceName() {
            return serviceName;
        }

        public int port() {
            return port;
        }
    }

    /**
     * Browser facing URL of the SPA, as baked in `src/test/resources/frontend/env.js`.
     *
     * <p>Nothing listens on 8099: the port is made up, the browser remaps it onto whatever
     * port docker published. The hostname has to be `localhost` though, because the OIDC PKCE
     * challenge relies on {@code crypto.subtle}, which browsers only expose to a secure
     * context -- and a plain http origin only qualifies when it is a loopback one.
     */
    public static final String FRONTEND_URL = "http://localhost:8099";

    private static volatile TwakeCalendarStack instance;

    public static TwakeCalendarStack singleton() {
        TwakeCalendarStack local = instance;
        if (local == null) {
            synchronized (TwakeCalendarStack.class) {
                local = instance;
                if (local == null) {
                    local = new TwakeCalendarStack();
                    local.start();
                    instance = local;
                }
            }
        }
        return local;
    }

    private final ComposeContainer environment;

    private TwakeCalendarStack() {
        try {
            environment = new ComposeContainer(
                new File(TwakeCalendarStack.class.getResource("/docker-twake-calendar-e2e.yml").toURI()))
                .withExposedService(Service.FRONTEND.serviceName(), Service.FRONTEND.port())
                .withExposedService(Service.PROXY.serviceName(), Service.PROXY.port())
                .withExposedService(Service.SSO.serviceName(), Service.SSO.port())
                .withExposedService(Service.SIDE_SERVICE.serviceName(), Service.SIDE_SERVICE.port())
                .withExposedService(Service.SIDE_SERVICE_ADMIN.serviceName(), Service.SIDE_SERVICE_ADMIN.port())
                .withExposedService(Service.SABRE.serviceName(), Service.SABRE.port())
                .withExposedService(Service.MONGO.serviceName(), Service.MONGO.port())
                .withExposedService(Service.LDAP.serviceName(), Service.LDAP.port())
                .waitingFor(Service.SIDE_SERVICE.serviceName(),
                    Wait.forLogMessage(".*StartUpChecks all succeeded.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(10)))
                .withLogConsumer(Service.SIDE_SERVICE.serviceName(),
                    log -> System.out.print("[side-service] " + log.getUtf8String()))
                .withLogConsumer(Service.SABRE.serviceName(),
                    log -> System.out.print("[esn-sabre] " + log.getUtf8String()));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to load the e2e docker compose definition", e);
        }
    }

    private void start() {
        LOGGER.info("Starting the Twake Calendar e2e stack, this takes a couple of minutes...");
        environment.start();
        Runtime.getRuntime().addShutdownHook(new Thread(environment::stop));
        LOGGER.info("Stack up. Host resolver rules: {}", hostResolverRules());
    }

    public String host(Service service) {
        return environment.getServiceHost(service.serviceName(), service.port());
    }

    public int port(Service service) {
        return environment.getServicePort(service.serviceName(), service.port());
    }

    public String url(Service service, String scheme) {
        return scheme + "://" + host(service) + ":" + port(service);
    }

    public String mongoUri() {
        return url(Service.MONGO, "mongodb");
    }

    /** Base URI of the side service webadmin API, used to provision test fixtures. */
    public String webAdminUri() {
        return url(Service.SIDE_SERVICE_ADMIN, "http");
    }

    /** Base URI of the Sabre DAV server, used to seed and assert calendar content. */
    public String davUri() {
        return url(Service.SABRE, "http");
    }

    /** Directory Dex authenticates against, where test accounts are created. */
    public String ldapUrl() {
        return url(Service.LDAP, "ldap");
    }

    /**
     * Chromium {@code --host-resolver-rules} value mapping every hostname the SPA is configured
     * with onto the host port docker published it on.
     */
    public String hostResolverRules() {
        return List.of(
                rule("localhost:8099", Service.FRONTEND),
                rule("api", Service.PROXY),
                rule("dav", Service.PROXY),
                rule("sso", Service.SSO),
                // the external hosts the SPA links out to: pointed at the frontend so that
                // following such a link lands on a real page instead of a browser error
                rule("meet.e2e.local", Service.FRONTEND))
            .stream()
            .collect(Collectors.joining(","));
    }

    private String rule(String hostname, Service service) {
        return "MAP " + hostname + " " + host(service) + ":" + port(service);
    }
}
