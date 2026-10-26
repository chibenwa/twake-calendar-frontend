import { renderHook } from '@testing-library/react'
import { useDocumentTitle } from '@common/hooks/useDocumentTitle'

describe('useDocumentTitle', () => {
  beforeEach(() => {
    document.title = 'Twake Calendar'
  })

  it('names the screen before the application', () => {
    renderHook(() => useDocumentTitle('Settings'))

    expect(document.title).toBe('Settings – Twake Calendar')
  })

  it('follows the screen when its title changes', () => {
    const { rerender } = renderHook(
      ({ title }: { title: string }) => useDocumentTitle(title),
      { initialProps: { title: 'October 2026' } }
    )

    rerender({ title: 'November 2026' })

    expect(document.title).toBe('November 2026 – Twake Calendar')
  })

  it('restores the previous title on unmount', () => {
    const { unmount } = renderHook(() => useDocumentTitle('Settings'))

    unmount()

    expect(document.title).toBe('Twake Calendar')
  })

  it('leaves the title alone without a screen title', () => {
    renderHook(() => useDocumentTitle(undefined))

    expect(document.title).toBe('Twake Calendar')
  })
})
