import { EventCounterModal } from '@common/features/Events/AttendanceValidation/EventCounterModal'
import { makeCounterProposalPayload } from '@common/features/Events/transformers/makeCounterProposalPayload'
import * as EventDao from '@common/features/Events/EventDao'
import { ContextualizedEvent } from '@common/types/EventsTypes'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '../../utils/Renderwithproviders'

jest.mock('@common/features/Events/EventDao', () => ({
  postCounterProposal: jest.fn()
}))

jest.mock(
  '@common/features/Events/transformers/makeCounterProposalPayload',
  () => ({
    makeCounterProposalPayload: jest.fn(() => ({ ical: 'BEGIN:VCALENDAR' }))
  })
)

const makeContext = (
  start: string,
  end: string,
  timezone = 'UTC'
): ContextualizedEvent =>
  ({
    event: {
      uid: 'event-1',
      calId: 'user1/cal1',
      title: 'Secret plan',
      start,
      end,
      timezone,
      allday: false,
      organizer: { cal_address: 'carol@example.com' },
      attendee: [{ cal_address: 'bob@example.com', partstat: 'NEEDS-ACTION' }]
    },
    calendar: {
      id: 'user1/cal1',
      name: 'Test',
      delegated: false,
      owner: { emails: ['bob@example.com'] },
      events: {}
    },
    currentUserAttendee: {
      cal_address: 'bob@example.com',
      partstat: 'NEEDS-ACTION'
    },
    isOwn: true,
    isOrganizer: false,
    isRecurring: false
  }) as unknown as ContextualizedEvent

describe('EventCounterModal', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  it('shows the validation error when the proposed end is not after the start', async () => {
    renderWithProviders(
      <EventCounterModal
        open
        setOpen={jest.fn()}
        contextualizedEvent={makeContext(
          '2099-01-15T20:00:00Z',
          '2099-01-15T20:00:00Z'
        )}
      />
    )

    fireEvent.click(screen.getByText('eventPreview.sendProposal'))

    expect(
      await screen.findByText('event.validation.endAfterStart')
    ).toBeInTheDocument()
    expect(EventDao.postCounterProposal).not.toHaveBeenCalled()
  })

  it('shows an error when sending the proposal fails', async () => {
    ;(EventDao.postCounterProposal as jest.Mock).mockRejectedValue(
      new Error('boom')
    )
    jest.spyOn(console, 'error').mockImplementation(() => {})
    const setOpen = jest.fn()

    renderWithProviders(
      <EventCounterModal
        open
        setOpen={setOpen}
        contextualizedEvent={makeContext(
          '2099-01-15T20:00:00Z',
          '2099-01-15T21:00:00Z'
        )}
      />
    )

    fireEvent.click(screen.getByText('eventPreview.sendProposal'))

    expect(await screen.findByRole('alert')).toHaveTextContent('error.unknown')
    await waitFor(() =>
      expect(EventDao.postCounterProposal).toHaveBeenCalledTimes(1)
    )
    expect(setOpen).not.toHaveBeenCalledWith(false)
  })

  it('shows the event and the proposal in the user time zone, not the organizer one', async () => {
    ;(EventDao.postCounterProposal as jest.Mock).mockResolvedValue(undefined)

    renderWithProviders(
      <EventCounterModal
        open
        setOpen={jest.fn()}
        contextualizedEvent={makeContext(
          '2099-01-15T08:00:00Z',
          '2099-01-15T09:00:00Z',
          'Europe/Paris'
        )}
      />,
      { settings: { timeZone: 'Asia/Tokyo' } } as never
    )

    expect(screen.getByText(/UTC\+9/)).toBeInTheDocument()
    expect(screen.getByDisplayValue('17:00')).toBeInTheDocument()
    expect(screen.getByDisplayValue('18:00')).toBeInTheDocument()

    fireEvent.click(screen.getByText('eventPreview.sendProposal'))

    await waitFor(() =>
      expect(makeCounterProposalPayload).toHaveBeenCalledWith(
        expect.objectContaining({
          proposedStart: '2099-01-15T08:00:00.000Z',
          proposedEnd: '2099-01-15T09:00:00.000Z'
        })
      )
    )
  })
})
