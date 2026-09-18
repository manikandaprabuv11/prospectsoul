import { AuthContext } from '@/auth/AuthContext'
import type { AuthContextValue } from '@/auth/AuthContext'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router'
import { vi } from 'vitest'
import type {
  AddedByOption,
  EligibleCompany,
  PageResponse,
  VerificationBatch,
  VerificationBatchItem,
  VerifiedCompany,
} from '../types'

/**
 * Renders a Verify component with the providers it needs: a throwaway
 * TanStack Query client (retries off so an error state is reached
 * immediately), a router, and an auth context with the given roles.
 *
 * @param initialEntries starting router location, needed when the component
 *                       under test reads route params
 */
export function renderWithProviders(
  ui: ReactElement,
  roles: string[] = ['PS_ANALYST'],
  initialEntries: string[] = ['/'],
) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  const authValue: AuthContextValue = {
    authenticated: true,
    user: { id: 'user-1', username: 'analyst', fullName: 'Priya Sharma', email: 'p@vyoog.com' },
    roles,
    token: 'token',
    hasRole: (role) => roles.includes(role),
    logout: vi.fn(),
  }

  return {
    queryClient,
    ...render(
      <AuthContext value={authValue}>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={initialEntries}>{ui}</MemoryRouter>
        </QueryClientProvider>
      </AuthContext>,
    ),
  }
}

export function page<T>(content: T[], overrides: Partial<PageResponse<T>> = {}): PageResponse<T> {
  return {
    content,
    page: 0,
    size: 25,
    total_elements: content.length,
    total_pages: content.length === 0 ? 0 : 1,
    ...overrides,
  }
}

export function eligibleCompany(overrides: Partial<EligibleCompany> = {}): EligibleCompany {
  return {
    id: 'company-1',
    canonical_name: 'ABC Pumps',
    city: 'Coimbatore',
    state: 'Tamil Nadu',
    primary_phone_normalized: '8131281281',
    verification_status: 'UNVERIFIED',
    added_by: 'user-1',
    added_by_name: 'Mani',
    added_at: '2026-09-09T10:00:00Z',
    phone_usable: true,
    in_flight: false,
    ...overrides,
  }
}

export function batch(overrides: Partial<VerificationBatch> = {}): VerificationBatch {
  return {
    id: 'batch-1',
    status: 'PROCESSING',
    requested_by: 'user-1',
    requested_by_name: 'Mani',
    filter_added_by: null,
    filter_added_by_name: null,
    filter_date_from: null,
    filter_date_to: null,
    total_count: 100,
    queued_count: 31,
    processing_count: 1,
    verified_count: 65,
    failed_count: 2,
    skipped_count: 1,
    progress_percent: 68,
    elapsed_seconds: 201,
    current_item: {
      company_id: 'company-1',
      company_name: 'ABC Pumps Pvt Ltd',
      phone_number: '+918131281281',
      provider: 'twilio',
    },
    started_at: '2026-09-09T11:12:00Z',
    completed_at: null,
    created_at: '2026-09-09T11:12:00Z',
    updated_at: '2026-09-09T11:15:21Z',
    ...overrides,
  }
}

export function batchItem(overrides: Partial<VerificationBatchItem> = {}): VerificationBatchItem {
  return {
    id: 'item-1',
    batch_id: 'batch-1',
    company_id: 'company-1',
    company_name: 'ABC Pumps',
    status: 'VERIFIED',
    phone_number: '8131281281',
    normalized_phone_number: '+918131281281',
    provider: 'twilio',
    provider_reference: 'https://lookups.twilio.com/v2/PhoneNumbers/+918131281281',
    phone_valid: true,
    line_type: 'mobile',
    carrier_name: 'Airtel',
    mobile_country_code: '404',
    mobile_network_code: '90',
    failure_code: null,
    failure_message: null,
    attempt_count: 1,
    started_at: '2026-09-09T11:12:05Z',
    completed_at: '2026-09-09T11:12:06Z',
    created_at: '2026-09-09T11:12:00Z',
    ...overrides,
  }
}

export function verifiedCompany(overrides: Partial<VerifiedCompany> = {}): VerifiedCompany {
  return {
    id: 'company-1',
    canonical_name: 'ABC Pumps',
    city: 'Coimbatore',
    state: 'Tamil Nadu',
    verified_by: 'user-1',
    verified_by_name: 'Mani',
    verified_at: '2026-09-09T11:20:00Z',
    added_by: 'user-1',
    added_by_name: 'Mani',
    added_at: '2026-09-09T10:00:00Z',
    verified_phone: '+918131281281',
    line_type: 'mobile',
    carrier_name: 'Airtel',
    provider: 'twilio',
    ...overrides,
  }
}

export function addedByOptions(): AddedByOption[] {
  return [
    { id: 'user-1', name: 'Mani', company_count: 12 },
    { id: 'user-2', name: 'Kumar', company_count: 4 },
  ]
}
