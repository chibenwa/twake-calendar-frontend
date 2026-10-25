import React from 'react'
import { Select, MenuItem } from '@linagora/twake-mui'
import { useI18n } from 'twake-i18n'
import { useFieldLabelId } from '@common/components/Event/components/FieldWithLabel'
import { TIME_SLOT_OPTIONS, TimeSlotSelectFieldProps } from './index.type'

export const DesktopTimeSlotSelectField: React.FC<TimeSlotSelectFieldProps> = ({
  duration,
  setDuration
}) => {
  const { t } = useI18n()
  const labelId = useFieldLabelId()

  return (
    <Select
      labelId={labelId}
      value={duration ?? ''}
      onChange={e => setDuration(Number(e.target.value))}
    >
      {TIME_SLOT_OPTIONS.map(({ value, label }) => (
        <MenuItem key={value} value={value}>
          {t(label)}
        </MenuItem>
      ))}
    </Select>
  )
}
