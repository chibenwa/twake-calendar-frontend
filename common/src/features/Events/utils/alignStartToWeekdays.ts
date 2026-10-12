import { RepetitionData } from '@common/types/Repetition'

const ICS_WEEKDAYS = ['SU', 'MO', 'TU', 'WE', 'TH', 'FR', 'SA']
const DAY_MS = 24 * 60 * 60 * 1000

function parseDatePart(formValue: string): Date {
  const [year, month, day] = formValue.split('T')[0].split('-').map(Number)
  return new Date(Date.UTC(year, month - 1, day))
}

function shiftFormValue(formValue: string, days: number): string {
  if (!formValue) return formValue
  const [, timePart] = formValue.split('T')
  const shifted = new Date(parseDatePart(formValue).getTime() + days * DAY_MS)
  const datePart = shifted.toISOString().split('T')[0]
  return timePart === undefined ? datePart : `${datePart}T${timePart}`
}

function daysUntilSelectedWeekday(start: string, byday: string[]): number {
  const startWeekday = parseDatePart(start).getUTCDay()
  const offsets = [0, 1, 2, 3, 4, 5, 6].filter(offset =>
    byday.includes(ICS_WEEKDAYS[(startWeekday + offset) % 7])
  )
  return offsets.length > 0 ? offsets[0] : 0
}

/**
 * A weekly series lists its weekdays in BYDAY, but RFC 5545 makes DTSTART the
 * first occurrence whatever its weekday. When the start date is not one of the
 * selected weekdays, move start and end (form strings, keeping the duration
 * and the time of day) to the first selected weekday on or after the start.
 */
export function alignStartToWeekdays<
  T extends {
    start: string
    end: string
    repetition?: Pick<RepetitionData, 'freq' | 'byday'>
  }
>(values: T): T {
  const byday = values.repetition?.byday
  if (values.repetition?.freq !== 'weekly' || !byday?.length || !values.start)
    return values

  const days = daysUntilSelectedWeekday(values.start, byday)
  if (days === 0) return values

  return {
    ...values,
    start: shiftFormValue(values.start, days),
    end: shiftFormValue(values.end, days)
  }
}
