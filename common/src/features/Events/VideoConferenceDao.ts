import { api } from '@common/utils/apiUtils'

interface VideoConferenceRoom {
  url: string
}

/**
 * Ask the side service to create a Meet room owned by the authenticated user.
 *
 * Resolves to null when the side service does not bind the route (404), that
 * is when Meet is not configured there: the caller then falls back to a
 * meeting code of its own.
 */
export async function createVideoConferenceRoom(): Promise<string | null> {
  const response = await api.post('api/videoconference', {
    throwHttpErrors: false
  })
  if (response.status === 404) {
    return null
  }
  if (!response.ok) {
    throw new Error(
      `createVideoConferenceRoom failed with status ${response.status}`
    )
  }
  const room = await response.json<VideoConferenceRoom>()
  return room.url
}
