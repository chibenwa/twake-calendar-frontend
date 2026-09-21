import { fireEvent, renderHook, screen } from '@testing-library/react'
import { useFilterEventAttendees } from '@common/components/Event/hooks/useFilterEventAttendees'
import { renderAttendeeBadge } from '@common/components/Event/utils/eventUtils'
import { VObjectProperty } from '@common/features/Calendars/types/CalendarData'
import { parseCalendarEvent } from '@common/features/Events/utils'
import { userAttendee } from '@common/features/User/models/attendee'
import { userOrganiser } from '@common/features/User/userDataTypes'
import { Calendar } from '@common/types/CalendarTypes'
import { CalendarEvent } from '@common/types/EventsTypes'
import { renderWithProviders } from '../../utils/Renderwithproviders'

const calendar = { id: 'calendar-123' } as Calendar

function parseOrganizerOf(
  organizerProp: VObjectProperty
): CalendarEvent['organizer'] {
  const event = parseCalendarEvent({
    data: [
      ['UID', {}, 'text', 'event-1'],
      ['DTSTART', {}, 'date-time', '2025-07-18T09:00:00Z'],
      organizerProp
    ] as VObjectProperty[],
    color: { light: '#00FF00' },
    calendar,
    eventURL: '/calendars/test.ics'
  })

  return event.organizer
}

function eventWith(organizer: userOrganiser): CalendarEvent {
  return { organizer, attendee: [] } as unknown as CalendarEvent
}

function t(key: string, vars?: Record<string, string>): string {
  if (!vars) return key

  const params = Object.entries(vars)
    .map(([name, value]) => `${name}=${value}`)
    .join(',')

  return `${key}(${params})`
}

describe('organizer SENT-BY parsing', () => {
  it('reads the delegate stated by the SENT-BY parameter', () => {
    const organizer = parseOrganizerOf([
      'ORGANIZER',
      { cn: 'Alice', 'sent-by': 'mailto:delegate@domain.tld' },
      'cal-address',
      'mailto:alice@example.com'
    ])

    expect(organizer?.cal_address).toBe('alice@example.com')
    expect(organizer?.sentBy).toBe('delegate@domain.tld')
  })

  it('leaves the delegate undefined when no SENT-BY is stated', () => {
    const organizer = parseOrganizerOf([
      'ORGANIZER',
      { cn: 'Alice' },
      'cal-address',
      'mailto:alice@example.com'
    ])

    expect(organizer?.sentBy).toBeUndefined()
  })

  it('writes the delegate back as a mailto URI', () => {
    const organizer = new userOrganiser({
      cn: 'Alice',
      cal_address: 'alice@example.com',
      sentBy: 'delegate@domain.tld'
    })

    expect(organizer.asJcal()).toEqual([
      'organizer',
      { cn: 'Alice', 'sent-by': 'mailto:delegate@domain.tld' },
      'cal-address',
      'mailto:alice@example.com'
    ])
  })
})

describe('useFilterEventAttendees organizerSentBy', () => {
  it('discloses a delegate distinct from the organizer', () => {
    const organizer = new userOrganiser({
      cal_address: 'alice@example.com',
      sentBy: 'delegate@domain.tld'
    })

    const { result } = renderHook(() =>
      useFilterEventAttendees({ event: eventWith(organizer) })
    )

    expect(result.current.organizerSentBy).toBe('delegate@domain.tld')
  })

  it('stays silent when the organizer sent the event themselves', () => {
    const organizer = new userOrganiser({
      cal_address: 'alice@example.com',
      sentBy: 'mailto:Alice@Example.com'
    })

    const { result } = renderHook(() =>
      useFilterEventAttendees({ event: eventWith(organizer) })
    )

    expect(result.current.organizerSentBy).toBeUndefined()
  })
})

describe('organizer badge with a delegate', () => {
  const organizer = new userAttendee({
    cal_address: 'alice@example.com',
    cn: 'Alice'
  })

  const renderBadge = (): void => {
    renderWithProviders(
      renderAttendeeBadge({
        a: organizer,
        key: 'org',
        t,
        isFull: true,
        isOrganizer: true,
        sentBy: 'delegate@domain.tld'
      }),
      { user: { userData: { email: 'bob@example.com' } } }
    )
  }

  it('spells the delegate out below the organizer name', () => {
    renderBadge()

    expect(screen.getByText('Alice')).toBeInTheDocument()
    expect(
      screen.getByText('event.sentBy(email=delegate@domain.tld)')
    ).toBeInTheDocument()
  })

  it('opens the delegate contact card on click', () => {
    renderBadge()

    fireEvent.click(screen.getByText('event.sentBy(email=delegate@domain.tld)'))

    expect(screen.getByText('delegate@domain.tld')).toBeInTheDocument()
  })

  it('opens the organizer contact card on click', () => {
    renderBadge()

    fireEvent.click(screen.getByText('Alice'))

    expect(screen.getByText('alice@example.com')).toBeInTheDocument()
  })
})
