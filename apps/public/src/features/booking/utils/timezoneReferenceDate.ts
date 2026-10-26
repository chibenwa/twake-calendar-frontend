import dayjs, { Dayjs } from 'dayjs'
import timezone from 'dayjs/plugin/timezone'
import utc from 'dayjs/plugin/utc'

dayjs.extend(utc)
dayjs.extend(timezone)

/**
 * Date used to label the time zone offset above the slots: noon of the
 * selected day in the selected zone (DST switches happen at night), so the
 * offset matches the one of the displayed slots. Falls back to the visible
 * month when no day is selected.
 */
export const getTimezoneReferenceDate = (
  selectedDay: Dayjs | null,
  visibleMonth: Date,
  timeZone: string
): Date => {
  if (!selectedDay) {
    return visibleMonth
  }
  return dayjs
    .tz(`${selectedDay.format('YYYY-MM-DD')}T12:00:00`, timeZone)
    .toDate()
}
