package com.linagora.calendar.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.linagora.calendar.e2e.TwakeCalendarE2ETest;
import com.linagora.calendar.e2e.backend.CalendarProbe;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.backend.E2EUserFactory;
import com.linagora.calendar.e2e.backend.ResourceProbe;
import com.linagora.calendar.e2e.backend.TeamCalendarProbe;
import com.linagora.calendar.e2e.docker.E2ESessions;
import com.linagora.calendar.e2e.pages.CalendarModal;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.linagora.calendar.e2e.pages.SharedCalendar;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/**
 * Somebody holding the administration right on a calendar that is not theirs manages who else
 * may use it, from the Access tab of the calendar dialog, exactly as its owner would.
 *
 * <p>One scenario per kind of calendar that can be administered by somebody else: a team
 * calendar, the calendar of another user, a resource. Each one gets there by a road of its own
 * -- a team membership, a share, the administrators of the resource -- and each one saves the
 * rights through a calendar node that is not the administrator's own.
 */
class AdministrationRightTest extends TwakeCalendarE2ETest {
    private static final long PROPAGATION_MS = SharedCalendar.PROPAGATION_MS;

    private static String unique(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Reloads, slowly, until a sidebar row containing the given text shows up. */
    private void awaitSidebarRow(Page page, String rowFragment) {
        Awaitility.await().atMost(Duration.ofMillis(PROPAGATION_MS))
            .pollInterval(Duration.ofSeconds(2))
            .ignoreExceptions()
            .untilAsserted(() -> {
                page.reload();
                new CalendarPage(page).waitUntilLoaded();
                page.locator("li").filter(new Locator.FilterOptions().setHasText(rowFragment))
                    .first().waitFor(new Locator.WaitForOptions().setTimeout(8_000));
            });
    }

    /** Grants a right from the Access tab of the calendar whose sidebar row holds that text. */
    private void grantFromTheDialog(Page page, String rowFragment, E2EUser grantee, String right) {
        CalendarModal modal = new CalendarPage(page).modifyCalendarMatching(rowFragment)
            .tab("Access");
        assertThat(modal.canGrantAccess())
            .as("an administrator of the calendar is offered to grant rights on it")
            .isTrue();
        modal.grantAccess(grantee.email(), right);
        modal.save();
    }

    /** Reopens that dialog on a fresh page and reads back the right the grantee holds. */
    private String rightShownAfterAReload(Page page, String rowFragment, E2EUser grantee) {
        page.reload();
        CalendarModal modal = new CalendarPage(page).waitUntilLoaded()
            .modifyCalendarMatching(rowFragment).tab("Access");
        String right = modal.hasAccessRow(grantee.email()) ? modal.rightOf(grantee.email()) : "";
        modal.close();
        return right;
    }

    /** Sets the public visibility of a calendar from its dialog, All or You, and saves. */
    private void setVisibility(Page page, String rowFragment, String audience) {
        page.reload();
        CalendarModal modal = new CalendarPage(page).waitUntilLoaded()
            .modifyCalendarMatching(rowFragment);
        assertThat(modal.showsVisibility())
            .as("an administrator of the calendar manages its public visibility")
            .isTrue();
        modal.newEventsVisibleTo(audience);
        modal.save();
    }

    /** Waits until Sabre answers the given status to somebody reading the calendar node. */
    private void awaitReadStatus(CalendarProbe probe, E2EUser reader, String node, int status,
                                 String because) {
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
            assertThat(probe.readStatus(reader, node)).as(because).isEqualTo(status));
    }

    @Test
    @DisplayName("ADMIN-04 An administrator of a team calendar makes it public, then private "
        + "again, and the other users of the domain can read it, then no longer")
    void aTeamAdministratorManagesThePublicVisibility(Page page, E2EUser user,
                                                      E2EUserFactory users, E2ESessions sessions,
                                                      TeamCalendarProbe teams,
                                                      CalendarProbe probe) {
        E2EUser stranger = users.newUser("stranger");
        sessions.pageFor(stranger);
        LoginPage.loginAs(page, user);
        String team = unique("Visible team");
        String teamId = teams.create("team-" + UUID.randomUUID().toString().substring(0, 8), team);
        teams.grant(teamId, user, TeamCalendarProbe.Right.ADMINISTRATION);
        String node = CalendarProbe.defaultCalendarNode(teamId);
        awaitSidebarRow(page, team);
        assertThat(probe.readStatus(stranger, node))
            .as("a team calendar starts private")
            .isEqualTo(403);

        setVisibility(page, team, "All");
        awaitReadStatus(probe, stranger, node, 200,
            "a team calendar made public is readable by the whole domain");

        setVisibility(page, team, "You");
        awaitReadStatus(probe, stranger, node, 403,
            "a team calendar made private again is readable by its members only");
    }

