import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { apiClient } from '@/api/client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ListTree, Star, Trash2 } from 'lucide-react'
import { useState } from 'react'

interface Row {
  id: string
  company_id: string
  nic_code_id: string | null
  nic_code_raw: string
  resolved_code: string | null
  resolved_description: string | null
  description_raw: string | null
  is_primary: boolean
  sequence_no: number
  created_at: string
}

const KEY = (id: string) => ['company', id, 'nic-codes'] as const

export function CompanyNicSection({ companyId }: { companyId: string }) {
  const qc = useQueryClient()
  const list = useQuery({
    queryKey: KEY(companyId),
    queryFn: () => apiClient.get<Row[]>(`/companies/${companyId}/nic-codes`),
    enabled: !!companyId,
  })
  const attach = useMutation({
    mutationFn: (body: { code: string }) =>
      apiClient.post<Row>(`/companies/${companyId}/nic-codes`, { body }),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY(companyId) }),
  })
  const makePrimary = useMutation({
    mutationFn: (rowId: string) =>
      apiClient.post<Row>(`/companies/${companyId}/nic-codes/${rowId}/make-primary`),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY(companyId) }),
  })
  const detach = useMutation({
    mutationFn: (rowId: string) =>
      apiClient.delete(`/companies/${companyId}/nic-codes/${rowId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY(companyId) }),
  })

  const [code, setCode] = useState('')
  return (
    <div className="rounded-xl border border-border bg-card shadow-card">
      <div className="flex items-center justify-between border-b border-border px-5 py-4">
        <div className="flex items-center gap-2.5">
          <div className="flex size-8 items-center justify-center rounded-lg bg-accent-sky/12 text-accent-sky">
            <ListTree className="size-4" />
          </div>
          <h3 className="text-sm font-bold tracking-tight">NIC codes</h3>
        </div>
        <div className="flex gap-2">
          <Input
            className="w-28"
            placeholder="e.g. 28132"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            maxLength={5}
          />
          <Button
            size="sm"
            variant="outline"
            disabled={!code || attach.isPending}
            onClick={() => {
              attach.mutate({ code })
              setCode('')
            }}
          >
            Attach
          </Button>
        </div>
      </div>
      <div className="px-5 py-4">
        {list.isLoading ? (
          <div className="space-y-2">
            {[0, 1].map((i) => <div key={i} className="h-8 animate-pulse rounded-lg bg-surface-1" />)}
          </div>
        ) : list.data && list.data.length === 0 ? (
          <div className="rounded-xl border border-dashed border-border bg-surface-1/50 py-6 text-center text-sm text-muted-foreground">
            No NIC codes attached.
          </div>
        ) : (
          <ul className="space-y-1.5">
            {list.data?.map((r) => (
              <li key={r.id} className="group flex items-center gap-3 rounded-lg px-2 py-1.5 hover:bg-surface-1 transition-colors">
                <Star className={`size-3.5 shrink-0 ${r.is_primary ? 'text-accent-amber fill-accent-amber' : 'text-muted-foreground/30'}`} />
                <span className="font-mono text-xs text-muted-foreground w-14 tabular-nums">{r.nic_code_raw}</span>
                <span className="flex-1 text-sm">
                  {r.resolved_description ?? r.description_raw ?? (
                    <span className="italic text-muted-foreground">not in master</span>
                  )}
                </span>
                <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  {!r.is_primary ? (
                    <Button variant="ghost" size="xs" onClick={() => makePrimary.mutate(r.id)}>
                      <Star className="size-3" /> Primary
                    </Button>
                  ) : null}
                  <Button
                    variant="ghost"
                    size="xs"
                    className="text-accent-rose hover:text-accent-rose"
                    onClick={() => detach.mutate(r.id)}
                  >
                    <Trash2 className="size-3" />
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
