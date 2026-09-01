package com.linagora.calendar.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** The settings panel, reachable from the user menu. */
public class SettingsPage {
    private final Page page;

    public SettingsPage(Page page) {
        this.page = page;
    }

    SettingsPage waitUntilOpen() {
        languageSelector().waitFor();
        return this;
    }

    /**
     * The language picker. Its own accessible name is translated along with everything else, so
     * it is located by shape once the English label is gone.
     */
    private Locator languageSelector() {
        Locator english = page.getByLabel("Language selector");
        if (english.count() > 0) {
            return english.first();
        }
        return page.getByRole(AriaRole.COMBOBOX).first();
    }

    /** One of English, Français, Русский, Tiếng Việt. */
    public SettingsPage selectLanguage(String language) {
        languageSelector().click();
        awaitPersisted(() -> page.getByRole(AriaRole.OPTION,
            new Page.GetByRoleOptions().setName(language).setExact(true)).click());
        return this;
    }

    /**
     * Pins the application timezone. Automatic detection has to go first, otherwise the
     * browser timezone wins straight back.
     */
    public SettingsPage selectTimezone(String timezone) {
        Locator autoDetect = page.getByLabel("Detect time zone automatically").first();
        if (autoDetect.isChecked()) {
            awaitPersisted(autoDetect::click);
        }
        Locator picker = page.getByPlaceholder("Select timezone");
        picker.click();
        picker.fill("");
        picker.pressSequentially(timezone, new Locator.PressSequentiallyOptions().setDelay(40));
        awaitPersisted(() -> page.locator("li[role=option]").first().click());
        return this;
    }

    /** One of the settings tabs: Settings, Notifications. */
    public SettingsPage tab(String tab) {
        Locator target = page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName(tab));
        target.click();
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(target)
            .hasAttribute("aria-selected", "true");
        return this;
    }

    /** Toggles one of the working day buttons, by its iCalendar code. */
    public SettingsPage workingDay(String icalDay) {
        awaitPersisted(() -> page.getByLabel(icalDay, new Page.GetByLabelOptions().setExact(true))
            .first().click());
        return this;
    }

    /** Flips one of the switches, designated by its visible label. */
    public SettingsPage toggle(String label) {
        awaitPersisted(() -> page.getByLabel(label).first().click());
        return this;
    }

    /**
     * Settings are relabelled in the store before the server knows about it. Waiting for the
     * write to come back keeps a following reload from racing it.
     */
    private void awaitPersisted(Runnable action) {
        page.waitForResponse(
            response -> response.url().contains("api/configurations")
                && "PATCH".equals(response.request().method()),
            action::run);
    }

    public CalendarPage backToCalendar() {
        page.getByLabel("Back to calendar").click();
        return new CalendarPage(page).waitUntilLoaded();
    }
}
