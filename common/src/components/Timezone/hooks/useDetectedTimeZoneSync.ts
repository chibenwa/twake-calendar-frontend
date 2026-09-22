import { useAppDispatch, useAppSelector } from '@common/app/hooks'
import type { AppDispatch, RootState } from '@common/app/store'
import { patchConfigurations } from '@common/features/User/UserDao'
import { makeConfigurationBody } from '@common/features/User/transformers'
import { setTimezone as setUserTimeZone } from '@common/features/User/UserSlice'
import { browserDefaultTimeZone } from '@common/utils/timezone'
import { useEffect, useRef } from 'react'

const selectCoreConfig = (state: RootState) => state.user?.coreConfig

const selectIsAutoDetected = (state: RootState) =>
  state.settings?.isBrowserDefaultTimeZone

const selectStoredTimeZone = (state: RootState) =>
  selectCoreConfig(state)?.datetime?.timeZone

// A deployment that does not advertise `autoDetect` is one where a stored zone
// cannot be told apart from a deliberate choice.
const selectBackendTracksAutoDetect = (state: RootState) =>
  typeof selectCoreConfig(state)?.datetime?.autoDetect === 'boolean'

type CoreConfig = ReturnType<typeof selectCoreConfig>

const pushDetectedTimeZone = async (
  coreConfig: CoreConfig,
  dispatch: AppDispatch,
  isStillAutoDetecting: () => boolean
): Promise<void> => {
  const body = makeConfigurationBody({
    timezone: browserDefaultTimeZone,
    autoDetectTimezone: true,
    previousConfig: coreConfig
  })

  try {
    await patchConfigurations(body.modules)
    // The user may have turned the detection off, and pinned a zone of their
    // own, while this was in flight: that choice is the newer one.
    if (isStillAutoDetecting()) {
      dispatch(setUserTimeZone(browserDefaultTimeZone))
    }
  } catch {
    // Saved straight rather than through `updateUserConfigurations`, whose
    // failures land on the error page: the calendar runs on the detected zone
    // either way, so a backend that did not take it waits for the next visit.
  }
}

/**
 * Hands the detected timezone over to the backend while the automatic
 * detection is on: the backend renders invitation mails and availability with
 * the zone it stores, and has no browser of its own to detect one. Without
 * this the stored zone would stay whatever it was the day the user travelled.
 *
 * Deployments that do not advertise `autoDetect` are left alone: there writing
 * the detected zone down would read as an opt out on the next visit.
 */
export const useDetectedTimeZoneSync = (): void => {
  const dispatch = useAppDispatch()
  const coreConfig = useAppSelector(selectCoreConfig)
  const isAutoDetected = useAppSelector(selectIsAutoDetected)
  const storedTimeZone = useAppSelector(selectStoredTimeZone)
  const backendTracksAutoDetect = useAppSelector(selectBackendTracksAutoDetect)
  const isAutoDetectedRef = useRef(isAutoDetected)

  useEffect(() => {
    isAutoDetectedRef.current = isAutoDetected

    const isAlreadyStored = storedTimeZone === browserDefaultTimeZone
    const shouldPush =
      isAutoDetected && backendTracksAutoDetect && !isAlreadyStored

    if (shouldPush) {
      void pushDetectedTimeZone(
        coreConfig,
        dispatch,
        () => isAutoDetectedRef.current
      )
    }
  }, [
    dispatch,
    isAutoDetected,
    backendTracksAutoDetect,
    storedTimeZone,
    coreConfig
  ])
}