    @Test
    @DisplayName("ADMIN-05 An administrator of a resource makes it private, then public again, "
        + "and the other users of the domain can no longer read it, then can")
    void aResourceAdministratorManagesThePublicVisibility(Page page, E2EUser user,
                                                          E2EUserFactory users,
                                                          E2ESessions sessions,
                                                          ResourceProbe resources,
                                                          CalendarProbe probe) {
        E2EUser stranger = users.newUser("stranger");
        sessions.pageFor(stranger);
        LoginPage.loginAs(page, user);
        String room = unique("Visible room");
        String node = CalendarProbe.defaultCalendarNode(resources.create(room, "A room", user));
        awaitSidebarRow(page, room);
        assertThat(probe.readStatus(stranger, node))
            .as("a resource starts public: anybody may see when it is booked")
            .isEqualTo(200);

        setVisibility(page, room, "You");
        awaitReadStatus(probe, stranger, node, 403,
            "a resource made private is readable by its administrators only");

        setVisibility(page, room, "All");
        awaitReadStatus(probe, stranger, node, 200,
            "a resource made public again is readable by the whole domain");
    }

    @Test
    @DisplayName("ADMIN-06 An administrator of somebody else's calendar makes it public, and the "
        + "other users of the domain can read it")
    void aDelegatedAdministratorManagesThePublicVisibility(Page page, E2EUser user,
                                                           E2EUserFactory users,
                                                           E2ESessions sessions,
                                                           CalendarProbe probe) {
        E2EUser administrator = users.newUser("admin");
        E2EUser stranger = users.newUser("stranger");
        Page administratorPage = sessions.pageFor(administrator);
        sessions.pageFor(stranger);
        CalendarPage owner = LoginPage.loginAs(page, user);
        CalendarModal ownerModal = owner.modifyCalendar("My calendar").tab("Access");
        ownerModal.grantAccess(administrator.email(), "Administrator");
        ownerModal.save();
        probe.setPublicRight(user, CalendarProbe.PublicRight.NONE);
        String node = CalendarProbe.defaultCalendarNode(probe.requireOpenPaasId(user));
        assertThat(probe.readStatus(stranger, node)).isEqualTo(403);
        new SharedCalendar(administratorPage, user).awaitInSidebar();

        setVisibility(administratorPage, user.uid(), "All");

        awaitReadStatus(probe, stranger, node, 200,
            "the calendar its administrator made public is readable by the whole domain");
        assertThat(probe.publicPrivileges(user))
            .as("it is the owner's calendar itself that became public")
            .contains("{DAV:}read");
    }

    @ParameterizedTest(name = "ADMIN-07 A grantee holding \"{0}\" sees the public visibility of "
        + "the calendar, and cannot change it")
    @ValueSource(strings = {"View all events", "Editor"})
    void aGranteeSeesThePublicVisibilityReadOnly(String right, Page page, E2EUser user,
                                                 E2EUserFactory users, E2ESessions sessions,
                                                 CalendarProbe probe) {
        E2EUser mate = users.newUser("mate");
        Page matePage = sessions.pageFor(mate);
        CalendarPage owner = LoginPage.loginAs(page, user);
        CalendarModal ownerModal = owner.modifyCalendar("My calendar").tab("Access");
        ownerModal.grantAccess(mate.email(), right);
        ownerModal.save();
        probe.setPublicRight(user, CalendarProbe.PublicRight.READ);
        new SharedCalendar(matePage, user).awaitInSidebar();

        CalendarModal borrowed = new CalendarPage(matePage).modifyCalendarMatching(user.uid());
        assertThat(borrowed.showsVisibility())
            .as("the grantee is told who else may see the events of the calendar")
            .isTrue();
        PlaywrightAssertions.assertThat(borrowed.visibilityOption("All"))
            .hasAttribute("aria-pressed", "true");
        PlaywrightAssertions.assertThat(borrowed.visibilityOption("All")).isDisabled();
        PlaywrightAssertions.assertThat(borrowed.visibilityOption("You")).isDisabled();
        borrowed.close();

        probe.setPublicRight(user, CalendarProbe.PublicRight.NONE);
        matePage.reload();
        CalendarModal reopened = new CalendarPage(matePage).waitUntilLoaded()
            .modifyCalendarMatching(user.uid());
        // what the grantee is shown follows the calendar
        PlaywrightAssertions.assertThat(reopened.visibilityOption("You"))
            .hasAttribute("aria-pressed", "true");
        reopened.close();
    }

