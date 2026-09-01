package com.linagora.calendar.e2e;

import org.junit.jupiter.api.extension.ExtendWith;

import com.linagora.calendar.e2e.docker.TwakeCalendarE2EExtension;

/**
 * Base class of every e2e test.
 *
 * <p>Subclasses get a {@code Page} on a fresh browser context, a dedicated {@code E2EUser} and,
 * on demand, a {@code CalendarProbe} to look at the backend, simply by declaring them as test
 * method parameters.
 *
 * <p>A scenario the suite cannot assert yet has no placeholder here: it stays an unticked line
 * in e2e.md with a note saying what stands in the way. The suite holds tests that run, nothing
 * else.
 */
@ExtendWith(TwakeCalendarE2EExtension.class)
public abstract class TwakeCalendarE2ETest {
}
