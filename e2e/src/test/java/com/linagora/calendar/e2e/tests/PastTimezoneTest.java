package com.linagora.calendar.e2e.tests;

import static com.linagora.calendar.e2e.pages.EventFormModal.Scope.ALL_EVENTS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.zone.ZoneOffsetTransition;
import java.util.Locale;
import java.util.UUID;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.linagora.calendar.e2e.TwakeCalendarE2ETest;
import com.linagora.calendar.e2e.backend.CalendarProbe;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.backend.E2EUserFactory;
import com.linagora.calendar.e2e.backend.Ical;
import com.linagora.calendar.e2e.backend.Ics;
import com.linagora.calendar.e2e.docker.E2EClock;
import com.linagora.calendar.e2e.docker.E2ESessions;
import com.linagora.calendar.e2e.pages.EventFormModal;
import com.linagora.calendar.e2e.pages.EventPreviewPopover;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.linagora.calendar.e2e.pages.RecurrenceSection;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.WaitForSelectorState;

/** Timezone defects this project has already shipped. */
class PastTimezoneTest extends TwakeCalendarE2ETest {

    private static String title(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private void awaitAttached(Locator locator) {
        locator.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
    }

    @Test
    @DisplayName("PAST-19 (#490) The update modal reopens an event in the timezone it was created with")
    void theUpdateModalKeepsTheOriginalTimezone(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = title("Tokyo meeting");

        calendar.createEvent().title(title).expand()
            .timezone("Asia/Tokyo")
            .startTime("11:00")
            .endTime("12:00")
            .save();
        awaitAttached(calendar.eventCard(title));

        var reopened = calendar.openEvent(title).edit().expand();

        assertThat(reopened.timezone())
            .as("the event was created in Tokyo, it must not silently come back in the browser timezone")
            .contains("Asia/Tokyo");
        assertThat(reopened.startTime()).isEqualTo("11:00");
    }

    @Test
    @DisplayName("PAST-20 (#1031) Answering twice never normalises DTSTART to UTC")
    void answeringTwiceKeepsTheTimezone(Page page, E2EUser organizer, E2EUserFactory users,
                                        E2ESessions sessions, CalendarProbe probe) {
        E2EUser guest = users.newUser();
        CalendarPage guestCalendar = sessions.openFor(guest);
        CalendarPage calendar = LoginPage.loginAs(page, organizer);

        String title = title("Timezone carrier");
        calendar.createEvent().title(title).addGuest(guest.email())
            .expand().startTime("09:00").endTime("10:00").save();
        awaitAttached(calendar.eventCard(title));

        guestCalendar.reload();
        awaitAttached(guestCalendar.eventCard(title));
        assertThat(Ics.parameters(Ics.event(probe.singleEvent(guest)), "DTSTART")).contains("TZID");

        guestCalendar.openEvent(title).answer("Yes");
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
            assertThat(probe.singleEvent(guest)).contains("PARTSTAT=ACCEPTED"));
        guestCalendar.reload();
        guestCalendar.openEvent(title).answer("Maybe");
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
            assertThat(probe.singleEvent(guest)).contains("PARTSTAT=TENTATIVE"));

