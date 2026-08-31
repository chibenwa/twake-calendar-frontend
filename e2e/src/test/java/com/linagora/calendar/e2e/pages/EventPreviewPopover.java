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
        // the title heading, not the Edit button: a guest invited to an event they do not own
        // gets the preview without the editing actions
        content().locator("h3").first().waitFor();
        return this;
    }

    public String text() {
        return page.locator(".MuiPopover-root, [role=dialog]").last().innerText();
    }

    public Locator content() {
        return page.locator(".MuiPopover-root, [role=dialog]").last();
    }

    public EventFormModal edit() {
        clickEdit();
        return new EventFormModal(page).waitUntilOpen();
    }

    /**
     * Editing an occurrence of a series: the scope dialog comes up first, and the form only
     * opens once the caller has said what the edit applies to.
     */
    public EventFormModal edit(EventFormModal.Scope scope) {
        clickEdit();
        ScopeDialog dialog = ScopeDialog.waitFor(page);
        if (scope == EventFormModal.Scope.THIS_EVENT) {
            dialog.thisEvent();
        } else {
            dialog.allEvents();
        }
        return new EventFormModal(page).waitUntilOpen();
    }

    /** Clicks Edit without assuming what comes next, for the tests that assert on it. */
    public void clickEdit() {
        page.getByLabel("Edit event").click();
    }

    public void delete() {
        page.getByLabel("Delete event").click();
        page.getByLabel("Delete event").waitFor(
            new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }

    /** Deleting an occurrence of a series first asks what it should apply to. */
    public void delete(EventFormModal.Scope scope) {
        page.getByLabel("Delete event").click();
        ScopeDialog dialog = ScopeDialog.waitFor(page);
        if (scope == EventFormModal.Scope.THIS_EVENT) {
            dialog.thisEvent();
        } else {
            dialog.allEvents();
        }
        page.getByLabel("Delete event").waitFor(
            new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }

    /** Answers the invitation: Yes, No or Maybe. */
    public void answer(String answer) {
        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(answer).setExact(true)).last().click();
    }

    /** Answers on a series: the scope dialog comes up in between. */
    public void answer(String answer, EventFormModal.Scope scope) {
        answer(answer);
        ScopeDialog dialog = ScopeDialog.waitFor(page);
        if (scope == EventFormModal.Scope.THIS_EVENT) {
            dialog.thisEvent();
        } else {
            dialog.allEvents();
        }
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
