import {
  formatLocalizedDate,
  getLongDateFormat,
  LONG_DATE_FORMAT
} from '@common/components/Event/utils/dateTimeFormatters'
import dayjs from 'dayjs'
import 'dayjs/locale/fr'
import 'dayjs/locale/es'
import 'dayjs/locale/de'
import 'dayjs/locale/it'
import 'dayjs/locale/ru'

describe('getLongDateFormat', () => {
  it('orders the day before the month in French', () => {
    const date = dayjs('2026-09-25').locale('fr')

    expect(date.format(getLongDateFormat('fr'))).toBe(
      'vendredi 25 septembre 2026'
    )
  })

  it('writes the Spanish long date with its prepositions', () => {
    const date = dayjs('2026-09-25').locale('es')

    expect(date.format(getLongDateFormat('es'))).toBe(
      'viernes, 25 de septiembre de 2026'
    )
  })

  it('writes the German day as an ordinal', () => {
    const date = dayjs('2026-09-25').locale('de')

    expect(date.format(getLongDateFormat('de'))).toBe(
      'Freitag, 25. September 2026'
    )
  })

  it('orders the day before the month in Italian', () => {
    const date = dayjs('2026-09-25').locale('it')

    expect(date.format(getLongDateFormat('it'))).toBe(
      'venerdì 25 settembre 2026'
    )
  })

  it('keeps the English ordering for English', () => {
    const date = dayjs('2026-09-25').locale('en')

    expect(date.format(getLongDateFormat('en'))).toBe(
      'Friday, September 25, 2026'
    )
  })

  it('falls back to the default format for unknown locales', () => {
    expect(getLongDateFormat('xx')).toBe(LONG_DATE_FORMAT)
    expect(getLongDateFormat(undefined)).toBe(LONG_DATE_FORMAT)
  })
})

describe('formatLocalizedDate', () => {
  it('capitalises the French weekday for standalone labels', () => {
    expect(formatLocalizedDate('2026-10-06T15:00:00', 'fr')).toBe(
      'Mardi 6 octobre 2026'
    )
  })

  it('keeps the French weekday lowercase inside a sentence', () => {
    expect(
      formatLocalizedDate('2026-10-06T15:00:00', 'fr', { capitalize: false })
    ).toBe('mardi 6 octobre 2026')
  })

  it('keeps the Russian weekday lowercase inside a sentence', () => {
    expect(
      formatLocalizedDate('2026-10-06T15:00:00', 'ru', { capitalize: false })
    ).toMatch(/^вторник/)
  })
})
