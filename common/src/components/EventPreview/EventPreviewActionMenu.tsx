import EventDuplication from '@common/components/Event/EventDuplicate'
import { Menu, MenuItem } from '@linagora/twake-mui'
import { useI18n } from 'twake-i18n'
import { CalendarEvent } from '@common/types/EventsTypes'
import { useAppSelector } from '@common/app/hooks'
import { buildMailComposerUrl, resolveMailSpaUrl } from '@linagora/twake-utils'

interface EventPreviewActionMenuProps {
  anchorEl: Element | null
  isEditable: boolean
  event: CalendarEvent
  userEmail: string
  onClose: () => void
  onDuplicate: () => void
  onEdit: () => void
}

export const EventPreviewActionMenu: React.FC<EventPreviewActionMenuProps> = ({
  anchorEl,
  isEditable,
  event,
  userEmail,
  onClose,
  onDuplicate,
  onEdit
}) => {
  const { t } = useI18n()
  const workplaceFqdn = useAppSelector(
    state => state.user.userData?.workplaceFqdn
  )
  const mailSpaUrl = resolveMailSpaUrl(window.MAIL_SPA_URL, {
    localpart: userEmail?.split('@')[0],
    workplaceFqdn,
    workplaceFqdnFallback: window.WORKPLACE_FQDN_FALLBACK
  })

  const attendees = event.attendee ?? []
  const otherAttendees = attendees.filter(
    a => a.cal_address !== userEmail && a.cutype !== 'RESOURCE'
  )
  const composeUrl = mailSpaUrl
    ? buildMailComposerUrl(
        mailSpaUrl,
        otherAttendees.map(a => a.cal_address),
        event.title ?? ''
      )
    : null

  return (
    <Menu open={Boolean(anchorEl)} onClose={onClose} anchorEl={anchorEl}>
      {isEditable && (
        <MenuItem
          onClick={() => {
            onClose()
            onEdit()
          }}
        >
          {t('eventPreview.editEventSpecificSettings')}
        </MenuItem>
      )}
      {composeUrl && (
        <MenuItem
          onClick={() =>
            window.open(composeUrl, '_blank', 'noopener,noreferrer')
          }
        >
          {t('eventPreview.emailAttendees')}
        </MenuItem>
      )}
      <EventDuplication
        onOpenDuplicate={() => {
          onClose()
          onDuplicate()
        }}
      />
    </Menu>
  )
}
