import { useAppDispatch, useAppSelector } from '@common/app/hooks'
import { useEffect, useRef } from 'react'
import { push } from 'redux-first-history'
import { setAppLoading } from '@common/app/loadingSlice'

export const HandleLogin: React.FC = () => {
  const userData = useAppSelector(state => state.user)
  const calendars = useAppSelector(state => state.calendars)
  const dispatch = useAppDispatch()
  const hasNavigatedRef = useRef(false)

  // Navigate to /calendar only when all data is ready
  useEffect(() => {
    if (hasNavigatedRef.current) return
    if (userData.loading || calendars.pending) return
    // Calendar loading failures are reported by a toast inside the calendar
    // view: only a broken user session justifies the full error page.
    if (userData.error) {
      dispatch(setAppLoading(false))
      dispatch(push('/error'))
      return
    }
    if (!userData.userData || !userData.tokens) return
    // Calendars list can be empty, that's valid - just need to finish loading

    // All data is ready, navigate to calendar
    hasNavigatedRef.current = true
    dispatch(setAppLoading(false))
    dispatch(push('/calendar'))
  }, [
    userData.loading,
    userData.userData,
    userData.tokens,
    userData.error,
    calendars.pending,
    dispatch
  ])

  return null
}

export default HandleLogin
