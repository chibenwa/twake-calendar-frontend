import {
  getBestColor,
  getTitleStyle
} from '@common/components/Event/EventChip/EventChipUtils'
import { formatDateInTimezone } from '@common/components/Event/utils/dateTimeFormatters'
import { Calendar } from '@common/types/CalendarTypes'
import { Box, Card, CardHeader, Typography } from '@linagora/twake-mui'
import React from 'react'
import { useI18n } from 'twake-i18n'
import { lastDayOf } from './searchResultsComponents'
import { SearchEventResult } from './types/SearchEventResult'

interface MobileDateProps {
  startDate: Date
  t: (key: string) => string
  timeZone: string
}

interface MobileEventCardProps {
  eventData: SearchEventResult
  calendar: Calendar | undefined
  timeZone: string
  customSubHeader?: (titleStyle: React.CSSProperties) => React.ReactNode
  effectiveColor?: { light: string; dark: string }
}

const yearOf = (date: Date, timeZone: string): string =>
  date.toLocaleDateString('en', { year: 'numeric', timeZone })

const captionSx = { color: 'text.secondary', display: 'block' }

export const RenderMobileDate: React.FC<MobileDateProps> = ({
  startDate,
  t,
  timeZone
}) => (
  <Box sx={{ width: '100%' }}>
    <Typography variant="h4" sx={{ fontWeight: 400 }}>
      {startDate.toLocaleDateString(t('locale'), { day: '2-digit', timeZone })}
    </Typography>
    <Typography variant="caption" sx={captionSx}>
      {startDate
        .toLocaleDateString(t('locale'), { weekday: 'short', timeZone })
        .toUpperCase()}
    </Typography>
    <Typography variant="caption" sx={captionSx}>
      {startDate
        .toLocaleDateString(t('locale'), { month: 'short', timeZone })
        .toUpperCase()}
    </Typography>
    {yearOf(startDate, timeZone) !== yearOf(new Date(), timeZone) && (
      <Typography variant="caption" sx={captionSx}>
        {startDate.toLocaleDateString(t('locale'), {
          year: 'numeric',
          timeZone
        })}
      </Typography>
    )}
  </Box>
)

// Same-day timed event: "10:00 - 11:00"; multi-day all-day event:
// "04 Nov - 08 Nov"; multi-day timed event: "20 Oct 18:00 - 21 Oct 09:00"
export const formatMobileEventSpan = (
  startDate: Date,
  endDate: Date,
  allDay: boolean,
  locale: string,
  timeZone: string
): string | null => {
  const lastDay = lastDayOf(startDate, endDate, allDay)
  const isMultiDay =
    formatDateInTimezone(startDate, timeZone) !==
    formatDateInTimezone(lastDay, timeZone)
  const formatDay = (d: Date): string =>
    d.toLocaleDateString(locale, { day: '2-digit', month: 'short', timeZone })
  const formatTime = (d: Date): string =>
    d.toLocaleTimeString(locale, {
      hour: '2-digit',
      minute: '2-digit',
      timeZone
    })

  if (allDay) {
    return isMultiDay ? `${formatDay(startDate)} - ${formatDay(lastDay)}` : null
  }
  if (isMultiDay) {
    return `${formatDay(startDate)} ${formatTime(startDate)} - ${formatDay(endDate)} ${formatTime(endDate)}`
  }
  return `${formatTime(startDate)} - ${formatTime(endDate)}`
}

const cardSx = {
  height: 'stretch',
  minHeight: '58px',
  width: '100%',
  borderRadius: '8px',
  padding: 1,
  boxShadow: 'none',
  border: '1px solid',
  borderColor: 'background.paper',
  display: 'flex'
}

// The calendar colors go through style, not sx: they come from the server and
// must not be able to add style rules.
const getCardColorStyle = (effectiveColor: {
  light: string
  dark: string
}): React.CSSProperties => ({
  backgroundColor: effectiveColor.light,
  color: effectiveColor.dark
})

const headerSx = {
  p: '0px',
  '& .MuiCardHeader-content': { overflow: 'hidden' }
}

const getSubheaderStyle = (color?: string): React.CSSProperties => ({
  color,
  opacity: '70%',
  fontWeight: '500',
  fontSize: '10px',
  lineHeight: '16px',
  letterSpacing: '0%',
  verticalAlign: 'middle'
})

export const RenderMobileEventCard: React.FC<MobileEventCardProps> = ({
  eventData,
  calendar,
  timeZone,
  customSubHeader,
  effectiveColor
}) => {
  const { t } = useI18n()

  if (!calendar) return null

  const { start, end, allDay, summary, uid } = eventData.data
  const startDate = new Date(start)
  const endDate = end ? new Date(end) : startDate

  const resolvedColor = effectiveColor ??
    (calendar.color as { light: string; dark: string }) ?? {
      light: '#fff',
      dark: '#000'
    }
  const bestColor = getBestColor(resolvedColor)
  const titleStyle = getTitleStyle(bestColor, 'ACCEPTED', resolvedColor, false)

  const eventSpan = formatMobileEventSpan(
    startDate,
    endDate,
    !!allDay,
    t('locale'),
    timeZone
  )
  const defaultSubHeader = eventSpan && (
    <Typography style={getSubheaderStyle(titleStyle.color)}>
      {eventSpan}
    </Typography>
  )

  return (
    <Card
      variant="outlined"
      sx={cardSx}
      style={getCardColorStyle(resolvedColor)}
      data-testid={`event-card-${uid}`}
    >
      <CardHeader
        sx={headerSx}
        title={
          <Typography variant="body2" noWrap style={titleStyle}>
            {summary || t('event.untitled')}
          </Typography>
        }
        subheader={
          customSubHeader ? customSubHeader(titleStyle) : defaultSubHeader
        }
      />
    </Card>
  )
}
