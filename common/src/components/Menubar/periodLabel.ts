import { CalendarApi } from '@fullcalendar/core'

type Translate = (key: string) => string

type YearMonth = { year: number; month: number }

// Read the date parts in the calendar timezone, not the browser one: the
// period boundaries are midnights in the calendar timezone
const yearMonthIn = (timeZone: string | undefined, date: Date): YearMonth => {
  const parts = new Intl.DateTimeFormat('en-US', {
    ...(timeZone && timeZone !== 'local' ? { timeZone } : {}),
    year: 'numeric',
    month: 'numeric'
  }).formatToParts(date)
  const part = (type: Intl.DateTimeFormatPartTypes): number =>
    Number(parts.find(p => p.type === type)?.value)

  return { year: part('year'), month: part('month') - 1 }
}

const monthLabel = (date: YearMonth, t: Translate): string =>
  t(`months.standalone.${date.month}`)

// The title depends on the displayed period only, not on how it was reached:
// FullCalendar's current date is today after "Today" but the period start otherwise
export const formatPeriodLabel = (
  calendarApi: CalendarApi | null | undefined,
  fallbackDate: Date,
  t: Translate
): string => {
  const view = calendarApi?.view
  const timeZone = view ? calendarApi?.getOption('timeZone') : undefined
  const first = yearMonthIn(timeZone, view ? view.currentStart : fallbackDate)
  // currentEnd is exclusive
  const last = yearMonthIn(
    timeZone,
    view ? new Date(view.currentEnd.getTime() - 1) : fallbackDate
  )

  if (first.year !== last.year) {
    return `${monthLabel(first, t)} ${first.year} – ${monthLabel(last, t)} ${last.year}`
  }
  if (first.month !== last.month) {
    return `${monthLabel(first, t)} – ${monthLabel(last, t)} ${last.year}`
  }
  return `${monthLabel(first, t)} ${first.year}`
}
