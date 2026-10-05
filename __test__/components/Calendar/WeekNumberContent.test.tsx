import { WeekNumberContent } from '@common/components/Calendar/WeekNumberContent'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '../../utils/Renderwithproviders'

describe('WeekNumberContent', () => {
  const props = {
    num: 41,
    timezone: 'Europe/Paris',
    selectedDate: new Date('2026-10-06T10:00:00Z')
  }

  it('shows the week number and the time zone selector', () => {
    renderWithProviders(<WeekNumberContent {...props} displayWeekNumbers />)

    expect(screen.getByText('menubar.views.week 41')).toBeInTheDocument()
    expect(screen.getByText(/UTC/)).toBeInTheDocument()
  })

  it('keeps the time zone selector when week numbers are hidden', () => {
    renderWithProviders(
      <WeekNumberContent {...props} displayWeekNumbers={false} />
    )

    expect(screen.queryByText(/menubar\.views\.week/)).not.toBeInTheDocument()
    expect(screen.getByText(/UTC/)).toBeInTheDocument()
  })
})
