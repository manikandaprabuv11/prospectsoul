import { ApiError } from '@/api/client'
import { QueryClient } from '@tanstack/react-query'

/** Shared TanStack Query client. Feature queries are added in later work. */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        // Client-side failures (4xx) will not succeed on a retry.
        if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
          return false
        }
        return failureCount < 2
      },
    },
  },
})
