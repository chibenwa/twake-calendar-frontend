import { formatPeriodLabel } from '@common/components/Menubar/periodLabel'
import { CalendarApi } from '@fullcalendar/core'

const MONTHS = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December'
]

const t = (key: string): string =>
  MONTHS[Number(key.replace('months.standalone.', ''))]

const calendarShowing = (currentStart: Date, currentEnd: Date): CalendarApi =>
  ({ view: { currentStart, currentEnd } }) as unknown as CalendarApi

describe('formatPeriodLabel', () => {
  it('shows both months for a week spanning two months', () => {
    const api = calendarShowing(new Date(2026, 8, 28), new Date(2026, 9, 5))

    expect(formatPeriodLabel(api, new Date(2026, 9, 2), t)).toBe(
      'September – October 2026'
    )
  })

  it('does not depend on the current date of the calendar', () => {
    const api = calendarShowing(new Date(2026, 8, 28), new Date(2026, 9, 5))

    expect(formatPeriodLabel(api, new Date(2026, 8, 28), t)).toBe(
      formatPeriodLabel(api, new Date(2026, 9, 2), t)
    )
  })

  it('shows both years for a week spanning two years', () => {
    const api = calendarShowing(new Date(2026, 11, 28), new Date(2027, 0, 4))

    expect(formatPeriodLabel(api, new Date(2026, 11, 28), t)).toBe(
      'December 2026 – January 2027'
    )
  })

  it('shows a single month for a month view', () => {
    const api = calendarShowing(new Date(2026, 9, 1), new Date(2026, 10, 1))

    expect(formatPeriodLabel(api, new Date(2026, 9, 1), t)).toBe(
      'October 2026'
    )
  })

  it('shows a single month for a week within one month', () => {
    const api = calendarShowing(new Date(2026, 9, 5), new Date(2026, 9, 12))

    expect(formatPeriodLabel(api, new Date(2026, 9, 5), t)).toBe(
      'October 2026'
    )
  })

  it('falls back to the given date without calendar', () => {
    expect(formatPeriodLabel(null, new Date(2024, 3, 15), t)).toBe('April 2024')
  })
})
