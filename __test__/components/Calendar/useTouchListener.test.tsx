import { buildSelectArg } from '@common/components/Calendar/hooks/useTouchListener'

describe('buildSelectArg (#1398)', () => {
  it('reads the tapped slot in the zone of the grid, not in the one of the browser', () => {
    const selection = buildSelectArg(
      '2026-09-24',
      '05:00:00',
      'Pacific/Tongatapu'
    )

    expect(selection.start.toISOString()).toBe('2026-09-23T16:00:00.000Z')
    expect(selection.end.toISOString()).toBe('2026-09-23T16:30:00.000Z')
    expect(selection.startStr).toBe('2026-09-24T05:00:00+13:00')
    expect(selection.endStr).toBe('2026-09-24T05:30:00+13:00')
    expect(selection.allDay).toBe(false)
  })
})
