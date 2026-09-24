import { buildCreateEventRange } from '@common/components/Calendar/utils/createEventRange'

describe('buildCreateEventRange (#1407)', () => {
  const now = new Date('2026-09-24T08:20:00Z')

  it('seeds the event on the displayed day, at the next full hour, in day view', () => {
    const range = buildCreateEventRange(
      'timeGridDay',
      new Date('2026-09-27T00:00:00Z'),
      'UTC',
      now
    )

    expect(range?.start.toISOString()).toBe('2026-09-27T09:00:00.000Z')
    expect(range?.end.toISOString()).toBe('2026-09-27T10:00:00.000Z')
    expect(range?.startStr).toBe('2026-09-27T09:00:00Z')
    expect(range?.allDay).toBe(false)
  })

  it('reads the displayed day and the hour in the zone of the grid', () => {
    const range = buildCreateEventRange(
      'timeGridDay',
      new Date('2026-09-26T11:00:00Z'),
      'Pacific/Tongatapu',
      now
    )

    expect(range?.startStr).toBe('2026-09-27T22:00:00+13:00')
    expect(range?.endStr).toBe('2026-09-27T23:00:00+13:00')
  })

  it('keeps the default when the day view shows today', () => {
    expect(
      buildCreateEventRange(
        'timeGridDay',
        new Date('2026-09-24T00:00:00Z'),
        'UTC',
        now
      )
    ).toBeNull()
  })

  it('keeps the default outside of the day view', () => {
    expect(
      buildCreateEventRange(
        'timeGridWeek',
        new Date('2026-09-27T00:00:00Z'),
        'UTC',
        now
      )
    ).toBeNull()
  })

  it('keeps the default when the displayed day is unknown', () => {
    expect(
      buildCreateEventRange('timeGridDay', undefined, 'UTC', now)
    ).toBeNull()
  })
})
