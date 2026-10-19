import { formatMobileEventSpan } from '@common/features/Search/mobileSearchResultsComponents'
import {
  lastDayOf,
  RenderTime
} from '@common/features/Search/searchResultsComponents'
import { render, screen } from '@testing-library/react'

// An event whose DTEND precedes its DTSTART, accepted by the server (#1712)
const start = new Date('2026-10-22T10:00:00Z')
const backwardsEnd = new Date('2026-10-22T08:00:00Z')
const t = (key: string): string => (key === 'locale' ? 'en-GB' : key)

describe('search results of an event ending before it starts', () => {
  it('desktop: shows the start time only', () => {
    render(
      <RenderTime
        startDate={start}
        endDate={backwardsEnd}
        allDay={false}
        t={t}
        timeZone="Europe/Paris"
      />
    )

    expect(screen.getByText('12:00')).toBeInTheDocument()
    expect(screen.queryByText(/10:00/)).not.toBeInTheDocument()
  })

  it('desktop: still shows the range of a regular event', () => {
    render(
      <RenderTime
        startDate={start}
        endDate={new Date('2026-10-22T11:00:00Z')}
        allDay={false}
        t={t}
        timeZone="Europe/Paris"
      />
    )

    expect(screen.getByText('12:00 - 13:00')).toBeInTheDocument()
  })

  it('mobile: shows the start time only', () => {
    expect(
      formatMobileEventSpan(start, backwardsEnd, false, 'en-GB', 'Europe/Paris')
    ).toBe('12:00')
  })

  it('mobile: shows the start time only when the end is days before', () => {
    expect(
      formatMobileEventSpan(
        start,
        new Date('2026-10-19T08:00:00Z'),
        false,
        'en-GB',
        'Europe/Paris'
      )
    ).toBe('12:00')
  })

  it('the last day of a backwards event is its first day', () => {
    expect(lastDayOf(start, new Date('2026-10-19T08:00:00Z'), false)).toEqual(
      start
    )
  })
})
