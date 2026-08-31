package com.linagora.calendar.e2e.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.linagora.calendar.e2e.TwakeCalendarE2ETest;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

class SettingsTest extends TwakeCalendarE2ETest {

    @Test
    @DisplayName("Switching the language to French relabels the interface")
    void switchingToFrenchRelabelsTheUi(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);

        calendar.openSettings().selectLanguage("Français");

        // The panel relabels itself right away...
        PlaywrightAssertions.assertThat(page.getByLabel("Retour au calendrier")).isVisible();
        // ...and reloading proves the preference reached the backend, not just the redux store.
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        page.reload();
        calendar.waitUntilLoaded();

        PlaywrightAssertions.assertThat(page.getByLabel("Aujourd'hui", new Page.GetByLabelOptions().setExact(true)))
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20_000));
    }

    @Test
    @DisplayName("Turning the week number off removes it from the grid")
    void turningOffTheWeekNumberHidesIt(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        PlaywrightAssertions.assertThat(calendar.weekNumber().first()).containsText("Week");

        calendar.openSettings().toggle("Show week number").backToCalendar();

        PlaywrightAssertions.assertThat(calendar.weekNumber().first())
            .not().containsText("Week", new LocatorAssertions.ContainsTextOptions().setTimeout(20_000));
    }
}
