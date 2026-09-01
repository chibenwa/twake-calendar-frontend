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
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.WaitForSelectorState;

/** The settings panel, and whether what it promises actually sticks. */
class SettingsFullTest extends TwakeCalendarE2ETest {

    private void awaitAttached(Locator locator) {
        locator.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
    }

    @Test
    @DisplayName("SET-03 The chosen language survives a reload")
    void theLanguageSurvivesAReload(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        calendar.openSettings().selectLanguage("Français");
        PlaywrightAssertions.assertThat(page.getByLabel("Retour au calendrier")).isVisible();

        page.reload();
        calendar.waitUntilLoaded();

        PlaywrightAssertions.assertThat(page.getByLabel("Aujourd'hui",
                new Page.GetByLabelOptions().setExact(true)))
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30_000));
    }

    @Test
    @DisplayName("SET-04 Every offered language relabels the interface")
    void everyLanguageRelabelsTheInterface(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        var settings = calendar.openSettings();

        settings.selectLanguage("Français");
        PlaywrightAssertions.assertThat(page.getByLabel("Retour au calendrier")).isVisible();
        assertThat(page.locator("body").innerText()).contains("Langue");

        settings.selectLanguage("Русский");
        assertThat(page.locator("body").innerText())
            .as("the panel must be in Russian now")
            .containsPattern("[А-Яа-я]");

        settings.selectLanguage("Tiếng Việt");
        assertThat(page.locator("body").innerText()).containsPattern("[ăâđêôơư]");
    }

    @Test
    @DisplayName("SET-05 Changing the timezone shifts how events are displayed")
    void changingTheTimezoneShiftsTheDisplay(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = "Shifting " + UUID.randomUUID().toString().substring(0, 8);
        calendar.createEvent().title(title).expand().startTime("10:00").endTime("11:00").save();
        awaitAttached(calendar.eventCard(title));
        String before = calendar.eventCard(title).first().innerText();

        calendar.openSettings().selectTimezone("Asia/Tokyo").backToCalendar();

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
            assertThat(calendar.eventCard(title).first().innerText())
                .as("the very same event, read from another timezone")
                .isNotEqualTo(before));
    }

    @Test
    @DisplayName("SET-06 Automatic timezone detection can be turned off")
    void automaticTimezoneDetectionCanBeTurnedOff(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);

        var settings = calendar.openSettings();
        Locator autoDetect = page.getByLabel("Detect time zone automatically").first();
        boolean initially = autoDetect.isChecked();
        settings.toggle("Detect time zone automatically");

        PlaywrightAssertions.assertThat(autoDetect)
            .isChecked(new LocatorAssertions.IsCheckedOptions().setChecked(!initially));
        page.reload();
        calendar.waitUntilLoaded();
        calendar.openSettings();
        PlaywrightAssertions.assertThat(page.getByLabel("Detect time zone automatically").first())
            .isChecked(new LocatorAssertions.IsCheckedOptions().setChecked(!initially));
    }

    @Test
    @DisplayName("SET-08 Show only working days hides the weekend from the grid")
    void showOnlyWorkingDaysHidesTheWeekend(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        assertThat(calendar.visibleDayHeaders()).hasSize(7);

        calendar.openSettings().toggle("Show only working days").backToCalendar();

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
            assertThat(calendar.visibleDayHeaders())
                .as("a week without its weekend is shorter")
                .hasSizeLessThan(7));
    }

    @Test
    @DisplayName("SET-09 Show declined events brings a declined event back")
    void showDeclinedEventsBringsThemBack(Page page, E2EUser organizer, E2EUserFactory users,
                                          E2ESessions sessions, CalendarProbe probe) {
        E2EUser guest = users.newUser();
        CalendarPage guestCalendar = sessions.openFor(guest);
        CalendarPage calendar = LoginPage.loginAs(page, organizer);
        String title = "Declined " + UUID.randomUUID().toString().substring(0, 8);
        calendar.createEvent().title(title).addGuest(guest.email()).save();
        awaitAttached(calendar.eventCard(title));

        guestCalendar.page().reload();
        guestCalendar.waitUntilLoaded();
        awaitAttached(guestCalendar.eventCard(title));
        guestCalendar.openEvent(title).answer("No");
        Awaitility.await().atMost(Duration.ofSeconds(45)).untilAsserted(() ->
            assertThat(probe.singleEvent(guest)).contains("PARTSTAT=DECLINED"));

        guestCalendar.page().reload();
        guestCalendar.waitUntilLoaded();
        PlaywrightAssertions.assertThat(guestCalendar.eventCard(title).first())
            .isAttached(new LocatorAssertions.IsAttachedOptions().setTimeout(30_000));
    }

    @Test
    @DisplayName("SET-10 Hiding declined events removes them from the grid")
    void hidingDeclinedEventsRemovesThem(Page page, E2EUser organizer, E2EUserFactory users,
                                         E2ESessions sessions, CalendarProbe probe) {
        E2EUser guest = users.newUser();
        CalendarPage guestCalendar = sessions.openFor(guest);
        CalendarPage calendar = LoginPage.loginAs(page, organizer);
        String title = "To hide " + UUID.randomUUID().toString().substring(0, 8);
        calendar.createEvent().title(title).addGuest(guest.email()).save();
        awaitAttached(calendar.eventCard(title));

        guestCalendar.page().reload();
        guestCalendar.waitUntilLoaded();
        awaitAttached(guestCalendar.eventCard(title));
        guestCalendar.openEvent(title).answer("No");
        Awaitility.await().atMost(Duration.ofSeconds(45)).untilAsserted(() ->
            assertThat(probe.singleEvent(guest)).contains("PARTSTAT=DECLINED"));

        // start from a clean page: the preview left open would swallow the click on the profile
        guestCalendar.page().reload();
        guestCalendar.waitUntilLoaded();
        guestCalendar.openSettings().toggle("Show declined events").backToCalendar();

        PlaywrightAssertions.assertThat(guestCalendar.eventCard(title))
            .hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(30_000));
    }

    @Test
    @DisplayName("SET-11 The email notification delivery method is saved")
    void theEmailNotificationSettingIsSaved(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);

        var settings = calendar.openSettings().tab("Notifications");
        Locator email = page.getByLabel("Email").first();
        boolean initially = email.isChecked();
        settings.toggle("Email");
        PlaywrightAssertions.assertThat(email)
            .isChecked(new LocatorAssertions.IsCheckedOptions().setChecked(!initially));

        page.reload();
        calendar.waitUntilLoaded();
        calendar.openSettings().tab("Notifications");

        PlaywrightAssertions.assertThat(page.getByLabel("Email").first())
            .isChecked(new LocatorAssertions.IsCheckedOptions().setChecked(!initially));
    }

    @Test
    @DisplayName("SET-12 The back button returns to the calendar")
    void theBackButtonReturnsToTheCalendar(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);

        calendar.openSettings().backToCalendar();

        PlaywrightAssertions.assertThat(page.locator(".fc-view-harness")).isVisible();
        PlaywrightAssertions.assertThat(page.getByLabel("Create a new event")).isVisible();
    }

    @Test
    @DisplayName("SET-14 One user's settings do not affect another's")
    void settingsAreNotShared(Page page, E2EUser user, E2EUserFactory users, E2ESessions sessions) {
        CalendarPage mine = LoginPage.loginAs(page, user);
        E2EUser other = users.newUser();
        CalendarPage theirs = sessions.openFor(other);

        mine.openSettings().selectLanguage("Français");
        PlaywrightAssertions.assertThat(page.getByLabel("Retour au calendrier")).isVisible();

        theirs.page().reload();
        theirs.waitUntilLoaded();
        PlaywrightAssertions.assertThat(theirs.page().getByLabel("Today",
                new Page.GetByLabelOptions().setExact(true)))
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30_000));
    }
}
