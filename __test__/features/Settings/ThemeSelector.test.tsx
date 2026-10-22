import SettingsPage from '@common/features/Settings/SettingsPage'
import { useScreenSizeDetection } from '@common/useScreenSizeDetection'
import { api } from '@common/utils/apiUtils'
import '@testing-library/jest-dom'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '../../utils/Renderwithproviders'

jest.mock('@common/utils/apiUtils')
jest.mock('@common/useScreenSizeDetection', () => ({
  useScreenSizeDetection: jest.fn(() => ({
    isTooSmall: false,
    isTablet: false
  }))
}))

function makeState(theme?: 'light' | 'dark' | 'auto' | null): object {
  return {
    user: {
      userData: {
        sub: 'test',
        email: 'test@test.com',
        family_name: 'Doe',
        name: 'John',
        sid: 'mockSid',
        openpaasId: '667037022b752d0026472254'
      },
      organiserData: null,
      tokens: null,
      coreConfig: {
        language: 'en',
        datetime: { timeZone: 'UTC' },
        ...(theme !== undefined ? { theme } : {})
      },
      alarmEmailsEnabled: false,
      loading: false,
      error: null
    },
    settings: {
      language: 'en',
      timeZone: 'UTC',
      isBrowserDefaultTimeZone: false,
      view: 'settings'
    }
  }
}

async function chooseTheme(name: RegExp): Promise<void> {
  fireEvent.mouseDown(screen.getByLabelText('settings.themeSelector'))
  fireEvent.click(await screen.findByRole('option', { name }))
}

describe('ThemeSelector', () => {
  beforeEach(() => {
    jest.clearAllMocks()
    ;(useScreenSizeDetection as jest.Mock).mockReturnValue({
      isTooSmall: false,
      isTablet: false
    })
    window.HIDE_THEME_SELECTOR = false
  })

  it('saves the chosen theme in the core.theme user setting', async () => {
    ;(api.patch as jest.Mock).mockResolvedValue({ status: 204 })
    const { store } = renderWithProviders(<SettingsPage />, makeState(null))

    await chooseTheme(/settings.themes.dark/)

    await waitFor(() => {
      expect(store.getState().user.coreConfig.theme).toBe('dark')
    })
    expect(api.patch).toHaveBeenCalledWith(
      'api/configurations?scope=user',
      expect.objectContaining({
        json: expect.arrayContaining([
          expect.objectContaining({
            name: 'core',
            configurations: expect.arrayContaining([
              expect.objectContaining({ name: 'theme', value: 'dark' })
            ])
          })
        ])
      })
    )
  })

  it('keeps the saved theme when the save fails', async () => {
    ;(api.patch as jest.Mock).mockRejectedValue(new Error('API Error'))
    const { store } = renderWithProviders(<SettingsPage />, makeState('light'))

    await chooseTheme(/settings.themes.dark/)

    expect(
      await screen.findByText('settings.themeUpdateError')
    ).toBeInTheDocument()
    expect(store.getState().user.coreConfig.theme).toBe('light')
  })

  it('is hidden where common settings own the theme', () => {
    window.HIDE_THEME_SELECTOR = true
    renderWithProviders(<SettingsPage />, makeState('dark'))

    expect(screen.queryByLabelText('settings.themeSelector')).toBe(null)
  })

  it('opens a bottom sheet of the themes on a phone, as Language does', async () => {
    ;(useScreenSizeDetection as jest.Mock).mockReturnValue({
      isTooSmall: true,
      isTablet: false
    })
    ;(api.patch as jest.Mock).mockResolvedValue({ status: 204 })
    const { store } = renderWithProviders(<SettingsPage />, makeState('light'))

    expect(screen.queryByLabelText('settings.themeSelector')).toBe(null)
    fireEvent.click(
      screen.getByRole('button', { name: /settings.themes.light/ })
    )
    fireEvent.click(
      await screen.findByRole('button', { name: /settings.themes.dark/ })
    )

    await waitFor(() => {
      expect(store.getState().user.coreConfig.theme).toBe('dark')
    })
  })

  it('names the phone Theme and Language fields after what they set', () => {
    ;(useScreenSizeDetection as jest.Mock).mockReturnValue({
      isTooSmall: true,
      isTablet: false
    })
    renderWithProviders(<SettingsPage />, makeState('dark'))

    const theme = screen.getByRole('button', {
      name: 'settings.theme: settings.themes.dark'
    })
    expect(theme).toHaveAttribute('aria-haspopup', 'dialog')
    expect(theme).toHaveAttribute('aria-expanded', 'false')
    expect(
      screen.getByRole('button', { name: 'settings.language: English' })
    ).toHaveAttribute('aria-haspopup', 'dialog')

    fireEvent.click(theme)

    expect(theme).toHaveAttribute('aria-expanded', 'true')
  })
})
