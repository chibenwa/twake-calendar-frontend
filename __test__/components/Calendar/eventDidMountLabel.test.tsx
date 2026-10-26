import Polyglot from 'node-polyglot'
import {
  createViewHandlers,
  ViewHandlers
} from '@common/components/Calendar/handlers/viewHandlers'
import { EventErrorHandler } from '@common/components/Error/EventErrorHandler'
import en from '@common/locales/en.json'
import { Calendar } from '@common/types/CalendarTypes'
import { EventMountArg } from '@fullcalendar/core'

const polyglot = new Polyglot({ locale: 'en', phrases: en })
const t = (key: string, options?: Record<string, unknown>): string =>
  polyglot.t(key, options)

const calendar = {
  id: 'bob/cal1',
  name: 'My calendar',
  owner: { emails: ['bob@example.com'] }
} as unknown as Calendar

const handlersFor = (calendars: Record<string, Calendar>): ViewHandlers =>
  createViewHandlers({
    calendarRef: { current: null },
    setSelectedDate: jest.fn(),
    setSelectedMiniDate: jest.fn(),
    calendars,
    tempcalendars: {},
    errorHandler: {} as EventErrorHandler,
    timezone: 'Europe/Paris',
    isTablet: false,
    isMobile: false,
    t
  })

const mountArg = (
  overrides: { partstat?: string; allDay?: boolean; viewType?: string } = {}
): EventMountArg =>
  ({
    el: document.createElement('a'),
    view: { type: overrides.viewType ?? 'timeGridWeek' },
    event: {
      id: 'event1',
      title: 'a10y-audit-1 meeting',
      start: new Date('2026-10-25T17:00:00Z'),
      end: new Date('2026-10-25T18:00:00Z'),
      allDay: overrides.allDay ?? false,
      _def: {
        extendedProps: {
          calId: 'bob/cal1',
          organizer: { cal_address: 'mailto:alice@example.com' },
          attendee: overrides.partstat
            ? [
                {
                  cal_address: 'mailto:bob@example.com',
                  partstat: overrides.partstat
                }
              ]
            : []
        }
      }
    }
  }) as unknown as EventMountArg

describe('handleEventDidMount accessible name (#1784)', () => {
  it('exposes an event as a button named after its day, time and calendar', () => {
    const arg = mountArg()

    handlersFor({ 'bob/cal1': calendar }).handleEventDidMount(arg)

    expect(arg.el.getAttribute('role')).toBe('button')
    expect(arg.el.getAttribute('aria-label')).toBe(
      'a10y-audit-1 meeting, Sunday, 25 October 2026, 18:00 – 19:00, My calendar'
    )
  })

  it.each([
    ['DECLINED', 'You declined'],
    ['NEEDS-ACTION', 'Awaiting your response']
  ])('announces the %s answer of the owner', (partstat, status) => {
    const arg = mountArg({ partstat })

    handlersFor({ 'bob/cal1': calendar }).handleEventDidMount(arg)

    expect(arg.el.getAttribute('aria-label')).toMatch(
      new RegExp(`, My calendar, ${status}$`)
    )
  })

  it('announces an all-day event as such', () => {
    const arg = mountArg({ allDay: true })

    handlersFor({ 'bob/cal1': calendar }).handleEventDidMount(arg)

    expect(arg.el.getAttribute('aria-label')).toContain(', All day, ')
  })

  it('keeps the row role of list views', () => {
    const arg = mountArg({ viewType: 'listWeek' })

    handlersFor({ 'bob/cal1': calendar }).handleEventDidMount(arg)

    expect(arg.el.hasAttribute('role')).toBe(false)
  })
})
