package com.linagora.calendar.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.linagora.calendar.e2e.TwakeCalendarE2ETest;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.backend.E2EUserFactory;
import com.linagora.calendar.e2e.backend.TeamCalendarProbe;
import com.linagora.calendar.e2e.backend.TeamCalendarProbe.Right;
import com.linagora.calendar.e2e.docker.E2ESessions;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.EventFormModal;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.linagora.calendar.e2e.pages.RecurrenceSection;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Team calendars: a calendar that belongs to a group rather than to a person.
 *
 * <p>Nothing in the product creates one, so the suite provisions it through the webadmin API and
 * then works entirely through the interface. Membership carries one of three DAV rights, which
 * the API names {@code dav:read}, {@code dav:read-write} and {@code dav:administration}.
 */
class TeamCalendarsTest extends TwakeCalendarE2ETest {
    private static final long PROPAGATION_MS = 60_000;

    private static String unique(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * A team whose only member is the given user, holding the given right.
     *
     * <p>The user has to have signed in once before: accounts are provisioned lazily, and the
     * API refuses a member it does not know with "Candidate member not found".
     */
    private String aTeamFor(TeamCalendarProbe teams, E2EUser member, Right right, String displayName) {
        String id = teams.create("team-" + UUID.randomUUID().toString().substring(0, 8), displayName);
        teams.grant(id, member, right);
        return id;
    }

    /** Reloads until the team shows up in the sidebar of that session. */
    private void awaitTeamVisible(Page page, String teamName) {
        Awaitility.await().atMost(Duration.ofMillis(PROPAGATION_MS))
            .pollInterval(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                page.reload();
                new CalendarPage(page).waitUntilLoaded();
                page.locator("li").filter(new Locator.FilterOptions().setHasText(teamName))
                    .first().waitFor(new Locator.WaitForOptions().setTimeout(8_000));
            });
    }

    private void showTeam(CalendarPage calendar, String teamName) {
        calendar.showCalendar(teamName);
    }

    @Test
    @DisplayName("TEAM-01 A team calendar shows in a section of its own")
    void aTeamCalendarShowsInItsOwnSection(Page page, E2EUser user, TeamCalendarProbe teams) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String name = unique("Design team");
        aTeamFor(teams, user, Right.READ, name);

        awaitTeamVisible(page, name);

