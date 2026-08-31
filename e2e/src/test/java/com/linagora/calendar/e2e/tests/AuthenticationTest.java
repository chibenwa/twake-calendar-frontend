package com.linagora.calendar.e2e.tests;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.linagora.calendar.e2e.TwakeCalendarE2ETest;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Page;

class AuthenticationTest extends TwakeCalendarE2ETest {

    @Test
    @DisplayName("An unauthenticated visitor is sent to the SSO and lands on their calendar")
    void loginLandsOnTheCalendar(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);

        assertThat(page).hasURL(java.util.regex.Pattern.compile(".*/calendar.*"));
        assertThat(page.getByLabel("Create a new event")).isVisible();
        org.assertj.core.api.Assertions.assertThat(calendar.loggedInUser()).contains(user.email());
    }

    @Test
    @DisplayName("Reloading the page keeps the session, no second trip to the SSO")
    void reloadKeepsTheSession(Page page, E2EUser user) {
        LoginPage.loginAs(page, user);

        page.reload();
        new CalendarPage(page).waitUntilLoaded();

        assertThat(page).hasURL(java.util.regex.Pattern.compile(".*/calendar.*"));
        assertThat(page.getByLabel("Create a new event")).isVisible();
    }

    @Test
    @DisplayName("Logging out hands the session over to the SSO end session endpoint")
    void logoutHandsOverToTheSso(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);

        calendar.logout();

        // The SPA leaves for the provider's end session endpoint, carrying the post logout
        // redirect it was configured with. Whether the provider then drops the session is its
        // own business -- Dex, for one, wants an id_token_hint the SPA does not send today.
        page.waitForURL(java.util.regex.Pattern.compile("https://sso:5554/.*"));
        org.assertj.core.api.Assertions.assertThat(page.url())
            .contains("client_id=twake-calendar")
            .contains("post_logout_redirect_uri");
    }
}
