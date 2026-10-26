import { PublicLayout } from '@public/components/PublicLayout'
import '@testing-library/jest-dom'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '../utils/Renderwithproviders'

describe('PublicLayout landmarks (#1791)', () => {
  it('exposes banner, main and contentinfo, and a skip link to main', () => {
    renderWithProviders(
      <PublicLayout>
        <p>Booking</p>
      </PublicLayout>
    )

    expect(screen.getByRole('banner')).toBeInTheDocument()
    expect(screen.getByRole('main')).toHaveTextContent('Booking')
    expect(screen.getByRole('contentinfo')).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'a11y.skipToContent' })
    ).toHaveAttribute('href', `#${screen.getByRole('main').id}`)
  })
})
