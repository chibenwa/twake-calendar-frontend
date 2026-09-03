package com.linagora.calendar.e2e.docker;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * What the browser complained about during a test.
 *
 * <p>A console error or an uncaught exception is a regression in its own right, even when the
 * screen still looks fine, so tests can assert on them rather than only on what is rendered.
 */
public class BrowserLog {
    private final List<String> consoleErrors = new CopyOnWriteArrayList<>();
    private final List<String> pageErrors = new CopyOnWriteArrayList<>();
    private final List<String> failedRequests = new CopyOnWriteArrayList<>();

    void recordConsoleError(String message) {
        consoleErrors.add(message);
    }

    void recordPageError(String message) {
        pageErrors.add(message);
    }

    /**
     * A request the server refused. The browser logs "Failed to load resource" without saying
     * which one, which makes a console error assertion useless on its own: this keeps the
     * address and the status beside it.
     */
    void recordFailedRequest(int status, String method, String url) {
        failedRequests.add(status + " " + method + " " + url);
    }

    /** Everything the page logged at error level. */
    public List<String> consoleErrors() {
        return List.copyOf(consoleErrors);
    }

    /** Uncaught exceptions, the ones that leave the interface in an unknown state. */
    public List<String> pageErrors() {
        return List.copyOf(pageErrors);
    }

    /** Every request answered with an error status, address included. */
    public List<String> failedRequests() {
        return List.copyOf(failedRequests);
    }

    /** The console errors, each with whatever failed request it most likely refers to. */
    public String explain() {
        return "console errors: " + consoleErrors
            + System.lineSeparator() + "failed requests: " + failedRequests;
    }

    public void clear() {
        consoleErrors.clear();
        pageErrors.clear();
        failedRequests.clear();
    }
}
