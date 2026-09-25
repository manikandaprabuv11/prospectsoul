import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { env } from '@/constants/env'
import { Download, Info, Loader2 } from 'lucide-react'
import { useState } from 'react'
import type { CompanyFilters } from '../types'

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  filters: CompanyFilters
  matchesCount?: number
  authHeader?: string
}

const AVAILABLE_COLUMNS: { key: string; label: string; def: boolean }[] = [
  { key: 'canonical_name',           label: 'Name',                def: true },
  { key: 'primary_phone_normalized', label: 'Phone',               def: true },
  { key: 'email',                    label: 'Email',               def: true },
  { key: 'website_domain',           label: 'Website',             def: true },
  { key: 'city',                     label: 'City',                def: true },
  { key: 'state',                    label: 'State',               def: true },
  { key: 'district',                 label: 'District',            def: false },
  { key: 'region',                   label: 'Region',              def: true },
  { key: 'pincode',                  label: 'Pincode',             def: true },
  { key: 'primary_nic_code',         label: 'Primary NIC',         def: true },
  { key: 'turnover',                 label: 'Turnover',            def: false },
  { key: 'employee_count',           label: 'Employees',           def: false },
  { key: 'gst_number',               label: 'GST',                 def: false },
  { key: 'pipeline_state',           label: 'Pipeline',            def: true },
  { key: 'verification_status',      label: 'Verification',        def: false },
  { key: 'primary_contact_name',     label: 'Primary contact name', def: true },
  { key: 'primary_contact_role',     label: 'Primary contact role', def: true },
]

export function DownloadModal({ open, onOpenChange, filters, matchesCount, authHeader }: Props) {
  const [format, setFormat] = useState<'csv' | 'xlsx'>('xlsx')
  const [columns, setColumns] = useState<Set<string>>(
    new Set(AVAILABLE_COLUMNS.filter((c) => c.def).map((c) => c.key)),
  )
  const [allContacts, setAllContacts] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [downloading, setDownloading] = useState(false)

  function toggle(k: string) {
    setColumns((cur) => {
      const next = new Set(cur)
      if (next.has(k)) next.delete(k)
      else next.add(k)
      return next
    })
  }

  async function download() {
    setError(null)
    setDownloading(true)
    try {
      const resp = await fetch(`${env.apiBaseUrl}/companies/download`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'content-type': 'application/json',
          ...(authHeader ? { authorization: authHeader } : {}),
        },
        body: JSON.stringify({
          format,
          columns: Array.from(columns),
          include_all_contacts: allContacts,
          filter: filters,
        }),
      })
      if (!resp.ok) {
        const problem = await resp.json().catch(() => ({}))
        setError(problem.detail ?? `Download failed (${resp.status})`)
        return
      }
      const blob = await resp.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = format === 'csv' ? 'companies.csv' : 'companies.xlsx'
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
      onOpenChange(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Download failed')
    } finally {
      setDownloading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Download className="size-5 text-primary" />
            Download Companies
          </DialogTitle>
        </DialogHeader>
        <div className="space-y-5 text-sm">
          {matchesCount != null ? (
            <p className="text-muted-foreground">
              Filter matches: <span className="font-semibold text-foreground tabular-nums">{matchesCount}</span> companies
            </p>
          ) : null}

          <div className="space-y-2">
            <label className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Format</label>
            <div className="flex gap-2">
              <Button
                size="sm"
                variant={format === 'csv' ? 'default' : 'outline'}
                onClick={() => setFormat('csv')}
              >
                CSV
              </Button>
              <Button
                size="sm"
                variant={format === 'xlsx' ? 'default' : 'outline'}
                onClick={() => setFormat('xlsx')}
              >
                Excel (.xlsx)
              </Button>
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Columns</label>
            <div className="grid grid-cols-3 gap-x-3 gap-y-1.5">
              {AVAILABLE_COLUMNS.map((c) => (
                <label key={c.key} className="flex items-center gap-2 text-xs cursor-pointer">
                  <input
                    type="checkbox"
                    checked={columns.has(c.key)}
                    onChange={() => toggle(c.key)}
                    className="size-3.5 rounded accent-primary"
                  />
                  {c.label}
                </label>
              ))}
            </div>
          </div>

          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={allContacts}
              onChange={(e) => setAllContacts(e.target.checked)}
              className="size-4 rounded accent-primary"
            />
            <span className="text-sm">All contacts (one row per contact)</span>
          </label>

          <div className="flex items-start gap-2.5 rounded-xl border border-accent-sky/30 bg-accent-sky/5 px-4 py-3 text-sm leading-relaxed">
            <Info className="size-4 shrink-0 mt-0.5 text-accent-sky" />
            <span>This download does NOT export the pipeline. Companies stay in their current state.</span>
          </div>

          {error ? (
            <div className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm font-medium text-destructive">
              {error}
            </div>
          ) : null}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={downloading}>Cancel</Button>
          <Button onClick={download} disabled={downloading || columns.size === 0}>
            {downloading ? <><Loader2 className="size-4 animate-spin" /> Downloading…</> : 'Download'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
