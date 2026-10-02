import { resolveUriTemplate } from '@linagora/twake-utils'
import { useAppSelector } from '@common/app/hooks'

interface UseTdriveUserContextReturn {
  localpart: string | undefined
  tdriveBaseUrl: string | null
}

export function useTdriveUserContext(): UseTdriveUserContextReturn {
  const email = useAppSelector(state => state.user.userData?.email)
  const workplaceFqdn = useAppSelector(
    state => state.user.userData?.workplaceFqdn
  )

  const localpart = email?.split('@')[0]
  const tdriveBaseUrl = window.TDRIVE_INTENT_URL
    ? resolveUriTemplate(window.TDRIVE_INTENT_URL, {
        localpart,
        workplaceFqdn,
        workplaceFqdnFallback: window.WORKPLACE_FQDN_FALLBACK
      })
    : null

  return { localpart, tdriveBaseUrl }
}
