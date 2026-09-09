import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { apiClient, setAuthTokenProvider, setOnUnauthorized, ApiError } from '../client'

const originalFetch = globalThis.fetch

beforeEach(() => {
  setAuthTokenProvider(() => undefined)
})

afterEach(() => {
  globalThis.fetch = originalFetch
})

function mockFetchResponse(body: unknown, status: number, contentType = 'application/json') {
  globalThis.fetch = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': contentType },
    }),
  )
}

describe('apiClient', () => {
  it('attaches bearer token when provider returns a token', async () => {
    setAuthTokenProvider(() => 'test-jwt-token')
    mockFetchResponse({ ok: true }, 200)

    await apiClient.get('/test')

    expect(globalThis.fetch).toHaveBeenCalledOnce()
    const [, options] = (globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls[0]!
    const headers = new Headers(options.headers)
    expect(headers.get('Authorization')).toBe('Bearer test-jwt-token')
  })

  it('does not duplicate the API base path when a service includes it', async () => {
    mockFetchResponse({ ok: true }, 200)

    await apiClient.get('/api/v1/companies')

    const [url] = (globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls[0]!
    expect(url).toBe('http://localhost:8080/api/v1/companies')
  })

  it('sends no Authorization header when no token', async () => {
    mockFetchResponse({ ok: true }, 200)

    await apiClient.get('/test')

    const [, options] = (globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls[0]!
    const headers = new Headers(options.headers)
    expect(headers.get('Authorization')).toBeNull()
  })

  it('throws ApiError with problem detail on 401', async () => {
    setAuthTokenProvider(() => 'expired-token')

    const problemJson = {
      type: 'about:blank',
      title: 'Unauthorized',
      status: 401,
      detail: 'Full authentication is required to access this resource',
    }

    mockFetchResponse(problemJson, 401, 'application/problem+json')

    try {
      await apiClient.get('/protected')
      expect.fail('Should have thrown')
    } catch (e) {
      expect(e).toBeInstanceOf(ApiError)
      const err = e as ApiError
      expect(err.status).toBe(401)
      expect(err.message).toBe('Full authentication is required to access this resource')
      expect(err.problem?.title).toBe('Unauthorized')
    }
  })

  it('calls onUnauthorized handler on 401 when token was sent', async () => {
    const handler = vi.fn()
    setAuthTokenProvider(() => 'expired-token')
    setOnUnauthorized(handler)

    mockFetchResponse({ status: 401, title: 'Unauthorized' }, 401)

    await expect(apiClient.get('/test')).rejects.toThrow()
    expect(handler).toHaveBeenCalledOnce()
  })

  it('does not call onUnauthorized when no token was sent', async () => {
    const handler = vi.fn()
    setOnUnauthorized(handler)

    mockFetchResponse({ status: 401, title: 'Unauthorized' }, 401)

    await expect(apiClient.get('/test')).rejects.toThrow()
    expect(handler).not.toHaveBeenCalled()
  })

  it('throws ApiError with problem detail on 403', async () => {
    setAuthTokenProvider(() => 'valid-token')

    const problemJson = {
      type: 'about:blank',
      title: 'Forbidden',
      status: 403,
      detail: 'You do not have permission to access this resource',
    }

    mockFetchResponse(problemJson, 403, 'application/problem+json')

    try {
      await apiClient.get('/admin')
      expect.fail('Should have thrown')
    } catch (e) {
      expect(e).toBeInstanceOf(ApiError)
      const err = e as ApiError
      expect(err.status).toBe(403)
      expect(err.message).toBe('You do not have permission to access this resource')
    }
  })

  it('throws ApiError on network failure', async () => {
    globalThis.fetch = vi.fn().mockRejectedValue(new Error('Failed to fetch'))

    try {
      await apiClient.get('/test')
      expect.fail('Should have thrown')
    } catch (e) {
      expect(e).toBeInstanceOf(ApiError)
      const err = e as ApiError
      expect(err.status).toBe(0)
      expect(err.message).toBe('Failed to fetch')
    }
  })
})
