import { DateSelectArg } from '@fullcalendar/core'
import moment from 'moment-timezone'
import { CALENDAR_VIEWS } from './constants'

/**
 * The create button of the day view shall seed the event on the day being
 * looked at rather than on today, keeping the usual "next full hour, one hour
 * long" slot. Other views, and the day view showing today, keep the default
 * (null) so that the form falls back on its own "next hour of today".
 */
export function buildCreateEventRange(
  currentView: string,
  displayedDate: Date | undefined,
  timezone: string,
  now: Date = new Date()
): DateSelectArg | null {
  if (currentView !== CALENDAR_VIEWS.timeGridDay || !displayedDate) {
    return null
  }

  const displayedDay = moment.tz(displayedDate, timezone).format('YYYY-MM-DD')
  const nowInZone = moment.tz(now, timezone)
  if (displayedDay === nowInZone.format('YYYY-MM-DD')) {
    return null
  }

  const nextHour = nowInZone.clone().add(1, 'hour').startOf('hour')
  const start = moment.tz(
    `${displayedDay}T${nextHour.format('HH:mm')}`,
    timezone
  )
  const end = start.clone().add(1, 'hour')

  return {
    start: start.toDate(),
    end: end.toDate(),
    startStr: start.format(),
    endStr: end.format(),
    allDay: false
  } as DateSelectArg
}
