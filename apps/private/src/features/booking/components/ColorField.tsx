import React from 'react'
import { useTheme } from '@linagora/twake-mui'
import { useI18n } from 'twake-i18n'
import { FieldWithLabel } from '@common/components/Event/components/FieldWithLabel'
import { ColorPicker } from '@common/components/Calendar/CalendarColorPicker'
import { getAccessiblePair } from '@common/utils/getAccessiblePair'

interface ColorFieldProps {
  showExpandedLabel: boolean
  color: string
  setColor: (value: string) => void
}

export const ColorField: React.FC<ColorFieldProps> = ({
  showExpandedLabel,
  color,
  setColor
}) => {
  const { t } = useI18n()
  const theme = useTheme()

  return (
    <FieldWithLabel
      label={t('booking.color')}
      isExpanded={showExpandedLabel}
      sx={{ padding: 0, margin: 0 }}
    >
      <ColorPicker
        selectedColor={{
          light: color,
          dark: getAccessiblePair(color, theme)
        }}
        onChange={c => setColor(c.light)}
        label={t('booking.color')}
      />
    </FieldWithLabel>
  )
}
