package com.linagora.calendar.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * The create / update event modal.
 *
 * <p>It opens collapsed, showing only a handful of fields; {@link #expand()} reveals the dates,
 * description, location, notification and visibility ones.
 */
public class EventFormModal {
    private final Page page;

    public EventFormModal(Page page) {
        this.page = page;
    }

    EventFormModal waitUntilOpen() {
        titleInput().waitFor();
        return this;
    }

    private Locator dialog() {
        return page.locator("[role=dialog]").last();
    }

    private Locator titleInput() {
        return page.getByLabel("Title").first();
    }

    public EventFormModal title(String title) {
        titleInput().fill(title);
        return this;
    }

    public String title() {
        return titleInput().inputValue();
    }

    /** Reveals every field of the form. */
    public EventFormModal expand() {
        page.getByLabel("expand").click();
        page.getByLabel("Start Date").waitFor();
        return this;
    }

    public EventFormModal startTime(String hhmm) {
        page.getByLabel("Start Time").fill(hhmm);
        return this;
    }

    public EventFormModal endTime(String hhmm) {
        page.getByLabel("End Time").fill(hhmm);
        return this;
    }

    public String startTime() {
        return page.getByLabel("Start Time").inputValue();
    }

    public EventFormModal allDay() {
        page.getByLabel("All day").check();
        return this;
    }

    public EventFormModal description(String description) {
        page.getByLabel("Description").fill(description);
        return this;
    }

    public EventFormModal location(String location) {
        page.getByLabel("Location").fill(location);
        return this;
    }

    /** Types an email in the guest picker and validates it. */
    public EventFormModal addGuest(String email) {
        Locator guests = page.getByPlaceholder("Add guests");
        guests.fill(email);
        guests.press("Enter");
        return this;
    }

    public Locator saveButton() {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save").setExact(true)).last();
    }

    public void save() {
        saveButton().click();
        titleInput().waitFor(new Locator.WaitForOptions()
            .setState(com.microsoft.playwright.options.WaitForSelectorState.DETACHED));
    }

    /** Submits without waiting for the modal to close: for the cases where it must not. */
    public EventFormModal trySave() {
        saveButton().click();
        return this;
    }

    public void cancel() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cancel").setExact(true)).last().click();
    }

    public void close() {
        page.getByLabel("close").click();
    }

    public boolean isOpen() {
        return titleInput().count() > 0 && titleInput().isVisible();
    }

    public String text() {
        return dialog().innerText();
    }
}
