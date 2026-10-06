import { RejectedError } from '@common/features/Calendars/types/RejectedError'

export function formatReduxError(error: unknown): string {
  if (!error) return 'Unknown error'

  if (typeof error === 'string') return error

  if (typeof error === 'object' && error !== null) {
    if (
      'message' in error &&
      typeof (error as { message: unknown }).message === 'string'
    ) {
      return (error as { message: string }).message
    }
  }

  return 'Unexpected error occurred'
}

export function httpStatusOf(err: unknown): number | undefined {
  return typeof err === 'object' && err !== null && 'response' in err
    ? (err as { response?: { status?: number } }).response?.status
    : undefined
}

export function toRejectedError(err: unknown): RejectedError {
  return { message: formatReduxError(err), status: httpStatusOf(err) }
}

/**
 * Message to store for a rejected thunk. A rejection without an HTTP status
 * (e.g. the browser's "Failed to fetch") carries untranslated text, so the
 * translated fallback is used instead, unless the message is itself a
 * translation key.
 */
export function rejectedErrorMessage(
  rejection: RejectedError | undefined,
  fallback: string
): string {
  const message = rejection?.message
  if (!message) return fallback
  if (message.startsWith('TRANSLATION:')) return message
  return rejection?.status === undefined ? fallback : message
}
