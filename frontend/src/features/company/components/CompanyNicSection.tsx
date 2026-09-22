import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { apiClient } from '@/api/client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
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
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>NIC codes</CardTitle>
        <div className="flex gap-2">
          <input
            className="rounded border px-2 py-1 text-sm w-28"
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
            + Attach code
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {list.isLoading ? (
          <p className="text-sm text-muted-foreground">Loading…</p>
        ) : list.data && list.data.length === 0 ? (
          <p className="text-sm text-muted-foreground">No NIC codes attached.</p>
        ) : (
          <ul className="text-sm space-y-1">
            {list.data?.map((r) => (
              <li key={r.id} className="flex items-center gap-2">
                <span className={r.is_primary ? 'text-amber-500' : 'text-muted-foreground'}>
                  {r.is_primary ? '★' : ' '}
                </span>
                <span className="font-mono text-xs w-14">{r.nic_code_raw}</span>
                <span className="flex-1">
                  {r.resolved_description ?? r.description_raw ?? (
                    <span className="text-muted-foreground italic">not in master</span>
                  )}
                </span>
                {!r.is_primary ? (
                  <Button variant="outline" size="sm" onClick={() => makePrimary.mutate(r.id)}>
                    Make primary
                  </Button>
                ) : null}
                <Button
                  variant="outline"
                  size="sm"
                  className="text-destructive"
                  onClick={() => detach.mutate(r.id)}
                >
                  Detach
                </Button>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}
