import React from 'react'
import moment from 'moment-timezone'
import {
  combineDateTime,
  splitDateTime
} from '@common/components/Event/utils/dateTimeHelpers'

const DATE_FORMAT = 'YYYY-MM-DD'
const TIME_FORMAT = 'HH:mm'
const MIDNIGHT = '00:00'
// Seconds are checked too: 00:00:30 is not an exclusive midnight end
const EXACT_MIDNIGHT = /^00:00(:00)?$/

function isExactMidnight(datetime: string): boolean {
  return EXACT_MIDNIGHT.test(datetime.split('T')[1] ?? '')
}

/**
 * A timed event ending at midnight does not occupy its end day: once converted
 * to all-day, its inclusive end date is the previous day, never before the start.
 */
export function toInclusiveAllDayEnd(start: string, end: string): string {
  const { date: startDateOnly } = splitDateTime(start)
  const { date: endDateOnly } = splitDateTime(end)
  if (!isExactMidnight(end) || endDateOnly <= startDateOnly) {
    return end
  }
  const previousDay = moment(endDateOnly, DATE_FORMAT)
    .subtract(1, 'day')
    .format(DATE_FORMAT)
  return combineDateTime(previousDay, MIDNIGHT)
}

/**
 * Parameters for all-day toggle hook
 */
export interface AllDayToggleParams {
  allday: boolean
  start: string
  end: string
  startDate: string
  startTime: string
  endDate: string
  endTime: string
  timezone?: string
  setStartTime: (time: string) => void
  setEndTime: (time: string) => void
  setStart: (start: string) => void
  setEnd: (end: string) => void
  setAllDay: (allday: boolean) => void
  onAllDayChange?: (allday: boolean, start: string, end: string) => void
}

/**
 * Handlers returned by all-day toggle hook
 */
export interface AllDayToggleHandlers {
  originalTimeRef: React.MutableRefObject<{
    start: string
    end: string
    endDate?: string
    fromAllDaySlot?: boolean
  } | null>
  handleAllDayToggle: () => void
}

/**
 * Custom hook for managing all-day toggle logic
 * Handles saving/restoring time values and endDate logic
 */
export function useAllDayToggle(
  params: AllDayToggleParams
): AllDayToggleHandlers {
  const {
    allday,
    start,
    end,
    startDate,
    startTime,
    endDate,
    endTime,
    timezone,
    setStartTime,
    setEndTime,
    setStart,
    setEnd,
    setAllDay,
    onAllDayChange
  } = params

  // Store original time before toggling to all-day
  const originalTimeRef = React.useRef<{
    start: string
    end: string
    endDate?: string
    fromAllDaySlot?: boolean
  } | null>(null)

  const handleAllDayToggle = React.useCallback(() => {
    const newAllDay = !allday
    let newStart = start
    let newEnd = end

    if (newAllDay) {
      newEnd = toInclusiveAllDayEnd(start, end)
      originalTimeRef.current =
        newEnd === end ? null : { start, end, endDate: newEnd }
    } else if (
      originalTimeRef.current?.start === start &&
      originalTimeRef.current.endDate === end
    ) {
      // Unticking right away gives back the original midnight end
      newEnd = originalTimeRef.current.end
      originalTimeRef.current = null
    }

    if (!newAllDay) {
      const hasTimeParts = start.includes('T') && end.includes('T')
      if (!hasTimeParts && !startTime && !endTime) {
        // Next round hour in the zone of the event, not in the one of the browser
        const now = timezone ? moment.tz(timezone) : moment()
        const slotStart = now.clone().startOf('hour').add(1, 'hour')
        const slotEnd = slotStart.clone().add(1, 'hour')
        const startTimeStr = slotStart.format(TIME_FORMAT)
        const endTimeStr = slotEnd.format(TIME_FORMAT)

        const startDateOnly = start.split('T')[0] || startDate
        const keptEndDate = end.split('T')[0] || endDate || startDateOnly
        const slotCrossesMidnight = !slotEnd.isSame(slotStart, 'day')
        const endDateOnly =
          slotCrossesMidnight && keptEndDate <= startDateOnly
            ? moment(startDateOnly, DATE_FORMAT)
                .add(1, 'day')
                .format(DATE_FORMAT)
            : keptEndDate
        newStart = combineDateTime(startDateOnly, startTimeStr)
        newEnd = combineDateTime(endDateOnly, endTimeStr)

        setStartTime(startTimeStr)
        setEndTime(endTimeStr)
      }
    }

    if (!onAllDayChange) {
      setStart(newStart)
      setEnd(newEnd)
      setAllDay(newAllDay)
    } else {
      onAllDayChange(newAllDay, newStart, newEnd)
    }
  }, [
    allday,
    start,
    end,
    startDate,
    startTime,
    endDate,
    endTime,
    timezone,
    setStartTime,
    setEndTime,
    setStart,
    setEnd,
    setAllDay,
    onAllDayChange
  ])

  return {
    originalTimeRef,
    handleAllDayToggle
  }
}
