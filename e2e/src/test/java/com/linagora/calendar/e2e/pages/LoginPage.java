package com.linagora.calendar.e2e.pages;

import com.linagora.calendar.e2e.backend.E2EUser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** The Dex login form the SPA redirects to when no session is present. */
public class LoginPage {
    private final Page page;

    public LoginPage(Page page) {
        this.page = page;
    }

    /** Goes through the whole OIDC dance and returns once the calendar is usable. */
    public static CalendarPage loginAs(Page page, E2EUser user) {
        page.navigate("/");
        return new LoginPage(page).submit(user);
    }

    public CalendarPage submit(E2EUser user) {
        waitUntilDisplayed();
        page.locator("input[name='login']").fill(user.email());
        page.locator("input[name='password']").fill(user.password());
        page.locator("button[type='submit']").click();
        return new CalendarPage(page).waitUntilLoaded();
    }

    public LoginPage waitUntilDisplayed() {
        page.waitForURL("**/auth**");
        loginField().waitFor();
        return this;
    }

    public Locator loginField() {
        return page.locator("input[name='login']");
    }
}
