import { Calendar } from '@common/types/CalendarTypes'
import { makeDisplayName } from '@common/utils/makeDisplayName'
import { renameDefault } from '@common/utils/renameDefault'

type Translate = (key: string, options?: Record<string, string>) => string

const ONE_DAY_MS = 24 * 60 * 60 * 1000

// Answers the chip only draws (strike-through, dashed border, transparency)
const ANNOUNCED_PARTSTATS = ['TENTATIVE', 'DECLINED', 'NEEDS-ACTION']

export interface EventAccessibleLabelParams {
  title?: string
  start: Date
  end?: Date | null
  allDay: boolean
  calendarName?: string
  partstat?: string
  timeZone: string
  t: Translate
}

/** The calendar name the sidebar shows, e.g. not "#default" */
export function calendarLabel(
  calendar: Calendar | undefined,
  userId: string | undefined,
  t: Translate
): string | undefined {
  if (!calendar) return undefined
  return renameDefault(
    calendar.name,
    makeDisplayName(calendar) ?? '',
    // renameDefault only passes string params
    t as Parameters<typeof renameDefault>[2],
    calendar.id?.split('/')[0] === userId
  )
}

/**
 * The name a screen reader gives an event chip or a search result: what the
 * user reads on screen (title, day, time) plus what is only drawn (calendar
 * colour, own answer).
 */
export function eventAccessibleLabel({
  title,
  start,
  end,
  allDay,
  calendarName,
  partstat,
  timeZone,
  t
}: EventAccessibleLabelParams): string {
  const locale = t('locale')
  // All-day dates are calendar dates, stored at midnight UTC
  const dayZone = allDay ? 'UTC' : timeZone
  const hasEnd = !!end && end > start
  const formatDay = (d: Date): string =>
    d.toLocaleDateString(locale, {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      timeZone: dayZone
    })
  const formatTime = (d: Date): string =>
    d.toLocaleTimeString(locale, {
      hour: '2-digit',
      minute: '2-digit',
      timeZone
    })

  // The end of an all-day event is the day following its last day
  const lastDay =
    hasEnd && end ? new Date(end.getTime() - (allDay ? ONE_DAY_MS : 0)) : start
  const firstDayLabel = formatDay(start)
  const lastDayLabel = formatDay(lastDay)
  const date =
    lastDay > start && lastDayLabel !== firstDayLabel
      ? `${firstDayLabel} – ${lastDayLabel}`
      : firstDayLabel

  const time = allDay
    ? t('event.form.allDay')
    : hasEnd && end
      ? `${formatTime(start)} – ${formatTime(end)}`
      : formatTime(start)

  const status =
    partstat && ANNOUNCED_PARTSTATS.includes(partstat)
      ? `, ${t(`eventPreview.attendee.${partstat}`)}`
      : ''

  return t('event.chipLabel', {
    title: title || t('event.untitled'),
    date,
    time,
    calendar: calendarName ?? '',
    status
  })
}
