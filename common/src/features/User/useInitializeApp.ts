import { useAppDispatch, useAppSelector } from '@common/app/hooks'
import { setAppLoading } from '@common/app/loadingSlice'
import { Auth } from '@common/features/User/oidcAuth'
import { getOpenPaasUserData } from '@common/features/User/UserSlice'
import { getAccessToken, redirectTo } from '@common/utils/apiUtils'
import { useEffect, useRef } from 'react'
import { getCalendarsList } from '../Calendars/CalendarSlice'

export const useInitializeApp = (): void => {
  const userData = useAppSelector(state => state.user)
  const calendars = useAppSelector(state => state.calendars)
  const dispatch = useAppDispatch()
  const hasInitiatedRef = useRef(false)

  useEffect(() => {
    if (hasInitiatedRef.current) return
    const isUserDataNotEmpty =
      userData.userData && Object.keys(userData.userData).length > 0
    if (isUserDataNotEmpty && !calendars.pending) return
    if (window.location.pathname === '/callback') return
    hasInitiatedRef.current = true

    const initiateLogin = async (): Promise<void> => {
      // Former versions kept the tokens and the user info in sessionStorage
      sessionStorage.removeItem('tokenSet')
      sessionStorage.removeItem('userData')

      // The tokens live in memory only: they are there when the application
      // navigates without reloading, and a reload signs in through the SSO.
      if (getAccessToken() && userData.userData) {
        dispatch(setAppLoading(true))
        try {
          await dispatch(getOpenPaasUserData())
          await dispatch(getCalendarsList())
        } finally {
          dispatch(setAppLoading(false))
        }

        return
      }

      const loginurl = await Auth()
      sessionStorage.setItem(
        'redirectState',
        JSON.stringify({
          code_verifier: loginurl.code_verifier,
          state: loginurl.state
        })
      )
      redirectTo(loginurl.redirectTo)
    }

    void initiateLogin()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userData.userData])
}
