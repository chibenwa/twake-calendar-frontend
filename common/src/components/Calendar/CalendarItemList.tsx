import { Calendar } from '@common/types/CalendarTypes'
import { MenuItem } from '@linagora/twake-mui'
import React from 'react'
import { CalendarName } from './CalendarName'

// Options of a Select: MenuItems throw outside of a Menu
export function CalendarItemList(
  userPersonalCalendars: Calendar[]
): React.ReactNode {
  return Object.values(userPersonalCalendars).map(calendar => (
    <MenuItem key={calendar.id} value={calendar.id}>
      <CalendarName calendar={calendar} />
    </MenuItem>
  ))
}
