/**
 * Verify module types.
 *
 * <p>Field names mirror the backend DTOs exactly, in `snake_case`, per the
 * project's API integration standard — the central client does no key
 * conversion, so a mismatch here is a silent `undefined` at runtime.
 */

export type VerificationBatchStatus =
  | 'QUEUED'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'COMPLETED_WITH_ERRORS'
  | 'FAILED'
  | 'CANCELLED'

export type VerificationItemStatus = 'QUEUED' | 'PROCESSING' | 'VERIFIED' | 'FAILED' | 'SKIPPED'

/** The company being looked up right now, for the progress card. */
export interface VerificationCurrentItem {
  company_id: string
  company_name: string | null
  phone_number: string | null
  provider: string | null
}

export interface VerificationBatch {
  id: string
  status: VerificationBatchStatus
  requested_by: string
  requested_by_name: string | null
  filter_added_by: string | null
  filter_added_by_name: string | null
  filter_date_from: string | null
  filter_date_to: string | null
  total_count: number
  queued_count: number
  processing_count: number
  verified_count: number
  failed_count: number
  skipped_count: number
  progress_percent: number
  elapsed_seconds: number | null
  current_item: VerificationCurrentItem | null
  started_at: string | null
  completed_at: string | null
  created_at: string
  updated_at: string
}

export interface VerificationBatchItem {
  id: string
  batch_id: string
  company_id: string
  company_name: string | null
  status: VerificationItemStatus
  phone_number: string | null
  normalized_phone_number: string | null
  provider: string | null
  provider_reference: string | null
  phone_valid: boolean | null
  line_type: string | null
  carrier_name: string | null
  mobile_country_code: string | null
  mobile_network_code: string | null
  failure_code: string | null
  failure_message: string | null
  attempt_count: number
  started_at: string | null
  completed_at: string | null
  created_at: string
}

export interface EligibleCompany {
  id: string
  canonical_name: string
  city: string | null
  state: string | null
  primary_phone_normalized: string | null
  verification_status: string
  added_by: string | null
  added_by_name: string | null
  added_at: string
  /** False when the stored phone cannot become a valid E.164 number. */
  phone_usable: boolean
  /** True when another batch is already verifying this company. */
  in_flight: boolean
}

export interface VerifiedCompany {
  id: string
  canonical_name: string
  city: string | null
  state: string | null
  verified_by: string | null
  verified_by_name: string | null
  verified_at: string | null
  added_by: string | null
  added_by_name: string | null
  added_at: string
  verified_phone: string | null
  line_type: string | null
  carrier_name: string | null
  provider: string | null
}

export interface AddedByOption {
  id: string
  name: string
  company_count: number
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  total_elements: number
  total_pages: number
}

export interface StartVerificationRequest {
  company_ids: string[]
  added_by?: string | undefined
  date_from?: string | undefined
  date_to?: string | undefined
}

export interface EligibleFilters {
  added_by?: string | undefined
  date_from?: string | undefined
  date_to?: string | undefined
  verification_status?: string | undefined
  q?: string | undefined
  page?: number
  size?: number
  sort?: string
  sort_dir?: 'asc' | 'desc'
}

export interface BatchListFilters {
  status?: string | undefined
  requested_by?: string | undefined
  from?: string | undefined
  to?: string | undefined
  page?: number
  size?: number
  sort?: string
  sort_dir?: 'asc' | 'desc'
}

export interface BatchItemFilters {
  status?: string | undefined
  q?: string | undefined
  page?: number
  size?: number
  sort?: string
  sort_dir?: 'asc' | 'desc'
}

export interface VerifiedCompanyFilters {
  q?: string | undefined
  verified_by?: string | undefined
  verified_from?: string | undefined
  verified_to?: string | undefined
  added_by?: string | undefined
  page?: number
  size?: number
  sort?: string
  sort_dir?: 'asc' | 'desc'
}

/** Batch statuses that are still moving, and therefore worth polling. */
export const NON_TERMINAL_STATUSES: readonly VerificationBatchStatus[] = ['QUEUED', 'PROCESSING']

export function isBatchRunning(batch: VerificationBatch | null | undefined): boolean {
  return batch != null && NON_TERMINAL_STATUSES.includes(batch.status)
}
