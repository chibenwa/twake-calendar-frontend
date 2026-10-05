import {
  AllDayToggleParams,
  useAllDayToggle
} from '@common/components/Event/hooks/useAllDayToggle'
import { renderHook } from '@testing-library/react'

const PARIS = 'Europe/Paris'
const TONGATAPU = 'Pacific/Tongatapu'

function untickAllDay(overrides: Partial<AllDayToggleParams> = {}): jest.Mock {
  const onAllDayChange = jest.fn()
  const { result } = renderHook(() =>
    useAllDayToggle({
      allday: true,
      start: '2026-10-10',
      end: '2026-10-10',
      startDate: '2026-10-10',
      startTime: '',
      endDate: '2026-10-10',
      endTime: '',
      timezone: PARIS,
      setStartTime: jest.fn(),
      setEndTime: jest.fn(),
      setStart: jest.fn(),
      setEnd: jest.fn(),
      setAllDay: jest.fn(),
      onAllDayChange,
      ...overrides
    })
  )

  result.current.handleAllDayToggle()
  return onAllDayChange
}

describe('useAllDayToggle (#1514)', () => {
  afterEach(() => {
    jest.useRealTimers()
  })

  it('moves the end to the next day when the proposed slot crosses midnight', () => {
    // 22:30 in Paris
    jest.useFakeTimers().setSystemTime(new Date('2026-10-05T20:30:00Z'))

    const onAllDayChange = untickAllDay()

    expect(onAllDayChange).toHaveBeenCalledWith(
      false,
      '2026-10-10T23:00',
      '2026-10-11T00:00'
    )
  })

  it('keeps the event day when the proposed slot does not cross midnight', () => {
    // 14:30 in Paris
    jest.useFakeTimers().setSystemTime(new Date('2026-10-05T12:30:00Z'))

    const onAllDayChange = untickAllDay()

    expect(onAllDayChange).toHaveBeenCalledWith(
      false,
      '2026-10-10T15:00',
      '2026-10-10T16:00'
    )
  })

  it('keeps the last day of a multi-day event when the slot crosses midnight', () => {
    // 22:30 in Paris
    jest.useFakeTimers().setSystemTime(new Date('2026-10-05T20:30:00Z'))

    const onAllDayChange = untickAllDay({
      end: '2026-10-12',
      endDate: '2026-10-12'
    })

    expect(onAllDayChange).toHaveBeenCalledWith(
      false,
      '2026-10-10T23:00',
      '2026-10-12T00:00'
    )
  })

  it('proposes the next round hour in the zone of the event, not in the one of the browser', () => {
    // 09:30 in Tongatapu (UTC+13)
    jest.useFakeTimers().setSystemTime(new Date('2026-10-05T20:30:00Z'))

    const onAllDayChange = untickAllDay({ timezone: TONGATAPU })

    expect(onAllDayChange).toHaveBeenCalledWith(
      false,
      '2026-10-10T10:00',
      '2026-10-10T11:00'
    )
  })
})
