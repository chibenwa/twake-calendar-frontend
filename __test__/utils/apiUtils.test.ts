jest.mock('@common/features/User/oidcAuth', () => ({ Auth: jest.fn() }))
jest.mock(
  '@common/websocket/connection/lifecycle/assertWebSocketAlive',
  () => ({ assertWebSocketAlive: jest.fn() })
)

describe('api client authorization', () => {
  const sent: Request[] = []

  beforeAll(() => {
    Object.assign(global, {
      window: {
        CALENDAR_BASE_URL: 'https://calendar.example.com',
        location: { href: 'https://calendar-app.example.com/calendar' }
      }
    })
  })

  beforeEach(() => {
    sent.length = 0
    global.fetch = jest.fn(async (request: Request) => {
      sent.push(request)
      return new Response('{}', { status: 200 })
    }) as unknown as typeof fetch
  })

  async function load(): Promise<typeof import('@common/utils/apiUtils')> {
    let module: typeof import('@common/utils/apiUtils') | undefined
    await jest.isolateModulesAsync(async () => {
      module = await import('@common/utils/apiUtils')
    })
    return module!
  }

  it('sends the access token to the Calendar backend', async () => {
    const { api, setTokenSet } = await load()
    setTokenSet({ access_token: 'calendar-token' })

    await api.get('api/something')

    expect(sent[0].headers.get('Authorization')).toBe('Bearer calendar-token')
  })

  it('does not send the access token to another origin', async () => {
    const { api, setTokenSet } = await load()
    setTokenSet({ access_token: 'calendar-token' })

    await api.get('auth/token_exchange', {
      prefixUrl: 'https://drive.example.com'
    })

    expect(sent[0].url).toBe('https://drive.example.com/auth/token_exchange')
    expect(sent[0].headers.get('Authorization')).toBeNull()
  })

  it('never sends the access token through the external client', async () => {
    const { externalApi, setTokenSet } = await load()
    setTokenSet({ access_token: 'calendar-token' })

    await externalApi.post('auth/token_exchange', {
      prefixUrl: 'https://calendar.example.com',
      json: {}
    })

    expect(sent[0].headers.get('Authorization')).toBeNull()
  })
})
