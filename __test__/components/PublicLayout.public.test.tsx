import { PublicLayout } from '@public/components/PublicLayout'
import { TwakeMuiThemeProvider } from '@linagora/twake-mui'
import '@testing-library/jest-dom'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

jest.mock('twake-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key, lang: 'en' })
}))

jest.mock('@public/components/PublicLanguageSelector', () => ({
  PublicLanguageSelector: () => <button>language</button>
}))

const renderLayout = (): void => {
  render(
    <TwakeMuiThemeProvider>
      <PublicLayout>
        <button>page action</button>
      </PublicLayout>
    </TwakeMuiThemeProvider>
  )
}

describe('PublicLayout', () => {
  it('exposes the banner, main and contentinfo landmarks', () => {
    renderLayout()

    expect(
      within(screen.getByRole('banner')).getByRole('button', {
        name: 'language'
      })
    ).toBeInTheDocument()
    expect(
      within(screen.getByRole('main')).getByRole('button', {
        name: 'page action'
      })
    ).toBeInTheDocument()
    expect(screen.getByRole('contentinfo')).toBeInTheDocument()
  })

  it('gives the first Tab to a link skipping to the page content', () => {
    renderLayout()

    userEvent.tab()

    const skipLink = screen.getByRole('link', { name: 'a11y.skipToContent' })
    expect(skipLink).toHaveFocus()
    expect(skipLink).toHaveAttribute('href', '#main-content')
    expect(screen.getByRole('main')).toHaveAttribute('id', 'main-content')
    expect(screen.getByRole('main')).toHaveAttribute('tabindex', '-1')
  })
})
