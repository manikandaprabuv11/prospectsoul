export interface EnrichmentJob {
  id: string
  company_id: string
  provider_key: string
  status: string
  attempt: number
  max_attempts: number
  facts_added: number
  facts_updated: number
  candidates_added: number
  cost_usd: number
  error_code: string | null
  error_message: string | null
  started_at: string | null
  completed_at: string | null
  triggered_via: string
  batch_id: string | null
  created_at: string
}

export interface EnrichmentCandidate {
  id: string
  company_id: string
  enrichment_job_id: string
  candidate_type: string
  field_name: string
  proposed_value: string
  current_value: string | null
  status: string
  provider_key: string
  resolved_by: string | null
  resolved_at: string | null
  created_at: string
}

export interface ProviderConfig {
  provider_key: string
  enabled: boolean
  rate_limit_per_sec: number | null
  rate_limit_per_day: number | null
  timeout_ms: number
  max_retries: number
  idempotency_window_hours: number
  cost_per_call_usd: number
  updated_at: string
}

export interface EnrichCompanyRequest {
  provider_keys: string[]
  force: boolean
}

export interface ResolveCandidateRequest {
  action: 'ACCEPTED' | 'REJECTED'
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
