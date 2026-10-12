import { CALENDAR_VIEWS } from '@common/components/Calendar/utils/constants'
import { MobileMenubar } from '@common/components/Menubar/MobileMenuBar'
import '@testing-library/jest-dom'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '../utils/Renderwithproviders'

describe('MobileMenubar', () => {
  const renderMobileMenubar = (dateLabel: string): HTMLElement => {
    const { container } = renderWithProviders(
      <MobileMenubar
        calendarRef={{ current: null }}
        currentDate={new Date('2026-09-28')}
        currentView={CALENDAR_VIEWS.timeGridWeek}
        dateLabel={dateLabel}
        handleNavigation={jest.fn()}
        onOpenSidebar={jest.fn()}
      />,
      { settings: { view: 'calendar' } }
    )
    return container
  }

  it('keeps the search button outside of the shrinkable title group', () => {
    const container = renderMobileMenubar('September – October 2026')

    const leftMenu = container.querySelector('.left-menu')
    const rightMenu = container.querySelector('.right-menu')
    const search = screen.getByRole('button', { name: 'common.search' })

    expect(leftMenu).not.toContainElement(search)
    expect(rightMenu).toContainElement(search)
    expect(leftMenu?.querySelector('.date-title')).toContainElement(
      screen.getByText('September – October 2026')
    )
  })

  it('clamps a long period title instead of letting it grow', () => {
    renderMobileMenubar('September – October 2026')

    expect(screen.getByText('September – October 2026')).toHaveStyle({
      minWidth: '0',
      overflow: 'hidden'
    })
  })
})
