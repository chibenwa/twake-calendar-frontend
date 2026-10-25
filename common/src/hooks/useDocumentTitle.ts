import { useLayoutEffect } from 'react'

const APP_NAME = 'Twake Calendar'

/**
 * Names the browser tab after the screen, so that screen-reader users hear
 * where they are on load, and anyone can tell tabs apart.
 */
export const useDocumentTitle = (title: string | undefined): void => {
  useLayoutEffect(() => {
    if (!title) return
    const previous = document.title
    document.title = `${title} – ${APP_NAME}`
    return (): void => {
      document.title = previous
    }
  }, [title])
}
