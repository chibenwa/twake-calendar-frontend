import { userOrganiser } from '@common/features/User/userDataTypes'
import { Calendar } from '@common/types/CalendarTypes'
import { makeDisplayName } from '@common/utils/makeDisplayName'
import { useMemo } from 'react'
import { normalizeIdentity } from '@common/utils/normalizeIdentity'

/**
 * Calculates the event organizer based on delegated calendar status.
 * Team calendars have no mailbox of their own: the current user organizes.
 */
function getDelegatedOrganizer(
  selectedCalendar: Calendar | undefined,
  userOrganizer: userOrganiser
): userOrganiser {
  if (
    !selectedCalendar?.delegated ||
    !selectedCalendar?.owner ||
    selectedCalendar.owner.teamCalendar
  ) {
    return userOrganizer
  }

  const { owner } = selectedCalendar
  const calAddress = owner.emails?.[0] ?? ''
  const displayName = makeDisplayName(selectedCalendar) ?? calAddress

  return new userOrganiser({
    cn: displayName,
    cal_address: calAddress
  })
}

/**
 * Determines if the current organizer is the organizer of the specified event.
 */
function checkIsOrganizer(
  selectedCalendar: Calendar | undefined,
  eventId: string | null | undefined,
  organizerAddress?: string
): boolean {
  if (!eventId || !selectedCalendar || !organizerAddress) return false

  const eventOrganizerAddress =
    selectedCalendar.events?.[eventId]?.organizer?.cal_address

  return (
    normalizeIdentity(eventOrganizerAddress) ===
    normalizeIdentity(organizerAddress)
  )
}

// Update event organizer accordingly to selected calendar's delegated status
export function useEventOrganizer({
  calendarid,
  eventId,
  calList,
  userOrganizer
}: {
  calendarid: string
  eventId: string | null | undefined
  calList: Record<string, Calendar>
  userOrganizer: userOrganiser
}): {
  organizer: userOrganiser
  selectedCalendar: Calendar
  isOrganizer: boolean
} {
  const selectedCalendar = useMemo(
    () => calList?.[calendarid],
    [calList, calendarid]
  )

  const organizer = useMemo(
    () => getDelegatedOrganizer(selectedCalendar, userOrganizer),
    [selectedCalendar, userOrganizer]
  )

  const isOrganizer = useMemo(
    () => checkIsOrganizer(selectedCalendar, eventId, organizer?.cal_address),
    [selectedCalendar, eventId, organizer?.cal_address]
  )

  return { organizer, selectedCalendar, isOrganizer }
}
