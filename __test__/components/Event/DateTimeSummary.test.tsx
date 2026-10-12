import { DateTimeSummary } from '@common/components/Event/components/DateTimeSummary'
import { RepetitionObject } from '@common/types/Repetition'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '../../utils/Renderwithproviders'

describe('DateTimeSummary', () => {
  const baseProps = {
    startDate: '2099-10-11',
    startTime: '22:00',
    endDate: '2099-10-11',
    endTime: '23:00',
    allday: false,
    timezone: 'Europe/Paris',
    repetition: new RepetitionObject(),
    showEndDate: false,
    onClick: jest.fn()
  }

  it('separates the date from the time range in its accessible name', () => {
    renderWithProviders(<DateTimeSummary {...baseProps} />)

    const name = screen.getByRole('button').getAttribute('aria-label')

    expect(name).toMatch(/2099, 22:00 - 23:00, .*Europe\/Paris, .+/)
    expect(name).not.toMatch(/209922:00/)
  })

  it('omits the time range of all-day events', () => {
    renderWithProviders(<DateTimeSummary {...baseProps} allday />)

    const name = screen.getByRole('button').getAttribute('aria-label')

    expect(name).not.toMatch(/22:00/)
    expect(name).toMatch(/2099, .*Europe\/Paris/)
  })
})
