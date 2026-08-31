package com.linagora.calendar.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.linagora.calendar.e2e.TwakeCalendarE2ETest;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Page;

class CalendarNavigationTest extends TwakeCalendarE2ETest {

    @Test
    @DisplayName("Next moves the week view to the following week")
    void nextMovesToTheFollowingWeek(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        List<String> currentWeek = calendar.visibleDayHeaders();

        calendar.next();

        assertThat(calendar.visibleDayHeaders())
            .hasSize(7)
            .isNotEqualTo(currentWeek);
    }

    @Test
    @DisplayName("Today comes back to the current week after browsing away")
    void todayComesBackToTheCurrentWeek(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        List<String> currentWeek = calendar.visibleDayHeaders();

        calendar.next().next().previous().previous();

        assertThat(calendar.visibleDayHeaders()).isEqualTo(currentWeek);

        calendar.next().today();

        assertThat(calendar.visibleDayHeaders()).isEqualTo(currentWeek);
    }

    @Test
    @DisplayName("Switching to the month view renders a month grid")
    void monthViewRendersAMonthGrid(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);

        calendar.switchView("Month");

        assertThat(calendar.currentViewClass()).contains("fc-dayGridMonth-view");
    }

    @Test
    @DisplayName("Switching to the day view narrows the grid down to a single column")
    void dayViewRendersASingleDay(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);

        calendar.switchView("Day");

        assertThat(calendar.currentViewClass()).contains("fc-timeGridDay-view");
        assertThat(calendar.visibleDayHeaders()).hasSize(1);
    }
}
