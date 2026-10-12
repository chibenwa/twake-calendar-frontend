import { MoreLinkContent } from '@common/components/Calendar/MoreLinkContent'
import { render, screen } from '@testing-library/react'

describe('MoreLinkContent', () => {
  const props = { text: '+5 more', shortText: '+5' }

  it('shows the full text on wide screens', () => {
    render(<MoreLinkContent {...props} isCompact={false} />)

    expect(screen.getByText('+5 more')).toBeVisible()
    expect(screen.queryByText('+5')).not.toBeInTheDocument()
  })

  it('shows the short text on narrow screens and keeps the full text for screen readers', () => {
    render(<MoreLinkContent {...props} isCompact />)

    expect(screen.getByText('+5')).toHaveAttribute('aria-hidden', 'true')
    expect(screen.getByText('+5 more')).toHaveClass('visually-hidden')
  })
})
