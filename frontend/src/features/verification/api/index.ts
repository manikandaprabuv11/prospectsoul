import { apiClient } from '@/api/client'
import type {
  AddedByOption,
  BatchItemFilters,
  BatchListFilters,
  EligibleCompany,
  EligibleFilters,
  PageResponse,
  StartVerificationRequest,
  VerificationBatch,
  VerificationBatchItem,
  VerifiedCompany,
  VerifiedCompanyFilters,
} from '../types'

/**
 * Verify feature API service.
 *
 * <p>Built on the central HTTP client, which owns the base URL, the bearer
 * token and error normalisation. Nothing here calls `fetch` and nothing here
 * knows a Twilio credential — the backend performs every provider call.
 */
export const verificationApi = {
  /** Starts a batch. The backend answers 202; the work happens in its worker. */
  start(request: StartVerificationRequest) {
    return apiClient.post<VerificationBatch>('/api/v1/verifications', { body: request })
  },

  /**
   * The batch currently running, or null when nothing is.
   *
   * <p>The backend answers 204 for "nothing running", which the client parses
   * as `undefined`; it is normalised to `null` here so the query has a real
   * value to cache and the UI has one shape to render.
   */
  async active(): Promise<VerificationBatch | null> {
    const result = await apiClient.get<VerificationBatch | undefined>(
      '/api/v1/verifications/active',
    )
    return result ?? null
  },

  list(filters: BatchListFilters = {}) {
    return apiClient.get<PageResponse<VerificationBatch>>('/api/v1/verifications', {
      query: {
        status: filters.status,
        requested_by: filters.requested_by,
        from: filters.from,
        to: filters.to,
        page: filters.page ?? 0,
        size: filters.size ?? 10,
        sort: filters.sort ?? 'createdAt',
        sort_dir: filters.sort_dir ?? 'desc',
      },
    })
  },

  getById(id: string) {
    return apiClient.get<VerificationBatch>(`/api/v1/verifications/${id}`)
  },

  items(id: string, filters: BatchItemFilters = {}) {
    return apiClient.get<PageResponse<VerificationBatchItem>>(
      `/api/v1/verifications/${id}/items`,
      {
        query: {
          status: filters.status,
          q: filters.q,
          page: filters.page ?? 0,
          size: filters.size ?? 25,
          sort: filters.sort ?? 'createdAt',
          sort_dir: filters.sort_dir ?? 'asc',
        },
      },
    )
  },

  eligible(filters: EligibleFilters = {}) {
    return apiClient.get<PageResponse<EligibleCompany>>('/api/v1/verifications/eligible', {
      query: {
        added_by: filters.added_by,
        date_from: filters.date_from,
        date_to: filters.date_to,
        verification_status: filters.verification_status,
        q: filters.q,
        page: filters.page ?? 0,
        size: filters.size ?? 25,
        sort: filters.sort ?? 'createdAt',
        sort_dir: filters.sort_dir ?? 'desc',
      },
    })
  },

  verifiedCompanies(filters: VerifiedCompanyFilters = {}) {
    return apiClient.get<PageResponse<VerifiedCompany>>('/api/v1/verifications/companies', {
      query: {
        q: filters.q,
        verified_by: filters.verified_by,
        verified_from: filters.verified_from,
        verified_to: filters.verified_to,
        added_by: filters.added_by,
        page: filters.page ?? 0,
        size: filters.size ?? 25,
        sort: filters.sort ?? 'verifiedAt',
        sort_dir: filters.sort_dir ?? 'desc',
      },
    })
  },

  addedByOptions() {
    return apiClient.get<AddedByOption[]>('/api/v1/verifications/added-by-options')
  },
}
