import CalendarSelection from '../../apps/private/src/components/Calendar/CalendarSelection'
import '@testing-library/jest-dom'
import { fireEvent, screen, within } from '@testing-library/react'
import { renderWithProviders } from '../utils/Renderwithproviders'

jest.mock('@common/useScreenSizeDetection', () => ({
  useScreenSizeDetection: (): { isTooSmall: boolean; isTablet: boolean } => ({
    isTooSmall: true,
    isTablet: false
  })
}))

jest.mock(
  '../../apps/private/src/features/booking/CreateAppointmentModal',
  () => ({
    CreateAppointmentModal: (): null => null
  })
)

jest.mock('@common/features/booking/BookingDao', () => ({
  listBookingLinks: jest.fn().mockResolvedValue([])
}))

describe('CalendarSelection on a phone-sized screen', () => {
  const renderSelection = (): void => {
    renderWithProviders(
      <CalendarSelection
        selectedCalendars={['user1/cal1']}
        setSelectedCalendars={jest.fn()}
      />,
      {
        user: {
          userData: {
            sub: 'test',
            email: 'test@test.com',
            sid: 'mockSid',
            openpaasId: 'user1'
          },
          tokens: { accessToken: 'token' }
        },
        calendars: {
          list: {
            'user1/cal1': {
              name: 'Calendar personal',
              id: 'user1/cal1',
              color: '#FF0000',
              owner: { emails: ['alice@example.com'], lastname: 'alice' }
            }
          },
          pending: false
        }
      }
    )
  }

  it('opens the calendar actions sheet without a touch long press', async () => {
    renderSelection()

    const row = screen
      .getByRole('checkbox', { name: 'Calendar personal' })
      .closest('li') as HTMLElement
    fireEvent.click(
      within(row).getByRole('button', { name: 'common.moreOptions' })
    )

    expect(
      await screen.findByRole('button', { name: 'actions.modify' })
    ).toBeInTheDocument()
  })
})
