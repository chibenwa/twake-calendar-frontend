import { useRef } from 'react'
import { useAppSelector } from '@common/app/hooks'
import { createVideoConferenceRoom } from '@common/features/Events/VideoConferenceDao'
import { generateMeetingLink } from '@common/utils/videoConferenceUtils'

interface UseVideoConferenceProps {
  description: string
  setDescription: (value: string) => void
  setHasVideoConference: (value: boolean) => void
  setMeetingLink: (value: string | null) => void
  showMore: boolean
  setShowDescription?: (value: boolean) => void
}

interface UseVideoConferenceReturn {
  handleAddVideoConference: () => void
  handleDeleteVideoConference: () => void
}

export const useVideoConference = ({
  setHasVideoConference,
  setMeetingLink,
  showMore,
  setShowDescription
}: UseVideoConferenceProps): UseVideoConferenceReturn => {
  const workplaceFqdn = useAppSelector(
    state => state.user.userData?.workplaceFqdn
  )
  const email = useAppSelector(state => state.user.userData?.email)
  const isAddingRef = useRef(false)

  const generateLocalMeetingLink = (): string =>
    generateMeetingLink({
      localpart: email?.split('@')[0],
      workplaceFqdn
    })

  // With MEET_BACKEND_INTEGRATION, Meet mints the room code: a code invented
  // here names no room Meet knows about. A side service without Meet does not
  // bind the route, in which case we fall back to the local code.
  const resolveMeetingLink = async (): Promise<string> => {
    if (!window.MEET_BACKEND_INTEGRATION) {
      return generateLocalMeetingLink()
    }
    const roomUrl = await createVideoConferenceRoom()
    return roomUrl ?? generateLocalMeetingLink()
  }

  const addVideoConference = async (): Promise<void> => {
    // Clicks landing while the room is being created would mint extra rooms
    // and let the last response to settle overwrite the meeting link.
    if (isAddingRef.current) {
      return
    }
    isAddingRef.current = true
    try {
      const newMeetingLink = await resolveMeetingLink()
      setHasVideoConference(true)
      setMeetingLink(newMeetingLink)
      if (showMore) {
        setShowDescription?.(true)
      }
    } catch (error) {
      console.error('Failed to create the video conference room:', error)
    } finally {
      isAddingRef.current = false
    }
  }

  const handleAddVideoConference = (): void => {
    void addVideoConference()
  }

  const handleDeleteVideoConference = (): void => {
    setHasVideoConference(false)
    setMeetingLink(null)
  }

  return {
    handleAddVideoConference,
    handleDeleteVideoConference
  }
}
