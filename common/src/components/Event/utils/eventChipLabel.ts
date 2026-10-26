import { lastDayOf } from './eventDays'
import moment from 'moment-timezone'

type Translate = (key: string, options?: Record<string, unknown>) => string

// Answers worth announcing: an accepted invitation reads as a plain event
const ANNOUNCED_PARTSTATS = new Set(['TENTATIVE', 'DECLINED', 'NEEDS-ACTION'])

export interface EventChipLabelParts {
  title?: string
  start: Date
  end?: Date | null
  allDay: boolean
  // The zone to read the days in (UTC for the all-day search results)
  dayZone: string
  timeZone: string
  calendarName?: string
  partstat?: string
}

const formatDay = (date: Date, timeZone: string, t: Translate): string =>
  date.toLocaleDateString(t('locale'), {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    timeZone
  })

const formatDays = (parts: EventChipLabelParts, t: Translate): string => {
  const { start, end, allDay, dayZone } = parts
  // A timed event ending at midnight does not run into the next day
  const lastInstant = allDay || !end ? end : new Date(end.getTime() - 1)
  const firstDay = formatDay(start, dayZone, t)
  const lastDay = formatDay(
    lastDayOf(start, lastInstant ?? start, allDay),
    dayZone,
    t
  )
  return firstDay === lastDay ? firstDay : `${firstDay} – ${lastDay}`
}

const formatTimes = (parts: EventChipLabelParts, t: Translate): string => {
  const { start, end, allDay, timeZone } = parts
  if (allDay) return t('event.form.allDay')
  const startTime = moment.tz(start, timeZone).format('HH:mm')
  return end && end > start
    ? `${startTime} – ${moment.tz(end, timeZone).format('HH:mm')}`
    : startTime
}

const formatStatus = (partstat: string | undefined, t: Translate): string =>
  partstat && ANNOUNCED_PARTSTATS.has(partstat)
    ? `, ${t(`eventPreview.attendee.${partstat}`)}`
    : ''

/**
 * The accessible name of an event: what the chip or the search result shows,
 * plus the day, the calendar and the answer of the user that are only drawn.
 */
export function buildEventChipLabel(
  parts: EventChipLabelParts,
  t: Translate
): string {
  return t('event.chipLabel', {
    title: parts.title || t('event.untitled'),
    date: formatDays(parts, t),
    time: formatTimes(parts, t),
    calendar: parts.calendarName ?? '',
    status: formatStatus(parts.partstat, t)
  })
}
