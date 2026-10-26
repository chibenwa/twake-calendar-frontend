import Polyglot from 'node-polyglot'
import {
  buildEventChipLabel,
  EventChipLabelParts
} from '@common/components/Event/utils/eventChipLabel'
import en from '@common/locales/en.json'
import fr from '@common/locales/fr.json'

const translatorOf =
  (locale: string, phrases: object) =>
  (key: string, options?: Record<string, unknown>): string =>
    new Polyglot({ locale, phrases }).t(key, options)

const t = translatorOf('en', en)

const meeting: EventChipLabelParts = {
  title: 'a10y-audit-1 meeting',
  start: new Date('2026-10-25T17:00:00Z'),
  end: new Date('2026-10-25T18:00:00Z'),
  allDay: false,
  dayZone: 'Europe/Paris',
  timeZone: 'Europe/Paris',
  calendarName: 'My calendar'
}

describe('buildEventChipLabel', () => {
  it('names the title, the day, the times and the calendar', () => {
    expect(buildEventChipLabel(meeting, t)).toBe(
      'a10y-audit-1 meeting, Sunday, 25 October 2026, 18:00 – 19:00, My calendar'
    )
  })

  it.each([
    ['DECLINED', 'You declined'],
    ['TENTATIVE', 'You tentatively accepted'],
    ['NEEDS-ACTION', 'Awaiting your response']
  ])('announces the %s answer of the user', (partstat, status) => {
    expect(buildEventChipLabel({ ...meeting, partstat }, t)).toBe(
      `a10y-audit-1 meeting, Sunday, 25 October 2026, 18:00 – 19:00, My calendar, ${status}`
    )
  })

  it('keeps an accepted invitation silent', () => {
    expect(
      buildEventChipLabel({ ...meeting, partstat: 'ACCEPTED' }, t)
    ).not.toContain('You are attending')
  })

  it('announces an all-day event spanning several days with its range', () => {
    expect(
      buildEventChipLabel(
        {
          ...meeting,
          title: 'Holidays',
          start: new Date('2026-10-26T00:00:00Z'),
          end: new Date('2026-10-29T00:00:00Z'),
          allDay: true,
          dayZone: 'UTC',
          timeZone: 'America/New_York'
        },
        t
      )
    ).toBe(
      'Holidays, Monday, 26 October 2026 – Wednesday, 28 October 2026, All day, My calendar'
    )
  })

  it('announces a one-day all-day event with its single day', () => {
    expect(
      buildEventChipLabel(
        {
          ...meeting,
          start: new Date('2026-10-26T00:00:00Z'),
          end: new Date('2026-10-27T00:00:00Z'),
          allDay: true,
          dayZone: 'UTC'
        },
        t
      )
    ).toBe(
      'a10y-audit-1 meeting, Monday, 26 October 2026, All day, My calendar'
    )
  })

  it('reads the day in the zone of the user', () => {
    expect(
      buildEventChipLabel(
        {
          ...meeting,
          start: new Date('2026-10-26T03:00:00Z'),
          end: new Date('2026-10-26T04:00:00Z'),
          dayZone: 'America/New_York',
          timeZone: 'America/New_York'
        },
        t
      )
    ).toBe(
      'a10y-audit-1 meeting, Sunday, 25 October 2026, 23:00 – 00:00, My calendar'
    )
  })

  it('shows the start only when the event has no end after it', () => {
    expect(buildEventChipLabel({ ...meeting, end: null }, t)).toBe(
      'a10y-audit-1 meeting, Sunday, 25 October 2026, 18:00, My calendar'
    )
  })

  it('names an event without title as untitled', () => {
    expect(buildEventChipLabel({ ...meeting, title: '' }, t)).toMatch(
      /^Untitled, /
    )
  })

  it('speaks the language of the user', () => {
    expect(
      buildEventChipLabel(
        { ...meeting, partstat: 'DECLINED' },
        translatorOf('fr', fr)
      )
    ).toBe(
      `a10y-audit-1 meeting, dimanche 25 octobre 2026, 18:00 – 19:00, My calendar, ${fr.eventPreview.attendee.DECLINED}`
    )
  })
})
