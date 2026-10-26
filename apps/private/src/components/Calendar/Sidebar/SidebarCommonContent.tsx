import { Box, Typography } from '@linagora/twake-mui'
import { useI18n } from 'twake-i18n'
import { visuallyHidden } from '@common/utils/visuallyHidden'
import CalendarSelection from '../CalendarSelection'
import { TempCalendarsInput } from '@common/components/Calendar/TempCalendarsInput'
import { CalendarSidebarProps } from './SideBar'

export const SidebarCommonContent: React.FC<
  Pick<
    CalendarSidebarProps,
    | 'onCreateEvent'
    | 'tempUsers'
    | 'setTempUsers'
    | 'selectedCalendars'
    | 'setSelectedCalendars'
  > & { openSearchOnMobile?: () => void }
> = ({
  onCreateEvent,
  tempUsers,
  setTempUsers,
  selectedCalendars,
  setSelectedCalendars,
  openSearchOnMobile
}) => {
  const { t } = useI18n()
  return (
    <>
      <Box sx={{ mb: 3, mt: 2 }}>
        <TempCalendarsInput
          tempUsers={tempUsers}
          setTempUsers={setTempUsers}
          handleToggleEventPreview={onCreateEvent}
          onOpenSearchOnMobile={openSearchOnMobile}
        />
      </Box>
      <Box className="calendarList">
        {/* Over the h3 of each calendar section */}
        <Typography component="h2" sx={visuallyHidden}>
          {t('a11y.calendarsHeading')}
        </Typography>
        <CalendarSelection
          selectedCalendars={selectedCalendars}
          setSelectedCalendars={setSelectedCalendars}
        />
      </Box>
    </>
  )
}
