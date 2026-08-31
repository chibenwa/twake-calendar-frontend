package com.linagora.calendar.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

/** The calendar dialog: creating one, and its Settings / Access / Import tabs. */
public class CalendarModal {
    private final Page page;

    CalendarModal(Page page) {
        this.page = page;
    }

    CalendarModal waitUntilOpen() {
        nameInput().waitFor();
        return this;
    }

    private Locator nameInput() {
        return page.getByLabel("Name", new Page.GetByLabelOptions().setExact(true));
    }

    public CalendarModal name(String name) {
        nameInput().fill(name);
        return this;
    }

    /** Picks one of the preset colours, by its hexadecimal value. */
    public CalendarModal color(String hex) {
        page.getByLabel("select color " + hex).click();
        return this;
    }

    public void create() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create").setExact(true))
            .last().click();
        nameInput().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }

    public void close() {
        page.getByLabel("close").last().click();
    }
}
