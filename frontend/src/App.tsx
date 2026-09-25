import { AuthProvider } from '@/auth/AuthProvider'
import { queryClient } from '@/lib/query-client'
import { router } from '@/routes'
import { ThemeContext, useThemeState } from '@/hooks/useTheme'
import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router'

export function App() {
  const themeState = useThemeState()

  return (
    <ThemeContext.Provider value={themeState}>
      <AuthProvider>
        <QueryClientProvider client={queryClient}>
          <RouterProvider router={router} />
        </QueryClientProvider>
      </AuthProvider>
    </ThemeContext.Provider>
  )
}
