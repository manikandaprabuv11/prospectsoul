import { AuthProvider } from '@/auth/AuthProvider'
import { queryClient } from '@/lib/query-client'
import { router } from '@/routes'
import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router'

export function App() {
  return (
    <AuthProvider>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </AuthProvider>
  )
}
