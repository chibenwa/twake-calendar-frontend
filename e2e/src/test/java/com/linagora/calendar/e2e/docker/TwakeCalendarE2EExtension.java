package com.linagora.calendar.e2e.docker;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import com.linagora.calendar.e2e.backend.CalendarProbe;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.backend.E2EUserFactory;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;

/**
 * Wires a Playwright {@link Page} onto the docker stack, one fresh browser context per test.
 *
 * <p>Injectable parameters: {@link Page}, {@link BrowserContext}, {@link TwakeCalendarStack} and
 * {@link CalendarProbe}.
 *
 * <p>Knobs, all through environment variables:
 * <ul>
 *   <li>{@code E2E_HEADLESS=false} to watch the browser drive the app</li>
 *   <li>{@code E2E_SLOWMO=<ms>} to slow every action down</li>
 *   <li>{@code E2E_TRACE=always} to always record a trace, not only on failure</li>
 * </ul>
 * Failures always leave a screenshot, the page HTML and a Playwright trace under
 * {@code target/e2e-artifacts/}. Open a trace with
 * {@code npx playwright show-trace target/e2e-artifacts/<test>/trace.zip}.
 */
public class TwakeCalendarE2EExtension implements BeforeEachCallback, AfterTestExecutionCallback,
    AfterEachCallback, ParameterResolver {

    private static final Path ARTIFACTS = Paths.get("target", "e2e-artifacts");
    private static final double DEFAULT_TIMEOUT_MS = 20_000;

    private static volatile Playwright playwright;
    private static volatile Browser browser;

    private BrowserContext context;
    private Page page;
    private E2EUser user;
    private E2ESessions sessions;

    private static volatile CalendarProbe probe;
    private static volatile E2EUserFactory userFactory;

    private static synchronized Browser browser(TwakeCalendarStack stack) {
        if (browser == null) {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(!"false".equals(System.getenv("E2E_HEADLESS")))
                .setSlowMo(slowMo())
                .setArgs(chromiumArgs(stack)));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                browser.close();
                playwright.close();
            }));
        }
        return browser;
    }

    /** Every session of the suite is opened the same way, primary or secondary. */
    static Browser.NewContextOptions contextOptions() {
        return new Browser.NewContextOptions()
            .setBaseURL(TwakeCalendarStack.FRONTEND_URL)
            // Dex serves a self signed certificate, see docker/Dockerfile.dex
            .setIgnoreHTTPSErrors(true)
            .setViewportSize(1500, 950)
            .setLocale("en-US")
            .setTimezoneId("Europe/Paris");
    }

    private static double slowMo() {
        String slowMo = System.getenv("E2E_SLOWMO");
        return slowMo == null ? 0 : Double.parseDouble(slowMo);
    }

    private static List<String> chromiumArgs(TwakeCalendarStack stack) {
        List<String> args = new ArrayList<>();
        // Resolve the docker hostnames the SPA is configured with
        args.add("--host-resolver-rules=" + stack.hostResolverRules());
        args.add("--disable-dev-shm-usage");
        args.add("--no-sandbox");
        return args;
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        Browser browser = browser(TwakeCalendarStack.singleton());
        context = browser.newContext(contextOptions());
        context.setDefaultTimeout(DEFAULT_TIMEOUT_MS);
        sessions = new E2ESessions(browser);
        context.tracing().start(new Tracing.StartOptions()
            .setScreenshots(true)
            .setSnapshots(true)
            .setSources(true));

        page = context.newPage();
        // Browser side errors are the usual suspects behind a flaky looking e2e failure:
        // surface them in the maven output rather than making people re-run with a debugger.
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                System.out.println("[browser console error] " + message.text());
            }
        });
        page.onPageError(error -> System.out.println("[browser page error] " + error));
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) {
        boolean failed = extensionContext.getExecutionException().isPresent();
        Path directory = ARTIFACTS.resolve(testId(extensionContext));

        if (failed) {
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(directory.resolve("failure.png"))
                .setFullPage(true));
            System.out.println("[e2e] failure artifacts in " + directory.toAbsolutePath());
        }

        if (failed || "always".equals(System.getenv("E2E_TRACE"))) {
            context.tracing().stop(new Tracing.StopOptions().setPath(directory.resolve("trace.zip")));
        } else {
            context.tracing().stop();
        }
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        if (sessions != null) {
            sessions.closeAll();
        }
        if (context != null) {
            context.close();
        }
        user = null;
    }

    /**
     * The account of the running test, created on first use: one dedicated user per test,
     * because the stack is shared and the user is what isolates one test from the next.
     */
    private E2EUser user() {
        if (user == null) {
            user = userFactory().newUser();
        }
        return user;
    }

    private static synchronized E2EUserFactory userFactory() {
        if (userFactory == null) {
            userFactory = new E2EUserFactory(TwakeCalendarStack.singleton());
        }
        return userFactory;
    }

    private static synchronized CalendarProbe probe() {
        if (probe == null) {
            probe = new CalendarProbe(TwakeCalendarStack.singleton());
        }
        return probe;
    }

    private String testId(ExtensionContext extensionContext) {
        return extensionContext.getRequiredTestClass().getSimpleName()
            + "/" + extensionContext.getRequiredTestMethod().getName();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type == Page.class
            || type == BrowserContext.class
            || type == TwakeCalendarStack.class
            || type == CalendarProbe.class
            || type == E2EUser.class
            || type == E2EUserFactory.class
            || type == E2ESessions.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        if (type == Page.class) {
            return page;
        }
        if (type == BrowserContext.class) {
            return context;
        }
        if (type == TwakeCalendarStack.class) {
            return TwakeCalendarStack.singleton();
        }
        if (type == E2EUser.class) {
            return user();
        }
        if (type == E2EUserFactory.class) {
            return userFactory();
        }
        if (type == E2ESessions.class) {
            return sessions;
        }
        return probe();
    }
}
