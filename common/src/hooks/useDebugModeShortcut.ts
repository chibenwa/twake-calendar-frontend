import { useEffect } from 'react'
import { toggleDebugMode } from '@common/utils/debugMode'

/**
 * `event.code` is layout independent, unlike `event.key` which Alt + Shift
 * turns into a diacritic on several keyboard layouts.
 */
const isDebugModeShortcut = (event: KeyboardEvent): boolean =>
  event.code === 'KeyD' &&
  !event.repeat &&
  event.ctrlKey &&
  event.shiftKey &&
  event.altKey &&
  !event.metaKey

/**
 * Enables or disables debug mode on CTRL + SHIFT + ALT + D, so that it can be
 * turned on for diagnostics on a deployment shipping DEBUG = false.
 */
export const useDebugModeShortcut = (
  onToggled: (enabled: boolean) => void
): void => {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent): void => {
      if (!isDebugModeShortcut(event)) return

      event.preventDefault()
      onToggled(toggleDebugMode())
    }

    window.addEventListener('keydown', handleKeyDown)

    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onToggled])
}
