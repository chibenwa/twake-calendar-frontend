package com.linagora.calendar.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.linagora.calendar.e2e.TwakeCalendarE2ETest;
import com.linagora.calendar.e2e.backend.CalendarProbe;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.EventFormModal;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Page;

class EventCreationTest extends TwakeCalendarE2ETest {

    private static String uniqueTitle(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("An event created from the form shows up in the grid")
    void createdEventShowsUpInTheGrid(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = uniqueTitle("Team sync");

        calendar.createEvent().title(title).save();

        com.microsoft.playwright.assertions.PlaywrightAssertions
            .assertThat(calendar.eventCard(title).first()).isAttached();
        assertThat(calendar.eventTitles()).contains(title);
    }

    @Test
    @DisplayName("An event created from the form reaches CalDAV")
    void createdEventIsPersistedInCalDav(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = uniqueTitle("Retrospective");

        calendar.createEvent(title);

        assertThat(probe.eventSummaries(user)).containsExactly(title);
    }

    @Test
    @DisplayName("Description and location typed in the expanded form are persisted")
    void descriptionAndLocationArePersisted(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = uniqueTitle("Onsite meeting");

        calendar.createEvent()
            .title(title)
            .expand()
            .description("Bring the roadmap")
            .location("Paris, rue de Rivoli")
            .save();
        calendar.eventCard(title).first().waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
            .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));

        assertThat(probe.rawEvents(user))
            .singleElement()
            .satisfies(ical -> assertThat(ical)
                .contains("SUMMARY:" + title)
                .contains("Bring the roadmap")
                .contains("Paris"));
    }

    @Test
    @DisplayName("A created event is still there after a reload")
    void eventSurvivesAReload(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = uniqueTitle("Persisted");

        calendar.createEvent(title);
        page.reload();
        new CalendarPage(page).waitUntilLoaded();

        com.microsoft.playwright.assertions.PlaywrightAssertions
            .assertThat(calendar.eventCard(title).first()).isAttached();
    }

    @Test
    @DisplayName("An end time before the start time is refused")
    void endTimeBeforeStartTimeBlocksTheSave(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = uniqueTitle("Backwards");

        EventFormModal form = calendar.createEvent()
            .title(title)
            .expand()
            .startTime("14:00")
            .endTime("09:00");
        form.trySave();

        com.microsoft.playwright.assertions.PlaywrightAssertions
            .assertThat(page.getByText("End time must be after start time").first()).isVisible();
        assertThat(probe.eventSummaries(user)).isEmpty();
    }

    @Test
    @DisplayName("An all day event lands in the all day row rather than in a time slot")
    void allDayEventIsRenderedInTheAllDayRow(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = uniqueTitle("Company offsite");

        calendar.createEvent()
            .title(title)
            .expand()
            .allDay()
            .save();
        calendar.eventCard(title).first().waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
            .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));

        assertThat(calendar.allDayEventCards().allInnerTexts())
            .anySatisfy(text -> assertThat(text).contains(title));
        assertThat(probe.rawEvents(user))
            .singleElement()
            .satisfies(ical -> assertThat(ical).contains("DTSTART;VALUE=DATE"));
    }
}
