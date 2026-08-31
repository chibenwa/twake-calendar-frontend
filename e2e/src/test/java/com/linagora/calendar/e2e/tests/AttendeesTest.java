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
import com.linagora.calendar.e2e.backend.E2EUserFactory;
import com.linagora.calendar.e2e.docker.E2ESessions;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

class AttendeesTest extends TwakeCalendarE2ETest {

    @Test
    @DisplayName("ATT-07 A guest added in the form is written as an ATTENDEE")
    void guestIsWrittenAsAttendee(Page page, E2EUser organizer, E2EUserFactory users, CalendarProbe probe) {
        E2EUser guest = users.newUser();
        CalendarPage calendar = LoginPage.loginAs(page, organizer);
        String title = "Kickoff " + UUID.randomUUID().toString().substring(0, 8);

        calendar.createEvent().title(title).addGuest(guest.email()).save();
        calendar.eventCard(title).first().waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
            .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));

        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
            assertThat(probe.rawEvents(organizer))
                .singleElement()
                .satisfies(ical -> assertThat(ical).contains(guest.email())));
    }

    @Test
    @DisplayName("ATT-09 An invited user sees the event in their own calendar")
    void invitedUserSeesTheEventInTheirCalendar(Page page, E2EUser organizer,
                                                E2EUserFactory users, E2ESessions sessions) {
        E2EUser guest = users.newUser();
        // The invitee has to exist in OpenPaaS before being invited, and the side service
        // provisions accounts lazily, on their first authenticated call.
        CalendarPage guestCalendar = sessions.openFor(guest);

        String title = "Design review " + UUID.randomUUID().toString().substring(0, 8);
        LoginPage.loginAs(page, organizer)
            .createEvent().title(title).addGuest(guest.email()).save();

        PlaywrightAssertions.assertThat(guestCalendar.eventCard(title).first())
            .isAttached(new LocatorAssertions.IsAttachedOptions().setTimeout(45_000));
    }
}
