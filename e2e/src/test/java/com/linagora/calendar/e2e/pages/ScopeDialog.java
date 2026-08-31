package com.linagora.calendar.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * The "This event / All the events" dialog, shown whenever an action targets a recurring
 * event: editing, deleting, or answering an invitation.
 */
public class ScopeDialog {
    private final Page page;

    ScopeDialog(Page page) {
        this.page = page;
    }

    public static ScopeDialog waitFor(Page page) {
        ScopeDialog dialog = new ScopeDialog(page);
        dialog.thisEventRadio().waitFor();
        return dialog;
    }

    /** True when the dialog is on screen, without waiting for it. */
    public static boolean isShowing(Page page) {
        return new ScopeDialog(page).thisEventRadio().count() > 0;
    }

    public Locator thisEventRadio() {
        return page.locator("input[type=radio][value=solo]");
    }

    public Locator allEventsRadio() {
        return page.locator("input[type=radio][value=all]");
    }

    public String title() {
        return page.locator("[role=dialog]").last().locator("h2").innerText();
    }

    /** Applies the action to the clicked occurrence only. */
    public void thisEvent() {
        thisEventRadio().check();
        confirm();
    }

    /** Applies the action to the whole series. */
    public void allEvents() {
        allEventsRadio().check();
        confirm();
    }

    public void cancel() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cancel").setExact(true)).last().click();
        awaitClosed();
    }

    private void confirm() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Ok").setExact(true)).last().click();
        awaitClosed();
    }

    private void awaitClosed() {
        thisEventRadio().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }
}
