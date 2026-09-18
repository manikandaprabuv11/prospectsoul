import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from '@/api/client'
import { verificationApi } from '../api'

vi.mock('@/api/client', () => ({
  apiClient: { get: vi.fn(), post: vi.fn() },
}))

const client = vi.mocked(apiClient)

/**
 * The feature API service is the only place that names a verification
 * endpoint. These tests pin the paths and query-parameter names to the backend
 * contract, since a rename on either side is otherwise a silent runtime
 * `undefined`.
 */
describe('verificationApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    client.get.mockResolvedValue(undefined)
    client.post.mockResolvedValue(undefined)
  })

  it('starts a batch by POSTing the snake_case body', async () => {
    await verificationApi.start({
      company_ids: ['c1', 'c2'],
      added_by: 'user-1',
      date_from: '2026-09-01',
      date_to: '2026-09-09',
    })

    expect(client.post).toHaveBeenCalledWith('/api/v1/verifications', {
      body: {
        company_ids: ['c1', 'c2'],
        added_by: 'user-1',
        date_from: '2026-09-01',
        date_to: '2026-09-09',
      },
    })
  })

  it('normalises the 204 "nothing running" answer to null', async () => {
    client.get.mockResolvedValue(undefined)

    await expect(verificationApi.active()).resolves.toBeNull()
    expect(client.get).toHaveBeenCalledWith('/api/v1/verifications/active')
  })

  it('returns the active batch when one is running', async () => {
    client.get.mockResolvedValue({ id: 'batch-1' })

    await expect(verificationApi.active()).resolves.toEqual({ id: 'batch-1' })
  })

  it('lists jobs with the backend pagination and filter parameter names', async () => {
    await verificationApi.list({ status: 'COMPLETED', requested_by: 'user-1', page: 2 })

    expect(client.get).toHaveBeenCalledWith('/api/v1/verifications', {
      query: {
        status: 'COMPLETED',
        requested_by: 'user-1',
        from: undefined,
        to: undefined,
        page: 2,
        size: 10,
        sort: 'createdAt',
        sort_dir: 'desc',
      },
    })
  })

  it('reads one job and its items', async () => {
    await verificationApi.getById('batch-1')
    expect(client.get).toHaveBeenCalledWith('/api/v1/verifications/batch-1')

    await verificationApi.items('batch-1', { status: 'FAILED', q: 'abc' })
    expect(client.get).toHaveBeenLastCalledWith('/api/v1/verifications/batch-1/items', {
      query: {
        status: 'FAILED',
        q: 'abc',
        page: 0,
        size: 25,
        sort: 'createdAt',
        sort_dir: 'asc',
      },
    })
  })

  it('asks for eligible companies with the Added By and date filters', async () => {
    await verificationApi.eligible({
      added_by: 'user-1',
      date_from: '2026-09-01',
      date_to: '2026-09-09',
    })

    expect(client.get).toHaveBeenCalledWith('/api/v1/verifications/eligible', {
      query: {
        added_by: 'user-1',
        date_from: '2026-09-01',
        date_to: '2026-09-09',
        verification_status: undefined,
        q: undefined,
        page: 0,
        size: 25,
        sort: 'createdAt',
        sort_dir: 'desc',
      },
    })
  })

  it('asks for verified companies from the verified-only endpoint', async () => {
    await verificationApi.verifiedCompanies({ q: 'pumps', verified_by: 'user-1' })

    expect(client.get).toHaveBeenCalledWith('/api/v1/verifications/companies', {
      query: {
        q: 'pumps',
        verified_by: 'user-1',
        verified_from: undefined,
        verified_to: undefined,
        added_by: undefined,
        page: 0,
        size: 25,
        sort: 'verifiedAt',
        sort_dir: 'desc',
      },
    })
  })

  it('reads the Added By options', async () => {
    await verificationApi.addedByOptions()

    expect(client.get).toHaveBeenCalledWith('/api/v1/verifications/added-by-options')
  })
})
