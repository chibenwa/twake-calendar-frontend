package com.linagora.calendar.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.linagora.calendar.e2e.TwakeCalendarE2ETest;
import com.linagora.calendar.e2e.backend.CalendarProbe;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.WaitForSelectorState;

class EventEditionTest extends TwakeCalendarE2ETest {

    private static String uniqueTitle(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("Renaming an event updates both the grid and CalDAV")
    void renamingAnEventUpdatesTheGridAndCalDav(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String initial = uniqueTitle("Draft name");
        String renamed = uniqueTitle("Final name");
        calendar.createEvent(initial);

        calendar.openEvent(initial).edit().title(renamed).save();

        PlaywrightAssertions.assertThat(calendar.eventCard(renamed).first()).isAttached();
        assertThat(calendar.eventCard(initial)).satisfies(card -> assertThat(card.count()).isZero());
        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
            assertThat(probe.eventSummaries(user)).containsExactly(renamed));
    }

    @Test
    @DisplayName("Changing the start time of an event is persisted")
    void changingTheStartTimeUpdatesTheEvent(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = uniqueTitle("Moved");
        calendar.createEvent(title);

        calendar.openEvent(title).edit()
            .expand()
            .startTime("09:00")
            .endTime("10:30")
            .save();
        calendar.eventCard(title).first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));

        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
            assertThat(calendar.eventCard(title).first().innerText()).contains("09:00"));
        assertThat(probe.rawEvents(user))
            .singleElement()
            .satisfies(ical -> assertThat(ical).contains("T090000"));
    }

    @Test
    @DisplayName("Deleting an event removes it from the grid and from CalDAV")
    void deletingAnEventRemovesItEverywhere(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = uniqueTitle("Cancelled");
        calendar.createEvent(title);

        calendar.openEvent(title).delete();

        PlaywrightAssertions.assertThat(calendar.eventCard(title)).hasCount(0);
        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
            assertThat(probe.eventSummaries(user)).isEmpty());
    }

    @Test
    @DisplayName("Cancelling the edit form leaves the event untouched")
    void cancellingTheFormLeavesTheEventUntouched(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = uniqueTitle("Keep me");
        calendar.createEvent(title);

        calendar.openEvent(title).edit().title("Should never be saved").cancel();
        page.waitForTimeout(1500);

        assertThat(probe.eventSummaries(user)).containsExactly(title);
    }
}
