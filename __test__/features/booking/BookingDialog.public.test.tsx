import { TwakeMuiThemeProvider } from '@linagora/twake-mui'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import '@testing-library/jest-dom'
import { BookingConfirmDialog } from '@public/features/booking/components/BookingDialog'
import {
  BookingSlotsResponse,
  Slot
} from '@common/features/booking/types/BookingTypes'

jest.mock('twake-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
    lang: 'en'
  })
}))

jest.mock(
  '@/components/Booking/BookingHeader/BookingOwnerInfo',
  () => ({ BookingOwnerDisplay: () => null }),
  { virtual: true }
)

const bookingInfo = {
  owner: { displayName: 'alice' },
  durationMinutes: 30
} as unknown as BookingSlotsResponse

const slot = {
  start: '2026-10-26T09:00:00Z',
  end: '2026-10-26T09:30:00Z'
} as Slot

const renderDialog = (
  onConfirm = jest.fn().mockResolvedValue(undefined)
): jest.Mock => {
  render(
    <TwakeMuiThemeProvider>
      <BookingConfirmDialog
        open
        onClose={jest.fn()}
        selectedSlot={slot}
        bookingInfo={bookingInfo}
        onConfirm={onConfirm}
        selectedTimezone="Europe/Paris"
      />
    </TwakeMuiThemeProvider>
  )
  return onConfirm
}

const nameField = (): HTMLElement =>
  screen.getByRole('textbox', { name: /booking\.form\.name/ })
const emailField = (): HTMLElement =>
  screen.getByRole('textbox', { name: /booking\.form\.email/ })

describe('BookingConfirmDialog', () => {
  it('labels the fields with persistent, required labels', () => {
    renderDialog()

    expect(nameField()).toBeRequired()
    expect(emailField()).toBeRequired()
    expect(nameField()).not.toHaveAttribute('placeholder')
    expect(emailField()).not.toHaveAttribute('placeholder')
    expect(screen.getByText('booking.form.name')).toBeInTheDocument()
    expect(screen.getByText('booking.form.email')).toBeInTheDocument()
  })

  it('explains the required mark', () => {
    renderDialog()

    expect(screen.getByText('booking.form.requiredHint')).toBeInTheDocument()
  })

  it('lets the browser autofill the visitor identity', () => {
    renderDialog()

    expect(nameField()).toHaveAttribute('autocomplete', 'name')
    expect(emailField()).toHaveAttribute('autocomplete', 'email')
    expect(emailField()).toHaveAttribute('type', 'email')
  })

  it('moves focus to the name field when it is missing', () => {
    renderDialog()

    fireEvent.click(
      screen.getByRole('button', { name: 'booking.confirm.button' })
    )

    expect(nameField()).toHaveFocus()
    expect(nameField()).toHaveAccessibleDescription(
      'booking.error.nameRequired'
    )
  })

  it('moves focus to the email field when it is missing', () => {
    renderDialog()

    fireEvent.change(nameField(), { target: { value: 'Visitor' } })
    fireEvent.click(
      screen.getByRole('button', { name: 'booking.confirm.button' })
    )

    expect(emailField()).toHaveFocus()
    expect(emailField()).toHaveAccessibleDescription(
      'booking.error.emailRequired'
    )
  })

  it('books the slot with valid values', async () => {
    const onConfirm = renderDialog()

    fireEvent.change(nameField(), { target: { value: 'Visitor' } })
    fireEvent.change(emailField(), {
      target: { value: 'visitor@example.com' }
    })
    fireEvent.click(
      screen.getByRole('button', { name: 'booking.confirm.button' })
    )

    await waitFor(() =>
      expect(onConfirm).toHaveBeenCalledWith('Visitor', 'visitor@example.com')
    )
  })
})
