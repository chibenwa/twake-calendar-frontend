package com.linagora.calendar.e2e.tests;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.linagora.calendar.e2e.TwakeCalendarE2ETest;
import com.linagora.calendar.e2e.backend.CalendarProbe;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.backend.Ical;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.WaitForSelectorState;

/** Calendar selection defects this project has already shipped. */
class PastCalendarsTest extends TwakeCalendarE2ETest {

    private void awaitAttached(Locator locator) {
        locator.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
    }

    private String seedEvent(CalendarProbe probe, E2EUser user) {
        String title = "Hidden " + UUID.randomUUID().toString().substring(0, 8);
        String uid = UUID.randomUUID().toString();
        probe.putEvent(user, uid, Ical.event(uid, title, LocalDate.now(), 9));
        return title;
    }

    @Test
    @DisplayName("PAST-34 (#242) A calendar created with a custom colour still renders after a reload")
    void aCalendarWithACustomColourSurvivesAReload(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String name = "Coloured " + UUID.randomUUID().toString().substring(0, 6);

        calendar.addCalendar().name(name).color("#AFCBEF").create();
        PlaywrightAssertions.assertThat(calendar.calendarCheckbox(name))
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20_000));

        page.reload();
        calendar.waitUntilLoaded();

        // #242 crashed here, in the contrast computation of the calendar colour
        PlaywrightAssertions.assertThat(calendar.calendarCheckbox(name))
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20_000));
        PlaywrightAssertions.assertThat(page.locator(".fc-view-harness")).isVisible();
    }

    @Test
    @DisplayName("PAST-35 (#159) A personal calendar can be unticked")
    void aPersonalCalendarCanBeUnticked(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = seedEvent(probe, user);
        page.reload();
        calendar.waitUntilLoaded();
        awaitAttached(calendar.eventCard(title));

        calendar.calendarCheckbox("My calendar").uncheck();

        PlaywrightAssertions.assertThat(calendar.calendarCheckbox("My calendar")).not().isChecked();
        PlaywrightAssertions.assertThat(calendar.eventCard(title))
            .hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(20_000));

        calendar.calendarCheckbox("My calendar").check();
        PlaywrightAssertions.assertThat(calendar.eventCard(title).first())
            .isAttached(new LocatorAssertions.IsAttachedOptions().setTimeout(20_000));
    }
}
