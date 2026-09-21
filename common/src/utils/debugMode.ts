type DebugModeListener = () => void

const listeners = new Set<DebugModeListener>()

export const isDebugModeEnabled = (): boolean => window.DEBUG === true

export const subscribeToDebugMode = (
  listener: DebugModeListener
): (() => void) => {
  listeners.add(listener)

  return () => {
    listeners.delete(listener)
  }
}

/**
 * Flips the runtime debug flag and returns its new value. Debug mode is not
 * persisted: a reload restores the DEBUG value shipped in .env.js.
 */
export const toggleDebugMode = (): boolean => {
  window.DEBUG = !isDebugModeEnabled()
  listeners.forEach(listener => listener())

  return window.DEBUG
}
