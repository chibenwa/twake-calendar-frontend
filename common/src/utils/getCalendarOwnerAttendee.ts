import { userAttendee } from '@common/features/User/models/attendee'
import { Calendar } from '@common/types/CalendarTypes'
import { normalizeIdentity } from '@common/utils/normalizeIdentity'

/**
 * The attendee whose answer the calendar displays.
 *
 * Every member of a team calendar counts as one of its owners, so matching
 * owners against attendees would pick whichever member is listed first. The
 * team organizes through one of its members: when there is an organizer, the
 * event of the team stands as that member answered.
 */
export function getCalendarOwnerAttendee(
  calendar: Calendar | undefined,
  attendees: userAttendee[] = [],
  organizer?: { cal_address?: string }
): userAttendee | undefined {
  if (calendar?.owner?.teamCalendar && organizer?.cal_address) {
    const organizerIdentity = normalizeIdentity(organizer.cal_address)
    return attendees.find(
      att => normalizeIdentity(att.cal_address) === organizerIdentity
    )
  }

  const ownerEmails = new Set(
    (calendar?.owner?.emails ?? []).map(email => normalizeIdentity(email))
  )
  return attendees.find(att =>
    ownerEmails.has(normalizeIdentity(att.cal_address))
  )
}
