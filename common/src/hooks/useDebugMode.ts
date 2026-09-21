import { useSyncExternalStore } from 'react'
import {
  isDebugModeEnabled,
  subscribeToDebugMode
} from '@common/utils/debugMode'

/**
 * Reads the debug flag and re-renders when it is toggled at runtime, which
 * `window.DEBUG` alone cannot do.
 */
export const useDebugMode = (): boolean =>
  useSyncExternalStore(subscribeToDebugMode, isDebugModeEnabled)
