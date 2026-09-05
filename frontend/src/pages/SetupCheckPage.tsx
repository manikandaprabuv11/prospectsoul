import { apiClient, ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { env } from '@/constants/env'
import { useMutation } from '@tanstack/react-query'
import { CheckCircle2, Loader2, XCircle } from 'lucide-react'

/**
 * Setup verification page. It exists only to prove the Vite + React +
 * TypeScript + Tailwind + Router + TanStack Query foundation is wired
 * correctly, and to check reachability of the backend. It is not part of the
 * ProspectSoul product UI.
 */
export function SetupCheckPage() {
  const check = useMutation({
    mutationKey: ['setup', 'backend-reachability'],
    // Actuator lives outside /api/v1, so this reaches past the API base URL.
    mutationFn: () =>
      apiClient.get<{ status: string }>(new URL('/actuator/health', env.apiBaseUrl).toString()),
  })

  return (
    <main className="mx-auto flex min-h-svh max-w-2xl flex-col justify-center gap-8 p-8">
      <header className="space-y-2">
        <h1 className="text-3xl font-semibold tracking-tight">ProspectSoul — setup check</h1>
        <p className="text-muted-foreground text-sm">
          Development scaffold only. If this page is styled and the button below responds, the
          frontend toolchain is working.
        </p>
      </header>

      <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 rounded-lg border p-4 text-sm">
        <dt className="text-muted-foreground">API base URL</dt>
        <dd className="font-mono">{env.apiBaseUrl}</dd>
        <dt className="text-muted-foreground">Keycloak</dt>
        <dd className="font-mono">
          {env.keycloakUrl} · realm {env.keycloakRealm} · client {env.keycloakClientId}
        </dd>
      </dl>

      <section className="space-y-3">
        <Button onClick={() => check.mutate()} disabled={check.isPending}>
          {check.isPending ? <Loader2 className="animate-spin" /> : null}
          Check backend connectivity
        </Button>

        {check.isSuccess ? (
          <p className="flex items-center gap-2 text-sm text-emerald-600">
            <CheckCircle2 className="size-4" />
            Backend reachable — status {check.data.status}
          </p>
        ) : null}

        {check.isError ? (
          <p className="text-destructive flex items-center gap-2 text-sm">
            <XCircle className="size-4" />
            {check.error instanceof ApiError && check.error.status > 0
              ? `Backend responded with HTTP ${check.error.status}`
              : 'Backend not reachable. Is it running on port 8080?'}
          </p>
        ) : null}
      </section>
    </main>
  )
}
