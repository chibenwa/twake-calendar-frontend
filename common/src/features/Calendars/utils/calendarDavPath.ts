import { Calendar } from '@common/types/CalendarTypes'

/**
 * The DAV path (`/calendars/<home>/<calendar>`, without `.json`) through which
 * the current user reads the content of a calendar.
 *
 * A calendar shared with the user is not read from its owner's node: sharing
 * creates a separate instance of it in the sharee's home, and only that
 * instance carries the sharee's rights. The owner's node (`calendar.id`) stays
 * the identity of the calendar, but talking to it gets a 403.
 */
export function calendarDavPath(
  calendar: Pick<Calendar, 'id' | 'link' | 'delegated'>
): string {
  if (calendar.delegated && calendar.link) {
    return calendar.link.replace(/\.json$/, '')
  }
  return `/calendars/${calendar.id}`
}

/** Where a new event of that calendar is written, see {@link calendarDavPath}. */
export function eventDavPath(
  calendar: Pick<Calendar, 'id' | 'link' | 'delegated'>,
  eventUid: string
): string {
  return `${calendarDavPath(calendar)}/${eventUid}.ics`
}
