import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/api/client'
import { verificationApi } from '../api'
import { VerifyNewCompaniesPanel } from '../components/VerifyNewCompaniesPanel'
import { addedByOptions, batch, eligibleCompany, page, renderWithProviders } from './testUtils'

vi.mock('../api', () => ({
  verificationApi: {
    eligible: vi.fn(),
    addedByOptions: vi.fn(),
    start: vi.fn(),
  },
}))

const api = vi.mocked(verificationApi)

describe('VerifyNewCompaniesPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.addedByOptions.mockResolvedValue(addedByOptions())
    api.eligible.mockResolvedValue(
      page([
        eligibleCompany({ id: 'c1', canonical_name: 'ABC Pumps' }),
        eligibleCompany({ id: 'c2', canonical_name: 'XYZ Engineering', added_by_name: 'Kumar' }),
      ]),
    )
    api.start.mockResolvedValue(batch({ status: 'QUEUED' }))
  })

  it('lists eligible companies with their added-by and phone', async () => {
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning={false} />)

    expect(await screen.findByText('ABC Pumps')).toBeInTheDocument()
    expect(screen.getByText('XYZ Engineering')).toBeInTheDocument()
    expect(screen.getByText('Kumar')).toBeInTheDocument()
    expect(screen.getAllByText('81312 81281').length).toBeGreaterThan(0)
  })

  it('defaults to unverified companies only', async () => {
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning={false} />)

    await screen.findByText('ABC Pumps')
    expect(screen.getByText('Unverified only')).toBeInTheDocument()
    // The endpoint itself never returns a VERIFIED company, so no status
    // parameter is needed to get unverified-only selection.
    expect(api.eligible).toHaveBeenCalled()
    const call = api.eligible.mock.calls[0]?.[0]
    expect(call?.verification_status).toBeUndefined()
  })

  it('sends the Added By filter to the server when applied', async () => {
    const user = userEvent.setup()
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning={false} />)
    await screen.findByText('ABC Pumps')

    await user.selectOptions(screen.getByLabelText('Added by'), 'user-1')
    await user.click(screen.getByRole('button', { name: 'Apply' }))

    await waitFor(() => {
      expect(api.eligible).toHaveBeenLastCalledWith(
        expect.objectContaining({ added_by: 'user-1' }),
      )
    })
  })

  it('sends the inclusive date range to the server when applied', async () => {
    const user = userEvent.setup()
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning={false} />)
    await screen.findByText('ABC Pumps')

    await user.clear(screen.getByLabelText('Date from'))
    await user.type(screen.getByLabelText('Date from'), '2026-09-01')
    await user.clear(screen.getByLabelText('Date to'))
    await user.type(screen.getByLabelText('Date to'), '2026-09-09')
    await user.click(screen.getByRole('button', { name: 'Apply' }))

    await waitFor(() => {
      expect(api.eligible).toHaveBeenLastCalledWith(
        expect.objectContaining({ date_from: '2026-09-01', date_to: '2026-09-09' }),
      )
    })
  })

  it('refuses an inverted date range instead of querying with it', async () => {
    const user = userEvent.setup()
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning={false} />)
    await screen.findByText('ABC Pumps')
    const callsBefore = api.eligible.mock.calls.length

    await user.clear(screen.getByLabelText('Date from'))
    await user.type(screen.getByLabelText('Date from'), '2026-09-10')
    await user.clear(screen.getByLabelText('Date to'))
    await user.type(screen.getByLabelText('Date to'), '2026-09-01')

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Date from must not be after date to',
    )
    expect(screen.getByRole('button', { name: 'Apply' })).toBeDisabled()
    expect(api.eligible.mock.calls.length).toBe(callsBefore)
  })

  it('tracks the selected count and starts the batch after confirmation', async () => {
    const user = userEvent.setup()
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning={false} />)
    await screen.findByText('ABC Pumps')

    expect(screen.getByText('No companies selected')).toBeInTheDocument()

    await user.click(screen.getByRole('checkbox', { name: 'Select ABC Pumps' }))
    expect(await screen.findByText('1 company selected')).toBeInTheDocument()

    await user.click(screen.getByRole('checkbox', { name: 'Select XYZ Engineering' }))
    expect(await screen.findByText('2 companies selected')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Start Verification' }))

    // Confirmation is required, and it must say the work continues in the
    // background.
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText('Start verification?')).toBeInTheDocument()
    expect(within(dialog).getByText(/leave this page/i)).toBeInTheDocument()
    expect(api.start).not.toHaveBeenCalled()

    await user.click(within(dialog).getByRole('button', { name: 'Start Verification' }))

    await waitFor(() => {
      expect(api.start).toHaveBeenCalledWith(
        expect.objectContaining({ company_ids: ['c1', 'c2'] }),
      )
    })
  })

  it('passes the applied filter snapshot along with the selection', async () => {
    const user = userEvent.setup()
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning={false} />)
    await screen.findByText('ABC Pumps')

    await user.selectOptions(screen.getByLabelText('Added by'), 'user-1')
    await user.click(screen.getByRole('button', { name: 'Apply' }))
    await waitFor(() => expect(api.eligible).toHaveBeenLastCalledWith(
      expect.objectContaining({ added_by: 'user-1' }),
    ))

    await user.click(await screen.findByRole('checkbox', { name: 'Select ABC Pumps' }))
    await user.click(screen.getByRole('button', { name: 'Start Verification' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Start Verification' }))

    await waitFor(() => {
      expect(api.start).toHaveBeenCalledWith(
        expect.objectContaining({ company_ids: ['c1'], added_by: 'user-1' }),
      )
    })
  })

  it('clears the selection when filters change, so a stale id cannot be submitted', async () => {
    const user = userEvent.setup()
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning={false} />)
    await screen.findByText('ABC Pumps')

    await user.click(screen.getByRole('checkbox', { name: 'Select ABC Pumps' }))
    expect(await screen.findByText('1 company selected')).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Added by'), 'user-2')
    await user.click(screen.getByRole('button', { name: 'Apply' }))

    expect(await screen.findByText('No companies selected')).toBeInTheDocument()
  })

  it('selects and clears every selectable row on the page at once', async () => {
    const user = userEvent.setup()
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning={false} />)
    await screen.findByText('ABC Pumps')

    const selectAll = screen.getByRole('checkbox', { name: 'Select all companies on this page' })
    await user.click(selectAll)
    expect(await screen.findByText('2 companies selected')).toBeInTheDocument()

    await user.click(selectAll)
    expect(await screen.findByText('No companies selected')).toBeInTheDocument()
  })

  it('does not allow selecting a company that is already being verified', async () => {
    api.eligible.mockResolvedValue(
      page([
        eligibleCompany({ id: 'c1', canonical_name: 'In Flight Corp', in_flight: true }),
      ]),
    )
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning={false} />)

    expect(await screen.findByText('In Flight Corp')).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Select In Flight Corp' })).toBeDisabled()
    expect(screen.getByText('Being verified')).toBeInTheDocument()
  })

  it('does not allow selecting a company without a usable phone', async () => {
    api.eligible.mockResolvedValue(
      page([
        eligibleCompany({
          id: 'c1',
          canonical_name: 'No Phone Corp',
          phone_usable: false,
          primary_phone_normalized: null,
        }),
      ]),
    )
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning={false} />)

    expect(await screen.findByText('No Phone Corp')).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Select No Phone Corp' })).toBeDisabled()
    expect(screen.getByText('No usable phone')).toBeInTheDocument()
  })

  it('will not start a second batch while one is running', async () => {
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning />)
    await screen.findByText('ABC Pumps')

    expect(screen.getByRole('button', { name: 'Start Verification' })).toBeDisabled()
    expect(screen.getByText(/already running/i)).toBeInTheDocument()
  })

  it('hides the start action from a read-only role', async () => {
    renderWithProviders(
      <VerifyNewCompaniesPanel canStart={false} batchRunning={false} />,
      ['PS_VIEWER'],
    )
    await screen.findByText('ABC Pumps')

    expect(screen.queryByRole('button', { name: 'Start Verification' })).not.toBeInTheDocument()
    expect(screen.getByText(/not start a verification/i)).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Select ABC Pumps' })).toBeDisabled()
  })

  it('teaches the next action when no company matches the filters', async () => {
    api.eligible.mockResolvedValue(page([]))
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning={false} />)

    expect(
      await screen.findByText('No unverified companies match these filters'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reset filters' })).toBeInTheDocument()
  })

  it('shows the problem detail verbatim when the server rejects the batch', async () => {
    const user = userEvent.setup()
    api.start.mockRejectedValue(
      new ApiError('fallback', 422, {
        status: 422,
        title: 'Business Rule Violation',
        detail: 'No eligible companies in the selection',
      }),
    )
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning={false} />)
    await screen.findByText('ABC Pumps')

    await user.click(screen.getByRole('checkbox', { name: 'Select ABC Pumps' }))
    await user.click(screen.getByRole('button', { name: 'Start Verification' }))
    const dialog = await screen.findByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Start Verification' }))

    expect(
      await screen.findByText('No eligible companies in the selection'),
    ).toBeInTheDocument()
  })

  it('shows an error state rather than an empty table when the list fails', async () => {
    api.eligible.mockRejectedValue(
      new ApiError('fallback', 500, { status: 500, detail: 'Database unavailable' }),
    )
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning={false} />)

    expect(await screen.findByText('Database unavailable')).toBeInTheDocument()
    expect(
      screen.queryByText('No unverified companies match these filters'),
    ).not.toBeInTheDocument()
  })

  it('pages through the eligible list server-side', async () => {
    const user = userEvent.setup()
    api.eligible.mockResolvedValue(
      page([eligibleCompany({ id: 'c1' })], { total_elements: 40, total_pages: 2 }),
    )
    renderWithProviders(<VerifyNewCompaniesPanel canStart batchRunning={false} />)
    await screen.findByText('ABC Pumps')

    await user.click(screen.getByRole('button', { name: 'Next' }))

    await waitFor(() => {
      expect(api.eligible).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }))
    })
  })
})
