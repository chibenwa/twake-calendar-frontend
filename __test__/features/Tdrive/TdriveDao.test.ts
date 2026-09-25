/**
 * @jest-environment jsdom
 */

import {
  exchangeToken,
  fetchIntentJSON
} from '@common/features/Tdrive/TdriveDao'
import { externalApi } from '@common/utils/apiUtils'

jest.mock('@common/utils/apiUtils')

describe('TdriveDao', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  describe('exchangeToken', () => {
    it('exchanges token with correct endpoint and body', async () => {
      const mockResponse = {
        json: jest.fn().mockResolvedValue({
          token_type: 'bearer',
          scope: 'io.cozy.files',
          access_token: 'test-access-token',
          refresh_token: 'test-refresh-token',
          client_id: 'client-123',
          client_secret: 'secret-123',
          registration_access_token: 'reg-token'
        })
      }
      ;(externalApi.post as jest.Mock).mockResolvedValue(mockResponse)

      const result = await exchangeToken(
        'https://drive.example.com',
        'user-id-token'
      )

      expect(externalApi.post).toHaveBeenCalledWith('auth/token_exchange', {
        prefixUrl: 'https://drive.example.com',
        json: {
          id_token: 'user-id-token',
          exchange_type: 'app'
        }
      })
      expect(result.access_token).toBe('test-access-token')
    })

    it('throws when API call fails', async () => {
      ;(externalApi.post as jest.Mock).mockRejectedValue(
        new Error('Network error')
      )

      await expect(
        exchangeToken('https://drive.example.com', 'token')
      ).rejects.toThrow('Network error')
    })
  })

  describe('fetchIntentJSON', () => {
    const mockFetch = jest.fn()

    beforeEach(() => {
      global.fetch = mockFetch
    })

    it('makes an authenticated request to the correct URL', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ data: 'response' })
      })

      const fetch = fetchIntentJSON({
        tdriveBaseUrl: 'https://drive.example.com',
        accessToken: 'my-token'
      })
      const result = await fetch('POST', '/intents', { foo: 'bar' })

      expect(mockFetch).toHaveBeenCalledWith(
        'https://drive.example.com/intents',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: 'Bearer my-token'
          },
          body: JSON.stringify({ foo: 'bar' }),
          signal: expect.any(AbortSignal)
        }
      )
      expect(result).toEqual({ data: 'response' })
    })

    it('omits body when not provided', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({})
      })

      const fetch = fetchIntentJSON({
        tdriveBaseUrl: 'https://drive.example.com',
        accessToken: 'my-token'
      })
      await fetch('GET', '/intents')

      expect(mockFetch).toHaveBeenCalledWith(
        'https://drive.example.com/intents',
        {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            Authorization: 'Bearer my-token'
          },
          body: undefined,
          signal: expect.any(AbortSignal)
        }
      )
    })

    it('throws when response is not ok', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        text: () => Promise.resolve('Unauthorized')
      })

      const fetch = fetchIntentJSON({
        tdriveBaseUrl: 'https://drive.example.com',
        accessToken: 'bad-token'
      })
      await expect(fetch('GET', '/intents')).rejects.toThrow('Unauthorized')
    })
  })
})
