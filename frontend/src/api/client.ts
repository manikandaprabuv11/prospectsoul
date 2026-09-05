import { env } from '@/constants/env'

/**
 * Central HTTP client for the ProspectSoul Spring Boot API.
 *
 * Feature API services are expected to build on top of this module rather than
 * calling `fetch` directly, so that base URL, authentication, and error
 * normalisation stay in one place. No business endpoints live here.
 */

export type Json = string | number | boolean | null | Json[] | { [key: string]: Json }

/** RFC 7807 problem detail, the error shape the backend returns. */
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  [key: string]: Json | undefined
}

/** Normalised transport/HTTP failure raised by every client method. */
export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail | undefined

  constructor(message: string, status: number, problem?: ProblemDetail) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

export interface RequestOptions extends Omit<RequestInit, 'body' | 'method'> {
  /** Query string parameters; `undefined` and `null` values are dropped. */
  query?: Record<string, string | number | boolean | undefined | null>
  /** JSON request payload. Mutually exclusive with `formData`. */
  body?: unknown
  /** Multipart payload; the browser sets the boundary itself. */
  formData?: FormData
}

type TokenProvider = () => string | undefined | Promise<string | undefined>

let tokenProvider: TokenProvider = () => undefined

/**
 * Registers how the client obtains the bearer token. The auth layer calls this
 * during start-up; until then requests are sent unauthenticated.
 */
export function setAuthTokenProvider(provider: TokenProvider): void {
  tokenProvider = provider
}

function buildUrl(path: string, query: RequestOptions['query']): string {
  const url = new URL(
    path.replace(/^\//, ''),
    env.apiBaseUrl.endsWith('/') ? env.apiBaseUrl : `${env.apiBaseUrl}/`,
  )

  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null) {
      url.searchParams.append(key, String(value))
    }
  }

  return url.toString()
}

async function toApiError(response: Response): Promise<ApiError> {
  const fallback = `${response.status} ${response.statusText}`.trim()

  if (response.headers.get('content-type')?.includes('json')) {
    try {
      const problem = (await response.json()) as ProblemDetail
      return new ApiError(problem.detail ?? problem.title ?? fallback, response.status, problem)
    } catch {
      // Body was not parseable JSON; fall through to the status-only error.
    }
  }

  return new ApiError(fallback, response.status)
}

async function parse<T>(response: Response): Promise<T> {
  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T
  }

  if (response.headers.get('content-type')?.includes('json')) {
    return (await response.json()) as T
  }

  return (await response.text()) as T
}

async function request<T>(
  method: string,
  path: string,
  { query, body, formData, headers, ...init }: RequestOptions = {},
): Promise<T> {
  const requestHeaders = new Headers(headers)
  requestHeaders.set('Accept', 'application/json')

  const token = await tokenProvider()
  if (token) {
    requestHeaders.set('Authorization', `Bearer ${token}`)
  }

  let payload: BodyInit | undefined
  if (formData) {
    payload = formData
  } else if (body !== undefined) {
    requestHeaders.set('Content-Type', 'application/json')
    payload = JSON.stringify(body)
  }

  let response: Response
  try {
    response = await fetch(buildUrl(path, query), {
      ...init,
      method,
      headers: requestHeaders,
      body: payload,
    })
  } catch (cause) {
    throw new ApiError(
      cause instanceof Error ? cause.message : 'Network request failed',
      0,
    )
  }

  if (!response.ok) {
    throw await toApiError(response)
  }

  return parse<T>(response)
}

export const apiClient = {
  get: <T>(path: string, options?: RequestOptions) => request<T>('GET', path, options),
  post: <T>(path: string, options?: RequestOptions) => request<T>('POST', path, options),
  put: <T>(path: string, options?: RequestOptions) => request<T>('PUT', path, options),
  patch: <T>(path: string, options?: RequestOptions) => request<T>('PATCH', path, options),
  delete: <T>(path: string, options?: RequestOptions) => request<T>('DELETE', path, options),
}
