import { formatEventChipTitle } from '@common/components/Calendar/utils/calendarUtils'
import Tooltip from '@common/components/Tooltip'
import { Box, Chip, Typography } from '@linagora/twake-mui'
import CircleIcon from '@mui/icons-material/Circle'
import LockOutlinedIcon from '@mui/icons-material/LockOutlined'
import React from 'react'
import { CalendarEvent } from '@common/types/EventsTypes'
import { EventTimeSubtitle } from './EventTimeSubtitle'

export interface EventPreviewTitleRowProps {
  event: CalendarEvent
  isOwn: boolean
  timezone: string
  t: (key: string, options?: Record<string, unknown>) => string
  // The level of the title: h1 when the event is the whole page
  titleComponent?: 'h1' | 'h3'
}

export const EventPreviewTitleRow: React.FC<EventPreviewTitleRowProps> = ({
  event,
  isOwn,
  timezone,
  t,
  titleComponent = 'h3'
}) => {
  return (
    <Box>
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'row',
          alignItems: 'center',
          gap: 1,
          mb: 1
        }}
      >
        {event.class === 'PRIVATE' &&
          (isOwn ? (
            <Tooltip
              title={t('eventPreview.privateEvent.tooltipOwn')}
              placement="top"
            >
              <LockOutlinedIcon />
            </Tooltip>
          ) : (
            <LockOutlinedIcon />
          ))}
        <Typography
          variant="h3"
          component={titleComponent}
          sx={{
            minWidth: 0,
            overflowWrap: 'anywhere'
          }}
        >
          {formatEventChipTitle(event, t)}
        </Typography>
        {event.transp === 'TRANSPARENT' && (
          <Tooltip title={t('eventPreview.free.tooltip')} placement="top">
            <Chip
              icon={<CircleIcon fontSize="small" />}
              label={t('eventPreview.free.label')}
              sx={{
                '& .MuiChip-icon': {
                  color: 'success.main'
                }
              }}
            />
          </Tooltip>
        )}
      </Box>
      <EventTimeSubtitle event={event} timezone={timezone} />
    </Box>
  )
}
