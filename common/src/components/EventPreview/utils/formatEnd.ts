import { formatDateInTimezone } from '@common/components/Event/utils/dateTimeFormatters'

interface FormatEndParams {
  start: Date | string
  end: Date | string
  t: (k: string, p?: Record<string, unknown>) => string
  timeZone: string
  allday?: boolean
}

export function formatEnd({
  start,
  end,
  t,
  timeZone,
  allday
}: FormatEndParams): string | null {
  const startDate = new Date(start)
  const endDate = new Date(end)

  const formatDatePart = (d: Date): string => formatDateInTimezone(d, timeZone)
  if (allday) {
    // All-day dates are floating YYYY-MM-DD values parsed as UTC midnight
    const inclusiveEndDate = new Date(endDate)
    inclusiveEndDate.setUTCDate(inclusiveEndDate.getUTCDate() - 1)
    const sameDay =
      formatDateInTimezone(startDate, 'UTC') ===
      formatDateInTimezone(inclusiveEndDate, 'UTC')
    return sameDay
      ? null
      : inclusiveEndDate.toLocaleDateString(t('locale'), {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
          timeZone: 'UTC'
        })
  } else {
    const sameDay = formatDatePart(startDate) === formatDatePart(endDate)
    if (sameDay) {
      return endDate.toLocaleTimeString(t('locale'), {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
        timeZone
      })
    }
    return endDate.toLocaleString(t('locale'), {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
      timeZone
    })
  }
}
