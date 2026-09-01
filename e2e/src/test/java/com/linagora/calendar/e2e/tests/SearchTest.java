package com.linagora.calendar.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.linagora.calendar.e2e.TwakeCalendarE2ETest;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.backend.E2EUserFactory;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.WaitForSelectorState;

/** Searching for events. */
class SearchTest extends TwakeCalendarE2ETest {

    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private void awaitAttached(Locator locator) {
        locator.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
    }

    @Test
    @DisplayName("SEARCH-01 Searching a keyword brings back the matching event")
    void searchingAKeywordFindsTheEvent(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = unique("Findable");
        calendar.createEvent(title);

        calendar.searchUntil(title, title);

        PlaywrightAssertions.assertThat(page.getByText(title).first()).isVisible();
    }

    @Test
    @DisplayName("SEARCH-02 A search with no match says so")
    void aSearchWithNoMatchSaysSo(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);

        calendar.search(unique("nothingmatchesthis"));

        PlaywrightAssertions.assertThat(page.getByText("No events found").first())
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30_000));
    }

    @Test
    @DisplayName("SEARCH-03 The search also covers the description")
    void theSearchCoversTheDescription(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = unique("Described");
        String keyword = unique("bythedescription");
        calendar.createEvent().title(title).expand().description(keyword).save();
        awaitAttached(calendar.eventCard(title));

        calendar.searchUntil(keyword, title);

        PlaywrightAssertions.assertThat(page.getByText(title).first()).isVisible();
    }

    @Test
    @DisplayName("SEARCH-04 The search also covers the location")
    void theSearchCoversTheLocation(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = unique("Located");
        String keyword = unique("bythelocation");
        calendar.createEvent().title(title).expand().location(keyword).save();
        awaitAttached(calendar.eventCard(title));

        calendar.searchUntil(keyword, title);

        PlaywrightAssertions.assertThat(page.getByText(title).first()).isVisible();
    }

    @Test
    @DisplayName("SEARCH-05 The My calendars filter narrows the search scope")
    void theCalendarFilterNarrowsTheScope(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = unique("Scoped");
        calendar.createEvent(title);
        calendar.searchUntil(title, title);

        page.getByLabel("Filters").click();
        page.waitForTimeout(1200);

        assertThat(page.locator("body").innerText())
            .as("the scope of a search must be visible and changeable")
            .contains("Search in");
    }

    @Test
    @DisplayName("SEARCH-06 The organizer filter is offered")
    void theOrganizerFilterIsOffered(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        calendar.createEvent(unique("Organised"));

        calendar.search("Organised");
        page.getByLabel("Filters").click();
        page.waitForTimeout(1200);

        assertThat(page.locator("body").innerText()).contains("Organizers");
    }

    @Test
    @DisplayName("SEARCH-07 The participant filter is offered")
    void theParticipantFilterIsOffered(Page page, E2EUser user, E2EUserFactory users) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        E2EUser guest = users.newUser();
        String title = unique("WithGuest");
        calendar.createEvent().title(title).addGuest(guest.email()).save();
        awaitAttached(calendar.eventCard(title));

        calendar.search(title);
        page.getByLabel("Filters").click();
        page.waitForTimeout(1200);

        assertThat(page.locator("body").innerText()).contains("Participants");
    }

    @Test
    @DisplayName("SEARCH-08 Clicking a result opens the event preview")
    void clickingAResultOpensThePreview(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        String title = unique("Clickable");
        calendar.createEvent().title(title).expand().location("Room 8").save();
        awaitAttached(calendar.eventCard(title));

        calendar.searchUntil(title, title);
        page.getByText(title).first().click();
        page.waitForTimeout(1500);

        assertThat(page.locator("body").innerText()).contains("Room 8");
    }

    @Test
    @DisplayName("SEARCH-10 An empty search invites the user to type keywords")
    void anEmptySearchInvitesKeywords(Page page, E2EUser user) {
        CalendarPage calendar = LoginPage.loginAs(page, user);

        calendar.search("");
        page.getByLabel("Filters").click();
        page.waitForTimeout(1200);

        assertThat(page.locator("body").innerText())
            .as("an empty query is not an error, it is an invitation")
            .containsIgnoringCase("keyword");
    }
}
