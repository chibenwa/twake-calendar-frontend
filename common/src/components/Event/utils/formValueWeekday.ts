const ICS_DAYS = ['SU', 'MO', 'TU', 'WE', 'TH', 'FR', 'SA']

/**
 * ICS weekday (MO, TU…) of the date a form value shows. Form values are
 * "YYYY-MM-DD" for an all-day event and "YYYY-MM-DDTHH:mm" otherwise; their
 * date part is read as a calendar date. `new Date("2026-11-16")` would be
 * midnight UTC, which is still Sunday west of UTC.
 */
export function formValueWeekday(value: string): string | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value ?? '')
  if (!match) return null
  const [, year, month, day] = match.map(Number)
  const date = new Date(Date.UTC(year, month - 1, day))
  return isNaN(date.getTime()) ? null : ICS_DAYS[date.getUTCDay()]
}
