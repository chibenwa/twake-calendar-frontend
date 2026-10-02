import { CalendarApi } from '@fullcalendar/core'

const HALF_DAY_MS = 12 * 60 * 60 * 1000

type Translate = (key: string) => string

const monthLabel = (date: Date, t: Translate): string =>
  t(`months.standalone.${date.getMonth()}`)

// The title depends on the displayed period only, not on how it was reached:
// FullCalendar's current date is today after "Today" but the period start otherwise
export const formatPeriodLabel = (
  calendarApi: CalendarApi | null | undefined,
  fallbackDate: Date,
  t: Translate
): string => {
  const view = calendarApi?.view
  // Shift by half a day so that midnight in the calendar timezone stays on
  // the same day in the browser timezone
  const first = view
    ? new Date(view.currentStart.getTime() + HALF_DAY_MS)
    : fallbackDate
  const last = view
    ? new Date(view.currentEnd.getTime() - HALF_DAY_MS)
    : fallbackDate

  if (first.getFullYear() !== last.getFullYear()) {
    return `${monthLabel(first, t)} ${first.getFullYear()} – ${monthLabel(last, t)} ${last.getFullYear()}`
  }
  if (first.getMonth() !== last.getMonth()) {
    return `${monthLabel(first, t)} – ${monthLabel(last, t)} ${last.getFullYear()}`
  }
  return `${monthLabel(first, t)} ${first.getFullYear()}`
}
