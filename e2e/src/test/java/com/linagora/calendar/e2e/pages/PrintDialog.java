package com.linagora.calendar.e2e.pages;

import java.time.LocalDate;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * The printable schedule dialog, reached from the overflow menu of a calendar.
 *
 * <p>Printing opens the rendered schedule in a window of its own, so {@link #print()} hands back
 * that window: what a user ends up with on paper is what is asserted, not the dialog that asked
 * for it.
 */
public class PrintDialog {
    private final Page page;

    PrintDialog(Page page) {
        this.page = page;
    }

    PrintDialog waitUntilOpen() {
        dialog().getByText("One page per").waitFor();
        return this;
    }

    private Locator dialog() {
        return page.locator("[role=dialog]").last();
    }

    private Locator button(String name) {
        return dialog().getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(name).setExact(true));
    }

    public String text() {
        return dialog().innerText();
    }

    /** One page per Day, Week or Month. */
    public PrintDialog scale(String label) {
        button(label).click();
        return this;
    }

    /** Grid or Schedule. */
    public PrintDialog layout(String label) {
        button(label).click();
        return this;
    }

    public PrintDialog thisWeek() {
        button("This week").click();
        return this;
    }

    public PrintDialog thisMonth() {
        button("This month").click();
        return this;
    }

    public String startDate() {
        return dialog().getByLabel("Start date").inputValue();
    }

    public String endDate() {
        return dialog().getByLabel("End date").inputValue();
    }

    /** The date fields are read only by design, so this drives their picker. */
    public PrintDialog startDate(LocalDate date) {
        DatePickerField.pick(page, dialog().getByLabel("Start date"), date);
        return this;
    }

    public PrintDialog endDate(LocalDate date) {
        DatePickerField.pick(page, dialog().getByLabel("End date"), date);
        return this;
    }

    /**
     * Adds a second calendar to the printout. The button does not offer the calendars itself:
     * it adds an empty picker row, which is what has to be opened and chosen from.
     */
    public PrintDialog addCalendar(String name) {
        button("Add a calendar").click();
        Locator picker = dialog().getByText("Select a calendar");
        picker.waitFor();
        picker.click();
        page.locator("li[role=option]").first().waitFor();
        page.locator("li[role=option]")
            .filter(new Locator.FilterOptions().setHasText(name)).first().click();
        return this;
    }

    /** Prints, and hands back the window the schedule was rendered into. */
    public Page print() {
        Page printed = page.waitForPopup(() -> button("Print").click());
        printed.waitForLoadState();
        // the window opens empty and is filled in afterwards: reading it too early reads nothing
        printed.waitForFunction("() => (document.body.innerText || '').trim().length > 0",
            null, new Page.WaitForFunctionOptions().setTimeout(30_000));
        return printed;
    }

    /** Clicks Print without expecting a window: for the cases the dialog must refuse. */
    public PrintDialog printExpectingRefusal() {
        button("Print").click();
        return this;
    }

    /** Makes the browser refuse the window, the way a pop-up blocker does. */
    public PrintDialog withPopupsBlocked() {
        page.evaluate("() => { window.open = () => null; }");
        return this;
    }

    public boolean isOpen() {
        return dialog().getByText("One page per").count() > 0;
    }

    public void close() {
        button("Cancel").click();
    }
}
