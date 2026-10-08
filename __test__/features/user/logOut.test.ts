import {
  logOut,
  onSessionEndedElsewhere,
  resetLogOutState
} from '@common/features/User/logOut'
import {
  logOut as endSession,
  onSessionEndedElsewhere as onSessionEnded
} from '@linagora/twake-oidc'

jest.mock('@linagora/twake-oidc', () => ({
  logOut: jest.fn(() => Promise.resolve()),
  onSessionEndedElsewhere: jest.fn(() => () => undefined)
}))

const announceSessionEnded = (): void => {
  const listener = (onSessionEnded as jest.Mock).mock.calls[0][0] as () => void
  listener()
}

describe('logOut', () => {
  beforeEach(() => {
    resetLogOutState()
  })

  it('ends the session through twake-oidc', async () => {
    await logOut()

    expect(endSession).toHaveBeenCalledTimes(1)
  })

  it('follows a logout started in another tab', () => {
    const onEnded = jest.fn()
    onSessionEndedElsewhere(onEnded)

    announceSessionEnded()

    expect(onEnded).toHaveBeenCalledTimes(1)
  })

  it('ignores the announcement of a logout started by this tab', async () => {
    const onEnded = jest.fn()
    onSessionEndedElsewhere(onEnded)

    await logOut()
    announceSessionEnded()

    expect(onEnded).not.toHaveBeenCalled()
  })

  it('returns the function that stops listening', () => {
    const stopListening = jest.fn()
    ;(onSessionEnded as jest.Mock).mockReturnValueOnce(stopListening)

    onSessionEndedElsewhere(jest.fn())()

    expect(stopListening).toHaveBeenCalled()
  })
})
