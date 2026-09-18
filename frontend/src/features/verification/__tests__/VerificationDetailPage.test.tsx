import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/client'
import { Route, Routes } from 'react-router'
import { verificationApi } from '../api'
import { VerificationDetailPage } from '../pages/VerificationDetailPage'
import { batch, batchItem, page, renderWithProviders } from './testUtils'

vi.mock('../api', () => ({
  verificationApi: {
    getById: vi.fn(),
    items: vi.fn(),
  },
}))

const api = vi.mocked(verificationApi)

/** Renders the page at `/verify/batch-1` so `useParams` resolves the id. */
function renderDetail(roles: string[] = ['PS_ANALYST']) {
  return renderWithProviders(
    <Routes>
      <Route path="/verify/:id" element={<VerificationDetailPage />} />
    </Routes>,
    roles,
    ['/verify/batch-1'],
  )
}

describe('VerificationDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.getById.mockResolvedValue(
      batch({
        status: 'COMPLETED_WITH_ERRORS',
        queued_count: 0,
        processing_count: 0,
        verified_count: 98,
        failed_count: 2,
        skipped_count: 0,
        progress_percent: 100,
        current_item: null,
        completed_at: '2026-09-09T11:20:00Z',
        filter_added_by: 'user-1',
        filter_added_by_name: 'Mani',
        filter_date_from: '2026-09-01',
        filter_date_to: '2026-09-09',
      }),
    )
    api.items.mockResolvedValue(page([batchItem()]))
  })

  it('renders a completed job with its summary statistics', async () => {
    renderDetail()

    expect(await screen.findByText('Completed with errors')).toBeInTheDocument()
    expect(screen.getByText('100 / 100 companies processed')).toBeInTheDocument()
    expect(screen.getByText('98')).toBeInTheDocument()
    expect(screen.getByRole('progressbar', { name: 'Job progress' })).toHaveAttribute(
      'aria-valuenow',
      '100',
    )
  })

  it('shows the filter snapshot the job was created under', async () => {
    renderDetail()

    await screen.findByText('Completed with errors')
    expect(screen.getByText('Filter — added by')).toBeInTheDocument()
    expect(screen.getByText('Mani')).toBeInTheDocument()
    expect(screen.getByText('Filter — date range')).toBeInTheDocument()
  })

  it('lists per-company results with provider detail', async () => {
    renderDetail()

    expect(await screen.findByRole('link', { name: 'ABC Pumps' })).toBeInTheDocument()
    expect(screen.getByText('Mobile')).toBeInTheDocument()
    expect(screen.getByText('Airtel')).toBeInTheDocument()
    expect(screen.getByText('+91 81312 81281')).toBeInTheDocument()
  })

  it('explains a failure in words, with the provider message as the tooltip', async () => {
    api.items.mockResolvedValue(
      page([
        batchItem({
          status: 'FAILED',
          phone_valid: true,
          line_type: 'landline',
          failure_code: 'NON_MOBILE_LINE_TYPE',
          failure_message: "Line type is 'landline', not mobile",
        }),
      ]),
    )
    renderDetail()

    const reason = await screen.findByText('Not a mobile line')
    expect(reason).toBeInTheDocument()
    expect(reason).toHaveAttribute('title', "Line type is 'landline', not mobile")
    expect(screen.getByText('Landline')).toBeInTheDocument()
    expect(screen.getAllByText('Failed').length).toBeGreaterThan(0)
  })

  it('explains a skip reason', async () => {
    api.items.mockResolvedValue(
      page([
        batchItem({
          status: 'SKIPPED',
          phone_valid: null,
          line_type: null,
          carrier_name: null,
          failure_code: 'ALREADY_VERIFIED',
          failure_message: 'Company was already verified',
        }),
      ]),
    )
    renderDetail()

    expect(await screen.findByText('Already verified')).toBeInTheDocument()
    expect(screen.getAllByText('Skipped').length).toBeGreaterThan(0)
  })

  it('filters the results by status server-side', async () => {
    const user = userEvent.setup()
    renderDetail()
    await screen.findByRole('link', { name: 'ABC Pumps' })

    await user.selectOptions(screen.getByLabelText('Status'), 'FAILED')

    await waitFor(() => {
      expect(api.items).toHaveBeenLastCalledWith(
        'batch-1',
        expect.objectContaining({ status: 'FAILED' }),
      )
    })
  })

  it('searches the results server-side', async () => {
    const user = userEvent.setup()
    renderDetail()
    await screen.findByRole('link', { name: 'ABC Pumps' })

    await user.type(screen.getByLabelText('Search'), 'ABC')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    await waitFor(() => {
      expect(api.items).toHaveBeenLastCalledWith(
        'batch-1',
        expect.objectContaining({ q: 'ABC' }),
      )
    })
  })

  it('shows the problem detail when the job cannot be loaded', async () => {
    api.getById.mockRejectedValue(
      new ApiError('fallback', 404, {
        status: 404,
        title: 'Not Found',
        detail: 'Verification batch not found: batch-1',
      }),
    )
    renderDetail()

    expect(await screen.findByText('Verification batch not found: batch-1')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Back to Verify/ })).toBeInTheDocument()
  })

  it('teaches the empty state when no result matches the filter', async () => {
    api.items.mockResolvedValue(page([]))
    renderDetail()

    expect(await screen.findByText('No results match this filter')).toBeInTheDocument()
  })

  it('is readable by a read-only role', async () => {
    renderDetail(['PS_COO'])

    expect(await screen.findByText('Completed with errors')).toBeInTheDocument()
  })
})
