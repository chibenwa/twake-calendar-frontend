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

    void recordConsoleError(String message) {
        consoleErrors.add(message);
    }

    void recordPageError(String message) {
        pageErrors.add(message);
    }

    /** Everything the page logged at error level. */
    public List<String> consoleErrors() {
        return List.copyOf(consoleErrors);
    }

    /** Uncaught exceptions, the ones that leave the interface in an unknown state. */
    public List<String> pageErrors() {
        return List.copyOf(pageErrors);
    }

    public void clear() {
        consoleErrors.clear();
        pageErrors.clear();
    }
}
