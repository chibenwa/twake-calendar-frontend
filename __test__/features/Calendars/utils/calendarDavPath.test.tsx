import {
  calendarDavPath,
  encodeDavSegment,
  eventDavPath
} from '@common/features/Calendars/utils/calendarDavPath'
import { normalizeCalendar } from '@common/features/Calendars/utils/normalizeCalendar'
import { CalendarData } from '@common/features/Calendars/types/CalendarData'

const OWNER = 'owner1'
const SHAREE = 'sharee1'

function rawCalendar(overrides: Partial<CalendarData>): CalendarData {
  return {
    _links: { self: { href: `/calendars/${OWNER}/cal1.json` } },
    acl: [],
    _embedded: { 'sync-token': '', 'dav:item': [] },
    ...overrides
  } as CalendarData
}

describe('calendarDavPath', () => {
  it('reads an own calendar from its own node', () => {
    const cal = normalizeCalendar(rawCalendar({}), OWNER)

    expect(calendarDavPath(cal)).toBe(`/calendars/${OWNER}/cal1`)
  })

  it("reads a delegated calendar through the sharee's instance, never the owner's node", () => {
    const cal = normalizeCalendar(
      rawCalendar({
        _links: { self: { href: `/calendars/${SHAREE}/instance1.json` } },
        'calendarserver:delegatedsource': `/calendars/${OWNER}/cal1.json`
      }),
      SHAREE
    )

    expect(cal.id).toBe(`${OWNER}/cal1`)
    expect(cal.ownerId).toBe(OWNER)
    expect(calendarDavPath(cal)).toBe(`/calendars/${SHAREE}/instance1`)
  })

  it('reads a public subscription from its source, as before', () => {
    const cal = normalizeCalendar(
      rawCalendar({
        _links: { self: { href: `/calendars/${SHAREE}/subscription1.json` } },
        'calendarserver:source': {
          _links: { self: { href: `/calendars/${OWNER}/cal1.json` } }
        }
      }),
      SHAREE
    )

    expect(calendarDavPath(cal)).toBe(`/calendars/${OWNER}/cal1`)
  })

  it('falls back to the id of a delegated calendar that has no link', () => {
    expect(
      calendarDavPath({ id: `${OWNER}/cal1`, link: '', delegated: true })
    ).toBe(`/calendars/${OWNER}/cal1`)
  })
})

describe('eventDavPath', () => {
  it("writes an event of a delegated calendar into the sharee's instance", () => {
    expect(
      eventDavPath(
        {
          id: `${OWNER}/cal1`,
          link: `/calendars/${SHAREE}/instance1.json`,
          delegated: true
        },
        'event-uid'
      )
    ).toBe(`/calendars/${SHAREE}/instance1/event-uid.ics`)
  })

  it('writes an event of an own calendar into its own node', () => {
    expect(
      eventDavPath(
        { id: `${OWNER}/cal1`, link: `/calendars/${OWNER}/cal1.json` },
        'event-uid'
      )
    ).toBe(`/calendars/${OWNER}/cal1/event-uid.ics`)
  })

  it('keeps an event UID chosen by someone else within its own segment', () => {
    expect(
      eventDavPath(
        { id: `${OWNER}/cal1`, link: `/calendars/${OWNER}/cal1.json` },
        '../other/x?y#z'
      )
    ).toBe(`/calendars/${OWNER}/cal1/..%2Fother%2Fx%3Fy%23z.ics`)
  })
})

describe('encodeDavSegment', () => {
  it.each([
    [
      '0b9ab7e8-6a4f-4b1d-9d7e-2f1c3a4b5c6d',
      '0b9ab7e8-6a4f-4b1d-9d7e-2f1c3a4b5c6d'
    ],
    [
      '040000008200E00074C5B7101A82E008@google.com',
      '040000008200E00074C5B7101A82E008@google.com'
    ],
    ['urn:uuid:1234', 'urn:uuid:1234'],
    ['a_b.c~d(e)', 'a_b.c~d(e)']
  ])('leaves an ordinary UID as the server writes it: %s', (uid, encoded) => {
    expect(encodeDavSegment(uid)).toBe(encoded)
  })

  it.each([
    ['a/b', 'a%2Fb'],
    ['a%2Fb', 'a%252Fb'],
    ['a?b#c', 'a%3Fb%23c'],
    ['a b', 'a%20b'],
    ["a!*'b", 'a%21%2A%27b'],
    ['é', '%C3%A9']
  ])('encodes %s', (uid, encoded) => {
    expect(encodeDavSegment(uid)).toBe(encoded)
  })
})
