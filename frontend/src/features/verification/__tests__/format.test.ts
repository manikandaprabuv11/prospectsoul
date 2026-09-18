import { describe, expect, it } from 'vitest'
import { ApiError } from '@/api/client'
import {
  defaultDateRange,
  formatElapsed,
  formatFailureCode,
  formatLineType,
  formatPhone,
  problemDetail,
  toDateInput,
} from '../components/format'

describe('formatElapsed', () => {
  it('renders mm:ss under an hour', () => {
    expect(formatElapsed(0)).toBe('00:00')
    expect(formatElapsed(9)).toBe('00:09')
    expect(formatElapsed(201)).toBe('03:21')
    expect(formatElapsed(3599)).toBe('59:59')
  })

  it('renders h:mm:ss once past an hour', () => {
    expect(formatElapsed(3600)).toBe('1:00:00')
    expect(formatElapsed(7325)).toBe('2:02:05')
  })

  it('renders a dash when the batch has not started', () => {
    expect(formatElapsed(null)).toBe('—')
    expect(formatElapsed(undefined)).toBe('—')
  })
})

describe('formatPhone', () => {
  it('groups a stored 10-digit national number', () => {
    expect(formatPhone('8131281281')).toBe('81312 81281')
  })

  it('groups an E.164 number the provider echoed back', () => {
    expect(formatPhone('+918131281281')).toBe('+91 81312 81281')
  })

  it('leaves an unexpected shape untouched rather than mangling it', () => {
    expect(formatPhone('12345')).toBe('12345')
  })

  it('renders a dash when there is no phone', () => {
    expect(formatPhone(null)).toBe('—')
    expect(formatPhone('')).toBe('—')
  })
})

describe('formatLineType', () => {
  it('reads back the provider values in words', () => {
    expect(formatLineType('mobile')).toBe('Mobile')
    expect(formatLineType('landline')).toBe('Landline')
    expect(formatLineType('nonfixedvoip')).toBe('Non-fixed VoIP')
    expect(formatLineType('tollfree')).toBe('Toll free')
  })

  it('passes an unknown value through instead of hiding it', () => {
    expect(formatLineType('somethingnew')).toBe('somethingnew')
  })

  it('renders a dash when the line type is absent', () => {
    expect(formatLineType(null)).toBe('—')
  })
})

describe('formatFailureCode', () => {
  it('explains every skip reason', () => {
    expect(formatFailureCode('NO_PHONE')).toBe('No usable phone')
    expect(formatFailureCode('ALREADY_VERIFIED')).toBe('Already verified')
    expect(formatFailureCode('FILTER_MISMATCH')).toBe('Outside selected filters')
    expect(formatFailureCode('NOT_ELIGIBLE')).toBe('Not eligible')
  })

  it('explains every provider failure', () => {
    expect(formatFailureCode('INVALID_NUMBER')).toBe('Invalid number')
    expect(formatFailureCode('NON_MOBILE_LINE_TYPE')).toBe('Not a mobile line')
    expect(formatFailureCode('PROVIDER_TIMEOUT')).toBe('Provider timed out')
    expect(formatFailureCode('RATE_LIMITED')).toBe('Provider rate limit')
    expect(formatFailureCode('PROVIDER_AUTH_ERROR')).toBe('Provider authentication failed')
    expect(formatFailureCode('PROVIDER_NOT_CONFIGURED')).toBe('Provider not configured')
    expect(formatFailureCode('MAX_ATTEMPTS_EXCEEDED')).toBe('Gave up after retries')
  })

  it('passes an unrecognised code through rather than swallowing it', () => {
    expect(formatFailureCode('SOMETHING_NEW')).toBe('SOMETHING_NEW')
  })
})

describe('problemDetail', () => {
  it('prefers the RFC-7807 detail so the user sees the real reason', () => {
    const error = new ApiError('fallback message', 422, {
      status: 422,
      title: 'Business Rule Violation',
      detail: 'No eligible companies in the selection',
    })

    expect(problemDetail(error, 'generic')).toBe('No eligible companies in the selection')
  })

  it('falls back to the problem title, then the message', () => {
    expect(problemDetail(new ApiError('msg', 403, { status: 403, title: 'Forbidden' }), 'generic'))
      .toBe('Forbidden')
    expect(problemDetail(new ApiError('transport failed', 0), 'generic')).toBe('transport failed')
  })

  it('uses a plain error message when there is no problem body', () => {
    expect(problemDetail(new Error('boom'), 'generic')).toBe('boom')
  })

  it('uses the caller fallback for a non-error', () => {
    expect(problemDetail('nope', 'generic')).toBe('generic')
    expect(problemDetail(null, 'generic')).toBe('generic')
  })
})

describe('defaultDateRange', () => {
  it('spans the last 30 days, inclusive of today', () => {
    const range = defaultDateRange()
    const today = toDateInput(new Date())

    expect(range.to).toBe(today)
    expect(range.from < range.to).toBe(true)

    const days = (Date.parse(range.to) - Date.parse(range.from)) / 86_400_000
    expect(days).toBe(30)
  })
})

describe('toDateInput', () => {
  it('formats as yyyy-MM-dd in local time, which is what a date input expects', () => {
    expect(toDateInput(new Date(2026, 8, 9))).toBe('2026-09-09')
    expect(toDateInput(new Date(2026, 0, 1))).toBe('2026-01-01')
  })
})
