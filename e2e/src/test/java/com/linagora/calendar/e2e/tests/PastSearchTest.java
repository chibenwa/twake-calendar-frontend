package com.linagora.calendar.e2e.tests;

import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.linagora.calendar.e2e.TwakeCalendarE2ETest;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/** Search defects this project has already shipped. */
class PastSearchTest extends TwakeCalendarE2ETest {

    @Test
    @Disabled("Not implemented: the search entry point is not reachable from a page that already "
        + "holds events -- getByLabel(\"Search for events or calendars\") times out after the "
        + "events are created, though it resolves on an empty calendar. The search surface needs "
        + "a proper exploration pass. See the Blocked section of e2e.md.")
    @DisplayName("PAST-38 (#998) Searching again sends the new keyword, not the previous one")
    void searchingAgainUsesTheNewKeyword(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String first = "alpha" + UUID.randomUUID().toString().substring(0, 6);
        String second = "beta" + UUID.randomUUID().toString().substring(0, 6);
        calendar.createEvent(first);
        calendar.createEvent(second);

        calendar.search(first);
        PlaywrightAssertions.assertThat(page.getByText(first).first())
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30_000));

        calendar.search(second);

        PlaywrightAssertions.assertThat(page.getByText(second).first())
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30_000));
        // the payload used to carry the first keyword for ever, so the old result stayed
        PlaywrightAssertions.assertThat(page.getByText(first))
            .hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(20_000));
    }
}
