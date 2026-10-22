package com.linagora.calendar.e2e.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * The "Filters" panel of the menubar search: the calendars to search in, the keywords, the
 * organizers and the participants.
 *
 * <p>The panel is a popover over the search results: {@link #search()} closes it and runs the
 * search, whose results the {@link CalendarPage} reads as usual.
 */
public class SearchFilters {
    private final Page page;

    SearchFilters(Page page) {
        this.page = page;
    }

    SearchFilters open() {
        page.getByLabel("Filters").click();
        panel().waitFor();
        return this;
    }

    private Locator panel() {
        return page.locator(".MuiPopover-paper")
            .filter(new Locator.FilterOptions().setHas(page.getByText("Search in")))
            .last();
    }

    private Locator combobox(String name) {
        return panel().getByRole(AriaRole.COMBOBOX,
            new Locator.GetByRoleOptions().setName(name).setExact(true));
    }

    /** Picks the calendars to search in: "All calendars", "My calendars", or a calendar name. */
    public SearchFilters searchIn(String option) {
        combobox("Search in").click();
        option(Pattern.compile("^\\s*" + literal(option) + "\\s*$")).click();
        page.getByRole(AriaRole.LISTBOX).waitFor(new Locator.WaitForOptions()
            .setState(com.microsoft.playwright.options.WaitForSelectorState.DETACHED));
        return this;
    }

    public SearchFilters organizer(String email) {
        return pickPerson("Organizers", email);
    }

    public SearchFilters participant(String email) {
        return pickPerson("Participants", email);
    }

    /** Takes a person off the organizers or the participants, by the delete control of their chip. */
    public SearchFilters remove(String email) {
        panel().locator(".MuiChip-root")
            .filter(new Locator.FilterOptions().setHasText(email))
            .first()
            .locator("svg, button")
            .last()
            .click();
        page.waitForTimeout(400);
        return this;
    }

    /**
     * Types the address and picks the matching suggestion. Typed rather than filled: the picker
     * is a controlled MUI autocomplete, and a fill() does not always reach its filter.
     */
    private SearchFilters pickPerson(String field, String email) {
        Locator input = combobox(field);
        input.click();
        input.pressSequentially(email, new Locator.PressSequentiallyOptions().setDelay(30));
        option(Pattern.compile(literal(email))).click();
        panel().locator(".MuiChip-root")
            .filter(new Locator.FilterOptions().setHasText(email))
            .first()
            .waitFor();
        return this;
    }

    private Locator option(Pattern name) {
        return page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(name)).first();
    }

    /** Escapes a text for a pattern Playwright hands to the browser, which knows no \Q...\E. */
    private static String literal(String text) {
        return text.replaceAll("[\\\\^$.|?*+()\\[\\]{}]", "\\\\$0");
    }

    /** Runs the search with these filters. */
    public void search() {
        panel().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Search").setExact(true))
            .click();
        panel().waitFor(new Locator.WaitForOptions()
            .setState(com.microsoft.playwright.options.WaitForSelectorState.DETACHED));
        page.waitForTimeout(1500);
    }
}
