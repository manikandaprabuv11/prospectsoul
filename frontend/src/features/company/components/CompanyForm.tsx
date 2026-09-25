import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { Building2, Globe, Loader2, MapPin, Phone } from 'lucide-react'
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
    <form onSubmit={handleSubmit} className="space-y-8">
      <FormSection icon={<Building2 className="size-4" />} title="Basic information" accent="teal">
        <div className="grid gap-4 md:grid-cols-2">
          <div className="space-y-1.5">
            <Label htmlFor="canonical_name" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Company Name *</Label>
            <Input
              id="canonical_name"
              required
              value={form.canonical_name}
              onChange={(e) => update('canonical_name', e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="industry" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Industry</Label>
            <Input
              id="industry"
              value={form.industry ?? ''}
              onChange={(e) => update('industry', e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="size_band" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Size Band</Label>
            <Input
              id="size_band"
              value={form.size_band ?? ''}
              onChange={(e) => update('size_band', e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="source" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Source</Label>
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
      </FormSection>

      <FormSection icon={<Phone className="size-4" />} title="Contact details" accent="violet">
        <div className="grid gap-4 md:grid-cols-2">
          <div className="space-y-1.5">
            <Label htmlFor="primary_phone" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Phone</Label>
            <Input
              id="primary_phone"
              value={form.primary_phone ?? ''}
              onChange={(e) => update('primary_phone', e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="email" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Email</Label>
            <Input
              id="email"
              type="email"
              value={form.email ?? ''}
              onChange={(e) => update('email', e.target.value)}
            />
          </div>
        </div>
      </FormSection>

      <FormSection icon={<Globe className="size-4" />} title="Web presence" accent="sky">
        <div className="grid gap-4 md:grid-cols-2">
          <div className="space-y-1.5">
            <Label htmlFor="website_domain" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Website</Label>
            <Input
              id="website_domain"
              value={form.website_domain ?? ''}
              onChange={(e) => update('website_domain', e.target.value)}
              placeholder="example.com"
            />
          </div>
        </div>
      </FormSection>

      <FormSection icon={<MapPin className="size-4" />} title="Location" accent="amber">
        <div className="grid gap-4 md:grid-cols-3">
          <div className="space-y-1.5">
            <Label htmlFor="city" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">City</Label>
            <Input
              id="city"
              value={form.city ?? ''}
              onChange={(e) => update('city', e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="state" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">State</Label>
            <Input
              id="state"
              value={form.state ?? ''}
              onChange={(e) => update('state', e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="cluster" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Cluster</Label>
            <Input
              id="cluster"
              value={form.cluster ?? ''}
              onChange={(e) => update('cluster', e.target.value)}
            />
          </div>
        </div>
      </FormSection>

      <div className="flex gap-3 pt-2">
        <Button type="submit" disabled={isPending} size="lg">
          {isPending && <Loader2 className="animate-spin" />}
          {submitLabel}
        </Button>
      </div>
    </form>
  )
}

function FormSection({ icon, title, accent, children }: {
  icon: React.ReactNode
  title: string
  accent: string
  children: React.ReactNode
}) {
  const accentColors: Record<string, string> = {
    teal: 'bg-accent-teal/12 text-accent-teal',
    violet: 'bg-accent-violet/12 text-accent-violet',
    sky: 'bg-accent-sky/12 text-accent-sky',
    amber: 'bg-accent-amber/12 text-accent-amber',
  }
  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2.5">
        <div className={`flex size-7 items-center justify-center rounded-lg ${accentColors[accent] ?? accentColors.teal}`}>
          {icon}
        </div>
        <h3 className="text-sm font-bold tracking-tight">{title}</h3>
      </div>
      {children}
    </div>
  )
}
