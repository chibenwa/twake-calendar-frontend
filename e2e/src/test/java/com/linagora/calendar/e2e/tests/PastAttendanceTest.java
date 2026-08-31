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
import com.linagora.calendar.e2e.backend.Ics;
import com.linagora.calendar.e2e.docker.E2ESessions;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

/** Guest and attendance defects this project has already shipped. */
class PastAttendanceTest extends TwakeCalendarE2ETest {

    private static String title(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private void awaitAttached(Locator locator) {
        locator.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
    }

    /** The ATTENDEE line of a given participant, folding undone. */
    private static String attendee(String ical, String email) {
        return Ics.properties(Ics.event(ical), "ATTENDEE").stream()
            .map(value -> value)
            .filter(value -> value.contains(email))
            .findFirst()
            .orElseGet(() -> java.util.Arrays.stream(Ics.unfold(ical).split("\r?\n"))
                .filter(line -> line.startsWith("ATTENDEE") && line.contains(email))
                .findFirst()
                .orElseThrow(() -> new AssertionError(email + " is not an attendee of:\n" + ical)));
    }

    private static String attendeeLine(String ical, String email) {
        return java.util.Arrays.stream(Ics.unfold(ical).split("\r?\n"))
            .filter(line -> line.startsWith("ATTENDEE") && line.contains(email))
            .findFirst()
            .orElseThrow(() -> new AssertionError(email + " is not an attendee of:\n" + ical));
    }

    @Test
    @DisplayName("PAST-27 (#307) Editing an event does not reset the guests' answers")
    void editingAnEventKeepsTheAnswers(Page page, E2EUser organizer, E2EUserFactory users,
                                       E2ESessions sessions, CalendarProbe probe) {
        E2EUser guest = users.newUser();
        CalendarPage guestCalendar = sessions.openFor(guest);
        CalendarPage calendar = LoginPage.loginAs(page, organizer);

        String title = title("Kickoff");
        calendar.createEvent().title(title).addGuest(guest.email()).save();
        awaitAttached(calendar.eventCard(title));

        guestCalendar.page().reload();
        guestCalendar.waitUntilLoaded();
        guestCalendar.openEvent(title).answer("Yes");
        Awaitility.await().atMost(Duration.ofSeconds(60)).untilAsserted(() ->
            assertThat(attendeeLine(probe.singleEvent(organizer), guest.email()))
                .contains("PARTSTAT=ACCEPTED"));

        page.reload();
        calendar.waitUntilLoaded();
        var edited = calendar.openEvent(title).edit();
        edited.title(title("Kickoff renamed"));
        edited.save();

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
            assertThat(attendeeLine(probe.singleEvent(organizer), guest.email()))
                .as("a title change is not a scheduling change, the answers must survive it")
                .contains("PARTSTAT=ACCEPTED"));
    }

    @Test
    @DisplayName("PAST-28 (#324) Changing the time keeps the organizer accepted")
    void changingTheTimeKeepsTheOrganizerAccepted(Page page, E2EUser organizer,
                                                  E2EUserFactory users, CalendarProbe probe) {
        E2EUser guest = users.newUser();
        CalendarPage calendar = LoginPage.loginAs(page, organizer);

        String title = title("Rescheduled");
        calendar.createEvent().title(title).addGuest(guest.email())
            .expand().startTime("09:00").endTime("10:00").save();
        awaitAttached(calendar.eventCard(title));

        var edited = calendar.openEvent(title).edit().expand();
        edited.startTime("14:00").endTime("15:00");
        edited.save();

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
            assertThat(attendeeLine(probe.singleEvent(organizer), organizer.email()))
                .as("the organizer is the one making the change, they are obviously available")
                .contains("PARTSTAT=ACCEPTED"));
    }

    @Test
    @DisplayName("PAST-29 (#500) The guest count of the preview matches the guest list")
    void theGuestCountMatchesTheGuestList(Page page, E2EUser organizer, E2EUserFactory users) {
        E2EUser first = users.newUser();
        E2EUser second = users.newUser();
        CalendarPage calendar = LoginPage.loginAs(page, organizer);

        String title = title("Three of us");
        calendar.createEvent().title(title)
            .addGuest(first.email())
            .addGuest(second.email())
            .save();
        awaitAttached(calendar.eventCard(title));

        String preview = calendar.openEvent(title).text();

        assertThat(preview)
            .as("organizer plus two guests: %s", preview)
            .contains("3 participants");
    }

    @Test
    @DisplayName("PAST-30 (#548) An address unknown to the directory is kept when the field loses focus")
    void anUnknownAddressIsKeptOnBlur(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String unknown = "unknown-" + UUID.randomUUID().toString().substring(0, 6) + "@other.com";

        var form = calendar.createEvent().title(title("External guest"));
        form.typeGuest(unknown);
        page.getByLabel("Title").first().click();
        page.waitForTimeout(1000);

        assertThat(form.text())
            .as("the user typed a valid address, losing focus must not throw it away")
            .contains(unknown);
    }

    @Test
    @DisplayName("PAST-31 (#319) Guests are required participants, the organizer alone chairs")
    void guestsAreRequiredParticipants(Page page, E2EUser organizer, E2EUserFactory users,
                                       CalendarProbe probe) {
        E2EUser guest = users.newUser();
        CalendarPage calendar = LoginPage.loginAs(page, organizer);

        String title = title("Roles");
        calendar.createEvent().title(title).addGuest(guest.email()).save();
        awaitAttached(calendar.eventCard(title));

        String ical = probe.singleEvent(organizer);
        assertThat(attendeeLine(ical, guest.email()))
            .as("a guest is not the chair of the meeting")
            .contains("ROLE=REQ-PARTICIPANT");
        assertThat(attendeeLine(ical, organizer.email())).contains("ROLE=CHAIR");
    }
}
