import { queryClient } from '@/lib/query-client'
import { useMutation, useQuery } from '@tanstack/react-query'
import { verificationApi } from '../api'
import type {
  BatchItemFilters,
  BatchListFilters,
  EligibleFilters,
  StartVerificationRequest,
  VerifiedCompanyFilters,
} from '../types'
import { isBatchRunning } from '../types'

/** How often the active batch is re-read while it is still moving. */
const ACTIVE_POLL_INTERVAL_MS = 2_000

export const verificationKeys = {
  active: ['verifications', 'active'] as const,
  list: (filters: BatchListFilters) => ['verifications', 'list', filters] as const,
  detail: (id: string) => ['verifications', 'detail', id] as const,
  items: (id: string, filters: BatchItemFilters) => ['verifications', 'items', id, filters] as const,
  eligible: (filters: EligibleFilters) => ['verifications', 'eligible', filters] as const,
  companies: (filters: VerifiedCompanyFilters) => ['verifications', 'companies', filters] as const,
  addedByOptions: ['verifications', 'added-by-options'] as const,
}

/**
 * The active batch, polled only while it is non-terminal.
 *
 * <p>This is the whole persistence story on the client: there is no local
 * progress state to lose. Mounting the hook after a route change or a full page
 * reload re-reads the batch from the backend, and polling stops by itself the
 * moment the batch reaches a terminal status.
 */
export function useActiveVerification() {
  return useQuery({
    queryKey: verificationKeys.active,
    queryFn: () => verificationApi.active(),
    // Progress is live data; a cached value would look frozen.
    staleTime: 0,
    refetchInterval: (query) => (isBatchRunning(query.state.data) ? ACTIVE_POLL_INTERVAL_MS : false),
    // A running batch must keep advancing even when the tab is in the background.
    refetchIntervalInBackground: true,
  })
}

export function useVerificationBatches(filters: BatchListFilters) {
  return useQuery({
    queryKey: verificationKeys.list(filters),
    queryFn: () => verificationApi.list(filters),
  })
}

export function useVerificationBatch(id: string | undefined) {
  return useQuery({
    queryKey: verificationKeys.detail(id ?? ''),
    queryFn: () => verificationApi.getById(id!),
    enabled: !!id,
    refetchInterval: (query) => (isBatchRunning(query.state.data) ? ACTIVE_POLL_INTERVAL_MS : false),
  })
}

export function useVerificationItems(id: string | undefined, filters: BatchItemFilters) {
  return useQuery({
    queryKey: verificationKeys.items(id ?? '', filters),
    queryFn: () => verificationApi.items(id!, filters),
    enabled: !!id,
  })
}

export function useEligibleCompanies(filters: EligibleFilters, enabled = true) {
  return useQuery({
    queryKey: verificationKeys.eligible(filters),
    queryFn: () => verificationApi.eligible(filters),
    enabled,
  })
}

export function useVerifiedCompanies(filters: VerifiedCompanyFilters) {
  return useQuery({
    queryKey: verificationKeys.companies(filters),
    queryFn: () => verificationApi.verifiedCompanies(filters),
  })
}

export function useAddedByOptions() {
  return useQuery({
    queryKey: verificationKeys.addedByOptions,
    queryFn: () => verificationApi.addedByOptions(),
    // The set of people who have added companies barely moves.
    staleTime: 5 * 60_000,
  })
}

/**
 * Starts a batch.
 *
 * <p>On success every server-derived list is invalidated rather than patched by
 * hand: the selection list changes (rows become in-flight), the active card
 * appears, and history gains a row.
 */
export function useStartVerification() {
  return useMutation({
    mutationFn: (request: StartVerificationRequest) => verificationApi.start(request),
    onSuccess: (batch) => {
      queryClient.setQueryData(verificationKeys.active, batch)
      queryClient.invalidateQueries({ queryKey: ['verifications'] })
    },
  })
}

/**
 * Refreshes the views a finished batch changes.
 *
 * <p>Called once when the active batch reaches a terminal status, so the
 * verified-companies table and the history list reflect the outcome without the
 * analyst reloading the page.
 */
export function invalidateAfterBatchCompletion(): void {
  queryClient.invalidateQueries({ queryKey: ['verifications', 'list'] })
  queryClient.invalidateQueries({ queryKey: ['verifications', 'companies'] })
  queryClient.invalidateQueries({ queryKey: ['verifications', 'eligible'] })
  queryClient.invalidateQueries({ queryKey: ['companies'] })
}
