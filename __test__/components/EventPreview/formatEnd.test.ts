import { formatEnd } from '@common/components/EventPreview/utils/formatEnd'

const t = (key: string): string => (key === 'locale' ? 'en-US' : key)

describe('formatEnd', () => {
  it('shows the inclusive last day of a multi-day all-day event', () => {
    expect(
      formatEnd({
        start: '2026-10-12',
        end: '2026-10-15',
        t,
        timeZone: 'UTC',
        allday: true
      })
    ).toBe('Oct 14, 2026')
  })

  it('shows no end for a single-day all-day event', () => {
    expect(
      formatEnd({
        start: '2026-10-15',
        end: '2026-10-16',
        t,
        timeZone: 'UTC',
        allday: true
      })
    ).toBeNull()
  })

  it('shows no end for an all-day event ending before it starts', () => {
    expect(
      formatEnd({
        start: '2026-10-15',
        end: '2026-10-13',
        t,
        timeZone: 'UTC',
        allday: true
      })
    ).toBeNull()
  })

  it('shows no end for an all-day event whose end equals its start', () => {
    expect(
      formatEnd({
        start: '2026-10-15',
        end: '2026-10-15',
        t,
        timeZone: 'UTC',
        allday: true
      })
    ).toBeNull()
  })

  it('shows the end time of a same-day timed event', () => {
    expect(
      formatEnd({
        start: '2026-10-15T10:00:00Z',
        end: '2026-10-15T11:30:00Z',
        t,
        timeZone: 'UTC'
      })
    ).toBe('11:30')
  })

  it('shows no end for a timed event ending before it starts', () => {
    expect(
      formatEnd({
        start: '2026-10-15T10:00:00Z',
        end: '2026-10-12T09:00:00Z',
        t,
        timeZone: 'UTC'
      })
    ).toBeNull()
  })

  it('shows no end for an all-day event whose end it cannot read', () => {
    expect(
      formatEnd({
        start: '2025-03-10',
        end: '20250312',
        t,
        timeZone: 'UTC',
        allday: true
      })
    ).toBeNull()
  })

  it('shows no end for a timed event whose end it cannot read', () => {
    expect(
      formatEnd({
        start: '2025-12-30T16:00:00.000Z',
        end: 'not a date',
        t,
        timeZone: 'Asia/Ho_Chi_Minh'
      })
    ).toBeNull()
  })

  it('shows no end for an event whose start it cannot read', () => {
    expect(
      formatEnd({
        start: 'not a date',
        end: '2025-12-30T17:00:00.000Z',
        t,
        timeZone: 'UTC'
      })
    ).toBeNull()
  })
})
