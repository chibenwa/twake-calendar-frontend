import { userAttendee } from '@common/features/User/models/attendee'
import { Calendar } from '@common/types/CalendarTypes'
import { normalizeIdentity } from '@common/utils/normalizeIdentity'

/**
 * The attendee whose answer the calendar displays.
 *
 * Every member of a team calendar counts as one of its owners, so matching
 * owners against attendees would pick whichever member is listed first. The
 * team organizes through one of its members: when that member organizes, the
 * event of the team stands as they answered, whoever else is invited, team
 * members or external participants alike.
 */
export function getCalendarOwnerAttendee(
  calendar: Calendar | undefined,
  attendees: userAttendee[] = [],
  organizer?: { cal_address?: string }
): userAttendee | undefined {
  const ownerIdentities = new Set(
    (calendar?.owner?.emails ?? []).map(email => normalizeIdentity(email))
  )
  const organizerIdentity = normalizeIdentity(organizer?.cal_address)
  const isTeamOrganized =
    calendar?.owner?.teamCalendar && ownerIdentities.has(organizerIdentity)

  if (isTeamOrganized) {
    return attendees.find(
      att => normalizeIdentity(att.cal_address) === organizerIdentity
    )
  }

  return attendees.find(att =>
    ownerIdentities.has(normalizeIdentity(att.cal_address))
  )
}
