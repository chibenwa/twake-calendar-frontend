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
import com.linagora.calendar.e2e.pages.CalendarModal;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Delegating a calendar to somebody else.
 *
 * <p>The owner works in the test's own browser, the grantee in a session of their own, so the
 * two views of the same calendar are never the same page: a right that only appears to work
 * because the owner is looking at it could not pass here.
 *
 * <p>The product offers three rights and no more: View all events, Editor and Administrator.
 */
class SharingTest extends TwakeCalendarE2ETest {
    private static final String OWN_CALENDAR = "My calendar";
    /** How long a share may take to reach the other session. */
    private static final long PROPAGATION_MS = 60_000;

    private static String uniqueTitle(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    /** The grantee's name for a calendar shared with them, as the sidebar spells it. */
    private static String sharedName(E2EUser owner) {
        return owner.email();
    }

    /** Grants a right and returns once the owner's dialog has saved it. */
    private void share(CalendarPage owner, E2EUser grantee, String right) {
        CalendarModal modal = owner.modifyCalendar(OWN_CALENDAR).tab("Access");
        modal.grantAccess(grantee.email(), right);
        modal.save();
    }

    /**
     * The sidebar row of a calendar somebody shared. It lands in a "Shared calendars" section
     * and is named after its owner, so the address is what identifies it -- the row carries no
     * label of the shape a personal calendar has.
     */
    private Locator sharedCalendarRow(Page page, E2EUser owner) {
        return page.locator("li").filter(new Locator.FilterOptions().setHasText(owner.email()));
    }

    /**
     * Reloads the grantee's page until the shared calendar shows up in their sidebar.
     *
     * <p>Polls slowly on purpose: each attempt reloads the application, and the sidebar needs a
     * moment after that to fetch the calendars. Hammering reload every few milliseconds keeps it
     * permanently at the beginning of that fetch, and the calendar would never appear.
     */
    private void awaitSharedCalendar(Page grantee, E2EUser owner) {
        Awaitility.await().atMost(Duration.ofMillis(PROPAGATION_MS))
            .pollInterval(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                grantee.reload();
                new CalendarPage(grantee).waitUntilLoaded();
                sharedCalendarRow(grantee, owner).first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(8_000));
            });
    }

    /**
     * Ticks a shared calendar. It arrives switched off, so its events are not drawn until the
     * grantee asks for them -- a test looking straight at the grid would conclude the share
     * failed.
     */
    private void showSharedCalendar(Page grantee, E2EUser owner) {
        Locator checkbox = sharedCalendarRow(grantee, owner).first()
            .locator("input[type=checkbox]").first();
        if (!checkbox.isChecked()) {
            checkbox.check();
        }
    }

    /** Waits for an event of the shared calendar to reach the grid of the grantee. */
    private void awaitEventVisible(Page grantee, E2EUser owner, String title) {
        awaitSharedCalendar(grantee, owner);
        Awaitility.await().atMost(Duration.ofMillis(PROPAGATION_MS))
            .pollInterval(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                grantee.reload();
                CalendarPage calendar = new CalendarPage(grantee).waitUntilLoaded();
                showSharedCalendar(grantee, owner);
                calendar.eventCard(title).first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(8_000));
            });
    }

    /** What the overflow menu of that row offers. */
    private java.util.List<String> menuOfSharedCalendar(Page page, E2EUser owner) {
        Locator row = sharedCalendarRow(page, owner).first();
        row.hover();
        row.locator("button").last().click();
        page.locator("[role=menuitem]").first().waitFor();
        java.util.List<String> entries = page.locator("[role=menuitem]").allInnerTexts();
        page.keyboard().press("Escape");
        return entries;
    }

    @Test
    @DisplayName("SHARE-01 The Access tab grants a right to another user")
    void theAccessTabGrantsARight(Page page, E2EUser user, E2EUserFactory users,
                                  E2ESessions sessions) {
        E2EUser mate = users.newUser("mate");
        sessions.openFor(mate);
        CalendarPage calendar = LoginPage.loginAs(page, user);

        CalendarModal modal = calendar.modifyCalendar(OWN_CALENDAR).tab("Access");
        modal.grantAccess(mate.email(), "View all events");

        assertThat(modal.hasAccessRow(mate.email())).isTrue();
        assertThat(modal.rightOf(mate.email())).isEqualTo("View all events");
        modal.save();
    }

    @Test
    @DisplayName("SHARE-02 The grantee finds the calendar in their own sidebar")
    void theGranteeFindsTheCalendarInTheirSidebar(Page page, E2EUser user, E2EUserFactory users,
                                                  E2ESessions sessions) {
        E2EUser mate = users.newUser("mate");
        Page matePage = sessions.pageFor(mate);
        CalendarPage calendar = LoginPage.loginAs(page, user);

        share(calendar, mate, "View all events");

        awaitSharedCalendar(matePage, user);
    }

    @Test
    @DisplayName("SHARE-04 An editing right allows creating in the shared calendar")
    void anEditingRightAllowsCreating(Page page, E2EUser user, E2EUserFactory users,
                                      E2ESessions sessions, CalendarProbe probe) {
        E2EUser mate = users.newUser("mate");
        Page matePage = sessions.pageFor(mate);
        CalendarPage calendar = LoginPage.loginAs(page, user);

        share(calendar, mate, "Editor");

        awaitSharedCalendar(matePage, user);
        CalendarPage mateCalendar = new CalendarPage(matePage);
        String title = uniqueTitle("Written by the delegate");
        mateCalendar.createEvent().title(title).expand()
            .calendar(user.email())
            .save();

        Awaitility.await().atMost(Duration.ofMillis(PROPAGATION_MS)).untilAsserted(() ->
            assertThat(probe.eventSummaries(user))
                .as("what the delegate wrote belongs in the owner's calendar")
                .contains(title));
    }

    @Test
    @DisplayName("SHARE-06 Only the three documented rights can be granted")
    void onlyTheThreeDocumentedRightsCanBeGranted(Page page, E2EUser user, E2EUserFactory users,
                                                  E2ESessions sessions) {
        E2EUser mate = users.newUser("mate");
        sessions.openFor(mate);
        CalendarPage calendar = LoginPage.loginAs(page, user);

        CalendarModal modal = calendar.modifyCalendar(OWN_CALENDAR).tab("Access");
        modal.grantAccess(mate.email(), "View all events");

        assertThat(modal.accessRightOptions(mate.email()))
            .containsExactly("View all events", "Editor", "Administrator");
    }

    @Test
    @DisplayName("SHARE-07 Revoking a right takes the calendar back")
    void revokingARightTakesTheCalendarBack(Page page, E2EUser user, E2EUserFactory users,
                                            E2ESessions sessions) {
        E2EUser mate = users.newUser("mate");
        Page matePage = sessions.pageFor(mate);
        CalendarPage calendar = LoginPage.loginAs(page, user);
        share(calendar, mate, "View all events");
        awaitSharedCalendar(matePage, user);

        CalendarModal modal = calendar.modifyCalendar(OWN_CALENDAR).tab("Access");
        modal.revokeAccess(mate.email());
        modal.save();

        Awaitility.await().atMost(Duration.ofMillis(PROPAGATION_MS))
            .pollInterval(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                matePage.reload();
                new CalendarPage(matePage).waitUntilLoaded();
                matePage.waitForTimeout(2000);
                assertThat(sharedCalendarRow(matePage, user).count())
                    .as("a right taken back has to disappear from the other side too")
                    .isZero();
            });
    }

    @Test
    @DisplayName("SHARE-08 The owner is named in the list of rights")
    void theOwnerIsNamedInTheListOfRights(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);

        CalendarModal modal = calendar.modifyCalendar(OWN_CALENDAR).tab("Access");

        assertThat(modal.text()).contains(user.email());
        assertThat(modal.text()).contains("Owner");
    }

    @Test
    @DisplayName("SHARE-19 A delegate is not offered to delete the calendar they were lent")
    void aDelegateCannotDeleteTheCalendar(Page page, E2EUser user, E2EUserFactory users,
                                          E2ESessions sessions) {
        E2EUser mate = users.newUser("mate");
        Page matePage = sessions.pageFor(mate);
        CalendarPage calendar = LoginPage.loginAs(page, user);
        share(calendar, mate, "Editor");

        awaitSharedCalendar(matePage, user);

        assertThat(menuOfSharedCalendar(matePage, user))
            .as("a borrowed calendar can be given back, never deleted for its owner")
            .noneMatch(entry -> entry.equalsIgnoreCase("Delete"));
    }

    @Test
    @DisplayName("SHARE-21 What the delegate changes reaches the owner without a reload")
    void whatTheDelegateChangesReachesTheOwnerLive(Page page, E2EUser user, E2EUserFactory users,
                                                   E2ESessions sessions) {
        E2EUser mate = users.newUser("mate");
        Page matePage = sessions.pageFor(mate);
        CalendarPage calendar = LoginPage.loginAs(page, user).waitUntilLiveConnected();
        share(calendar, mate, "Editor");

        awaitSharedCalendar(matePage, user);
        CalendarPage mateCalendar = new CalendarPage(matePage);
        String title = uniqueTitle("Live from the delegate");
        mateCalendar.createEvent().title(title).expand().calendar(user.email()).save();

        PlaywrightAssertions.assertThat(calendar.eventCard(title).first())
            .isAttached(new LocatorAssertions.IsAttachedOptions().setTimeout(PROPAGATION_MS));
    }

    @Test
    @DisplayName("SHARE-23 A calendar cannot be shared with its own owner")
    void aCalendarCannotBeSharedWithItsOwner(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);

        CalendarModal modal = calendar.modifyCalendar(OWN_CALENDAR).tab("Access");
        Locator search = page.getByPlaceholder("Start typing a name or email");
        search.click();
        search.pressSequentially(user.email(), new Locator.PressSequentiallyOptions().setDelay(30));
        page.waitForTimeout(3000);

        assertThat(page.locator("li[role=option]").allInnerTexts())
            .as("the owner already has every right, offering them is meaningless")
            .noneMatch(option -> option.contains(user.email()));
    }

    @Test
    @DisplayName("SHARE-20 A share outlives a fresh login on both sides")
    void aShareOutlivesAFreshLogin(Page page, E2EUser user, E2EUserFactory users,
                                   E2ESessions sessions) {
        E2EUser mate = users.newUser("mate");
        sessions.openFor(mate);
        CalendarPage calendar = LoginPage.loginAs(page, user);
        share(calendar, mate, "View all events");

        Page freshMate = sessions.pageFor(mate);
        awaitSharedCalendar(freshMate, user);

        CalendarModal modal = calendar.modifyCalendar(OWN_CALENDAR).tab("Access");
        assertThat(modal.rightOf(mate.email())).isEqualTo("View all events");
        modal.close();
    }
}
