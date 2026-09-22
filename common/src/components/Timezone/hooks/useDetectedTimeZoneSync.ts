import { useAppDispatch, useAppSelector } from '@common/app/hooks'
import { patchConfigurations } from '@common/features/User/UserDao'
import { makeConfigurationBody } from '@common/features/User/transformers'
import { setTimezone as setUserTimeZone } from '@common/features/User/UserSlice'
import { browserDefaultTimeZone } from '@common/utils/timezone'
import { useEffect } from 'react'

/**
 * Hands the detected timezone over to the backend while the automatic
 * detection is on: the backend renders invitation mails and availability with
 * the zone it stores, and has no browser of its own to detect one. Without
 * this the stored zone would stay whatever it was the day the user travelled.
 *
 * Deployments that do not advertise `autoDetect` are left alone: there a
 * stored zone cannot be told apart from a deliberate choice, and writing the
 * detected one down would read as an opt out on the next visit.
 */
export const useDetectedTimeZoneSync = (): void => {
  const dispatch = useAppDispatch()
  const coreConfig = useAppSelector(state => state.user?.coreConfig)
  const isAutoDetected = useAppSelector(
    state => state.settings?.isBrowserDefaultTimeZone
  )

  const storedTimeZone = coreConfig?.datetime?.timeZone
  const backendTracksAutoDetect =
    typeof coreConfig?.datetime?.autoDetect === 'boolean'

  useEffect(() => {
    if (!isAutoDetected || !backendTracksAutoDetect) return
    if (storedTimeZone === browserDefaultTimeZone) return

    const body = makeConfigurationBody({
      timezone: browserDefaultTimeZone,
      autoDetectTimezone: true,
      previousConfig: coreConfig
    })

    // Saved straight rather than through `updateUserConfigurations`, whose
    // failures land on the error page: the calendar runs on the detected zone
    // either way, so a backend that did not take it waits for the next visit.
    void patchConfigurations(body.modules)
      .then(() => dispatch(setUserTimeZone(browserDefaultTimeZone)))
      .catch(() => undefined)
  }, [
    dispatch,
    isAutoDetected,
    backendTracksAutoDetect,
    storedTimeZone,
    coreConfig
  ])
}
