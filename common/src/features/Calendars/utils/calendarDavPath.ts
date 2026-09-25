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

/**
 * Encodes a value, typically an event UID, as a single segment of a DAV path.
 *
 * The UID of an event comes from whoever wrote it, the sender of an
 * invitation included, and may hold "/", "?", "#" or "%" which would change
 * the path it is put in. Encodes like Sabre's encodePath, which writes the
 * hrefs the server hands back, so that the path of an ordinary UID ("@" and
 * ":" included) stays the very string the server uses.
 */
export function encodeDavSegment(value: string): string {
  return encodeURIComponent(value)
    .replace(/[!*']/g, c => `%${c.charCodeAt(0).toString(16).toUpperCase()}`)
    .replace(/%40/g, '@')
    .replace(/%3A/gi, ':')
}

/** Where a new event of that calendar is written, see {@link calendarDavPath}. */
export function eventDavPath(
  calendar: Pick<Calendar, 'id' | 'link' | 'delegated'>,
  eventUid: string
): string {
  return `${calendarDavPath(calendar)}/${encodeDavSegment(eventUid)}.ics`
}
