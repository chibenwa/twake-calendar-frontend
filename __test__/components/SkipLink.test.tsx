import { MAIN_CONTENT_ID, SkipLink } from '@common/components/SkipLink/SkipLink'
import '@testing-library/jest-dom'
import { fireEvent, screen } from '@testing-library/react'
import { renderWithProviders } from '../utils/Renderwithproviders'

describe('SkipLink (#1791)', () => {
  it('moves the focus to the content without touching the URL', () => {
    renderWithProviders(
      <>
        <SkipLink />
        <button>Menu</button>
        <div id={MAIN_CONTENT_ID} tabIndex={-1}>
          Content
        </div>
      </>
    )
    const hash = window.location.hash

    fireEvent.click(screen.getByRole('link', { name: 'a11y.skipToContent' }))

    expect(screen.getByText('Content')).toHaveFocus()
    expect(window.location.hash).toBe(hash)
  })
})
