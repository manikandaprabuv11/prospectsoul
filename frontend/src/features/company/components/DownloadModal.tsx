import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { env } from '@/constants/env'
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

/**
 * Companies List download modal (UI/UX Addendum §6). ADR-0005: the word
 * "Export" MUST NOT appear in this flow — the modal makes the download-vs-
 * export distinction visible to the user.
 */
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
      // POST + read-as-blob so the browser triggers the download itself.
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
          <DialogTitle>Download Companies</DialogTitle>
        </DialogHeader>
        <div className="space-y-4 text-sm">
          {matchesCount != null ? (
            <p className="text-muted-foreground">Filter matches: <strong>{matchesCount}</strong> companies</p>
          ) : null}

          <div>
            <label className="text-xs text-muted-foreground">Format</label>
            <div className="flex gap-2 mt-1">
              <Button size="sm" variant={format === 'csv' ? 'default' : 'outline'} onClick={() => setFormat('csv')}>CSV</Button>
              <Button size="sm" variant={format === 'xlsx' ? 'default' : 'outline'} onClick={() => setFormat('xlsx')}>Excel (.xlsx)</Button>
            </div>
          </div>

          <div>
            <label className="text-xs text-muted-foreground">Columns</label>
            <div className="grid grid-cols-3 gap-1 mt-1">
              {AVAILABLE_COLUMNS.map((c) => (
                <label key={c.key} className="flex items-center gap-2 text-xs">
                  <input type="checkbox" checked={columns.has(c.key)} onChange={() => toggle(c.key)} />
                  {c.label}
                </label>
              ))}
            </div>
          </div>

          <label className="flex items-center gap-2">
            <input type="checkbox" checked={allContacts} onChange={(e) => setAllContacts(e.target.checked)} />
            All contacts (one row per contact)
          </label>

          <p className="rounded bg-muted p-2 text-xs text-muted-foreground">
            ⓘ This download does NOT export the pipeline. Companies stay in their current state.
          </p>

          {error ? <p className="text-sm text-destructive">{error}</p> : null}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={downloading}>Cancel</Button>
          <Button onClick={download} disabled={downloading || columns.size === 0}>
            {downloading ? 'Downloading…' : 'Download'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
