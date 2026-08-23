/**
 * @jest-environment jsdom
 */

import React from 'react'
import { renderHook } from '@testing-library/react'

const mockIsInIframe = jest.fn()

jest.mock('cozy-external-bridge', () => ({
  __esModule: true,
  default: jest.fn().mockImplementation(() => ({
    isInIframe: mockIsInIframe
  }))
}))

import { EmbeddingProvider } from '@common/contexts/EmbeddingContext'
import { useIsTdrivePickerAvailable } from '@common/features/Tdrive/hooks/useIsTdrivePickerAvailable'

const wrapper = ({ children }: { children: React.ReactNode }): JSX.Element => (
  <EmbeddingProvider>{children}</EmbeddingProvider>
)

describe('useIsTdrivePickerAvailable', () => {
  const originalUrl = window.TDRIVE_INTENT_URL
  const originalEnabled = window.TDRIVE_ENABLED

  beforeEach(() => {
    mockIsInIframe.mockReset()
    window.TDRIVE_INTENT_URL = 'https://drive.example.com'
    window.TDRIVE_ENABLED = true
  })

  afterEach(() => {
    window.TDRIVE_INTENT_URL = originalUrl
    window.TDRIVE_ENABLED = originalEnabled
  })

  it('should be available when configured and served as a Cozy app', () => {
    mockIsInIframe.mockReturnValue(true)
    const { result } = renderHook(() => useIsTdrivePickerAvailable(), {
      wrapper
    })
    expect(result.current).toBe(true)
  })

  it('should not be available when configured but served directly', () => {
    mockIsInIframe.mockReturnValue(false)
    const { result } = renderHook(() => useIsTdrivePickerAvailable(), {
      wrapper
    })
    expect(result.current).toBe(false)
  })

  it('should not be available when TDRIVE_ENABLED is false', () => {
    mockIsInIframe.mockReturnValue(true)
    window.TDRIVE_ENABLED = false
    const { result } = renderHook(() => useIsTdrivePickerAvailable(), {
      wrapper
    })
    expect(result.current).toBe(false)
  })

  it('should not be available when TDRIVE_INTENT_URL is missing', () => {
    mockIsInIframe.mockReturnValue(true)
    window.TDRIVE_INTENT_URL = ''
    const { result } = renderHook(() => useIsTdrivePickerAvailable(), {
      wrapper
    })
    expect(result.current).toBe(false)
  })
})