        assertThat(Ics.parameters(Ics.event(probe.singleEvent(guest)), "DTSTART"))
            .as("updating a participation status must not rewrite DTSTART as a floating UTC time")
            .contains("TZID");
    }

    @Test
    @DisplayName("PAST-21 (#632) A user in Asia/Jakarta sees a Monday event on Monday")
    void anEventIsShownOnTheRightDayInAFarEasternTimezone(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        calendar.openSettings().selectTimezone("Asia/Jakarta").backToCalendar();

        // read off the grid, drawn in Jakarta: late on a Sunday in Paris it is already next week there
        LocalDate monday = calendar.firstVisibleDate();
        String title = title("Monday standup");
        calendar.createEvent().title(title).expand()
            .startDate(monday)
            .startTime("10:00")
            .endTime("11:00")
            .save();
        awaitAttached(calendar.eventCard(title));

        // the column, not merely the presence: the bug put the event one day early
        PlaywrightAssertions.assertThat(calendar.dayColumn(monday).locator(CalendarPage.EVENT_CARD)
                .filter(new Locator.FilterOptions().setHasText(title)).first())
            .isAttached(new LocatorAssertions.IsAttachedOptions().setTimeout(30_000));
    }

    @Test
    @DisplayName("PAST-22 (#896) An explicit timezone creates the event at that timezone's hour")
    void anExplicitTimezoneIsHonouredOnCreation(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = title("Jakarta call");

        // midweek: 05:00 in Jakarta is the evening before in Paris, off the grid on a Monday
        calendar.createEvent().title(title).expand()
            .startDate(calendar.firstVisibleDate().plusDays(2))
            .timezone("Asia/Jakarta")
            .startTime("05:00")
            .endTime("06:00")
            .save();
        awaitAttached(calendar.eventCard(title));

        String ical = probe.singleEvent(user);
        assertThat(Ics.parameters(Ics.event(ical), "DTSTART"))
            .as("the timezone picked in the form is the one the event must be stored in")
            .contains("Asia/Jakarta");
        assertThat(Ics.property(Ics.event(ical), "DTSTART").orElseThrow())
            .as("05:00 in Jakarta, not 05:00 converted from the browser timezone")
            .endsWith("T050000");
    }

    @Test
    @DisplayName("PAST-52 (#1455) An all day event on the day the clocks fall back ends on that same day")
    void anAllDayEventOnTheFallBackDayEndsThatDay(E2EUser user, CalendarProbe probe, E2ESessions sessions) {
        LocalDate day = nextFallBack("America/New_York");
        String title = title("Fall back");
        String uid = UUID.randomUUID().toString();
        probe.putEvent(user, uid, Ical.allDayEvent(uid, title, day, day));

        // the browser crosses the clock change, the display timezone sits on UTC: one hour back
        // in the browser is a whole day back once shown in London
        CalendarPage calendar = sessions.openFor(user, "America/New_York");
        calendar.openSettings().selectTimezone("Europe/London").backToCalendar();
        calendar.goToDate(day);
        awaitAttached(calendar.eventCardOn(title, day));

        EventPreviewPopover preview = calendar.openEventOn(title, day);
        assertThat(preview.text())
            .as("a single day off has no end date of its own, least of all the day before")
            .doesNotContain(day.minusDays(1).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)));

        EventFormModal form = preview.edit().expand();
        assertThat(form.endDate())
            .as("the inclusive end of a one day event is its start, not the day before")
            .isEqualTo(form.startDate());
    }

    @Test
    @DisplayName("PAST-54 (#1760) West of UTC, a weekly all day series created on a Monday repeats on Mondays from that Monday")
    void aWeeklyAllDaySeriesWestOfUtcKeepsItsWeekday(E2EUser user, CalendarProbe probe, E2ESessions sessions) {
        LocalDate monday = aMondayAhead();
        String title = title("NY weekly");

        // a click on the all day row starts the form on a bare date: read as midnight UTC, it is
        // still Sunday in New York
        CalendarPage calendar = sessions.openFor(user, "America/New_York");
        calendar.goToDate(monday);
        EventFormModal form = calendar.selectAllDayCell(monday).title(title).expand();
        RecurrenceSection repeat = form.repeat();
        assertThat(repeat.isWeekdaySelected("MO"))
            .as("ticking Repeat proposes the weekday of the event")
            .isTrue();
        assertThat(repeat.isWeekdaySelected("SU"))
            .as("not the day before it")
            .isFalse();
        repeat.endsAfter(3);
        form.save();

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            String master = Ics.master(probe.singleEvent(user));
            assertThat(Ics.property(master, "DTSTART"))
                .as("the series starts on the Monday picked, not on the next Sunday")
                .hasValue(monday.format(DateTimeFormatter.BASIC_ISO_DATE));
            assertThat(Ics.rulePart(Ics.property(master, "RRULE").orElseThrow(), "BYDAY")).hasValue("MO");
        });
        calendar.goToDate(monday);
        awaitAttached(calendar.eventCardOn(title, monday));
    }

    @Test
    @DisplayName("PAST-55 (#1760) West of UTC, switching an all day repetition to weekly proposes the weekday of the event")
    void switchingToWeeklyWestOfUtcProposesTheWeekdayOfTheEvent(E2EUser user, E2ESessions sessions) {
        LocalDate monday = aMondayAhead();

        CalendarPage calendar = sessions.openFor(user, "America/New_York");
        calendar.goToDate(monday);
        EventFormModal form = calendar.selectAllDayCell(monday).title(title("NY switch")).expand();
        RecurrenceSection repeat = form.repeat().frequency(RecurrenceSection.DAILY).frequency(RecurrenceSection.WEEKLY);

        assertThat(repeat.isWeekdaySelected("MO"))
            .as("the event is on a Monday, the browser being west of UTC changes nothing to that")
            .isTrue();
        assertThat(repeat.isWeekdaySelected("SU")).isFalse();
    }

    /** A Monday at least a week ahead, so that the series is created in the future. */
    private static LocalDate aMondayAhead() {
        return E2EClock.today().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);
    }

    /** The day a clock change brings an hour back, the next one from today. */
    private static LocalDate nextFallBack(String zone) {
        Instant from = E2EClock.today().atStartOfDay(ZoneOffset.UTC).toInstant();
        ZoneOffsetTransition transition = ZoneId.of(zone).getRules().nextTransition(from);
        while (!transition.isOverlap()) {
            transition = ZoneId.of(zone).getRules().nextTransition(transition.getInstant());
        }
        return transition.getDateTimeBefore().toLocalDate();
    }
}
