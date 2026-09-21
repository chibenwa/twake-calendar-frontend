import { fireEvent, screen } from '@testing-library/react'
import { renderWithProviders } from '../../utils/Renderwithproviders'
import { DebugModeToggle } from '@common/components/Debug/DebugModeToggle'
import { isDebugModeEnabled } from '@common/utils/debugMode'

const pressShortcut = (overrides: Partial<KeyboardEventInit> = {}): boolean =>
  fireEvent.keyDown(window, {
    code: 'KeyD',
    key: 'D',
    ctrlKey: true,
    shiftKey: true,
    altKey: true,
    ...overrides
  })

describe('DebugModeToggle', () => {
  beforeEach(() => {
    window.DEBUG = false
  })

  it('shows nothing until the shortcut is pressed', () => {
    renderWithProviders(<DebugModeToggle />)

    expect(screen.queryByText('debug.enabled')).not.toBeInTheDocument()
    expect(screen.queryByText('debug.disabled')).not.toBeInTheDocument()
  })

  it('enables debug mode and reports it on the shortcut', () => {
    renderWithProviders(<DebugModeToggle />)

    pressShortcut()

    expect(isDebugModeEnabled()).toBe(true)
    expect(screen.getByText('debug.enabled')).toBeInTheDocument()
  })

  it('disables debug mode on a second strike', () => {
    renderWithProviders(<DebugModeToggle />)

    pressShortcut()
    pressShortcut()

    expect(isDebugModeEnabled()).toBe(false)
    expect(screen.getByText('debug.disabled')).toBeInTheDocument()
  })

  it('disables debug mode shipped as enabled by .env.js', () => {
    window.DEBUG = true
    renderWithProviders(<DebugModeToggle />)

    pressShortcut()

    expect(isDebugModeEnabled()).toBe(false)
    expect(screen.getByText('debug.disabled')).toBeInTheDocument()
  })

  it('ignores the key without the whole modifier combination', () => {
    renderWithProviders(<DebugModeToggle />)

    pressShortcut({ altKey: false })
    pressShortcut({ ctrlKey: false })
    pressShortcut({ shiftKey: false })
    pressShortcut({ code: 'KeyE' })

    expect(isDebugModeEnabled()).toBe(false)
    expect(screen.queryByText('debug.enabled')).not.toBeInTheDocument()
  })

  it('ignores the auto repeat of a held down key', () => {
    renderWithProviders(<DebugModeToggle />)

    pressShortcut()
    pressShortcut({ repeat: true })
    pressShortcut({ repeat: true })

    expect(isDebugModeEnabled()).toBe(true)
    expect(screen.getByText('debug.enabled')).toBeInTheDocument()
  })

  it('stops listening once unmounted', () => {
    const { unmount } = renderWithProviders(<DebugModeToggle />)

    unmount()
    pressShortcut()

    expect(isDebugModeEnabled()).toBe(false)
  })
})
