import { externalApi } from '@common/utils/apiUtils'

export interface TokenExchangeResponse {
  token_type: 'bearer'
  scope: string
  access_token: string
  refresh_token: string
  client_id: string
  client_secret: string
  registration_access_token: string
}

/**
 * Exchange the ID token for a Tdrive-specific token.
 *
 * Tdrive is another service: the request goes through the external client,
 * which never adds the Calendar access token.
 */
export async function exchangeToken(
  tdriveBaseUrl: string,
  idToken: string
): Promise<TokenExchangeResponse> {
  const json = { id_token: idToken, exchange_type: 'app' }
  const response = await externalApi.post('auth/token_exchange', {
    prefixUrl: tdriveBaseUrl,
    json
  })
  return response.json<TokenExchangeResponse>()
}

interface FetchIntentJSONOptions {
  tdriveBaseUrl: string
  accessToken: string
}

export const fetchIntentJSON =
  ({ tdriveBaseUrl, accessToken }: FetchIntentJSONOptions) =>
  async (method: string, path: string, body?: unknown): Promise<unknown> => {
    const normalizedBase = tdriveBaseUrl.replace(/\/+$/, '')
    const normalizedPath = path.replace(/^\/+/, '')
    const url = `${normalizedBase}/${normalizedPath}`

    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 30000)

    try {
      const res = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${accessToken}`
        },
        body: body ? JSON.stringify(body) : undefined,
        signal: controller.signal
      })
      if (!res.ok) throw new Error(await res.text())
      return res.json()
    } finally {
      clearTimeout(timeoutId)
    }
  }
