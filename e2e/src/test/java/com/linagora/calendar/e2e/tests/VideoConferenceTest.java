package com.linagora.calendar.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.linagora.calendar.e2e.pages.RecurrenceSection;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.WaitForSelectorState;

/** The video conference link: how it is made, where it is stored, and who can reach it. */
class VideoConferenceTest extends TwakeCalendarE2ETest {

    /** Whatever the deployment configured as the meeting host, see frontend/env.js. */
    private static final String MEETING_HOST = "meet.e2e.local";

    private static String title(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private void awaitAttached(Locator locator) {
        locator.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
    }

    /** The meeting URL stored on the event, wherever the SPA chose to put it. */
    private static String meetingLink(String icalendar) {
        Matcher matcher = Pattern.compile("https?://" + Pattern.quote(MEETING_HOST) + "\\S*")
            .matcher(Ics.unfold(icalendar));
        if (!matcher.find()) {
            throw new AssertionError("No meeting link in:\n" + icalendar);
        }
        return matcher.group().trim();
    }

    @Test
    @DisplayName("VISIO-01 Adding a video conference turns the offer into a joinable meeting")
    void addingAVideoConferenceTurnsIntoAMeeting(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = title("Meeting");

        var form = calendar.createEvent().title(title);
        assertThat(form.text()).contains("Add Visio conference");
        form.addVideoConference();
        page.waitForTimeout(1500);

        // the room is minted on save, so what changes in the form is the offer itself
        assertThat(form.text())
            .as("the button must stop offering what has already been added")
            .doesNotContain("Add Visio conference");
        form.save();
        awaitAttached(calendar.eventCard(title));
        assertThat(probe.singleEvent(user)).contains(MEETING_HOST);
    }

    @Test
    @DisplayName("VISIO-02 The link is built on the configured meeting host")
    void theLinkUsesTheConfiguredHost(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = title("Configured host");

        calendar.createEvent().title(title).addVideoConference().save();
        awaitAttached(calendar.eventCard(title));

        String link = meetingLink(probe.singleEvent(user));
        assertThat(link).startsWith("http://" + MEETING_HOST + "/");
        assertThat(link.substring(("http://" + MEETING_HOST + "/").length()))
            .as("a room identifier follows the host, and only one slash separates them")
            .isNotEmpty();
    }

    @Test
    @DisplayName("VISIO-03 The link is persisted in CalDAV")
    void theLinkIsPersisted(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = title("Stored");

        calendar.createEvent().title(title).addVideoConference().save();
        awaitAttached(calendar.eventCard(title));

        assertThat(probe.singleEvent(user)).contains(MEETING_HOST);
        page.reload();
        calendar.waitUntilLoaded();
        assertThat(calendar.openEvent(title).text())
            .as("and comes back after a reload")
            .containsIgnoringCase("join");
    }

    @Test
    @DisplayName("VISIO-04 Copying the meeting link says so")
    void copyingTheLinkSaysSo(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);

        String title = title("Copy me");
        calendar.createEvent().title(title).addVideoConference().save();
        awaitAttached(calendar.eventCard(title));
        // the link only exists once the event is saved, and so does the control that copies it
        calendar.openEvent(title).edit();
        page.getByLabel("Copy meeting link").last().click();

        PlaywrightAssertions.assertThat(page.getByText(
                Pattern.compile("Meeting link copied", Pattern.CASE_INSENSITIVE)).first())
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20_000));
    }

    @Test
    @DisplayName("VISIO-05 Removing the video conference takes the link off the event")
    void removingTheVideoConference(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = title("No longer online");

        var form = calendar.createEvent().title(title).addVideoConference();
        page.waitForTimeout(1500);
        page.getByLabel("Remove video conference").last().click();
        page.waitForTimeout(1000);
        assertThat(form.text()).doesNotContain(MEETING_HOST);
        form.save();
        awaitAttached(calendar.eventCard(title));

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
            assertThat(probe.singleEvent(user)).doesNotContain(MEETING_HOST));
    }

    @Test
    @DisplayName("VISIO-06 The preview offers to join the conference")
    void thePreviewOffersToJoin(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = title("Joinable");
        calendar.createEvent().title(title).addVideoConference().save();
        awaitAttached(calendar.eventCard(title));

        assertThat(calendar.openEvent(title).text()).containsIgnoringCase("join");
    }

    @Test
    @DisplayName("VISIO-07 Joining opens the meeting in another tab")
    void joiningOpensAnotherTab(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = title("Open elsewhere");
        calendar.createEvent().title(title).addVideoConference().save();
        awaitAttached(calendar.eventCard(title));
        calendar.openEvent(title);

        Page meeting = page.waitForPopup(() ->
            page.getByText(Pattern.compile("^Join", Pattern.CASE_INSENSITIVE)).last().click());

        assertThat(meeting.url())
            .as("the calendar must not be replaced by the meeting")
            .contains(MEETING_HOST);
        assertThat(page.url()).contains("/calendar");
    }

    @Test
    @DisplayName("VISIO-08 A guest sees the same meeting link")
    void aGuestSeesTheSameLink(Page page, E2EUser organizer, E2EUserFactory users,
                               E2ESessions sessions, CalendarProbe probe) {
        E2EUser guest = users.newUser();
        CalendarPage guestCalendar = sessions.openFor(guest);
        CalendarPage calendar = LoginPage.loginAs(page, organizer);
        String title = title("Everyone joins");

        calendar.createEvent().title(title).addGuest(guest.email()).addVideoConference().save();
        awaitAttached(calendar.eventCard(title));
        String organizerLink = meetingLink(probe.singleEvent(organizer));

        guestCalendar.page().reload();
        guestCalendar.waitUntilLoaded();
        PlaywrightAssertions.assertThat(guestCalendar.eventCard(title).first())
            .isAttached(new LocatorAssertions.IsAttachedOptions().setTimeout(60_000));

        Awaitility.await().atMost(Duration.ofSeconds(45)).untilAsserted(() ->
            assertThat(meetingLink(probe.singleEvent(guest)))
                .as("one meeting, one room, or the guests end up alone in theirs")
                .isEqualTo(organizerLink));
    }

    @Test
    @DisplayName("VISIO-09 The generated section warns against editing it")
    void theGeneratedSectionWarnsAgainstEditing(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = title("Do not touch");

        calendar.createEvent().title(title).addVideoConference().save();
        awaitAttached(calendar.eventCard(title));

        assertThat(Ics.unfold(probe.singleEvent(user)))
            .as("whoever opens this in another client must be told not to edit the block")
            .containsIgnoringCase("do not edit");
    }

    @Test
    @DisplayName("VISIO-10 Editing the event leaves the link alone")
    void editingTheEventLeavesTheLinkAlone(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = title("Renamed with a link");
        calendar.createEvent().title(title).addVideoConference().save();
        awaitAttached(calendar.eventCard(title));
        String before = meetingLink(probe.singleEvent(user));

        String renamed = title("Renamed again");
        var form = calendar.openEvent(title).edit();
        form.title(renamed);
        form.save();

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            String ical = probe.singleEvent(user);
            assertThat(ical).contains("SUMMARY:" + renamed);
            assertThat(meetingLink(ical))
                .as("the room people already have in their diary must not change")
                .isEqualTo(before);
        });
    }

    @Test
    @DisplayName("VISIO-11 A recurring event shares one room across its occurrences")
    void aRecurringEventSharesOneRoom(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = title("Weekly call");

        var form = calendar.createEvent().title(title).addVideoConference()
            .expand().startTime("09:00").endTime("10:00");
        form.repeat().frequency(RecurrenceSection.DAILY).endsAfter(3);
        form.save();
        awaitAttached(calendar.eventCard(title));

        String ical = probe.singleEvent(user);
        assertThat(Ics.property(Ics.master(ical), "RRULE")).isPresent();
        long rooms = Pattern.compile(Pattern.quote(MEETING_HOST) + "/([A-Za-z0-9._~-]+)")
            .matcher(Ics.unfold(ical)).results()
            .map(result -> result.group(1))
            .distinct()
            .count();
        assertThat(rooms).as("one series, one room").isEqualTo(1);
    }

    @Test
    @DisplayName("VISIO-12 The link survives a change of time")
    void theLinkSurvivesATimeChange(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = title("Rescheduled call");
        calendar.createEvent().title(title).addVideoConference()
            .expand().startTime("09:00").endTime("10:00").save();
        awaitAttached(calendar.eventCard(title));
        String before = meetingLink(probe.singleEvent(user));

        var form = calendar.openEvent(title).edit().expand();
        form.startTime("15:00").endTime("16:00");
        form.save();

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            String ical = probe.singleEvent(user);
            assertThat(Ics.property(Ics.event(ical), "DTSTART").orElseThrow()).contains("T150000");
            assertThat(meetingLink(ical)).isEqualTo(before);
        });
    }
}