        PlaywrightAssertions.assertThat(calendar.sidebarSection("Team calendars").first()).isVisible();
        assertThat(page.locator("li").filter(new Locator.FilterOptions().setHasText(name)).count())
            .isPositive();
    }

    @Test
    @DisplayName("TEAM-04 A member who may write creates an event in the team calendar")
    void aWritingMemberCreatesAnEvent(Page page, E2EUser user, TeamCalendarProbe teams) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String name = unique("Writers");
        aTeamFor(teams, user, Right.READ_WRITE, name);
        awaitTeamVisible(page, name);
        showTeam(calendar, name);

        String title = unique("Team meeting");
        calendar.createEvent().title(title).expand().calendar(name).save();

        PlaywrightAssertions.assertThat(calendar.eventCard(title).first())
            .isAttached(new LocatorAssertions.IsAttachedOptions().setTimeout(PROPAGATION_MS));
    }

    @Test
    @DisplayName("TEAM-02 Another member sees what the team put in the calendar")
    void anotherMemberSeesTheTeamEvents(Page page, E2EUser user, E2EUserFactory users,
                                        E2ESessions sessions, TeamCalendarProbe teams) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String name = unique("Shared team");
        String id = aTeamFor(teams, user, Right.READ_WRITE, name);
        E2EUser mate = users.newUser("mate");
        Page matePage = sessions.pageFor(mate);
        teams.grant(id, mate, Right.READ);
        awaitTeamVisible(page, name);
        showTeam(calendar, name);
        String title = unique("For the whole team");
        calendar.createEvent().title(title).expand().calendar(name).save();
        calendar.eventCard(title).first().waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.ATTACHED).setTimeout(PROPAGATION_MS));

        awaitTeamVisible(matePage, name);
        CalendarPage mateCalendar = new CalendarPage(matePage);
        showTeam(mateCalendar, name);

        PlaywrightAssertions.assertThat(mateCalendar.eventCard(title).first())
            .isAttached(new LocatorAssertions.IsAttachedOptions().setTimeout(PROPAGATION_MS));
    }

    @Test
    @DisplayName("TEAM-03 A member who may only read is not offered the team calendar to write in")
    void aReadingMemberCannotWriteInTheTeamCalendar(Page page, E2EUser user,
                                                    TeamCalendarProbe teams) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String name = unique("Read only team");
        aTeamFor(teams, user, Right.READ, name);
        awaitTeamVisible(page, name);

        assertThat(calendar.calendarNamesInForm())
            .as("a calendar one may not write in has no business in the destination picker")
            .noneMatch(option -> option.contains(name));
    }

    @Test
    @DisplayName("TEAM-05 A member who may write edits an event of the team")
    void aWritingMemberEditsATeamEvent(Page page, E2EUser user, TeamCalendarProbe teams) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String name = unique("Editors");
        aTeamFor(teams, user, Right.READ_WRITE, name);
        awaitTeamVisible(page, name);
        showTeam(calendar, name);
        String title = unique("Draft team event");
        String renamed = unique("Renamed team event");
        calendar.createEvent().title(title).expand().calendar(name).save();
        calendar.eventCard(title).first().waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.ATTACHED).setTimeout(PROPAGATION_MS));

        calendar.openEvent(title).edit().title(renamed).save();

        PlaywrightAssertions.assertThat(calendar.eventCard(renamed).first())
            .isAttached(new LocatorAssertions.IsAttachedOptions().setTimeout(PROPAGATION_MS));
    }

    @Test
    @DisplayName("TEAM-06 An administrator of the team deletes an event of the team")
    void anAdministratorDeletesATeamEvent(Page page, E2EUser user, TeamCalendarProbe teams) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String name = unique("Admin team");
        aTeamFor(teams, user, Right.ADMINISTRATION, name);
        awaitTeamVisible(page, name);
        showTeam(calendar, name);
        String title = unique("Doomed team event");
        calendar.createEvent().title(title).expand().calendar(name).save();
        calendar.eventCard(title).first().waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.ATTACHED).setTimeout(PROPAGATION_MS));

        calendar.openEvent(title).delete();

        PlaywrightAssertions.assertThat(calendar.eventCard(title))
            .hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(PROPAGATION_MS));
    }

    @Test
    @DisplayName("TEAM-08 An event of a team names the team as its organizer")
    void aTeamEventNamesTheTeamAsOrganizer(Page page, E2EUser user, TeamCalendarProbe teams) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String name = unique("Organising team");
        aTeamFor(teams, user, Right.READ_WRITE, name);
        awaitTeamVisible(page, name);
        showTeam(calendar, name);
        String title = unique("Organised by the team");
        calendar.createEvent().title(title).expand().calendar(name).save();
        calendar.eventCard(title).first().waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.ATTACHED).setTimeout(PROPAGATION_MS));

        String preview = calendar.openEvent(title).text();

        assertThat(preview)
            .as("the preview has to say the event belongs to the team, not to whoever typed it")
            .containsIgnoringCase("team");
    }

    @Test
    @DisplayName("TEAM-11 Unticking the team calendar takes its events off the grid")
    void untickingTheTeamCalendarHidesItsEvents(Page page, E2EUser user, TeamCalendarProbe teams) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String name = unique("Hideable team");
        aTeamFor(teams, user, Right.READ_WRITE, name);
        awaitTeamVisible(page, name);
        showTeam(calendar, name);
        String title = unique("Team event to hide");
        calendar.createEvent().title(title).expand().calendar(name).save();
        calendar.eventCard(title).first().waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.ATTACHED).setTimeout(PROPAGATION_MS));

        calendar.calendarCheckbox(name).uncheck();

        PlaywrightAssertions.assertThat(calendar.eventCard(title))
            .hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(30_000));
    }

    @Test
    @DisplayName("TEAM-13 Somebody who is not a member sees nothing of the team")
    void aNonMemberSeesNothingOfTheTeam(Page page, E2EUser user, E2EUserFactory users,
                                        E2ESessions sessions, TeamCalendarProbe teams) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String name = unique("Private team");
        String id = aTeamFor(teams, user, Right.READ_WRITE, name);
        E2EUser outsider = users.newUser("outsider");
        Page outsiderPage = sessions.pageFor(outsider);
        awaitTeamVisible(page, name);
        showTeam(calendar, name);
        String title = unique("Not for outsiders");
        calendar.createEvent().title(title).expand().calendar(name).save();
        calendar.eventCard(title).first().waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.ATTACHED).setTimeout(PROPAGATION_MS));

        outsiderPage.reload();
        new CalendarPage(outsiderPage).waitUntilLoaded();
        outsiderPage.waitForTimeout(6000);

        assertThat(outsiderPage.locator("li")
            .filter(new Locator.FilterOptions().setHasText(name)).count())
            .as("a team calendar is not public to the domain")
            .isZero();
        assertThat(new CalendarPage(outsiderPage).eventCard(title).count()).isZero();
        assertThat(teams.members(id)).doesNotContain(outsider.email());
    }

    @Test
    @DisplayName("TEAM-14 Taking a membership back takes the calendar away")
    void takingAMembershipBackTakesTheCalendarAway(Page page, E2EUser user, E2EUserFactory users,
                                                   E2ESessions sessions, TeamCalendarProbe teams) {
        LoginPage.loginAs(page, user);
        String name = unique("Revocable team");
        String id = aTeamFor(teams, user, Right.ADMINISTRATION, name);
        E2EUser mate = users.newUser("mate");
        Page matePage = sessions.pageFor(mate);
        teams.grant(id, mate, Right.READ);
        awaitTeamVisible(matePage, name);

        teams.revoke(id, mate);

        assertThat(teams.members(id)).doesNotContain(mate.email());
        Awaitility.await().atMost(Duration.ofMillis(PROPAGATION_MS))
            .pollInterval(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                matePage.reload();
                new CalendarPage(matePage).waitUntilLoaded();
                matePage.waitForTimeout(2000);
                assertThat(matePage.locator("li")
                    .filter(new Locator.FilterOptions().setHasText(name)).count())
                    .as("a membership taken back has to take the calendar with it")
                    .isZero();
            });
    }

    @Test
    @DisplayName("TEAM-15 A team event can invite somebody from outside the team")
    void aTeamEventCanInviteAnOutsider(Page page, E2EUser user, E2EUserFactory users,
                                       E2ESessions sessions, TeamCalendarProbe teams) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String name = unique("Inviting team");
        aTeamFor(teams, user, Right.READ_WRITE, name);
        E2EUser guest = users.newUser("guest");
        awaitTeamVisible(page, name);
        showTeam(calendar, name);

        String title = unique("Team event with a guest");
        calendar.createEvent().title(title)
            .addGuest(guest.email())
            .expand().calendar(name).save();

        PlaywrightAssertions.assertThat(calendar.eventCard(title).first())
            .isAttached(new LocatorAssertions.IsAttachedOptions().setTimeout(PROPAGATION_MS));
        // the preview counts the participants but only names them once expanded
        assertThat(calendar.openEvent(title).showMore().text()).contains(guest.email());
    }

    @Test
    @DisplayName("TEAM-16 A recurring team event carries its rule like any other")
    void aRecurringTeamEventCarriesItsRule(Page page, E2EUser user, TeamCalendarProbe teams) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String name = unique("Recurring team");
        aTeamFor(teams, user, Right.READ_WRITE, name);
        awaitTeamVisible(page, name);
        showTeam(calendar, name);

        String title = unique("Team standup");
        EventFormModal form = calendar.createEvent().title(title).expand().calendar(name);
        form.repeat().frequency(RecurrenceSection.DAILY).endsAfter(3);
        form.save();

        PlaywrightAssertions.assertThat(calendar.eventCard(title).first())
            .isAttached(new LocatorAssertions.IsAttachedOptions().setTimeout(PROPAGATION_MS));
        assertThat(calendar.openEvent(title).text())
            .as("a series in a team calendar spells its rule out like any other")
            .containsIgnoringCase("dai");
    }

    @Test
    @DisplayName("TEAM-17 The team calendar is offered as a destination in the form")
    void theTeamCalendarIsOfferedInTheForm(Page page, E2EUser user, TeamCalendarProbe teams) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String name = unique("Pickable team");
        aTeamFor(teams, user, Right.READ_WRITE, name);
        awaitTeamVisible(page, name);

        assertThat(calendar.calendarNamesInForm()).anyMatch(option -> option.contains(name));
    }

    @Test
    @DisplayName("TEAM-18 A personal event moved to the team calendar lands there")
    void aPersonalEventMovedToTheTeamLandsThere(Page page, E2EUser user, TeamCalendarProbe teams) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String name = unique("Destination team");
        aTeamFor(teams, user, Right.READ_WRITE, name);
        awaitTeamVisible(page, name);
        showTeam(calendar, name);
        String title = unique("Started personal");
        calendar.createEvent(title);

        calendar.openEvent(title).edit().expand().calendar(name).save();

        PlaywrightAssertions.assertThat(calendar.eventCard(title).first())
            .isAttached(new LocatorAssertions.IsAttachedOptions().setTimeout(PROPAGATION_MS));
        // hiding the team hides the event: that is what says it really moved there
        calendar.calendarCheckbox(name).uncheck();
        PlaywrightAssertions.assertThat(calendar.eventCard(title))
            .hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(30_000));
    }
}
