import { useIsInIframe } from '@common/contexts/EmbeddingContext'

/**
 * Whether the Tdrive file picker can be opened here.
 *
 * The picker talks to the Tdrive stack cross-origin (token exchange, then the
 * intents API and the picker iframe). Only the Cozy-embedded deployment is a
 * whitelisted origin there; the very same SPA served directly on its own
 * domain fails on CORS. Both are driven by the same runtime configuration, so
 * `TDRIVE_ENABLED` alone cannot tell them apart: complement it with the
 * embedding check, which is what distinguishes the two ways of serving the SPA
 * at runtime.
 *
 * This gates the picker only. Attachments already carried by an event are
 * plain iCalendar `ATTACH` properties and are displayed regardless.
 */
export const useIsTdrivePickerAvailable = (): boolean => {
  const isInIframe = useIsInIframe()

  return (
    Boolean(window.TDRIVE_INTENT_URL) &&
    Boolean(window.TDRIVE_ENABLED) &&
    isInIframe
  )
}
