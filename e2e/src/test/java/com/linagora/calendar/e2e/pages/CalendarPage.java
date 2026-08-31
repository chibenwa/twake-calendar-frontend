package com.linagora.calendar.e2e.pages;

import java.util.List;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * The main authenticated screen: menubar, sidebar and the FullCalendar grid.
 *
 * <p>Locators go through accessible names rather than CSS classes: the app is built on MUI and
 * emotion, its class names are generated and change on every dependency bump, whereas an
 * aria-label change is a user visible change worth failing on.
 */
public class CalendarPage {
    /** FullCalendar renders one such element per event, carrying the event uid. */
    public static final String EVENT_CARD = "[data-testid^=event-card]";

    private final Page page;

    public CalendarPage(Page page) {
        this.page = page;
    }

    public Page page() {
        return page;
    }

    public CalendarPage waitUntilLoaded() {
        page.waitForURL("**/calendar**");
        page.locator(".fc-view-harness").waitFor(
            new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        return this;
    }

    // ------------------------------------------------------------------ events

    /** Opens the creation modal, collapsed. Call {@link EventFormModal#expand()} for the rest. */
    public EventFormModal createEvent() {
        page.getByLabel("Create a new event").click();
        return new EventFormModal(page).waitUntilOpen();
    }

    /** Creates a plain event with only a title, and returns once it shows up in the grid. */
    public CalendarPage createEvent(String title) {
        createEvent().title(title).save();
        eventCard(title).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
        return this;
    }

    public Locator eventCards() {
        return page.locator(EVENT_CARD);
    }

    public Locator eventCard(String title) {
        return page.locator(EVENT_CARD).filter(new Locator.FilterOptions().setHasText(title));
    }

    public List<String> eventTitles() {
        return page.locator(EVENT_CARD).allInnerTexts().stream()
            .map(text -> text.split("\n")[0].trim())
            .toList();
    }

    public EventPreviewPopover openEvent(String title) {
        eventCard(title).first().click();
        return new EventPreviewPopover(page).waitUntilOpen();
    }

    /** All day events live in their own row, above the time grid. */
    public Locator allDayEventCards() {
        return page.locator(".fc-daygrid-body " + EVENT_CARD);
    }

    // -------------------------------------------------------------- navigation

    public CalendarPage next() {
        // exact: the mini calendar has its own "Next month" button
        page.getByLabel("Next", new Page.GetByLabelOptions().setExact(true)).click();
        return this;
    }

    public CalendarPage previous() {
        page.getByLabel("Previous", new Page.GetByLabelOptions().setExact(true)).click();
        return this;
    }

    public CalendarPage today() {
        page.getByLabel("Today", new Page.GetByLabelOptions().setExact(true)).click();
        return this;
    }

    public CalendarPage refresh() {
        page.getByLabel("Refresh").click();
        return this;
    }

    /** One of Month, Week, Day, Schedule. */
    public CalendarPage switchView(String view) {
        page.getByLabel("Select view").click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(view).setExact(true)).click();
        return this;
    }

    /** FullCalendar view class, e.g. {@code fc-dayGridMonth-view}. */
    public String currentViewClass() {
        return String.valueOf(page.evaluate(
            "() => document.querySelector('.fc-view-harness').firstElementChild.className"));
    }

    /** The date range currently displayed, as the column headers spell it. */
    public List<String> visibleDayHeaders() {
        return page.locator(".fc-col-header-cell-cushion").allInnerTexts().stream()
            .map(header -> header.replace("\n", " ").trim())
            .toList();
    }

    public Locator weekNumber() {
        return page.locator(".fc-timegrid-axis-cushion");
    }

    // ------------------------------------------------------------------- menus

    public SettingsPage openSettings() {
        page.getByLabel("User profile").click();
        page.getByText("Settings").last().click();
        return new SettingsPage(page).waitUntilOpen();
    }

    public void logout() {
        page.getByLabel("User profile").click();
        page.getByText("Logout").last().click();
    }

    public String loggedInUser() {
        page.getByLabel("User profile").click();
        String menu = page.locator(".MuiPopover-root").innerText();
        page.keyboard().press("Escape");
        return menu;
    }

    public CalendarPage search(String keywords) {
        page.getByLabel("Search for events or calendars").click();
        page.locator("input[placeholder='Search']").fill(keywords);
        page.keyboard().press("Enter");
        return this;
    }
}
