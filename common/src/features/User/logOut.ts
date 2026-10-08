import {
  logOut as endSession,
  onSessionEndedElsewhere as onSessionEnded
} from '@linagora/twake-oidc'

/*
 * twake-oidc announces the end of the session on a BroadcastChannel, which
 * also delivers to the listener this very tab installed. Without this flag,
 * that listener would navigate away while logOut is still fetching the SSO
 * discovery document: Firefox and Safari then abort the fetch, the SSO
 * end-session endpoint is never reached and the user is signed back in.
 */
let loggingOutHere = false

/**
 * Ends the session here and on the SSO, then leaves the application.
 */
export function logOut(): Promise<void> {
  loggingOutHere = true
  return endSession()
}

/**
 * Calls onEnded when another tab of the application ends the session. The
 * announcement of a logout started by this tab is ignored: logOut navigates
 * on its own.
 *
 * @returns a function that stops listening.
 */
export function onSessionEndedElsewhere(onEnded: () => void): () => void {
  return onSessionEnded(() => {
    if (!loggingOutHere) {
      onEnded()
    }
  })
}

/** Test helper: forgets a logout started by this tab. */
export function resetLogOutState(): void {
  loggingOutHere = false
}
