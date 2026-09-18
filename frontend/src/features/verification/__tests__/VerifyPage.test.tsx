import { screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/client'
import { verificationApi } from '../api'
import { VerifyPage } from '../pages/VerifyPage'
import {
  addedByOptions,
  batch,
  eligibleCompany,
  page,
  renderWithProviders,
  verifiedCompany,
} from './testUtils'

vi.mock('../api', () => ({
  verificationApi: {
    active: vi.fn(),
    list: vi.fn(),
    eligible: vi.fn(),
    verifiedCompanies: vi.fn(),
    addedByOptions: vi.fn(),
    start: vi.fn(),
    getById: vi.fn(),
    items: vi.fn(),
  },
}))

const api = vi.mocked(verificationApi)

describe('VerifyPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.active.mockResolvedValue(null)
    api.list.mockResolvedValue(page([]))
    api.eligible.mockResolvedValue(page([eligibleCompany()]))
    api.verifiedCompanies.mockResolvedValue(page([]))
    api.addedByOptions.mockResolvedValue(addedByOptions())
  })

  it('renders all four sections of the Verify workspace', async () => {
    renderWithProviders(<VerifyPage />)

    expect(screen.getByRole('heading', { level: 1, name: 'Verify' })).toBeInTheDocument()
    expect(await screen.findByText('Verify New Companies')).toBeInTheDocument()
    expect(screen.getByText('Current Verification')).toBeInTheDocument()
    expect(screen.getByText('Previous Verification Jobs')).toBeInTheDocument()
    expect(screen.getByText('Verified Companies')).toBeInTheDocument()
  })

  it('reconstructs a running batch from the server after a fresh mount', async () => {
    // This is the persistence guarantee: a newly mounted page — which is what a
    // route change or a browser refresh produces — shows the running batch
    // purely from GET /verifications/active.
    api.active.mockResolvedValue(batch())
    renderWithProviders(<VerifyPage />)

    expect(await screen.findByText('68 / 100 companies processed')).toBeInTheDocument()
    expect(screen.getByText('68%')).toBeInTheDocument()
    expect(screen.getAllByText('Processing').length).toBeGreaterThan(0)
    expect(screen.getByText('ABC Pumps Pvt Ltd')).toBeInTheDocument()
    expect(screen.getByText(/twilio lookup/i)).toBeInTheDocument()
    expect(screen.getByRole('progressbar', { name: 'Verification progress' })).toHaveAttribute(
      'aria-valuenow',
      '68',
    )
  })

  it('shows every counter the progress card is specified to display', async () => {
    api.active.mockResolvedValue(batch())
    renderWithProviders(<VerifyPage />)

    await screen.findByText('68 / 100 companies processed')
    for (const label of ['Verified', 'Processing', 'Queued', 'Failed', 'Skipped']) {
      expect(screen.getAllByText(label).length).toBeGreaterThan(0)
    }
    expect(screen.getByText('65')).toBeInTheDocument()
    expect(screen.getByText('31')).toBeInTheDocument()
    expect(screen.getByText(/Elapsed 03:21/)).toBeInTheDocument()
  })

  it('polls while the batch is running', async () => {
    vi.useFakeTimers()
    try {
      api.active.mockResolvedValue(batch({ status: 'PROCESSING' }))
      renderWithProviders(<VerifyPage />)

      await vi.waitFor(() => expect(api.active).toHaveBeenCalledTimes(1))
      const afterFirst = api.active.mock.calls.length

      await vi.advanceTimersByTimeAsync(5_000)

      expect(api.active.mock.calls.length).toBeGreaterThan(afterFirst)
    } finally {
      vi.useRealTimers()
    }
  })

  it('stops polling once the batch reaches a terminal status', async () => {
    vi.useFakeTimers()
    try {
      api.active.mockResolvedValue(
        batch({ status: 'COMPLETED', queued_count: 0, processing_count: 0, progress_percent: 100 }),
      )
      renderWithProviders(<VerifyPage />)

      await vi.waitFor(() => expect(api.active).toHaveBeenCalled())
      const afterSettle = api.active.mock.calls.length

      await vi.advanceTimersByTimeAsync(30_000)

      expect(api.active.mock.calls.length).toBe(afterSettle)
    } finally {
      vi.useRealTimers()
    }
  })

  it('shows final statistics when the batch has completed with errors', async () => {
    api.active.mockResolvedValue(
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
      }),
    )
    renderWithProviders(<VerifyPage />)

    expect(await screen.findByText('Completed with errors')).toBeInTheDocument()
    expect(screen.getByText('100 / 100 companies processed')).toBeInTheDocument()
    expect(screen.getByText('98')).toBeInTheDocument()
    expect(screen.getByText(/Took 03:21/)).toBeInTheDocument()
  })

  it('teaches the next action when no batch is running', async () => {
    renderWithProviders(<VerifyPage />)

    expect(await screen.findByText('No verification is running')).toBeInTheDocument()
    expect(screen.getByText(/runs in the background/i)).toBeInTheDocument()
  })

  it('states that verification proves line validity, not ownership', async () => {
    api.active.mockResolvedValue(batch())
    renderWithProviders(<VerifyPage />)

    expect(
      await screen.findByText(/does not confirm ownership or that anyone answered/i),
    ).toBeInTheDocument()
  })

  it('surfaces the problem detail when the active read fails', async () => {
    api.active.mockRejectedValue(
      new ApiError('fallback', 500, { status: 500, detail: 'Active lookup failed' }),
    )
    renderWithProviders(<VerifyPage />)

    expect(await screen.findByText('Active lookup failed')).toBeInTheDocument()
  })

  it('does not offer the selection panel to a read-only role', async () => {
    renderWithProviders(<VerifyPage />, ['PS_VIEWER'])

    await screen.findByText('Current Verification')
    expect(screen.queryByRole('button', { name: /Verify New Companies/ })).not.toBeInTheDocument()
    expect(screen.queryByText('Verify New Companies')).not.toBeInTheDocument()
  })

  it('shows the verified-companies table with provider detail', async () => {
    api.verifiedCompanies.mockResolvedValue(page([verifiedCompany()]))
    renderWithProviders(<VerifyPage />)

    expect(await screen.findByRole('link', { name: 'ABC Pumps' })).toBeInTheDocument()
    expect(screen.getByText('Mobile')).toBeInTheDocument()
    expect(screen.getByText('Airtel')).toBeInTheDocument()
    expect(screen.getByText('+91 81312 81281')).toBeInTheDocument()
  })

  it('asks the verified table only for verified companies', async () => {
    renderWithProviders(<VerifyPage />)

    await waitFor(() => expect(api.verifiedCompanies).toHaveBeenCalled())
    // The endpoint returns VERIFIED companies only; the client never has to
    // filter, and cannot widen it.
    expect(api.eligible).toHaveBeenCalled()
    expect(api.verifiedCompanies).toHaveBeenCalledWith(
      expect.objectContaining({ sort: 'verifiedAt' }),
    )
  })

  it('shows the history table with its filter snapshot and statistics', async () => {
    api.list.mockResolvedValue(
      page([
        batch({
          id: 'batch-9',
          status: 'COMPLETED_WITH_ERRORS',
          filter_added_by: 'user-1',
          filter_added_by_name: 'Mani',
          filter_date_from: '2026-09-09',
          filter_date_to: '2026-09-09',
          total_count: 100,
          verified_count: 98,
          failed_count: 2,
          skipped_count: 0,
        }),
      ]),
    )
    renderWithProviders(<VerifyPage />)

    expect(await screen.findByText('Completed with errors')).toBeInTheDocument()
    expect(screen.getByText(/^Mani · /)).toBeInTheDocument()
    expect(screen.getByText('98')).toBeInTheDocument()
  })

  it('teaches the empty state for history', async () => {
    renderWithProviders(<VerifyPage />)

    expect(await screen.findByText('No verification jobs yet')).toBeInTheDocument()
  })

  it('teaches the empty state for verified companies', async () => {
    renderWithProviders(<VerifyPage />)

    expect(await screen.findByText('No verified companies yet')).toBeInTheDocument()
  })
})
