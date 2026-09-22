/**
 * @jest-environment jsdom
 */

import { act, renderHook } from '@testing-library/react'
import { useVideoConference } from '@common/components/Event/hooks/useVideoConference'
import { createVideoConferenceRoom } from '@common/features/Events/VideoConferenceDao'

jest.mock('@common/features/Events/VideoConferenceDao')
jest.mock('@common/app/hooks', () => ({
  useAppSelector: jest.fn(() => undefined)
}))

const LOCAL_LINK_PATTERN = /^https:\/\/meet\.local\/[a-z]{3}-[a-z]{4}-[a-z]{3}$/

describe('useVideoConference', () => {
  const setHasVideoConference = jest.fn()
  const setMeetingLink = jest.fn()
  const setShowDescription = jest.fn()
  const originalBaseUrl = window.VIDEO_CONFERENCE_BASE_URL
  const originalIntegration = window.MEET_BACKEND_INTEGRATION

  const renderVideoConferenceHook = () =>
    renderHook(() =>
      useVideoConference({
        description: '',
        setDescription: jest.fn(),
        setHasVideoConference,
        setMeetingLink,
        showMore: true,
        setShowDescription
      })
    )

  const addVideoConference = async (): Promise<void> => {
    const { result } = renderVideoConferenceHook()
    await act(async () => {
      result.current.handleAddVideoConference()
    })
  }

  beforeEach(() => {
    jest.clearAllMocks()
    window.VIDEO_CONFERENCE_BASE_URL = 'https://meet.local'
  })

  afterEach(() => {
    window.VIDEO_CONFERENCE_BASE_URL = originalBaseUrl
    window.MEET_BACKEND_INTEGRATION = originalIntegration
  })

  it('generates the meeting code locally by default', async () => {
    window.MEET_BACKEND_INTEGRATION = undefined

    await addVideoConference()

    expect(createVideoConferenceRoom).not.toHaveBeenCalled()
    expect(setHasVideoConference).toHaveBeenCalledWith(true)
    expect(setMeetingLink).toHaveBeenCalledWith(
      expect.stringMatching(LOCAL_LINK_PATTERN)
    )
    expect(setShowDescription).toHaveBeenCalledWith(true)
  })

  it('uses the room created by the side service when enabled', async () => {
    window.MEET_BACKEND_INTEGRATION = true
    ;(createVideoConferenceRoom as jest.Mock).mockResolvedValue(
      'https://meet.example.com/vep-txbc-trh'
    )

    await addVideoConference()

    expect(setHasVideoConference).toHaveBeenCalledWith(true)
    expect(setMeetingLink).toHaveBeenCalledWith(
      'https://meet.example.com/vep-txbc-trh'
    )
  })

  it('falls back to a local code when Meet is not configured', async () => {
    window.MEET_BACKEND_INTEGRATION = true
    ;(createVideoConferenceRoom as jest.Mock).mockResolvedValue(null)

    await addVideoConference()

    expect(setMeetingLink).toHaveBeenCalledWith(
      expect.stringMatching(LOCAL_LINK_PATTERN)
    )
  })

  it('adds no video conference when the room creation fails', async () => {
    window.MEET_BACKEND_INTEGRATION = true
    ;(createVideoConferenceRoom as jest.Mock).mockRejectedValue(
      new Error('createVideoConferenceRoom failed with status 502')
    )
    const consoleError = jest
      .spyOn(console, 'error')
      .mockImplementation(() => {})

    await addVideoConference()

    expect(setHasVideoConference).not.toHaveBeenCalled()
    expect(setMeetingLink).not.toHaveBeenCalled()
    consoleError.mockRestore()
  })

  it('creates a single room when adding twice while the request is pending', async () => {
    window.MEET_BACKEND_INTEGRATION = true
    let resolveRoom: (url: string) => void = () => {}
    ;(createVideoConferenceRoom as jest.Mock).mockReturnValue(
      new Promise<string>(resolve => {
        resolveRoom = resolve
      })
    )
    const { result } = renderVideoConferenceHook()

    await act(async () => {
      result.current.handleAddVideoConference()
      result.current.handleAddVideoConference()
      resolveRoom('https://meet.example.com/vep-txbc-trh')
    })

    expect(createVideoConferenceRoom).toHaveBeenCalledTimes(1)
    expect(setMeetingLink).toHaveBeenCalledTimes(1)
  })
})
