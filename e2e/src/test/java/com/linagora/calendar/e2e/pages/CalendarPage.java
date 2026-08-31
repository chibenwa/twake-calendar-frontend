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

    /** The grid column of a given day, to assert an event landed on the right one. */
    public Locator dayColumn(java.time.LocalDate day) {
        return page.locator("[data-date='" + day + "']");
    }

    /** The long date format the pickers display: "Monday, August 31, 2026". */
    public static String longDate(java.time.LocalDate day) {
        return day.format(java.time.format.DateTimeFormatter
            .ofPattern("EEEE, MMMM d, yyyy", java.util.Locale.ENGLISH));
    }

    /** The period the grid currently shows, as the menubar spells it out. */
    public String periodTitle() {
        return page.locator(".current-date-time").innerText().replace("\n", " ").trim();
    }

    public Locator menubar() {
        return page.locator(".menubar");
    }

    /** A day cell of the sidebar mini calendar. */
    public Locator miniCalendarDay(int dayOfMonth) {
        return page.locator("button.MuiPickerDay-root:not(.MuiPickerDay-dayOutsideMonth)")
            .filter(new Locator.FilterOptions()
                .setHasText(java.util.regex.Pattern.compile("^" + dayOfMonth + "$")))
            .first();
    }

    public String miniCalendarMonth() {
        return page.locator(".MuiPickersCalendarHeader-label").first().innerText().trim();
    }

    public CalendarPage miniCalendarNextMonth() {
        page.getByLabel("Next month").click();
        return this;
    }

    /** The expandable sidebar sections: My calendars, Other calendars, Resources... */
    public Locator sidebarSection(String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name));
    }

    public Locator weekNumber() {
        return page.locator(".fc-timegrid-axis-cushion");
    }

    // ------------------------------------------------------------------- menus

    /** The sidebar checkbox toggling the display of one calendar. */
    public Locator calendarCheckbox(String calendarName) {
        return page.locator("input[type=checkbox][aria-label='" + calendarName + "']");
    }

    /** Opens the "Add new calendar" dialog from the sidebar. */
    public CalendarModal addCalendar() {
        page.getByLabel("Add a new personal calendar").click();
        return new CalendarModal(page).waitUntilOpen();
    }

    /** The per calendar menu button of a sidebar row. */
    public Locator calendarMenu(String calendarName) {
        return page.locator("li:has(label[aria-label='" + calendarName + "']) button");
    }

    /** Removes a calendar through its sidebar menu, confirmation included. */
    public void deleteCalendar(String calendarName) {
        calendarMenu(calendarName).last().click();
        page.getByText("Remove", new Page.GetByTextOptions().setExact(true)).last().click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Remove").setExact(true))
            .last().click();
    }

    /**
     * Selects a time range in the week grid the way a user does, by dragging, which opens the
     * creation modal prefilled with the range.
     */
    public EventFormModal selectTimeRange(java.time.LocalDate day, String fromSlot, String toSlot) {
        var column = page.locator(".fc-timegrid-col[data-date='" + day + "']").last().boundingBox();
        var from = page.locator(".fc-timegrid-slot[data-time='" + fromSlot + "']").first().boundingBox();
        var to = page.locator(".fc-timegrid-slot[data-time='" + toSlot + "']").first().boundingBox();
        double x = column.x + column.width / 2;

        page.mouse().move(x, from.y + 2);
        page.mouse().down();
        page.mouse().move(x, to.y + to.height - 2, new com.microsoft.playwright.Mouse.MoveOptions().setSteps(12));
        page.mouse().up();
        return new EventFormModal(page).waitUntilOpen();
    }

    /** Clicks a day cell of the month grid, which starts an all day event on that day. */
    public EventFormModal selectMonthCell(java.time.LocalDate day) {
        page.locator(".fc-daygrid-day[data-date='" + day + "'] .fc-daygrid-day-frame").first()
            .click(new Locator.ClickOptions().setPosition(20, 40));
        return new EventFormModal(page).waitUntilOpen();
    }

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
