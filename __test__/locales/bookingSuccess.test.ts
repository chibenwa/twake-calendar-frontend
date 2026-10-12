import de from '@common/locales/de.json'
import en from '@common/locales/en.json'
import es from '@common/locales/es.json'
import fr from '@common/locales/fr.json'
import itIT from '@common/locales/it.json'
import ru from '@common/locales/ru.json'
import vi from '@common/locales/vi.json'

const locales = { de, en, es, fr, it: itIT, ru, vi }

describe('booking success "cancel your meeting" link', () => {
  it.each(Object.entries(locales))(
    '%s starts with a capital letter as it follows a question',
    (_, locale) => {
      const cancelLabel = locale.booking.success.cancelMeeting
      const firstLetter = cancelLabel.charAt(0)

      expect(firstLetter).toBe(firstLetter.toLocaleUpperCase())
    }
  )
})
