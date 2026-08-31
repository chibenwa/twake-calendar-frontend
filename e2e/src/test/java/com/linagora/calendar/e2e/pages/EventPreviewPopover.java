package com.linagora.calendar.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

/** The popover opened by clicking an event in the grid. */
public class EventPreviewPopover {
    private final Page page;

    public EventPreviewPopover(Page page) {
        this.page = page;
    }

    EventPreviewPopover waitUntilOpen() {
        page.getByLabel("Edit event").waitFor();
        return this;
    }

    public String text() {
        return page.locator(".MuiPopover-root, [role=dialog]").last().innerText();
    }

    public Locator content() {
        return page.locator(".MuiPopover-root, [role=dialog]").last();
    }

    public EventFormModal edit() {
        page.getByLabel("Edit event").click();
        return new EventFormModal(page).waitUntilOpen();
    }

    public void delete() {
        page.getByLabel("Delete event").click();
        page.getByLabel("Delete event").waitFor(
            new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }

    public void close() {
        page.keyboard().press("Escape");
    }

    /** Expands the participant list and the extra details. */
    public EventPreviewPopover showMore() {
        page.getByText("Show more").last().click();
        return this;
    }
}
