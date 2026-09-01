package com.linagora.calendar.e2e.tests;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.linagora.calendar.e2e.TwakeCalendarE2ETest;
import com.linagora.calendar.e2e.backend.CalendarProbe;
import com.linagora.calendar.e2e.backend.E2EUser;
import com.linagora.calendar.e2e.pages.CalendarPage;
import com.linagora.calendar.e2e.pages.LoginPage;
import com.microsoft.playwright.Page;

public class ExploreDndTest extends TwakeCalendarE2ETest {

    @Test
    void exploreDrag(Page page, E2EUser user, CalendarProbe probe) {
        CalendarPage calendar = LoginPage.loginAs(page, user);
        calendar.createEvent().title("Draggable").expand()
            .startTime("09:00").endTime("10:00").save();
        calendar.eventCard("Draggable").first().waitFor();

        System.out.println("[g] viewport=" + page.viewportSize().width + "x" + page.viewportSize().height);
        System.out.println("[g] card box=" + calendar.eventCard("Draggable").first().boundingBox());
        System.out.println("[g] card html=" + page.evaluate("""
            () => { const e = document.querySelector('[data-testid^=event-card]');
                    return e ? e.outerHTML.slice(0, 200) + ' ||PARENT|| '
                             + e.parentElement.outerHTML.slice(0, 160) : 'none'; }"""));
        System.out.println("[g] slots present=" + page.evaluate("""
            () => Array.from(document.querySelectorAll('.fc-timegrid-slot[data-time]'))
              .map(e => e.getAttribute('data-time') + '@' + Math.round(e.getBoundingClientRect().top))
              .slice(0, 60)"""));
        System.out.println("[g] scroller=" + page.evaluate("""
            () => { const s = document.querySelector('.fc-scroller-liquid-absolute')
                            || document.querySelector('.fc-timegrid-body')?.parentElement;
                    return s ? s.scrollTop + '/' + s.scrollHeight + ' clientH=' + s.clientHeight : 'none'; }"""));

        String before = probe.singleEvent(user).replaceAll("(?s).*(DTSTART[^\\r\\n]*).*", "$1");
        System.out.println("[g] before=" + before);

        calendar.dragEventToSlot("Draggable", LocalDate.now(), "14:00:00");
        page.waitForTimeout(4000);
        String after = probe.singleEvent(user).replaceAll("(?s).*(DTSTART[^\\r\\n]*).*", "$1");
        System.out.println("[g] after=" + after);
    }
}
