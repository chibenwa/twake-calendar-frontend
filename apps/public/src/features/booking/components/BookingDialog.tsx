import {
  BookingSlotsResponse,
  Slot
} from '@common/features/booking/types/BookingTypes'
import { formatDateInTimezone } from '@common/components/Event/utils/dateTimeFormatters'
import { isValidEmail } from '@common/utils/isValidEmail'
import { Box, Button, TextField, Typography } from '@linagora/twake-mui'
import React, { useEffect, useRef, useState } from 'react'
import { useI18n } from 'twake-i18n'
import { BookingOwnerDisplay } from '@/components/Booking/BookingHeader/BookingOwnerInfo'
import { StaticDateTimeSummary } from './StaticDateTimeSummary'
import { ResponsiveDialog } from '@common/components/Dialog'

interface BookingConfirmDialogProps {
  open: boolean
  onClose: () => void
  selectedSlot: Slot | null
  bookingInfo: BookingSlotsResponse | null
  onConfirm: (name: string, email: string) => Promise<void>
  selectedTimezone: string
}

interface BookingDetailsProps {
  bookingInfo: BookingSlotsResponse | null
  selectedSlot: Slot | null
  selectedTimezone: string
}

const BookingDetails: React.FC<BookingDetailsProps> = ({
  bookingInfo,
  selectedSlot,
  selectedTimezone
}) => {
  let startDateStr = ''
  let startTimeStr = ''
  let endDateStr = ''
  let endTimeStr = ''

  if (selectedSlot) {
    const start = new Date(selectedSlot.start)
    const end = new Date(
      start.getTime() + (bookingInfo?.durationMinutes ?? 30) * 60000
    )

    const timeFmt = new Intl.DateTimeFormat('en-GB', {
      timeZone: selectedTimezone,
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    })

    startDateStr = formatDateInTimezone(start, selectedTimezone)
    startTimeStr = timeFmt.format(start)
    endDateStr = formatDateInTimezone(end, selectedTimezone)
    endTimeStr = timeFmt.format(end)
  }

  return (
    <>
      {bookingInfo?.name && (
        <Typography variant="h4" sx={{ mb: '24px' }}>
          {bookingInfo.name}
        </Typography>
      )}
      {selectedSlot && (
        <StaticDateTimeSummary
          startDate={startDateStr}
          startTime={startTimeStr}
          endDate={endDateStr}
          endTime={endTimeStr}
          timezone={selectedTimezone}
        />
      )}
    </>
  )
}

interface BookingFormProps {
  name: string
  email: string
  nameError: string | null
  emailError: string | null
  bookingError: string | null
  onNameChange: (value: string) => void
  onEmailChange: (value: string) => void
  nameRef: React.Ref<HTMLInputElement>
  emailRef: React.Ref<HTMLInputElement>
}

const BookingForm: React.FC<BookingFormProps> = ({
  name,
  email,
  nameError,
  emailError,
  bookingError,
  onNameChange,
  onEmailChange,
  nameRef,
  emailRef
}) => {
  const { t } = useI18n()
  return (
    <>
      <Typography variant="caption" component="p" sx={{ mt: 2 }}>
        {t('booking.form.requiredHint')}
      </Typography>
      <TextField
        label={t('booking.form.name')}
        autoComplete="name"
        inputRef={nameRef}
        value={name}
        onChange={e => onNameChange(e.target.value)}
        fullWidth
        margin="normal"
        size="small"
        required
        error={!!nameError}
        helperText={nameError}
      />
      <TextField
        label={t('booking.form.email')}
        autoComplete="email"
        inputRef={emailRef}
        type="email"
        value={email}
        onChange={e => onEmailChange(e.target.value)}
        fullWidth
        margin="normal"
        size="small"
        required
        error={!!emailError}
        helperText={emailError}
      />
      {bookingError && (
        <Typography color="error" variant="body2" sx={{ mt: 1 }}>
          {bookingError}
        </Typography>
      )}
    </>
  )
}

export const BookingConfirmDialog: React.FC<BookingConfirmDialogProps> = ({
  open,
  onClose,
  selectedSlot,
  bookingInfo,
  onConfirm,
  selectedTimezone
}) => {
  const { t } = useI18n()
  const [name, setName] = useState<string>('')
  const [email, setEmail] = useState<string>('')
  const [nameError, setNameError] = useState<string | null>(null)
  const [emailError, setEmailError] = useState<string | null>(null)
  const [bookingInProgress, setBookingInProgress] = useState<boolean>(false)
  const [bookingError, setBookingError] = useState<string | null>(null)
  const nameRef = useRef<HTMLInputElement>(null)
  const emailRef = useRef<HTMLInputElement>(null)
  // After a refused Confirm, the first wrong field takes the focus, once its
  // error is rendered, so that a screen reader reads it
  const [invalidField, setInvalidField] = useState<'name' | 'email' | null>(
    null
  )

  useEffect(() => {
    if (!invalidField) return
    ;(invalidField === 'name' ? nameRef : emailRef).current?.focus()
    setInvalidField(null)
  }, [invalidField])

  const handleEmailChange = (value: string): void => {
    setEmail(value)
    if (!isValidEmail(value)) {
      setEmailError(t('peopleSearch.invalidEmail').replace('%{email}', value))
    } else {
      setEmailError(null)
    }
  }

  const handleConfirm = async (): Promise<void> => {
    setNameError(null)
    setEmailError(null)
    setBookingError(null)

    if (!name.trim()) {
      setNameError(t('booking.error.nameRequired'))
      setInvalidField('name')
      return
    }

    if (!email) {
      setEmailError(t('booking.error.emailRequired'))
      setInvalidField('email')
      return
    }

    if (!isValidEmail(email)) {
      setEmailError(t('peopleSearch.invalidEmail').replace('%{email}', email))
      setInvalidField('email')
      return
    }

    setBookingInProgress(true)
    try {
      await onConfirm(name, email)
      setName('')
      setEmail('')
    } catch (err) {
      const message = err instanceof Error ? err.message : undefined
      setBookingError(message || t('booking.error.createFailed'))
    } finally {
      setBookingInProgress(false)
    }
  }

  const handleClose = (): void => {
    if (bookingInProgress) {
      return
    }
    onClose()
    setBookingError(null)
    setNameError(null)
    setEmailError(null)
    setName('')
    setEmail('')
  }

  const confirmButtonText = bookingInProgress
    ? t('booking.confirm.inProgress')
    : t('booking.confirm.button')

  const title = bookingInfo?.owner ? (
    <BookingOwnerDisplay owner={bookingInfo.owner} />
  ) : (
    <Box />
  )

  const actions = (
    <>
      <Button onClick={handleClose} variant="text" disabled={bookingInProgress}>
        {t('common.cancel')}
      </Button>
      <Button
        onClick={() => void handleConfirm()}
        variant="contained"
        disabled={bookingInProgress}
      >
        {confirmButtonText}
      </Button>
    </>
  )

  return (
    <ResponsiveDialog
      open={open}
      onClose={handleClose}
      title={title}
      actions={actions}
      normalMaxWidth="570px"
      titleSx={{
        my: '16px'
      }}
    >
      <BookingDetails
        bookingInfo={bookingInfo}
        selectedSlot={selectedSlot}
        selectedTimezone={selectedTimezone}
      />
      <BookingForm
        name={name}
        email={email}
        nameError={nameError}
        emailError={emailError}
        bookingError={bookingError}
        onNameChange={setName}
        onEmailChange={handleEmailChange}
        nameRef={nameRef}
        emailRef={emailRef}
      />
    </ResponsiveDialog>
  )
}
