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
import com.linagora.calendar.e2e.backend.Ical;
import com.linagora.calendar.e2e.backend.TeamCalendarProbe;
import com.linagora.calendar.e2e.backend.TeamCalendarProbe.Right;
import com.linagora.calendar.e2e.docker.E2EClock;
import com.linagora.calendar.e2e.docker.E2ESessions;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.EventFormModal;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;

/**
 * The team calendar as Twake Workplace frames it: /embed/calendars/<id>, the month of the team
 * as a list, without the bar and the sidebar of the application. Opened top-level, it signs in
 * silently with the session of its context, which is all these tests need.
 */
class EmbeddedCalendarTest extends TwakeCalendarE2ETest {
    private static final long PROPAGATION_MS = 60_000;
    private static final String UNAVAILABLE =
        "This calendar is not available. You may not be a member of it.";

    private static String unique(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    /** A team whose only member is the given user, who must have signed in once. */
    private String aTeamFor(TeamCalendarProbe teams, E2EUser member, String displayName) {
        String id = teams.create("team-" + UUID.randomUUID().toString().substring(0, 8), displayName);
        teams.grant(id, member, Right.READ_WRITE);
        return id;
    }

    /** Writes an event of today into the team calendar, and returns its UID. */
    private String seedTeamEvent(CalendarProbe probe, E2EUser writer, String teamId, String title) {
        String uid = UUID.randomUUID().toString();
        probe.putEventIn(writer, teamId, uid, Ical.event(uid, title, E2EClock.today(), 9));
        return uid;
    }

    /**
     * Opens the embed route in a session of its own, as the frame of TwakeSpace is: the route
     * changes the calendars its context selects, which #1669 had leak into the main app.
     */
    private Page openEmbed(E2ESessions sessions, E2EUser user, String path) {
        Page embed = sessions.pageFor(user);
        embed.navigate("/embed/calendars/" + path);
        return embed;
    }

    private Locator createButton(Page embed) {
        return embed.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Create").setExact(true));
    }

    @Test
    @DisplayName("EMBED-01 The embed route shows the team calendar alone, as a month list")
    void theEmbedRouteShowsTheTeamAlone(Page page, E2EUser user, E2ESessions sessions,
                                        TeamCalendarProbe teams, CalendarProbe probe) {
        LoginPage.loginAs(page, user);
        String id = aTeamFor(teams, user, unique("Embedded team"));
        String teamEvent = unique("Team stand-up");
        seedTeamEvent(probe, user, id, teamEvent);
        String personal = unique("Dentist");
        String uid = UUID.randomUUID().toString();
        probe.putEvent(user, uid, Ical.event(uid, personal, E2EClock.today(), 11));

        Page embed = openEmbed(sessions, user, id);

        PlaywrightAssertions.assertThat(embed.getByText(teamEvent).first())
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PROPAGATION_MS));
        assertThat(embed.url()).endsWith("/embed/calendars/" + id);
        // the events of the member's own calendars stay out of the team's page
        PlaywrightAssertions.assertThat(embed.getByText(personal)).hasCount(0);
        // neither the sidebar nor the menubar of the application
        PlaywrightAssertions.assertThat(embed.getByLabel("Create a new event")).hasCount(0);
        PlaywrightAssertions.assertThat(embed.getByText("My calendars")).hasCount(0);
        PlaywrightAssertions.assertThat(embed.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Today").setExact(true))).isVisible();
        PlaywrightAssertions.assertThat(createButton(embed)).isVisible();
    }

    @Test
    @DisplayName("EMBED-02 Create on the embed route writes into the team calendar")
    void createOnTheEmbedWritesIntoTheTeam(Page page, E2EUser user, E2ESessions sessions,
                                           TeamCalendarProbe teams, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String team = unique("Embedded team");
        String id = aTeamFor(teams, user, team);
        Page embed = openEmbed(sessions, user, id);

        createButton(embed).click();
        EventFormModal form = new EventFormModal(embed);
        PlaywrightAssertions.assertThat(embed.getByRole(AriaRole.DIALOG).last())
            .containsText("Create in " + team + "'s agenda");
        String title = unique("From Workplace");
        form.title(title).save();

        PlaywrightAssertions.assertThat(embed.getByText(title).first())
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PROPAGATION_MS));
        // filed under the team, not in the default calendar of the member
        assertThat(probe.eventSummaries(user)).doesNotContain(title);
        Awaitility.await().atMost(Duration.ofMillis(PROPAGATION_MS))
            .pollInterval(Duration.ofSeconds(2))
            .ignoreExceptions()
            .untilAsserted(() -> {
                calendar.reload();
                calendar.showCalendar(team);
                calendar.eventCard(title).first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(8_000));
            });
        assertThat(calendar.openEvent(title).text()).contains(team);
    }

    @Test
    @DisplayName("EMBED-03 Somebody who is not a member is refused the embedded team calendar")
    void aNonMemberIsRefused(Page page, E2EUser member, E2EUserFactory users, E2ESessions sessions,
                             TeamCalendarProbe teams, CalendarProbe probe) {
        LoginPage.loginAs(page, member);
        String id = aTeamFor(teams, member, unique("Closed team"));
        String secret = unique("Team only");
        seedTeamEvent(probe, member, id, secret);
        E2EUser stranger = users.newUser("stranger");

        Page embed = openEmbed(sessions, stranger, id);

        PlaywrightAssertions.assertThat(embed.locator("body")).containsText(UNAVAILABLE);
        PlaywrightAssertions.assertThat(createButton(embed)).hasCount(0);
        PlaywrightAssertions.assertThat(embed.getByText(secret)).hasCount(0);

        embed.navigate("/embed/calendars/" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        PlaywrightAssertions.assertThat(embed.locator("body")).containsText(UNAVAILABLE);
    }

    @Test
    @DisplayName("EMBED-04 The event route of the embed opens the preview of that event")
    void theEventRouteOpensItsPreview(Page page, E2EUser user, E2ESessions sessions,
                                      TeamCalendarProbe teams, CalendarProbe probe) {
        LoginPage.loginAs(page, user);
        String id = aTeamFor(teams, user, unique("Linked team"));
        String title = unique("Linked meeting");
        String uid = seedTeamEvent(probe, user, id, title);

        Page embed = openEmbed(sessions, user, id + "/events/" + uid);

        PlaywrightAssertions.assertThat(embed.getByRole(AriaRole.DIALOG).getByText(title).first())
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PROPAGATION_MS));
    }

    @Test
    @DisplayName("EMBED-05 Opening the embed route leaves the selection of the main app alone")
    void theEmbedRouteLeavesTheMainSelectionAlone(Page page, E2EUser user, TeamCalendarProbe teams,
                                                  CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String team = unique("Side team");
        String id = aTeamFor(teams, user, team);
        String teamEvent = unique("Team event");
        seedTeamEvent(probe, user, id, teamEvent);

        // a tab of the same browser, as the frame of TwakeSpace shares the storage of the app
        Page embed = page.context().newPage();
        embed.navigate("/embed/calendars/" + id);
        PlaywrightAssertions.assertThat(embed.getByText(teamEvent).first())
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PROPAGATION_MS));
        embed.close();

        calendar.reload();
        PlaywrightAssertions.assertThat(calendar.calendarCheckbox("My calendar")).isChecked();
        EventFormModal form = calendar.createEvent();
        assertThat(form.expand().text())
            .as("new events still go to the user's own calendar")
            .contains("My calendar")
            .doesNotContain(team);
        form.cancel();
    }
}
