import { useDetectedTimeZoneSync } from '@common/components/Timezone/hooks/useDetectedTimeZoneSync'
import { patchConfigurations } from '@common/features/User/UserDao'
import { browserDefaultTimeZone } from '@common/utils/timezone'
import { waitFor } from '@testing-library/react'
import React from 'react'
import { renderWithProviders } from '../../utils/Renderwithproviders'

jest.mock('@common/features/User/UserDao', () => {
  const actual = jest.requireActual('@common/features/User/UserDao')
  return {
    ...actual,
    patchConfigurations: jest.fn()
  }
})

const Probe: React.FC = () => {
  useDetectedTimeZoneSync()
  return null
}

describe('useDetectedTimeZoneSync', () => {
  const elsewhere =
    browserDefaultTimeZone === 'Asia/Ho_Chi_Minh'
      ? 'America/New_York'
      : 'Asia/Ho_Chi_Minh'

  const stateWith = (datetime: object, isBrowserDefaultTimeZone: boolean) => ({
    user: {
      userData: { sub: 'test' },
      organiserData: null,
      tokens: null,
      coreConfig: { language: 'en', datetime },
      loading: false,
      error: null
    },
    settings: {
      language: 'en',
      timeZone: browserDefaultTimeZone,
      isBrowserDefaultTimeZone,
      view: 'calendar'
    }
  })

  beforeEach(() => {
    ;(patchConfigurations as jest.Mock).mockResolvedValue({ status: 204 })
  })

  it('hands the detected zone to a backend holding another one', async () => {
    const { store } = renderWithProviders(
      <Probe />,
      stateWith({ timeZone: elsewhere, autoDetect: true }, true)
    )

    await waitFor(() => {
      expect(patchConfigurations).toHaveBeenCalledWith([
        {
          name: 'core',
          configurations: [
            {
              name: 'datetime',
              value: { timeZone: browserDefaultTimeZone, autoDetect: true }
            }
          ]
        }
      ])
    })
    await waitFor(() => {
      expect(store.getState().user.coreConfig.datetime.timeZone).toBe(
        browserDefaultTimeZone
      )
    })
  })

  it('stays quiet once the backend holds the detected zone', () => {
    renderWithProviders(
      <Probe />,
      stateWith({ timeZone: browserDefaultTimeZone, autoDetect: true }, true)
    )

    expect(patchConfigurations).not.toHaveBeenCalled()
  })

  it('stays quiet when the user opted out of the detection', () => {
    renderWithProviders(
      <Probe />,
      stateWith({ timeZone: elsewhere, autoDetect: false }, false)
    )

    expect(patchConfigurations).not.toHaveBeenCalled()
  })

  it('leaves a backend that does not know about the flag alone', () => {
    renderWithProviders(<Probe />, stateWith({ timeZone: elsewhere }, true))

    expect(patchConfigurations).not.toHaveBeenCalled()
  })
})
