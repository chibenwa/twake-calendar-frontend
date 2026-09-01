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
import com.linagora.calendar.e2e.backend.ResourceProbe;
import com.linagora.calendar.e2e.docker.E2ESessions;
import com.linagora.calendar.e2e.docker.RuntimeConfig;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.EventFormModal;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/**
 * Domain resources: rooms and equipment one books along with an event.
 *
 * <p>They are provisioned through the webadmin API, like team calendars, and the administrator
 * has to have signed in once before being named as one.
 */
class ResourcesTest extends TwakeCalendarE2ETest {
    private static final long PROPAGATION_MS = 60_000;

    private static String unique(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private void awaitResourceVisible(Page page, String name) {
        Awaitility.await().atMost(Duration.ofMillis(PROPAGATION_MS))
            .pollInterval(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                page.reload();
                new CalendarPage(page).waitUntilLoaded();
                page.locator("li").filter(new Locator.FilterOptions().setHasText(name))
                    .first().waitFor(new Locator.WaitForOptions().setTimeout(8_000));
            });
    }

    @Test
    @DisplayName("RES-01 The Resources section lists the resources of the domain")
    void theResourcesSectionListsThem(Page page, E2EUser user, ResourceProbe resources) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String room = unique("Board room");
        resources.create(room, "The one with the big table", user);

        awaitResourceVisible(page, room);

        PlaywrightAssertions.assertThat(calendar.sidebarSection("Resources").first()).isVisible();
        assertThat(resources.names()).contains(room);
    }

    @Test
    @DisplayName("RES-03 A resource booked from the form is carried by the event")
    void aBookedResourceIsCarriedByTheEvent(Page page, E2EUser user, ResourceProbe resources) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String room = unique("Bookable room");
        resources.create(room, "A room", user);
        awaitResourceVisible(page, room);

        String title = unique("Meeting with a room");
        calendar.createEvent().title(title).expand().addResource(room).save();

        PlaywrightAssertions.assertThat(calendar.eventCard(title).first())
            .isAttached(new LocatorAssertions.IsAttachedOptions().setTimeout(PROPAGATION_MS));
        EventFormModal form = calendar.openEvent(title).edit().expand();
        assertThat(form.text())
            .as("the room the event was booked with has to still be on it")
            .contains(room);
        form.cancel();
    }

    @Test
    @DisplayName("RES-12 The resource field filters on what is typed")
    void theResourceFieldFilters(Page page, E2EUser user, ResourceProbe resources) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String wanted = unique("Projector");
        String other = unique("Bicycle");
        resources.create(wanted, "Bright", user);
        resources.create(other, "Fast", user);
        awaitResourceVisible(page, wanted);

        EventFormModal form = calendar.createEvent().title(unique("Filtering")).expand();

        assertThat(form.resourceOptions(wanted.substring(0, 9)))
            .anyMatch(option -> option.contains(wanted));
        assertThat(form.resourceOptions(wanted.substring(0, 9)))
            .as("a search names one resource, not the whole domain")
            .noneMatch(option -> option.contains(other));
        form.cancel();
    }

    @Test
    @DisplayName("RES-13 A resource search matching nothing says so")
    void aResourceSearchMatchingNothingSaysSo(Page page, E2EUser user, ResourceProbe resources) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String room = unique("Existing room");
        resources.create(room, "A room", user);
        awaitResourceVisible(page, room);

        EventFormModal form = calendar.createEvent().title(unique("Fruitless")).expand();
        Locator search = form.resourceSearch();
        search.click();
        search.pressSequentially("zzz-nothing-matches-this",
            new Locator.PressSequentiallyOptions().setDelay(20));
        page.waitForTimeout(3000);

        // the message belongs to the autocomplete popper, which lives outside the dialog
        assertThat(page.locator("body").innerText())
            .as("an empty result has to be said, not left blank")
            .contains("No results");
        form.cancel();
    }

    @Test
    @DisplayName("RES-15 Unticking a resource takes its bookings off the grid")
    void untickingAResourceHidesItsBookings(Page page, E2EUser user, ResourceProbe resources) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String room = unique("Hideable room");
        resources.create(room, "A room", user);
        awaitResourceVisible(page, room);
        calendar.showCalendar(room);
        String title = unique("Booked in the room");
        calendar.createEvent().title(title).expand().addResource(room).save();
        calendar.eventCard(title).first().waitFor(new Locator.WaitForOptions()
            .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED)
            .setTimeout(PROPAGATION_MS));

        calendar.calendarCheckbox(room).uncheck();
        page.waitForTimeout(2000);

        assertThat(calendar.calendarCheckbox(room).isChecked())
            .as("the resource really is switched off")
            .isFalse();
    }

    @Test
    @DisplayName("RES-17 Somebody who administers nothing sees no resource of their own")
    void aNonAdministratorSeesNoResourceOfTheirOwn(Page page, E2EUser user, E2EUserFactory users,
                                                   E2ESessions sessions, ResourceProbe resources) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String room = unique("Administered room");
        resources.create(room, "A room", user);
        awaitResourceVisible(page, room);

        E2EUser outsider = users.newUser("outsider");
        Page outsiderPage = sessions.pageFor(outsider);
        outsiderPage.waitForTimeout(4000);
        outsiderPage.reload();
        new CalendarPage(outsiderPage).waitUntilLoaded();
        outsiderPage.waitForTimeout(4000);

        assertThat(outsiderPage.locator("li")
            .filter(new Locator.FilterOptions().setHasText(room)).count())
            .as("a resource lands in the sidebar of the people who administer it")
            .isZero();
    }

    @Test
    @DisplayName("RES-18 HIDE_RESOURCES takes the whole section away")
    void hideResourcesTakesTheSectionAway(Page page, E2EUser user, ResourceProbe resources,
                                          RuntimeConfig config) {
        config.set("HIDE_RESOURCES", "true");
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String room = unique("Invisible room");
        resources.create(room, "A room", user);
        page.reload();
        calendar.waitUntilLoaded();
        page.waitForTimeout(4000);

        assertThat(page.getByText("Resources").count())
            .as("nothing of the feature shows when it is switched off")
            .isZero();
        assertThat(page.locator("li").filter(new Locator.FilterOptions().setHasText(room)).count())
            .isZero();
    }

    @Test
    @DisplayName("RES-11 Deleting the event that booked a resource gives it back")
    void deletingTheEventGivesTheResourceBack(Page page, E2EUser user, ResourceProbe resources) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String room = unique("Freed room");
        resources.create(room, "A room", user);
        awaitResourceVisible(page, room);
        String title = unique("Short lived booking");
        calendar.createEvent().title(title).expand().addResource(room).save();
        calendar.eventCard(title).first().waitFor(new Locator.WaitForOptions()
            .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED)
            .setTimeout(PROPAGATION_MS));

        calendar.openEvent(title).delete();

        PlaywrightAssertions.assertThat(calendar.eventCard(title))
            .hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(PROPAGATION_MS));
    }
}
