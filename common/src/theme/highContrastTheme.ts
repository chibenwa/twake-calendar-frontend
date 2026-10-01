import type { ThemeOptions } from '@mui/material/styles'
import { enUS, frFR, ruRU, viVN } from '@mui/material/locale'
import { merge } from 'lodash'

const MUI_LOCALES: Record<string, ThemeOptions> = {
  en: enUS,
  fr: frFR,
  ru: ruRU,
  vi: viVN
}

/** "fr-fr", "fr" → "fr" */
export const languageCode = (lang: string): string =>
  (lang || 'en').slice(0, 2).toLowerCase()

/**
 * Theme options of the high contrast mode, merged on top of the application
 * theme when the mode is on (see accessibility/A10Y_REMEDIATIONS).
 */
export const highContrastThemeOptions = (lang: string): ThemeOptions =>
  merge(
    {},
    // R-06: MUI's own texts (Autocomplete "No options", Alert "Close"…) in
    // the language of the user rather than in English
    MUI_LOCALES[languageCode(lang)] ?? enUS
  )

export const withHighContrast = (
  base: ThemeOptions | undefined,
  enabled: boolean,
  lang: string
): ThemeOptions | undefined =>
  enabled ? merge({}, base, highContrastThemeOptions(lang)) : base
