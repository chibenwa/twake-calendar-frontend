import { useRef, useState } from 'react'
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
  isCreatingRoom: boolean
  hasCreationFailed: boolean
  dismissCreationFailure: () => void
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
  const [isCreatingRoom, setIsCreatingRoom] = useState(false)
  const [hasCreationFailed, setHasCreationFailed] = useState(false)

  const generateLocalMeetingLink = (): string =>
    generateMeetingLink({
      localpart: email?.split('@')[0],
      workplaceFqdn,
      workplaceFqdnFallback: window.WORKPLACE_FQDN_FALLBACK
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
    setIsCreatingRoom(true)
    setHasCreationFailed(false)
    try {
      const newMeetingLink = await resolveMeetingLink()
      setHasVideoConference(true)
      setMeetingLink(newMeetingLink)
      if (showMore) {
        setShowDescription?.(true)
      }
    } catch (error) {
      console.error('Failed to create the video conference room:', error)
      // Without feedback, the user may send an invitation believing it
      // carries a meeting link
      setHasCreationFailed(true)
    } finally {
      isAddingRef.current = false
      setIsCreatingRoom(false)
    }
  }

  const handleAddVideoConference = (): void => {
    void addVideoConference()
  }

  const handleDeleteVideoConference = (): void => {
    setHasVideoConference(false)
    setMeetingLink(null)
  }

  const dismissCreationFailure = (): void => {
    setHasCreationFailed(false)
  }

  return {
    handleAddVideoConference,
    handleDeleteVideoConference,
    isCreatingRoom,
    hasCreationFailed,
    dismissCreationFailure
  }
}
