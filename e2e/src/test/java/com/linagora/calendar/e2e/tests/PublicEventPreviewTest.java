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
import com.linagora.calendar.e2e.backend.Ics;
import com.linagora.calendar.e2e.backend.PublicEventLink;
import com.linagora.calendar.e2e.docker.E2ESessions;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.linagora.calendar.e2e.pages.PublicEventPreviewPage;
import com.linagora.calendar.e2e.pages.RecurrenceSection;
import com.microsoft.playwright.Page;

/**
 * The public preview of an invitation, played by somebody with no account.
 *
 * <p>In production the link arrives by mail. This stack sends none, so the suite signs its own
 * token with the key the side service verifies against -- see {@link PublicEventLink}. That
 * covers the page and the endpoint behind it, and deliberately not the mail that would carry it.
 */
class PublicEventPreviewTest extends TwakeCalendarE2ETest {
    /** An address outside the instance, the case the public pages exist for. */
    private static final String OUTSIDER = "outsider1@external.test";

    private static String unique(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    /** An event of the user with an outsider invited, and the outsider's link to it. */
    private String anInvitationFor(CalendarPage calendar, E2EUser user, CalendarProbe probe,
                                   PublicEventLink links, String title) {
        calendar.createEvent().title(title).addGuest(OUTSIDER)
            .expand().startTime("14:00").endTime("15:00").save();
        calendar.eventCard(title).first().waitFor();
        return links.previewUrl(links.tokenFor(probe.requireOpenPaasId(user), uidOf(probe, user),
            user.email(), OUTSIDER, PublicEventLink.Action.ACCEPTED));
    }

    /**
     * The whole ATTENDEE line of somebody, parameters included. Reading the property values
     * alone would drop the very thing under test here, which lives in a parameter.
     */
    private String attendeeLineOf(CalendarProbe probe, E2EUser user, String attendee) {
        return java.util.Arrays.stream(Ics.unfold(probe.singleEvent(user)).split("\r?\n"))
            .filter(line -> line.startsWith("ATTENDEE") && line.contains(attendee))
            .findFirst()
            .orElseThrow(() -> new AssertionError(attendee + " is not on the event any more"));
    }

    private String uidOf(CalendarProbe probe, E2EUser user) {
        return Ics.property(Ics.event(probe.singleEvent(user)), "UID").orElseThrow();
    }

    @Test
    @DisplayName("PUB-15 A valid link opens the event for somebody with no account")
    void aValidLinkOpensTheEvent(Page page, E2EUser user, CalendarProbe probe,
                                 E2ESessions sessions) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = unique("Invitation to preview");
        String url = anInvitationFor(calendar, user, probe, new PublicEventLink(), title);

        Page visitor = sessions.blankPage();
        PublicEventPreviewPage preview = PublicEventPreviewPage.open(visitor, url);

        assertThat(preview.text()).contains(title);
        assertThat(visitor.evaluate("() => Object.keys(sessionStorage).length"))
            .as("the public preview carries no session of anybody")
            .isEqualTo(0);
    }

    @Test
    @DisplayName("PUB-16 A link with no token says so")
    void aLinkWithNoTokenSaysSo(Page page, E2EUser user, E2ESessions sessions) {
        Page visitor = sessions.blankPage();

        PublicEventPreviewPage preview = PublicEventPreviewPage.open(visitor, "http://public/excal");

        assertThat(preview.text()).contains("Your link is invalid");
        assertThat(preview.offersToAnswer()).isFalse();
    }

    @Test
    @DisplayName("PUB-17 A token past its expiry is refused")
    void aTokenPastItsExpiryIsRefused(Page page, E2EUser user, CalendarProbe probe,
                                      E2ESessions sessions) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = unique("Stale invitation");
        calendar.createEvent().title(title).addGuest(OUTSIDER)
            .expand().startTime("14:00").endTime("15:00").save();
        calendar.eventCard(title).first().waitFor();

        PublicEventLink links = new PublicEventLink();
        String expired = links.previewUrl(links.expiredTokenFor(probe.requireOpenPaasId(user),
            uidOf(probe, user), user.email(), OUTSIDER, PublicEventLink.Action.ACCEPTED));

        Page visitor = sessions.blankPage();
        PublicEventPreviewPage preview = PublicEventPreviewPage.open(visitor, expired);

