import { useMemo } from 'react'
import moment from 'moment-timezone'
import { CALENDAR_VIEWS } from '../utils/constants'
import { EventInput } from '@fullcalendar/core'

export const useHiddenDays = (
  hideWorkingDays: boolean,
  workingDays: number[] | undefined
): number[] => {
  return useMemo(() => {
    if (!hideWorkingDays || !workingDays || workingDays.length === 0) return []
    const validWorkingDays = workingDays.filter(d => d >= 0 && d <= 6)
    if (validWorkingDays.length === 0) return []
    return [0, 1, 2, 3, 4, 5, 6].filter(d => !validWorkingDays.includes(d))
  }, [hideWorkingDays, workingDays])
}

export const useFilteredCalendarEvents = (
  fullCalendarEvents: EventInput[],
  bookingListEvents: EventInput[],
  currentView: string,
  timezone: string
): EventInput[] => {
  return useMemo(() => {
    if (currentView === CALENDAR_VIEWS.dayGridMonth) {
      return [...fullCalendarEvents, ...bookingListEvents]
    }

    if (currentView !== CALENDAR_VIEWS.listWeek) {
      return fullCalendarEvents
    }

    const now = moment().tz(timezone)
    const startOfToday = now.clone().startOf('day')

    return fullCalendarEvents.filter(event => {
      if (!event.start) return false

      const eventEnd = event.end
        ? moment.tz(event.end as moment.MomentInput, timezone)
        : moment.tz(event.start as moment.MomentInput, timezone)
      return eventEnd.isSameOrAfter(startOfToday, 'day')
    })
  }, [fullCalendarEvents, bookingListEvents, currentView, timezone])
}
