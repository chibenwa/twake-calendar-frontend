package com.linagora.calendar.e2e.pages;

import java.time.LocalDate;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** The dialog a guest uses to propose another time for an event to its organizer. */
public class ProposalDialog {
    private final Page page;

    ProposalDialog(Page page) {
        this.page = page;
    }

    ProposalDialog waitUntilOpen() {
        sendButton().waitFor();
        return this;
    }

    private Locator sendButton() {
        return page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Send proposal").setExact(true)).last();
    }

    /** The date fields are read only, so this drives the picker the way a user would. */
    public ProposalDialog startDate(LocalDate date) {
        DatePickerField.pick(page, page.getByTestId("start-date-input").last(), date);
        return this;
    }

    public ProposalDialog send() {
        sendButton().click();
        return this;
    }

    /** Waits for the dialog to say something, and returns what it says. */
    public String awaitMessage(String expected) {
        page.getByText(expected).last().waitFor();
        return page.locator("[role=dialog]").last().innerText();
    }

    public boolean isOpen() {
        return sendButton().isVisible();
    }
}
