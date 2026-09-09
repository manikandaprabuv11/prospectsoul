import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { Loader2 } from 'lucide-react'
import { useState } from 'react'
import type { Company, CompanyCreateRequest } from '../types'

interface Props {
  initialData?: Company
  onSubmit: (data: CompanyCreateRequest) => void
  isPending: boolean
  submitLabel: string
}

const SOURCES = ['MANUAL_ENTRY', 'EXCEL_CSV', 'INDIAMART', 'TRADEINDIA', 'LINKEDIN']

export function CompanyForm({ initialData, onSubmit, isPending, submitLabel }: Props) {
  const [form, setForm] = useState<CompanyCreateRequest>({
    canonical_name: initialData?.canonical_name ?? '',
    primary_phone: initialData?.primary_phone_normalized ?? '',
    email: initialData?.email ?? '',
    website_domain: initialData?.website_domain ?? '',
    city: initialData?.city ?? '',
    state: initialData?.state ?? '',
    cluster: initialData?.cluster ?? '',
    industry: initialData?.industry ?? '',
    size_band: initialData?.size_band ?? '',
    source: initialData?.source ?? 'MANUAL_ENTRY',
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSubmit(form)
  }

  const update = (field: keyof CompanyCreateRequest, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="canonical_name">Company Name *</Label>
          <Input
            id="canonical_name"
            required
            value={form.canonical_name}
            onChange={(e) => update('canonical_name', e.target.value)}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="primary_phone">Phone</Label>
          <Input
            id="primary_phone"
            value={form.primary_phone ?? ''}
            onChange={(e) => update('primary_phone', e.target.value)}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="email">Email</Label>
          <Input
            id="email"
            type="email"
            value={form.email ?? ''}
            onChange={(e) => update('email', e.target.value)}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="website_domain">Website</Label>
          <Input
            id="website_domain"
            value={form.website_domain ?? ''}
            onChange={(e) => update('website_domain', e.target.value)}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="city">City</Label>
          <Input
            id="city"
            value={form.city ?? ''}
            onChange={(e) => update('city', e.target.value)}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="state">State</Label>
          <Input
            id="state"
            value={form.state ?? ''}
            onChange={(e) => update('state', e.target.value)}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="industry">Industry</Label>
          <Input
            id="industry"
            value={form.industry ?? ''}
            onChange={(e) => update('industry', e.target.value)}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="cluster">Cluster</Label>
          <Input
            id="cluster"
            value={form.cluster ?? ''}
            onChange={(e) => update('cluster', e.target.value)}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="size_band">Size Band</Label>
          <Input
            id="size_band"
            value={form.size_band ?? ''}
            onChange={(e) => update('size_band', e.target.value)}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="source">Source</Label>
          <Select
            id="source"
            value={form.source ?? 'MANUAL_ENTRY'}
            onChange={(e) => update('source', e.target.value)}
          >
            {SOURCES.map((s) => (
              <option key={s} value={s}>{s.replace('_', ' ')}</option>
            ))}
          </Select>
        </div>
      </div>
      <div className="flex gap-3">
        <Button type="submit" disabled={isPending}>
          {isPending && <Loader2 className="animate-spin" />}
          {submitLabel}
        </Button>
      </div>
    </form>
  )
}
