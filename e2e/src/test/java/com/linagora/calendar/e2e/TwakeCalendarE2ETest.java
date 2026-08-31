package com.linagora.calendar.e2e;

import org.junit.jupiter.api.extension.ExtendWith;

import com.linagora.calendar.e2e.docker.TwakeCalendarE2EExtension;

/**
 * Base class of every e2e test.
 *
 * <p>Subclasses get a {@code Page} on a fresh browser context, a dedicated {@code E2EUser} and,
 * on demand, a {@code CalendarProbe} to look at the backend, simply by declaring them as test
 * method parameters.
 */
@ExtendWith(TwakeCalendarE2EExtension.class)
public abstract class TwakeCalendarE2ETest {
}
