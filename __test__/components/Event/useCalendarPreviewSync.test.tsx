import type { EventFormHandle } from '@common/components/Event/EventFormFields.types'
import { useCalendarPreviewSync } from '@common/components/Event/hooks/useCalendarPreviewSync'
import { CalendarApi, DateSelectArg } from '@fullcalendar/core'
import { renderHook } from '@testing-library/react'
import React from 'react'

const TONGATAPU = 'Pacific/Tongatapu'

type RangeUpdate = (prev: DateSelectArg | null) => DateSelectArg

function renderWithFormIn(timezone: string): {
  result: { current: ReturnType<typeof useCalendarPreviewSync> }
  nextRange: () => DateSelectArg
} {
  const formRef = {
    current: {
      getValues: () => ({ allday: false, timezone })
    }
  } as unknown as React.RefObject<EventFormHandle | null>
  const calendarRef = { current: null } as React.RefObject<CalendarApi | null>
  const setSelectedRange = jest.fn()

  const { result } = renderHook(() =>
    useCalendarPreviewSync({ formRef, setSelectedRange, calendarRef })
  )

  const nextRange = (): DateSelectArg => {
    const update: RangeUpdate = setSelectedRange.mock.lastCall?.[0]
    return update(null)
  }

  return { result, nextRange }
}

describe('useCalendarPreviewSync (#1398)', () => {
  it('reads an edited start in the zone of the event, not in the one of the browser', () => {
    const { result, nextRange } = renderWithFormIn(TONGATAPU)

    result.current.handleStartChange('2026-09-24T05:00')

    expect(nextRange().start.toISOString()).toBe('2026-09-23T16:00:00.000Z')
    expect(nextRange().startStr).toBe('2026-09-24T05:00')
  })

  it('reads an edited end in the zone of the event, not in the one of the browser', () => {
    const { result, nextRange } = renderWithFormIn(TONGATAPU)

    result.current.handleEndChange('2026-09-24T06:00')

    expect(nextRange().end.toISOString()).toBe('2026-09-23T17:00:00.000Z')
  })

  it('reads the bounds of a timed range in the zone of the event when all day is unticked', () => {
    const { result, nextRange } = renderWithFormIn(TONGATAPU)

    result.current.handleAllDayChange(
      false,
      '2026-09-24T05:00',
      '2026-09-24T06:00'
    )

    expect(nextRange().start.toISOString()).toBe('2026-09-23T16:00:00.000Z')
    expect(nextRange().end.toISOString()).toBe('2026-09-23T17:00:00.000Z')
  })
})
