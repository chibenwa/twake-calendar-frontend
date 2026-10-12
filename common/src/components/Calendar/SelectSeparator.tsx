import { Divider } from '@linagora/twake-mui'
import React from 'react'

// Visual separator between the options of a Select. Select clones every
// child with role="option" and option handlers: drop them so assistive
// technologies see a separator rather than an empty option.
export const SelectSeparator: React.FC = () => (
  <Divider component="li" role="separator" />
)
