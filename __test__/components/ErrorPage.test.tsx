import { Error as ErrorPage } from '@common/components/Error/Error'
import { clearError } from '@common/features/User/UserSlice'
import { act, screen } from '@testing-library/react'
import { renderWithProviders } from '../utils/Renderwithproviders'

describe('Error page', () => {
  it('translates an error carrying a translation key', () => {
    renderWithProviders(<ErrorPage />, {
      user: {
        userData: null,
        tokens: null,
        loading: false,
        error: 'TRANSLATION:error.ssoUnreachable',
        coreConfig: { language: 'en' }
      }
    })

    expect(screen.getByText(/^error\.ssoUnreachable/)).toBeInTheDocument()
  })

  it('shows a plain error as is', () => {
    renderWithProviders(<ErrorPage />, {
      user: {
        userData: null,
        tokens: null,
        loading: false,
        error: 'OAuth callback failed',
        coreConfig: { language: 'en' }
      }
    })

    expect(screen.getByText('OAuth callback failed')).toBeInTheDocument()
  })

  it('keeps the message once the error is cleared from the store', () => {
    const { store } = renderWithProviders(<ErrorPage />, {
      user: {
        userData: null,
        tokens: null,
        loading: false,
        error: 'TRANSLATION:error.ssoUnreachable',
        coreConfig: { language: 'en' }
      }
    })

    act(() => {
      store.dispatch(clearError())
    })

    expect(screen.getByText(/^error\.ssoUnreachable/)).toBeInTheDocument()
    expect(screen.queryByText('error.unknown')).not.toBeInTheDocument()
  })
})
