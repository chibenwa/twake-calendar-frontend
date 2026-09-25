import { clearTokenSet } from '@common/utils/apiUtils'

const CHANNEL_NAME = 'twake-calendar-session'
const SESSION_ENDED = 'session-ended'

const openChannel = (): BroadcastChannel | null =>
  typeof BroadcastChannel === 'function'
    ? new BroadcastChannel(CHANNEL_NAME)
    : null

/**
 * Ends the session in this browser: drops the tokens this tab holds in memory
 * and tells the other tabs of the application to drop theirs. The SSO
 * session, and the tokens already issued, are ended by the SSO itself
 * (back-channel logout).
 */
export function endLocalSession(): void {
  clearTokenSet()
  const channel = openChannel()
  channel?.postMessage(SESSION_ENDED)
  channel?.close()
}

/**
 * Calls onEnded when another tab of the application ends the session, after
 * dropping the tokens this tab holds.
 *
 * @returns a function that stops listening.
 */
export function onSessionEndedElsewhere(onEnded: () => void): () => void {
  const channel = openChannel()
  if (!channel) return () => {}

  channel.onmessage = (event: MessageEvent): void => {
    if (event.data === SESSION_ENDED) {
      clearTokenSet()
      onEnded()
    }
  }
  return () => channel.close()
}
