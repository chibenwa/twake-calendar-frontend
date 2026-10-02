package com.linagora.calendar.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.linagora.calendar.e2e.TwakeCalendarE2ETest;
import com.linagora.calendar.e2e.backend.CalendarProbe;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.backend.E2EUserFactory;
import com.linagora.calendar.e2e.backend.Ical;
import com.linagora.calendar.e2e.docker.E2EClock;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/**
 * Search regressions: the event search of the menubar, and the quick search of the sidebar,
 * which looks for the calendars of other people.
 *
 * <p>The event search is otherwise covered by `SearchTest`; only the regression of searching
 * twice in a row lives here. The quick search lives behind "Add shared calendar", offers the
 * people of the domain, and then lists whichever of their calendars are publicly available.
 *
 * <p>Both quick search scenarios are about a request racing another. The component keeps a
 * sequence number and drops the answers to keystrokes that have since been replaced, which is
 * what makes a slow answer to an old keyword harmless.
 */
class PastSearchTest extends TwakeCalendarE2ETest {

    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Asserts the search results show the expected event, and none of the others. */
    private static void assertResultsShowOnly(Page page, String expected, List<String> others) {
        PlaywrightAssertions.assertThat(page.getByText(expected).first())
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(15_000));
        others.forEach(other -> PlaywrightAssertions.assertThat(page.getByText(other))
            .hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(15_000)));
    }

    @Test
    @DisplayName("PAST-38 (#998, #1459) Searching events again sends the new keyword, not the previous one")
    void searchingEventsAgainSendsTheNewKeyword(Page page, E2EUser user, CalendarProbe probe) {
        // a single word each, shared by nobody else, so that each search matches one event
        String alpha = unique("alpha");
        String beta = unique("beta");
        String gamma = unique("gamma");
        // a word they all share, to wait for the indexing without searching their own words
        String batch = unique("batch");
        // two months ahead, out of the grid on screen: only the search results can show them
        LocalDate day = E2EClock.today().plusMonths(2);
        List<String> titles = Stream.of(alpha, beta, gamma)
            .map(word -> word + " " + batch)
            .toList();
        titles.forEach(title -> {
            String uid = UUID.randomUUID().toString();
            probe.putEvent(user, uid, Ical.event(uid, title, day, 10));
        });
        CalendarPage calendar = LoginPage.loginAs(page, user);
        titles.forEach(title -> calendar.searchUntil(batch, title));

        // one search each, no retry: a retry of the same keyword would hide a search lagging
        // one step behind the field
        Stream.of(alpha, beta, gamma).forEach(word -> {
            calendar.search(word);
            assertResultsShowOnly(page, word + " " + batch, titles.stream()
                .filter(title -> !title.startsWith(word))
                .toList());
        });
    }

    /** The sidebar rows, which is where a calendar picked in that dialog would land. */
    private List<String> sidebarRows(Page page) {
        @SuppressWarnings("unchecked")
        List<String> rows = (List<String>) page.evaluate(
            "() => Array.from(document.querySelectorAll('li')).map(row => row.innerText.trim())");
        return rows;
    }

    @Test
    @DisplayName("PAST-51 A second keyword in the quick search replaces the first, it does not add to it")
    void aSecondKeywordReplacesTheFirst(Page page, E2EUser user, E2EUserFactory users) {
        E2EUser first = users.newUser("alpha");
        E2EUser second = users.newUser("beta");
        CalendarPage calendar = LoginPage.loginAs(page, user);

        calendar.browseOtherCalendars();
        assertThat(calendar.searchOtherCalendars(first.email()))
            .anyMatch(option -> option.contains(first.email()));
        List<String> afterSecond = calendar.searchOtherCalendars(second.email());

        assertThat(afterSecond)
            .as("the answer on screen has to be the one to the keyword now in the field")
            .anyMatch(option -> option.contains(second.email()));
        assertThat(afterSecond)
            .as("and the previous keyword's answer has to be gone, not merely pushed down")
            .noneMatch(option -> option.contains(first.email()));
    }

    @Test
    @DisplayName("PAST-40 (#271) A search called off leaves no calendar behind")
    void aSearchCalledOffLeavesNothingBehind(Page page, E2EUser user, E2EUserFactory users) {
        E2EUser other = users.newUser("other");
        CalendarPage calendar = LoginPage.loginAs(page, user);
        // the sidebar fills in after the grid: compare against it once it holds the calendar
        calendar.calendarCheckbox("My calendar").waitFor(
            new com.microsoft.playwright.Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
        List<String> before = sidebarRows(page);

        calendar.browseOtherCalendars();
        page.getByPlaceholder("Start typing a name or email")
            .pressSequentially(other.email(),
                new com.microsoft.playwright.Locator.PressSequentiallyOptions().setDelay(10));
        // called off while the answer is still on its way
        calendar.cancelBrowsing();
        page.waitForTimeout(6000);

        assertThat(sidebarRows(page))
            .as("a search nobody went through with must not leave a calendar in the sidebar")
            .isEqualTo(before);
    }

    @Test
    @DisplayName("Picking somebody with nothing published says so, rather than staying blank")
    void somebodyWithNothingPublishedSaysSo(Page page, E2EUser user, E2EUserFactory users) {
        E2EUser other = users.newUser("other");
        CalendarPage calendar = LoginPage.loginAs(page, user);

        calendar.browseOtherCalendars();
        String answer = calendar.pickInOtherCalendars(other.email());

        assertThat(answer)
            .as("picking somebody has to say what came of it, even when the answer is nothing")
            .contains("No publicly available calendars");
    }
}
