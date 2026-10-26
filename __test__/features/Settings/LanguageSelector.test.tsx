import { AVAILABLE_LANGUAGES } from '@common/features/Settings/constants'
import { DesktopSelectLanguage } from '@common/features/Settings/LanguageSelector/DesktopLanguageSelector'
import { MobileLanguageSelector } from '@common/features/Settings/LanguageSelector/MobileLanguageSelector'
import { PublicLanguageSelector } from '@public/components/PublicLanguageSelector'
import { PublicLanguageContext } from '@public/context/PublicLanguageContext'
import '@testing-library/jest-dom'
import { fireEvent, screen } from '@testing-library/react'
import { renderWithProviders } from '../../utils/Renderwithproviders'

const expectEachLabelMarkedWithItsLanguage = (scope: HTMLElement): void => {
  AVAILABLE_LANGUAGES.forEach(({ code, label }) => {
    const labels = Array.from(scope.querySelectorAll(`[lang="${code}"]`))
    expect(labels.map(element => element.textContent)).toContain(label)
  })
}

describe('Language selectors mark each language name with its language', () => {
  it('desktop selector marks the options and the selected value', async () => {
    renderWithProviders(
      <DesktopSelectLanguage currentLanguage="ru" onChange={jest.fn()} />
    )

    const select = screen.getByLabelText('settings.languageSelector')
    expect(select.querySelector('[lang="ru"]')).toHaveTextContent('Русский')

    fireEvent.mouseDown(select)
    expectEachLabelMarkedWithItsLanguage(await screen.findByRole('listbox'))
  })

  it('mobile selector marks the options', () => {
    renderWithProviders(
      <MobileLanguageSelector currentLanguage="en" onChange={jest.fn()} />
    )

    fireEvent.click(screen.getByRole('button', { name: /settings.language/ }))

    expectEachLabelMarkedWithItsLanguage(document.body)
  })

  it('public selector marks the options and the selected value', async () => {
    renderWithProviders(
      <PublicLanguageContext.Provider
        value={{ currentLanguage: 'vi', setCurrentLanguage: jest.fn() }}
      >
        <PublicLanguageSelector />
      </PublicLanguageContext.Provider>
    )

    const select = screen.getByRole('combobox')
    expect(select.querySelector('[lang="vi"]')).toHaveTextContent('Tiếng Việt')

    fireEvent.mouseDown(select)
    expectEachLabelMarkedWithItsLanguage(await screen.findByRole('listbox'))
  })
})
