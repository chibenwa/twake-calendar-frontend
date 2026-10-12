import React from 'react'
import { fireEvent, screen, act, waitFor } from '@testing-library/react'
import { renderWithProviders } from '../utils/Renderwithproviders'
import { AttendeePopover } from '@common/components/Attendees/AttendeePopover'
import { userAttendee } from '@common/features/User/models/attendee'
import { useScreenSizeDetection } from '@common/useScreenSizeDetection'

jest.mock('@common/useScreenSizeDetection', () => ({
  useScreenSizeDetection: jest.fn(() => ({
    isTooSmall: false,
    isTablet: false
  }))
}))

describe('AttendeePopover (Private)', () => {
  beforeEach(() => {
    window.MAIL_SPA_URL = 'https://mail.example.com'
  })

  it('uses private AttendeeActions implementation with chat and create event actions', () => {
    const attendee = new userAttendee({
      cal_address: 'john@example.com',
      cn: 'John Doe'
    })

    renderWithProviders(
      <AttendeePopover attendee={attendee}>
        <span>Open Popover</span>
      </AttendeePopover>,
      {
        user: {
          userData: {
            email: 'user@example.com',
            workplaceFqdn: 'example.com'
          }
        }
      }
    )

    const trigger = screen.getByText('Open Popover')
    fireEvent.click(trigger)

    // Should render mail action button
    expect(screen.getByText('attendees.sendMail')).toBeInTheDocument()

    // Should render create event action button present in private AttendeeActions
    expect(
      screen.getByLabelText(
        'tooltip.createEventWithAttendee(attendee=John Doe)'
      )
    ).toBeInTheDocument()
  })

  it('opens popover after pending hover delay on mouse enter and closes on mouse leave', () => {
    jest.useFakeTimers()
    const attendee = new userAttendee({
      cal_address: 'john@example.com',
      cn: 'John Doe'
    })

    renderWithProviders(
      <AttendeePopover attendee={attendee}>
        <span>Open Popover</span>
      </AttendeePopover>,
      {
        user: {
          userData: {
            email: 'user@example.com',
            workplaceFqdn: 'example.com'
          }
        }
      }
    )

    const trigger = screen.getByText('Open Popover')
    expect(screen.queryByText('attendees.sendMail')).not.toBeInTheDocument()

    // Mouse enter on trigger
    fireEvent.mouseEnter(trigger)
    // Not open yet before pending time delay
    expect(screen.queryByText('attendees.sendMail')).not.toBeInTheDocument()

    // Fast-forward open timer
    act(() => {
      jest.advanceTimersByTime(500)
    })
    expect(screen.getByText('attendees.sendMail')).toBeInTheDocument()

    // Mouse leave from trigger
    fireEvent.mouseLeave(trigger)
    // Still open before close timer runs
    expect(screen.getByText('attendees.sendMail')).toBeInTheDocument()

    // Fast-forward close timer
    act(() => {
      jest.advanceTimersByTime(200)
    })
    expect(screen.queryByText('attendees.sendMail')).not.toBeInTheDocument()

    jest.useRealTimers()
  })

  it('does not open popover on hover when openOnHover is false but opens on click', () => {
    jest.useFakeTimers()
    const attendee = new userAttendee({
      cal_address: 'john@example.com',
      cn: 'John Doe'
    })

    renderWithProviders(
      <AttendeePopover attendee={attendee} openOnHover={false}>
        <span>Open Popover</span>
      </AttendeePopover>,
      {
        user: {
          userData: {
            email: 'user@example.com',
            workplaceFqdn: 'example.com'
          }
        }
      }
    )

    const trigger = screen.getByText('Open Popover')

    fireEvent.mouseEnter(trigger)
    act(() => {
      jest.advanceTimersByTime(1000)
    })
    expect(screen.queryByText('attendees.sendMail')).not.toBeInTheDocument()

    fireEvent.click(trigger)
    expect(screen.getByText('attendees.sendMail')).toBeInTheDocument()

    fireEvent.mouseLeave(trigger)
    act(() => {
      jest.advanceTimersByTime(1000)
    })
    expect(screen.getByText('attendees.sendMail')).toBeInTheDocument()

    jest.useRealTimers()
  })

  describe('placement', () => {
    const openCard = (): HTMLElement => {
      renderWithProviders(
        <AttendeePopover
          attendee={new userAttendee({ cal_address: 'bob@example.com' })}
        >
          <span>Open Popover</span>
        </AttendeePopover>,
        {
          user: {
            userData: {
              email: 'user@example.com',
              workplaceFqdn: 'example.com'
            }
          }
        }
      )
      fireEvent.click(screen.getByText('Open Popover'))
      return screen.getByRole('tooltip')
    }

    afterEach(() => {
      ;(useScreenSizeDetection as jest.Mock).mockReturnValue({
        isTooSmall: false,
        isTablet: false
      })
    })

    it('opens the contact card on the right of the attendee on desktop', async () => {
      const card = openCard()

      await waitFor(() =>
        expect(card).toHaveAttribute('data-popper-placement', 'right')
      )
    })

    it('opens the contact card below the attendee on mobile so it stays on screen', async () => {
      ;(useScreenSizeDetection as jest.Mock).mockReturnValue({
        isTooSmall: true,
        isTablet: false
      })

      const card = openCard()

      await waitFor(() =>
        expect(card).toHaveAttribute('data-popper-placement', 'bottom-start')
      )
    })
  })
})
