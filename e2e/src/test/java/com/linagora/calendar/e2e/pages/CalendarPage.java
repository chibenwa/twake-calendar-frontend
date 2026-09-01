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

    /**
     * The days an event is rendered on, as ISO dates, read from the grid column each card sits
     * in. The natural way to assert what a recurrence rule actually produces.
     */
    @SuppressWarnings("unchecked")
    public List<String> eventDates(String title) {
        return (List<String>) page.evaluate(
            "(t) => Array.from(document.querySelectorAll('[data-testid^=event-card]'))"
            + ".filter(e => e.innerText.includes(t))"
            + ".map(e => { const c = e.closest('[data-date]'); return c ? c.getAttribute('data-date') : null; })"
            + ".filter(Boolean).sort()", title);
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

    /**
     * Walks the grid to the given month, reading where it actually is at every step rather than
     * counting from today: successive calls would otherwise navigate from the wrong origin.
     */
    public CalendarPage goToMonth(java.time.YearMonth month) {
        for (int guard = 0; guard < 60; guard++) {
            java.time.YearMonth displayed = displayedMonth();
            if (displayed.equals(month)) {
                return this;
            }
            if (displayed.isBefore(month)) {
                next();
            } else {
                previous();
            }
            page.waitForTimeout(150);
        }
        throw new AssertionError("Could not reach " + month + ", the grid shows " + periodTitle());
    }

    /** The month the menubar title names. */
    public java.time.YearMonth displayedMonth() {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("([A-Z][a-z]+)\\s+(\\d{4})").matcher(periodTitle());
        if (!matcher.find()) {
            throw new AssertionError("No month in the menubar title: " + periodTitle());
        }
        return java.time.YearMonth.parse(matcher.group(1) + " " + matcher.group(2),
            java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH));
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

    /** The first day the grid currently shows, read from the grid itself. */
    public java.time.LocalDate firstVisibleDate() {
        String date = String.valueOf(page.evaluate(
            "() => { const c = document.querySelector('[data-date]');"
            + " return c ? c.getAttribute('data-date') : null; }"));
        return java.time.LocalDate.parse(date);
    }

    /** Every day the grid currently shows. */
    @SuppressWarnings("unchecked")
    public List<String> visibleDates() {
        return (List<String>) page.evaluate(
            "() => Array.from(new Set(Array.from(document.querySelectorAll('[data-date]'))"
            + ".map(e => e.getAttribute('data-date')))).sort()");
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

    /**
     * Makes sure a calendar is ticked, so its events actually reach the grid. A freshly created
     * calendar is not always part of the selection.
     */
    public CalendarPage showCalendar(String calendarName) {
        Locator checkbox = calendarCheckbox(calendarName);
        checkbox.waitFor();
        if (!checkbox.isChecked()) {
            checkbox.check();
            page.waitForTimeout(800);
        }
        return this;
    }

    /** The per calendar menu button of a sidebar row. */
    public Locator calendarMenu(String calendarName) {
        return page.locator("li:has(label[aria-label='" + calendarName + "']) button");
    }

    /**
     * Opens the overflow menu of a sidebar calendar row. The button only shows on hover, so the
     * row is hovered first.
     */
    public CalendarPage openCalendarMenu(String calendarName) {
        Locator row = page.locator("li:has(label[aria-label='" + calendarName + "'])");
        row.hover();
        row.locator("button").last().click();
        page.locator("[role=menuitem]").first().waitFor();
        return this;
    }

    /** What the overflow menu of a calendar offers. */
    public List<String> calendarMenuEntries(String calendarName) {
        openCalendarMenu(calendarName);
        List<String> entries = page.locator("[role=menuitem]").allInnerTexts();
        page.keyboard().press("Escape");
        return entries;
    }

    /** Opens the settings dialog of a calendar, through Modify. */
    public CalendarModal modifyCalendar(String calendarName) {
        openCalendarMenu(calendarName);
        page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Modify")).click();
        return new CalendarModal(page).waitUntilOpen();
    }

    /** Deletes a calendar, going through the confirmation. */
    public void deleteCalendar(String calendarName) {
        openCalendarMenu(calendarName);
        page.getByRole(AriaRole.MENUITEM,
            new Page.GetByRoleOptions().setName(java.util.regex.Pattern.compile("Delete|Remove"))).click();
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(java.util.regex.Pattern.compile("^(Delete|Remove)$")))
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

    /** Searches for events. Opens the search bar first when it is not already showing. */
    public CalendarPage search(String keywords) {
        Locator input = page.getByPlaceholder("Search");
        if (input.count() == 0) {
            page.getByLabel("Search for events or calendars").click();
            input.first().waitFor();
        }
        input.first().fill(keywords);
        input.first().press("Enter");
        page.waitForTimeout(1500);
        return this;
    }

    /** Empties the search field, which takes the user back to the calendar. */
    public CalendarPage clearSearch() {
        Locator input = page.getByPlaceholder("Search");
        if (input.count() > 0) {
            input.first().fill("");
            input.first().press("Enter");
            page.waitForTimeout(1200);
        }
        if (page.locator(".fc-view-harness").count() == 0) {
            // emptying the field is not always enough: the logo takes the user home, which is
            // the gesture left once the search toggle has given way to the search bar
            page.getByLabel("Calendar", new Page.GetByLabelOptions().setExact(true)).first().click();
            page.locator(".fc-view-harness").waitFor();
        }
        return this;
    }

    /**
     * Searches until the expected text shows up. Events are indexed asynchronously, so a query
     * fired right after a creation legitimately comes back empty the first time.
     */
    public CalendarPage searchUntil(String keywords, String expected) {
        org.awaitility.Awaitility.await()
            .atMost(java.time.Duration.ofSeconds(60))
            .pollInterval(java.time.Duration.ofSeconds(3))
            .until(() -> {
                search(keywords);
                return page.getByText(expected).count() > 0;
            });
        return this;
    }

    public String searchResults() {
        return page.locator("body").innerText();
    }
}
