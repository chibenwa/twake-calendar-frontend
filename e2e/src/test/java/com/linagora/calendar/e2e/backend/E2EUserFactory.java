package com.linagora.calendar.e2e.backend;

import java.util.Hashtable;
import java.util.UUID;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.linagora.calendar.e2e.docker.TwakeCalendarStack;

/**
 * Creates accounts on demand, one per test.
 *
 * <p>Dex authenticates against the OpenLDAP of the stack rather than against a list written in
 * its configuration file, so an account is just an LDAP entry: it can be created a millisecond
 * before the test logs in, and nothing caps how many a run may consume. Everything downstream
 * follows on its own — the side service provisions the OpenPaaS user on the first
 * authenticated call, which in turn provisions the default calendar.
 *
 * <p>Accounts are never reused, which is the whole point: the docker stack is shared by the
 * run because booting it costs minutes, so the user is the unit of isolation. A test that owns
 * its user cannot inherit the events, calendars or settings of another, and none has to clean
 * up after itself.
 */
public class E2EUserFactory {
    public static final String DOMAIN = "open-paas.org";
    public static final String PASSWORD = "secret";

    private static final String USERS_BASE_DN = "ou=users,dc=open-paas.org,dc=lng";
    private static final String ADMIN_DN = "cn=admin,dc=open-paas.org,dc=lng";
    private static final String ADMIN_PASSWORD = "admin";

    private final DirContext directory;
    private final java.net.http.HttpClient httpClient;
    private final String webAdminUri;

    public E2EUserFactory(TwakeCalendarStack stack) {
        this.httpClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
        this.webAdminUri = stack.webAdminUri();
        Hashtable<String, Object> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, stack.ldapUrl());
        environment.put(Context.SECURITY_AUTHENTICATION, "simple");
        environment.put(Context.SECURITY_PRINCIPAL, ADMIN_DN);
        environment.put(Context.SECURITY_CREDENTIALS, ADMIN_PASSWORD);
        try {
            this.directory = new InitialDirContext(environment);
        } catch (NamingException e) {
            throw new RuntimeException("Cannot reach the e2e directory at " + stack.ldapUrl(), e);
        }
    }

    /** A brand new account, ready to log in. */
    public E2EUser newUser() {
        return newUser("e2e-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    }

    /**
     * A brand new account whose local part starts with the given prefix, for the rare test
     * that asserts on what it displays. The prefix is suffixed to keep it unique.
     */
    public E2EUser newUser(String prefix) {
        return newUser(prefix, null);
    }

    /**
     * A brand new account whose display name is the one given, which the SPA then writes as
     * the `CN` of `ORGANIZER` and `ATTENDEE`. Use it to cover non-ASCII names.
     */
    public E2EUser newUser(String prefix, String displayName) {
        String uid = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        String email = uid + "@" + DOMAIN;
        String commonName = displayName == null ? uid : displayName;
        create(uid, email, commonName);
        register(email, commonName);
        return new E2EUser(uid, email, PASSWORD, commonName);
    }

    /**
     * Declares the account to the side service, which otherwise only learns of it when somebody
     * signs in with it.
     *
     * <p>That matters for how long the suite takes: a test needing three people used to need
     * three trips through the identity provider, one per person, purely so that the backend
     * would know they exist. The directory entry and this call together are enough.
     */
    private void register(String email, String commonName) {
        String body = "{\"email\":\"" + email + "\","
            + "\"firstname\":\"" + commonName + "\","
            + "\"lastname\":\"" + commonName + "\","
            + "\"id\":\"" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 24)
            + "\"}";
        try {
            java.net.http.HttpResponse<String> response = httpClient.send(
                java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(webAdminUri + "/registeredUsers"))
                    .header("Content-Type", "application/json")
                    .timeout(java.time.Duration.ofSeconds(30))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                    .build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("The side service refused to register " + email
                    + ": " + response.statusCode() + " " + response.body());
            }
        } catch (java.io.IOException | InterruptedException e) {
            throw new RuntimeException("Could not register " + email, e);
        }
    }

    private synchronized void create(String uid, String email, String commonName) {
        Attribute objectClass = new BasicAttribute("objectClass");
        objectClass.add("inetOrgPerson");

        Attributes attributes = new BasicAttributes(true);
        attributes.put(objectClass);
        attributes.put("uid", uid);
        attributes.put("cn", commonName);
        attributes.put("sn", commonName);
        attributes.put("givenName", commonName);
        attributes.put("mail", email);
        attributes.put("userPassword", PASSWORD);

        try {
            directory.createSubcontext("uid=" + uid + "," + USERS_BASE_DN, attributes);
        } catch (NamingException e) {
            throw new RuntimeException("Failed to create the e2e account " + email, e);
        }
    }
}
