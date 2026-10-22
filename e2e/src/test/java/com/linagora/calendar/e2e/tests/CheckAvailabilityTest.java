package com.linagora.calendar.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.linagora.calendar.e2e.TwakeCalendarE2ETest;
import com.linagora.calendar.e2e.backend.CalendarProbe;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.backend.E2EUserFactory;
import com.linagora.calendar.e2e.backend.Ical;
import com.linagora.calendar.e2e.backend.ResourceProbe;
import com.linagora.calendar.e2e.docker.E2EClock;
import com.linagora.calendar.e2e.docker.E2ESessions;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/**
 * The sidebar "Check availability" field: it overlays the public calendar of a colleague, or
 * the bookings of a resource, on the grid, for as long as their chip stays.
 */
class CheckAvailabilityTest extends TwakeCalendarE2ETest {

    private static String unique(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    /** A colleague with an event today the user is not invited to. */
    private String seedEventOf(CalendarProbe probe, E2EUser colleague, String prefix) {
        String title = unique(prefix);
        String uid = UUID.randomUUID().toString();
        probe.putEvent(colleague, uid, Ical.event(uid, title, E2EClock.today(), 9));
        return title;
    }

    @Test
    @DisplayName("FB-13 The \"Check availability\" field searches both users and resources")
    void theFieldSearchesUsersAndResources(Page page, E2EUser user, E2EUserFactory users,
                                           E2ESessions sessions, ResourceProbe resources) {
        E2EUser colleague = users.newUser("colleague");
        sessions.openFor(colleague);
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String room = unique("Huddle room");
        resources.create(room, "Four chairs", user);

        assertThat(calendar.searchAvailability(colleague.uid()))
            .anySatisfy(option -> assertThat(option).contains(colleague.email()));
        assertThat(calendar.searchAvailability(room))
            .anySatisfy(option -> assertThat(option).contains(room));
    }

    @Test
    @DisplayName("FB-15 Check availability overlays the public events of a colleague")
    void checkAvailabilityOverlaysAColleague(Page page, E2EUser user, E2EUserFactory users,
                                             E2ESessions sessions, CalendarProbe probe) {
        E2EUser colleague = users.newUser("colleague");
        sessions.openFor(colleague);
        String title = seedEventOf(probe, colleague, "Busy");
        probe.setPublicRight(colleague, CalendarProbe.PublicRight.READ);

        CalendarPage calendar = LoginPage.loginAs(page, user);
        PlaywrightAssertions.assertThat(calendar.eventCard(title)).hasCount(0);
        calendar.checkAvailabilityOf(colleague.uid(), colleague.email());

        PlaywrightAssertions.assertThat(calendar.availabilityChip(colleague.uid()).first()).isVisible();
        PlaywrightAssertions.assertThat(calendar.eventCard(title).first()).isAttached();
        assertThat(probe.eventSummaries(user))
            .as("an overlay, not a copy into the user's calendar").doesNotContain(title);
    }

    @Test
    @DisplayName("FB-16 Removing the chip of Check availability takes the overlay away")
    void removingTheChipTakesTheOverlayAway(Page page, E2EUser user, E2EUserFactory users,
                                            E2ESessions sessions, CalendarProbe probe) {
        E2EUser colleague = users.newUser("colleague");
        sessions.openFor(colleague);
        String title = seedEventOf(probe, colleague, "Busy");
        probe.setPublicRight(colleague, CalendarProbe.PublicRight.READ);
        CalendarPage calendar = LoginPage.loginAs(page, user);
        calendar.checkAvailabilityOf(colleague.uid(), colleague.email());
        PlaywrightAssertions.assertThat(calendar.eventCard(title).first()).isAttached();

        calendar.removeAvailabilityOf(colleague.uid());

        PlaywrightAssertions.assertThat(calendar.eventCard(title)).hasCount(0);
        calendar.reload();
        PlaywrightAssertions.assertThat(calendar.eventCard(title)).hasCount(0);
        PlaywrightAssertions.assertThat(calendar.availabilityChip(colleague.uid())).hasCount(0);
    }

    @Test
    @DisplayName("FB-17 Check availability shows nothing of a colleague whose calendar is private")
    void aPrivateCalendarIsNotOverlaid(Page page, E2EUser user, E2EUserFactory users,
                                       E2ESessions sessions, CalendarProbe probe) {
        E2EUser colleague = users.newUser("colleague");
        sessions.openFor(colleague);
        String title = seedEventOf(probe, colleague, "Private");
        probe.setPublicRight(colleague, CalendarProbe.PublicRight.NONE);

        CalendarPage calendar = LoginPage.loginAs(page, user);
        calendar.checkAvailabilityOf(colleague.uid(), colleague.email());

        PlaywrightAssertions.assertThat(page.getByText("does not have public calendars").first())
            .isVisible();
        PlaywrightAssertions.assertThat(calendar.eventCard(title)).hasCount(0);
    }
}
