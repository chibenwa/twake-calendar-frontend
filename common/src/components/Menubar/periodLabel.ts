type Translate = (key: string) => string

// The period FullCalendar displays, as its datesSet hands it over: the end is
// exclusive
export type DisplayedPeriod = {
  start: Date
  end: Date
  timeZone?: string
}

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

// "short" ("Oct") fits the narrowest phones, where full names get clipped
export type MonthFormat = 'standalone' | 'short'

const monthLabel = (
  date: YearMonth,
  t: Translate,
  format: MonthFormat
): string => t(`months.${format}.${date.month}`)

// The title depends on the displayed period only, not on how it was reached:
// FullCalendar's current date is today after "Today" but the period start otherwise
export const formatPeriodLabel = (
  period: DisplayedPeriod | undefined,
  fallbackDate: Date,
  t: Translate,
  monthFormat: MonthFormat = 'standalone'
): string => {
  const month = (date: YearMonth): string => monthLabel(date, t, monthFormat)
  const timeZone = period?.timeZone
  const first = yearMonthIn(timeZone, period ? period.start : fallbackDate)
  const last = yearMonthIn(
    timeZone,
    period ? new Date(period.end.getTime() - 1) : fallbackDate
  )

  if (first.year !== last.year) {
    return `${month(first)} ${first.year} – ${month(last)} ${last.year}`
  }
  if (first.month !== last.month) {
    return `${month(first)} – ${month(last)} ${last.year}`
  }
  return `${month(first)} ${first.year}`
}
