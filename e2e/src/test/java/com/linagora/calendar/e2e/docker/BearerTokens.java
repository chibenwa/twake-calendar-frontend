package com.linagora.calendar.e2e.docker;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

/**
 * The access token a session sends to the backend.
 *
 * <p>The application keeps its tokens in memory, out of reach of the page's scripts, so the
 * tests that call the backend on the user's behalf read the token off the requests the
 * application sends instead.
 */
public final class BearerTokens {
    private static final String BEARER = "Bearer ";
    private static final Map<BrowserContext, String> LAST_SEEN =
        Collections.synchronizedMap(new WeakHashMap<>());

    private BearerTokens() {
    }

    /** Records the token of every request the given session sends. */
    public static void track(BrowserContext context) {
        context.onRequest(request -> {
            String authorization = request.headers().get("authorization");
            if (authorization != null && authorization.startsWith(BEARER)) {
                LAST_SEEN.put(context, authorization.substring(BEARER.length()));
            }
        });
    }

    /** The token the application of this page last sent, empty when it sent none yet. */
    public static String of(Page page) {
        return LAST_SEEN.getOrDefault(page.context(), "");
    }
}
