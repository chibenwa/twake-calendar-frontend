import { CALENDAR_VIEWS } from '@common/components/Calendar/utils/constants'
import { MobileMenubar } from '@common/components/Menubar/MobileMenuBar'
import '@testing-library/jest-dom'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '../utils/Renderwithproviders'

describe('MobileMenubar', () => {
  const renderMobileMenubar = (
    dateLabel: string,
    shortDateLabel?: string
  ): HTMLElement => {
    const { container } = renderWithProviders(
      <MobileMenubar
        calendarRef={{ current: null }}
        currentDate={new Date('2026-09-28')}
        currentView={CALENDAR_VIEWS.timeGridWeek}
        dateLabel={dateLabel}
        shortDateLabel={shortDateLabel}
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

  const mockViewportWidth = (width: number): void => {
    window.matchMedia = jest.fn().mockImplementation((query: string) => ({
      matches: /max-width: 359/.test(query) && width < 360,
      media: query,
      onchange: null,
      addListener: jest.fn(),
      removeListener: jest.fn(),
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
      dispatchEvent: jest.fn()
    }))
  }

  describe('period title width (#1719)', () => {
    const originalMatchMedia = window.matchMedia
    afterEach(() => {
      window.matchMedia = originalMatchMedia
    })

    it('shows the short month names below 360px', () => {
      mockViewportWidth(320)
      renderMobileMenubar('October 2026', 'Oct 2026')

      expect(screen.getByText('Oct 2026')).toBeInTheDocument()
      expect(screen.queryByText('October 2026')).not.toBeInTheDocument()
    })

    it('keeps the full month names from 360px', () => {
      mockViewportWidth(360)
      renderMobileMenubar('October 2026', 'Oct 2026')

      expect(screen.getByText('October 2026')).toBeInTheDocument()
    })
  })

  it('clamps a long period title instead of letting it grow', () => {
    renderMobileMenubar('September – October 2026')

    expect(screen.getByText('September – October 2026')).toHaveStyle({
      minWidth: '0',
      overflow: 'hidden'
    })
  })

  it('vertically centres the period title with its date-picker arrow', () => {
    const container = renderMobileMenubar('January 2026')

    expect(container.querySelector('.current-date-time')).toHaveStyle({
      alignItems: 'center'
    })
  })
})
