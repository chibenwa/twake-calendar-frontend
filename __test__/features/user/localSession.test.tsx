import {
  endLocalSession,
  onSessionEndedElsewhere
} from '@common/features/User/localSession'
import { getAccessToken, setTokenSet } from '@common/utils/apiUtils'

class FakeChannel {
  static open: FakeChannel[] = []
  onmessage: ((event: MessageEvent) => void) | null = null
  closed = false

  constructor(readonly name: string) {
    FakeChannel.open.push(this)
  }

  postMessage(data: unknown): void {
    FakeChannel.open
      .filter(
        other => other !== this && !other.closed && other.name === this.name
      )
      .forEach(other => other.onmessage?.({ data } as MessageEvent))
  }

  close(): void {
    this.closed = true
  }
}

describe('localSession', () => {
  const original = global.BroadcastChannel

  beforeEach(() => {
    FakeChannel.open = []
    global.BroadcastChannel = FakeChannel as unknown as typeof BroadcastChannel
  })

  afterEach(() => {
    global.BroadcastChannel = original
  })

  it('drops the tokens of this tab', () => {
    setTokenSet({ access_token: 'token' })

    endLocalSession()

    expect(getAccessToken()).toBeUndefined()
  })

  it('tells the other tabs, which drop their tokens too', () => {
    const onEnded = jest.fn()
    const stop = onSessionEndedElsewhere(onEnded)
    setTokenSet({ access_token: 'token' })

    // what another tab sends when the user logs out there
    new FakeChannel('twake-calendar-session').postMessage('session-ended')

    expect(onEnded).toHaveBeenCalledTimes(1)
    expect(getAccessToken()).toBeUndefined()
    stop()
  })

  it('ignores unrelated messages', () => {
    const onEnded = jest.fn()
    const stop = onSessionEndedElsewhere(onEnded)
    setTokenSet({ access_token: 'token' })

    new FakeChannel('twake-calendar-session').postMessage('something else')

    expect(onEnded).not.toHaveBeenCalled()
    expect(getAccessToken()).toBe('token')
    stop()
  })

  it('does nothing when BroadcastChannel is not available', () => {
    global.BroadcastChannel = undefined as unknown as typeof BroadcastChannel
    setTokenSet({ access_token: 'token' })

    expect(() => onSessionEndedElsewhere(jest.fn())()).not.toThrow()
    endLocalSession()
    expect(getAccessToken()).toBeUndefined()
  })
})
