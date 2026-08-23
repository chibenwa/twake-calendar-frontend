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

import CozyBridge from 'cozy-external-bridge'
import {
  EmbeddingProvider,
  useIsInIframe
} from '@common/contexts/EmbeddingContext'

const MockedCozyBridge = CozyBridge as unknown as jest.Mock

describe('EmbeddingContext', () => {
  beforeEach(() => {
    mockIsInIframe.mockReset()
    MockedCozyBridge.mockClear()
  })

  it('should expose the embedding state detected by the bridge', () => {
    mockIsInIframe.mockReturnValue(true)
    const { result } = renderHook(() => useIsInIframe(), {
      wrapper: ({ children }) => (
        <EmbeddingProvider>{children}</EmbeddingProvider>
      )
    })
    expect(result.current).toBe(true)
  })

  it('should detect only once for every consumer under the provider', () => {
    mockIsInIframe.mockReturnValue(false)
    const { result } = renderHook(
      () => [useIsInIframe(), useIsInIframe(), useIsInIframe()],
      {
        wrapper: ({ children }) => (
          <EmbeddingProvider>{children}</EmbeddingProvider>
        )
      }
    )
    expect(result.current).toEqual([false, false, false])
    expect(MockedCozyBridge).toHaveBeenCalledTimes(1)
  })

  it('should fall back to detecting on the spot outside the provider', () => {
    mockIsInIframe.mockReturnValue(true)
    const { result } = renderHook(() => useIsInIframe())
    expect(result.current).toBe(true)
  })
})
