import { useEffect } from 'react'

export const APP_NAME = 'Twake Calendar'

/**
 * Names the screen in the browser title, the first thing a screen reader
 * announces on load and on tab switch. Restores the previous title on unmount.
 * An undefined title (still loading, or shown by a nested screen) leaves the
 * title alone.
 */
export const useDocumentTitle = (title: string | undefined): void => {
  useEffect(() => {
    if (!title) return
    const previousTitle = document.title
    document.title = `${title} – ${APP_NAME}`
    return (): void => {
      document.title = previousTitle
    }
  }, [title])
}
