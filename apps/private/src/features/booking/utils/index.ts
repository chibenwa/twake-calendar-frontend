import { Resource } from '@common/components/Attendees/ResourceSearch'
import { Valarms } from '@common/types/Valarms'
import { VAlarm } from '@common/types/VAlarm'
import { userAttendee } from '@common/features/User/models/attendee'
import { expandHexColor } from '@common/utils/expandHexColor'
import {
  DayAvailability,
  TimeSlot
} from '../components/RegularHoursField/RegularHoursTypes'
import {
  AvailabilityRule,
  CreateBookingLinkRequest,
  UpdateBookingLinkRequest
} from '@common/features/booking/types/BookingTypes'

export const formatResourceIds = (selectedResources: Resource[]): string[] =>
  selectedResources
    .map(r => r.openpaasId || r.email || r.displayName)
    .filter((id): id is string => Boolean(id))

export const formatAlarms = (
  alarms: Valarms
): {
  period: string
  action: string
}[] =>
  alarms.getAlarms().map((a: VAlarm) => ({
    period: a.trigger.startsWith('-') ? a.trigger : `-${a.trigger}`,
    action: a.action || 'EMAIL'
  }))

export const formatExtraAttendees = (attendees: userAttendee[]): string[] =>
  attendees.map(a => a.openpaasId || '').filter(Boolean)

export const formatAvailabilityRules = (
  availabilityRules: DayAvailability[],
  timezone: string
): AvailabilityRule[] =>
  availabilityRules
    .filter(rule => rule.enabled)
    .flatMap(rule =>
      rule.slots.map((slot: TimeSlot) => ({
        type: 'weekly' as const,
        dayOfWeek: rule.dayOfWeek,
        start: slot.start,
        end: slot.end,
        timeZone: timezone
      }))
    )

export const buildBookingPayload = (data: {
  name: string
  duration: number
  calendarid: string
  active: boolean
  availabilityRules: DayAvailability[]
  timezone: string
  description: string
  color: string
  location: string
  eventClass: string
  busy: string
  resourceIds: string[]
  alarmList: { period: string; action: string }[]
  extraAttendeesList: string[]
}): CreateBookingLinkRequest => ({
  name: data.name || undefined,
  durationMinutes: data.duration,
  calendarUrl: `/calendars/${data.calendarid}`,
  active: data.active,
  autoAccept: false,
  availabilityRules: formatAvailabilityRules(
    data.availabilityRules,
    data.timezone
  ),
  description: data.description || undefined,
  color: expandHexColor(data.color),
  location: data.location.trim() || undefined,
  visibility: data.eventClass as 'PUBLIC' | 'PRIVATE' | 'CONFIDENTIAL',
  transparency:
    data.busy === 'TRANSPARENT'
      ? ('TRANSPARENT' as const)
      : ('OPAQUE' as const),
  resources: data.resourceIds.length > 0 ? data.resourceIds : undefined,
  alarm: data.alarmList.length > 0 ? data.alarmList : undefined,
  extraAttendees:
    data.extraAttendeesList.length > 0
      ? { and: data.extraAttendeesList.map(participant => ({ participant })) }
      : undefined
})

export const buildUpdateBookingPayload = (data: {
  name: string
  duration: number
  calendarid: string
  active: boolean
  availabilityRules: DayAvailability[]
  timezone: string
  description: string
  color: string
  location: string
  eventClass: string
  busy: string
  resourceIds: string[]
  alarmList: { period: string; action: string }[]
  extraAttendeesList: string[]
}): UpdateBookingLinkRequest => ({
  name: data.name || null,
  durationMinutes: data.duration,
  calendarUrl: `/calendars/${data.calendarid}`,
  active: data.active,
  availabilityRules: formatAvailabilityRules(
    data.availabilityRules,
    data.timezone
  ),
  description: data.description || null,
  color: expandHexColor(data.color),
  location: data.location.trim() || null,
  visibility:
    data.eventClass === 'PRIVATE' || data.eventClass === 'CONFIDENTIAL'
      ? ('PRIVATE' as const)
      : ('PUBLIC' as const),
  transparency:
    data.busy === 'TRANSPARENT'
      ? ('TRANSPARENT' as const)
      : ('OPAQUE' as const),
  resources: data.resourceIds.length > 0 ? data.resourceIds : null,
  alarm: data.alarmList.length > 0 ? data.alarmList : null,
  extraAttendees:
    data.extraAttendeesList.length > 0
      ? { and: data.extraAttendeesList.map(participant => ({ participant })) }
      : null
})
