import de from '@common/locales/de.json'
import en from '@common/locales/en.json'
import es from '@common/locales/es.json'
import fr from '@common/locales/fr.json'
import italian from '@common/locales/it.json'
import ru from '@common/locales/ru.json'
import vi from '@common/locales/vi.json'

const locales = { de, en, es, fr, it: italian, ru, vi }

describe('import result translations', () => {
  it.each(Object.entries(locales))(
    '%s starts the imported items sentence with a capital letter',
    (_, locale) => {
      // importedItems follows the "." closing the first sentence of importCompleted
      const pluralForms = locale.websocket.importedItems.split('||||')

      pluralForms.forEach(form => {
        expect(form.trim()).not.toMatch(/^\p{Ll}/u)
      })
    }
  )
})