    @Test
    @DisplayName("ADMIN-01 An administrator of a team calendar grants a right on it "
        + "from the calendar dialog")
    void aTeamAdministratorGrantsARight(Page page, E2EUser user, E2EUserFactory users,
                                        E2ESessions sessions, TeamCalendarProbe teams) {
        E2EUser mate = users.newUser("mate");
        Page matePage = sessions.pageFor(mate);
        LoginPage.loginAs(page, user);
        String team = unique("Administered team");
        String teamId = teams.create("team-" + UUID.randomUUID().toString().substring(0, 8), team);
        teams.grant(teamId, user, TeamCalendarProbe.Right.ADMINISTRATION);
        awaitSidebarRow(page, team);

        grantFromTheDialog(page, team, mate, "View all events");

        Awaitility.await().atMost(Duration.ofMillis(PROPAGATION_MS)).untilAsserted(() ->
            assertThat(teams.members(teamId))
                .as("the right granted from the dialog is a membership of the team")
                .contains(mate.email()));
        assertThat(rightShownAfterAReload(page, team, mate)).isEqualTo("View all events");
        awaitSidebarRow(matePage, team);
    }

    @Test
    @DisplayName("ADMIN-02 An administrator of somebody else's calendar grants a right on it "
        + "from the calendar dialog")
    void aDelegatedAdministratorGrantsARight(Page page, E2EUser user, E2EUserFactory users,
                                             E2ESessions sessions) {
        E2EUser administrator = users.newUser("admin");
        E2EUser mate = users.newUser("mate");
        Page administratorPage = sessions.pageFor(administrator);
        Page matePage = sessions.pageFor(mate);
        CalendarPage owner = LoginPage.loginAs(page, user);
        CalendarModal ownerModal = owner.modifyCalendar("My calendar").tab("Access");
        ownerModal.grantAccess(administrator.email(), "Administrator");
        ownerModal.save();
        new SharedCalendar(administratorPage, user).awaitInSidebar();

        grantFromTheDialog(administratorPage, user.uid(), mate, "View all events");

        new SharedCalendar(matePage, user).awaitInSidebar();
        page.reload();
        CalendarModal reopened = owner.waitUntilLoaded().modifyCalendar("My calendar").tab("Access");
        assertThat(reopened.hasAccessRow(mate.email()))
            .as("the owner sees who their administrator let in")
            .isTrue();
        assertThat(reopened.rightOf(mate.email())).isEqualTo("View all events");
        assertThat(reopened.rightOf(administrator.email())).isEqualTo("Administrator");
        reopened.close();
    }

    @Test
    @DisplayName("ADMIN-03 An administrator of a resource grants a right on it "
        + "from the calendar dialog")
    void aResourceAdministratorGrantsARight(Page page, E2EUser user, E2EUserFactory users,
                                            E2ESessions sessions, ResourceProbe resources) {
        E2EUser mate = users.newUser("mate");
        Page matePage = sessions.pageFor(mate);
        LoginPage.loginAs(page, user);
        String room = unique("Administered room");
        resources.create(room, "A room", user);
        awaitSidebarRow(page, room);

        grantFromTheDialog(page, room, mate, "View all events");

        assertThat(rightShownAfterAReload(page, room, mate)).isEqualTo("View all events");
        awaitSidebarRow(matePage, room);
    }
}