        assertThat(preview.text())
            .as("a link that has expired must say so rather than show the event")
            .containsIgnoringCase("expired");
        assertThat(preview.text()).doesNotContain(title);
    }

    @Test
    @DisplayName("PUB-18 A link to an event that is gone says the event is gone")
    void aLinkToAGoneEventSaysSo(Page page, E2EUser user, CalendarProbe probe,
                                 E2ESessions sessions) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = unique("Deleted before reading");
        String url = anInvitationFor(calendar, user, probe, new PublicEventLink(), title);
        probe.clearCalendar(user);

        Page visitor = sessions.blankPage();
        PublicEventPreviewPage preview = PublicEventPreviewPage.open(visitor, url);

        assertThat(preview.text()).doesNotContain(title);
        assertThat(preview.text())
            .as("the page has to account for an event that is no longer there")
            .containsIgnoringCase("not");
    }

    @Test
    @DisplayName("PUB-19 The invited outsider answers from the public page")
    void theInvitedOutsiderAnswers(Page page, E2EUser user, CalendarProbe probe,
                                   E2ESessions sessions) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = unique("Answerable invitation");
        String url = anInvitationFor(calendar, user, probe, new PublicEventLink(), title);

        Page visitor = sessions.blankPage();
        PublicEventPreviewPage preview = PublicEventPreviewPage.open(visitor, url);

        assertThat(preview.offersToAnswer())
            .as("an invitation nobody can answer is not an invitation")
            .isTrue();
        preview.answer("No");
        visitor.waitForTimeout(3000);
        assertThat(preview.text()).containsIgnoringCase("declin");
    }

    @Test
    @DisplayName("PUB-20 The answer given publicly reaches the calendar of the organizer")
    void theAnswerReachesTheOrganizer(Page page, E2EUser user, CalendarProbe probe,
                                      E2ESessions sessions) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = unique("Answer travelling back");
        String url = anInvitationFor(calendar, user, probe, new PublicEventLink(), title);

        Page visitor = sessions.blankPage();
        PublicEventPreviewPage.open(visitor, url).answer("No");

        Awaitility.await().atMost(Duration.ofSeconds(60)).untilAsserted(() ->
            assertThat(attendeeLineOf(probe, user, OUTSIDER))
                .as("what the outsider answered belongs in the organizer's copy")
                .contains("PARTSTAT=DECLINED"));
    }

    @Test
    @DisplayName("PUB-25 A link to one occurrence of a series shows that occurrence")
    void aLinkToOneOccurrenceShowsIt(Page page, E2EUser user, CalendarProbe probe,
                                     E2ESessions sessions) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = unique("Recurring invitation");
        var form = calendar.createEvent().title(title).addGuest(OUTSIDER)
            .expand().startTime("14:00").endTime("15:00");
        form.repeat().frequency(RecurrenceSection.DAILY).endsAfter(3);
        form.save();
        calendar.eventCard(title).first().waitFor();

        PublicEventLink links = new PublicEventLink();
        String url = links.previewUrl(links.tokenFor(probe.requireOpenPaasId(user),
            uidOf(probe, user), user.email(), OUTSIDER, PublicEventLink.Action.ACCEPTED));

        Page visitor = sessions.blankPage();
        PublicEventPreviewPage preview = PublicEventPreviewPage.open(visitor, url);

        assertThat(preview.text()).contains(title);
        assertThat(preview.text())
            .as("a series has to read as one on the public side too")
            .containsIgnoringCase("recurrent");
    }

    @Test
    @DisplayName("PUB-26 A token opens the event it names and nothing else")
    void aTokenOpensOnlyTheEventItNames(Page page, E2EUser user, CalendarProbe probe,
                                        E2ESessions sessions,
                                        com.linagora.calendar.e2e.backend.E2EUserFactory users) {
        E2EUser other = users.newUser("other");
        CalendarPage otherCalendar = sessions.openFor(other);
        String secret = unique("Somebody else's meeting");
        otherCalendar.createEvent(secret);

        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = unique("My own invitation");
        String url = anInvitationFor(calendar, user, probe, new PublicEventLink(), title);

        Page visitor = sessions.blankPage();
        PublicEventPreviewPage preview = PublicEventPreviewPage.open(visitor, url);

        assertThat(preview.text()).contains(title);
        assertThat(preview.text())
            .as("a token for one event is not a key to the instance")
            .doesNotContain(secret)
            .doesNotContain(other.email());
    }
}
