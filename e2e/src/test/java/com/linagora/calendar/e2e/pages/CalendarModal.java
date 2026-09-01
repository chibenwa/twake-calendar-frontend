package com.linagora.calendar.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

/** The calendar dialog: creating one, and the Settings, Access and Import tabs of an existing one. */
public class CalendarModal {
    private final Page page;

    CalendarModal(Page page) {
        this.page = page;
    }

    CalendarModal waitUntilOpen() {
        page.locator("[role=dialog]").last().waitFor();
        return this;
    }

    private Locator dialog() {
        return page.locator("[role=dialog]").last();
    }

    private Locator nameInput() {
        return page.getByLabel("Name", new Page.GetByLabelOptions().setExact(true));
    }

    public CalendarModal name(String name) {
        nameInput().fill(name);
        return this;
    }

    public String name() {
        return nameInput().inputValue();
    }

    /** One of Settings, Access, Import, Add new calendar. */
    public CalendarModal tab(String tab) {
        Locator target = page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName(tab));
        target.click();
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(target)
            .hasAttribute("aria-selected", "true");
        return this;
    }

    public java.util.List<String> tabs() {
        return page.getByRole(AriaRole.TAB).allInnerTexts();
    }

    /** Picks one of the preset colours, by its hexadecimal value. */
    public CalendarModal color(String hex) {
        page.getByLabel("select color " + hex).click();
        return this;
    }

    public CalendarModal customColor(String hex) {
        page.getByLabel("Select custom color").click();
        page.waitForTimeout(600);
        // the hex field carries neither label nor placeholder; once the picker is open it is
        // the last input on the page, already holding the current colour
        Locator hexInput = page.locator("input").last();
        hexInput.fill(hex);
        hexInput.press("Enter");
        page.waitForTimeout(600);
        // the picker sits over the dialog: confirm it so the Create button is reachable again
        Locator confirm = page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(java.util.regex.Pattern.compile("^(Ok|OK|Apply|Save)$")));
        if (confirm.count() > 0) {
            confirm.last().click();
            page.waitForTimeout(500);
        }
        return this;
    }

    /** New events of this calendar are visible to All, or to You only. */
    public CalendarModal newEventsVisibleTo(String audience) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(audience).setExact(true))
            .last().click();
        return this;
    }

    public String text() {
        return dialog().innerText();
    }

    public void create() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create").setExact(true))
            .last().click();
        nameInput().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }

    public void save() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save").setExact(true))
            .last().click();
        // the dialog closing is the signal the write went through, not an arbitrary delay
        dialog().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }

    public void close() {
        page.getByLabel("close").last().click();
    }
}
