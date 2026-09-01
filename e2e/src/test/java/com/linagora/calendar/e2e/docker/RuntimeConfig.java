package com.linagora.calendar.e2e.docker;

import java.util.LinkedHashMap;
import java.util.Map;

import com.microsoft.playwright.BrowserContext;

/**
 * Overrides the runtime configuration of the SPA for one test.
 *
 * <p>The application reads `.env.js` at load time. Rather than baking a variant image per
 * feature flag, the browser is told to serve a doctored copy: the original is fetched and the
 * overrides appended, and since the file is a plain sequence of `var` declarations, the last
 * one wins.
 *
 * <p>Set the overrides <b>before logging in</b>, the file is only read once:
 *
 * <pre>{@code
 * void bannerCanBeTurnedOff(Page page, E2EUser user, RuntimeConfig config) {
 *     config.set("ASK_FOR_TZ_UPDATE", "false");
 *     LoginPage.loginAs(page, user);
 * }
 * }</pre>
 */
public class RuntimeConfig {
    private final BrowserContext context;
    private final Map<String, String> overrides = new LinkedHashMap<>();
    private boolean installed;

    RuntimeConfig(BrowserContext context) {
        this.context = context;
    }

    /** The value is raw JavaScript: {@code "false"}, {@code "'fr'"}, {@code "42"}. */
    public RuntimeConfig set(String name, String javascriptValue) {
        overrides.put(name, javascriptValue);
        install();
        return this;
    }

    /**
     * The configuration the frontend image ships, read from the very file that was baked into
     * it. Fetching it back through the browser is not an option: the hostname only resolves
     * inside Chromium, through its resolver rules.
     */
    private static String readShippedConfig() {
        try (var stream = RuntimeConfig.class.getResourceAsStream("/frontend/env.js")) {
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Cannot read the shipped .env.js from the classpath", e);
        }
    }

    private void install() {
        if (installed) {
            return;
        }
        installed = true;
        String original = readShippedConfig();
        context.route("**/.env.js", route -> {
            StringBuilder patched = new StringBuilder(original).append('\n');
            overrides.forEach((name, value) ->
                patched.append("var ").append(name).append(" = ").append(value).append(";\n"));
            route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                .setStatus(200)
                .setContentType("application/javascript")
                .setBody(patched.toString()));
        });
    }
}
