import React from 'react'
import { useAppDispatch } from '@common/app/hooks'
import { TimezoneSelector } from '@common/components/Timezone/TimezoneSelector'
import { setTimeZone } from '@common/features/Settings/SettingsSlice'
import { useI18n } from 'twake-i18n'
import { Typography } from '@linagora/twake-mui'

export interface WeekNumberContentProps {
  num: number
  displayWeekNumbers: boolean
  timezone: string
  selectedDate: Date
}

export const WeekNumberContent: React.FC<WeekNumberContentProps> = ({
  num,
  displayWeekNumbers,
  timezone,
  selectedDate
}) => {
  const { t } = useI18n()
  const dispatch = useAppDispatch()

  return (
    <div className="weekSelector">
      {displayWeekNumbers && (
        <Typography
          variant="caption"
          sx={{
            whiteSpace: 'nowrap',
            flexShrink: 1,
            fontSize: { xs: '0.7rem', sm: '0.75rem' }
          }}
        >
          {t('menubar.views.week')} {num}
        </Typography>
      )}
      <TimezoneSelector
        value={timezone}
        referenceDate={selectedDate}
        onChange={(newTimezone: string) => {
          dispatch(setTimeZone(newTimezone))
        }}
      />
    </div>
  )
}
