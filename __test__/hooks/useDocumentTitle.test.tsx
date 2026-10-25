import { useDocumentTitle } from '@common/hooks/useDocumentTitle'
import { renderHook } from '@testing-library/react'

describe('useDocumentTitle (#1790)', () => {
  beforeEach(() => {
    document.title = 'Twake Calendar'
  })

  it('names the tab after the screen and restores the title on unmount', () => {
    const { rerender, unmount } = renderHook(
      ({ title }: { title?: string }) => useDocumentTitle(title),
      { initialProps: { title: 'October 2026' } }
    )
    expect(document.title).toBe('October 2026 – Twake Calendar')

    rerender({ title: 'Settings' })
    expect(document.title).toBe('Settings – Twake Calendar')

    unmount()
    expect(document.title).toBe('Twake Calendar')
  })

  it('keeps the title while the screen has none yet', () => {
    renderHook(() => useDocumentTitle(undefined))

    expect(document.title).toBe('Twake Calendar')
  })
})
