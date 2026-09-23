import type { PageResponse } from '@/features/company/types'

export type { PageResponse }

export interface ImportBatch {
  id: string
  file_name: string
  file_type: string
  source: string
  status: string
  total_rows: number
  processed_rows: number
  created_rows: number
  duplicate_rows: number
  rejected_rows: number
  error_message: string | null
  created_by: string | null
  created_at: string
  updated_at: string
}

export interface ImportRow {
  id: string
  row_number: number
  raw_data: Record<string, string>
  mapped_data: Record<string, string> | null
  status: string
  error_message: string | null
  company_id: string | null
  duplicate_of_company_id: string | null
  outcome_reason: string | null
  created_at: string
}

export interface MappingSuggestion {
  source_column: string
  target_field: string | null
  confidence: number
  match_type: string
  ambiguous: boolean
}

export interface MappingSuggestionResponse {
  detected_headers: string[]
  suggestions: MappingSuggestion[]
  available_target_fields: string[]
}

export interface ImportPreview {
  total_rows: number
  preview_rows: Record<string, string>[]
  validation_errors: { row_number: number; field: string; message: string }[]
}

export interface ColumnMapping {
  source_column: string
  target_field: string | null
}
