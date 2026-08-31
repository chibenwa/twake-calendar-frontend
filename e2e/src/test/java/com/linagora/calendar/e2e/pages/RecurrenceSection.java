package com.linagora.calendar.e2e.pages;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * The repeat panel of the expanded event form: interval, frequency, weekdays and the three
 * ways a series can end.
 */
public class RecurrenceSection {
    public static final String DAILY = "Day(s)";
    public static final String WEEKLY = "Week(s)";
    public static final String MONTHLY = "Month(s)";
    public static final String YEARLY = "Year(s)";

    private static final Pattern ANY_FREQUENCY = Pattern.compile("Day\\(s\\)|Week\\(s\\)|Month\\(s\\)|Year\\(s\\)");

    private final Page page;

    RecurrenceSection(Page page) {
        this.page = page;
    }

    public RecurrenceSection every(int interval) {
        intervalInput().fill(String.valueOf(interval));
        return this;
    }

    public Locator intervalInput() {
        return page.getByTestId("repeat-interval");
    }

    /** One of {@link #DAILY}, {@link #WEEKLY}, {@link #MONTHLY}, {@link #YEARLY}. */
    public RecurrenceSection frequency(String frequency) {
        frequencySelect().click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(frequency).setExact(true)).click();
        return this;
    }

    public String frequency() {
        return frequencySelect().innerText().trim();
    }

    private Locator frequencySelect() {
        return page.getByRole(AriaRole.COMBOBOX)
            .filter(new Locator.FilterOptions().setHasText(ANY_FREQUENCY))
            .last();
    }

    /** Weekdays as iCalendar codes: MO, TU, WE, TH, FR, SA, SU. */
    public RecurrenceSection onWeekdays(String... weekdays) {
        List.of(weekdays).forEach(day -> weekday(day).click());
        return this;
    }

    public Locator weekday(String icalDay) {
        return page.getByLabel(icalDay, new Page.GetByLabelOptions().setExact(true));
    }

    public boolean isWeekdaySelected(String icalDay) {
        // MUI marks the pressed toggle with a `Mui-selected` class
        return weekday(icalDay).getAttribute("class").contains("Mui-selected");
    }

    /** Never ends. */
    public RecurrenceSection endsNever() {
        page.locator("input[type=radio][value=never]").check();
        return this;
    }

    public RecurrenceSection endsAfter(int occurrences) {
        page.locator("input[type=radio][value=after]").check();
        page.getByTestId("occurrences-input").fill(String.valueOf(occurrences));
        return this;
    }

    /**
     * Ends on the given date. The field is read only by design, so this drives the date picker
     * the way a user would: open it, walk to the right month, click the day.
     */
    public RecurrenceSection endsOn(LocalDate date) {
        page.locator("input[type=radio][value=on]").check();
        endDateInput().click();

        DatePickerField.pick(page, endDateInput(), date);
        return this;
    }

    public Locator endDateInput() {
        return page.getByTestId("event-repeat-end-date");
    }

    public Locator occurrencesInput() {
        return page.getByTestId("occurrences-input");
    }

    public boolean isVisible() {
        return intervalInput().count() > 0 && intervalInput().isVisible();
    }
}
