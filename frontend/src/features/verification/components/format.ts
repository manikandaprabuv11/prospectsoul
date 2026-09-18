import { ApiError } from '@/api/client'

/**
 * Display helpers for the Verify screens.
 *
 * <p>Backend timestamps are UTC ISO-8601; these render them in the viewer's
 * local zone, which is what an operator reading a queue expects.
 */

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

/** Elapsed time as mm:ss, or h:mm:ss once it passes an hour. */
export function formatElapsed(seconds: number | null | undefined): string {
  if (seconds == null) return '—'
  const total = Math.max(0, Math.floor(seconds))
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const secs = total % 60

  const pad = (n: number) => String(n).padStart(2, '0')
  return hours > 0 ? `${hours}:${pad(minutes)}:${pad(secs)}` : `${pad(minutes)}:${pad(secs)}`
}

/** A stored 10-digit national number as `+91 98765 43210`. */
export function formatPhone(phone: string | null | undefined): string {
  if (!phone) return '—'
  const digits = phone.replace(/[^0-9]/g, '')
  if (phone.startsWith('+') && digits.length === 12) {
    return `+${digits.slice(0, 2)} ${digits.slice(2, 7)} ${digits.slice(7)}`
  }
  if (digits.length === 10) {
    return `${digits.slice(0, 5)} ${digits.slice(5)}`
  }
  return phone
}

export function formatLineType(lineType: string | null | undefined): string {
  if (!lineType) return '—'
  // Provider values arrive lower-cased and squashed (`nonfixedvoip`).
  switch (lineType) {
    case 'mobile': return 'Mobile'
    case 'landline': return 'Landline'
    case 'fixedvoip': return 'Fixed VoIP'
    case 'nonfixedvoip': return 'Non-fixed VoIP'
    case 'tollfree': return 'Toll free'
    case 'sharedcost': return 'Shared cost'
    case 'voicemail': return 'Voicemail'
    case 'personal': return 'Personal'
    case 'premium': return 'Premium'
    case 'pager': return 'Pager'
    case 'uan': return 'UAN'
    case 'unknown': return 'Unknown'
    default: return lineType
  }
}

/** Human wording for the normalized failure/skip codes. */
export function formatFailureCode(code: string | null | undefined): string {
  if (!code) return '—'
  switch (code) {
    case 'NO_PHONE': return 'No usable phone'
    case 'ALREADY_VERIFIED': return 'Already verified'
    case 'NOT_ELIGIBLE': return 'Not eligible'
    case 'FILTER_MISMATCH': return 'Outside selected filters'
    case 'INVALID_NUMBER': return 'Invalid number'
    case 'NON_MOBILE_LINE_TYPE': return 'Not a mobile line'
    case 'MALFORMED_REQUEST': return 'Malformed request'
    case 'PROVIDER_AUTH_ERROR': return 'Provider authentication failed'
    case 'PROVIDER_NOT_CONFIGURED': return 'Provider not configured'
    case 'PROVIDER_ERROR': return 'Provider error'
    case 'PROVIDER_TIMEOUT': return 'Provider timed out'
    case 'RATE_LIMITED': return 'Provider rate limit'
    case 'PROVIDER_UNAVAILABLE': return 'Provider unavailable'
    case 'MAX_ATTEMPTS_EXCEEDED': return 'Gave up after retries'
    default: return code
  }
}

/** Today and 30 days ago as `yyyy-MM-dd`, the default selection window. */
export function defaultDateRange(): { from: string; to: string } {
  const today = new Date()
  const from = new Date(today)
  from.setDate(from.getDate() - 30)
  return { from: toDateInput(from), to: toDateInput(today) }
}

export function toDateInput(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * The message to show the user for a failed request.
 *
 * <p>Prefers the RFC-7807 `detail` the backend sent, so the analyst reads the
 * real reason ("No eligible companies in the selection") rather than a generic
 * "something went wrong" or a transport-level fallback.
 */
export function problemDetail(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.problem?.detail ?? error.problem?.title ?? error.message ?? fallback
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}
