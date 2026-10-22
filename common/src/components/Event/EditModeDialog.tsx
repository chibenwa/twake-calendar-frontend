import {
  Button,
  ButtonGroup,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Radio,
  RadioGroup
} from '@linagora/twake-mui'
import { Dialog } from '@common/ds/Dialog/Dialog'
import { useState } from 'react'
import { useI18n } from 'twake-i18n'

export function EditModeDialog({
  type,
  setOpen,
  eventAction,
  onCancel
}: {
  type: string | null
  setOpen: (e: string | null) => void
  eventAction: (type: 'solo' | 'all' | undefined) => void
  /** Called when the dialog closes without a choice: Cancel, Escape, backdrop */
  onCancel?: () => void
}) {
  const { t } = useI18n()
  const [typeOfAction, setTypeOfAction] = useState<'solo' | 'all' | undefined>(
    'solo'
  )
  const handleEvent = () => {
    eventAction(typeOfAction)
    handleClose()
  }
  const handleClose = () => {
    setOpen(null)
    setTypeOfAction('solo')
  }
  const handleCancel = () => {
    onCancel?.()
    handleClose()
  }
  return (
    <Dialog open={Boolean(type)} onClose={handleCancel}>
      <DialogTitle>
        {type === 'edit' && t('editModeDialog.updateRecurrentEvent')}
        {type === 'delete' && t('editModeDialog.deleteRecurrentEvent')}
        {type === 'attendance' && t('editModeDialog.updateParticipationStatus')}
      </DialogTitle>
      <DialogContent>
        <RadioGroup
          value={typeOfAction}
          onChange={e =>
            setTypeOfAction(e.target.value as 'solo' | 'all' | undefined)
          }
        >
          <FormControlLabel
            value="solo"
            control={<Radio />}
            label={t('editModeDialog.thisEvent')}
          />
          <FormControlLabel
            value="all"
            control={<Radio />}
            label={t('editModeDialog.allEvents')}
          />
        </RadioGroup>
      </DialogContent>
      <DialogActions>
        <ButtonGroup>
          <Button variant="text" onClick={handleCancel}>
            {t('common.cancel')}
          </Button>
          <Button variant="text" onClick={handleEvent}>
            {t('common.ok')}
          </Button>
        </ButtonGroup>
      </DialogActions>
    </Dialog>
  )
}
