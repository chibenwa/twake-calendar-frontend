package com.linagora.calendar.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.linagora.calendar.e2e.TwakeCalendarE2ETest;
import com.linagora.calendar.e2e.backend.CalendarProbe;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.backend.Ical;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/**
 * What the SPA does with changes it did not initiate: the websocket live updates and the
 * manual refresh. Both are frequent regression spots and invisible to unit tests.
 */
class BackendSyncTest extends TwakeCalendarE2ETest {

    @Test
    @DisplayName("An event written on CalDAV pops up in the grid without a reload")
    void eventCreatedOnCalDavShowsUpLive(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = "Pushed live " + UUID.randomUUID().toString().substring(0, 8);

        probe.putEvent(user, UUID.randomUUID().toString(), Ical.event(
            UUID.randomUUID().toString(), title, LocalDate.now(), 9));

        PlaywrightAssertions.assertThat(calendar.eventCard(title).first())
            .isAttached(new LocatorAssertions.IsAttachedOptions().setTimeout(30_000));
    }

    @Test
    @DisplayName("An event deleted on CalDAV is gone after a refresh")
    void eventDeletedOnCalDavDisappearsAfterRefresh(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = "Deleted behind your back " + UUID.randomUUID().toString().substring(0, 8);
        String uid = UUID.randomUUID().toString();
        probe.putEvent(user, uid, Ical.event(uid, title, LocalDate.now(), 10));
        calendar.eventCard(title).first().waitFor();

        probe.deleteEvent(user, uid);
        calendar.refresh();

        PlaywrightAssertions.assertThat(calendar.eventCard(title))
            .hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(30_000));
    }

    @Test
    @DisplayName("Browsing to another week loads the events of that week")
    void eventsOfAnotherWeekAreLoadedWhenNavigating(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = "Next week " + UUID.randomUUID().toString().substring(0, 8);
        String uid = UUID.randomUUID().toString();
        probe.putEvent(user, uid, Ical.event(uid, title, LocalDate.now().plusWeeks(1), 9));
        // Reload so that the SPA starts from a clean slate and really has to fetch the range
        page.reload();
        calendar.waitUntilLoaded();

        assertThat(calendar.eventTitles()).doesNotContain(title);
        calendar.next();

        PlaywrightAssertions.assertThat(calendar.eventCard(title).first())
            .isAttached(new LocatorAssertions.IsAttachedOptions().setTimeout(30_000));
    }
}
