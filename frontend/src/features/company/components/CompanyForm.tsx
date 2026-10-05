import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { Building2, Globe, Loader2, MapPin, Phone, Plus, Star, Trash2 } from 'lucide-react'
import { useState } from 'react'
import type { Company, CompanyCreateRequest, CompanyPhoneRequest, ConfidenceLevel, NumberSourceType } from '../types'

interface Props {
  initialData?: Company
  onSubmit: (data: CompanyCreateRequest) => void
  isPending: boolean
  submitLabel: string
}

const SOURCES = ['MANUAL_ENTRY', 'EXCEL_CSV', 'INDIAMART', 'TRADEINDIA', 'LINKEDIN']
const NUMBER_SOURCES: NumberSourceType[] = [
  'MANUAL_ENTRY', 'BUSINESS_CARD', 'FIELD_VISIT', 'REFERENCE',
  'WEBSITE', 'GOOGLE_API', 'LINKEDIN', 'INDIAMART', 'IMPORT_DEFAULT',
]
const CONFIDENCE_LEVELS: ConfidenceLevel[] = ['HIGH', 'MEDIUM', 'LOW']

const GST_REGEX = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/

function emptyPhone(isPrimary: boolean): CompanyPhoneRequest {
  return { number_raw: '', number_source: 'MANUAL_ENTRY', confidence: 'MEDIUM', is_primary: isPrimary }
}

export function CompanyForm({ initialData, onSubmit, isPending, submitLabel }: Props) {
  const [form, setForm] = useState<CompanyCreateRequest>({
    canonical_name: initialData?.canonical_name ?? '',
    email: initialData?.email ?? '',
    website_domain: initialData?.website_domain ?? '',
    city: initialData?.city ?? '',
    state: initialData?.state ?? '',
    cluster: initialData?.cluster ?? '',
    industry: initialData?.industry ?? '',
    size_band: initialData?.size_band ?? '',
    source: initialData?.source ?? 'MANUAL_ENTRY',
    gst_number: initialData?.gst_number ?? '',
  })

  const [phones, setPhones] = useState<CompanyPhoneRequest[]>(() => {
    if (initialData?.phones && initialData.phones.length > 0) {
      return initialData.phones.map((p) => ({
        id: p.id,
        number_raw: p.number_raw,
        number_source: p.number_source,
        confidence: p.confidence,
        designation_override: p.designation_override,
        is_primary: p.is_primary,
      }))
    }
    return [emptyPhone(true)]
  })

  const [gstError, setGstError] = useState<string | null>(null)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (form.gst_number && form.gst_number.trim()) {
      const upper = form.gst_number.trim().toUpperCase()
      if (!GST_REGEX.test(upper)) {
        setGstError('Invalid GSTIN format')
        return
      }
    }
    setGstError(null)
    const validPhones = phones.filter((p) => p.number_raw.trim() !== '')
    onSubmit({ ...form, phones: validPhones.length > 0 ? validPhones : undefined })
  }

  const update = (field: keyof CompanyCreateRequest, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }))
    if (field === 'gst_number') setGstError(null)
  }

  const addPhone = () => setPhones((prev) => [...prev, emptyPhone(prev.length === 0)])
  const removePhone = (idx: number) => {
    setPhones((prev) => {
      const next = prev.filter((_, i) => i !== idx)
      if (next.length > 0 && !next.some((p) => p.is_primary)) {
        next[0] = { ...next[0], is_primary: true }
      }
      return next
    })
  }
  const updatePhone = (idx: number, field: keyof CompanyPhoneRequest, value: unknown) => {
    setPhones((prev) => prev.map((p, i) => i === idx ? { ...p, [field]: value } : p))
  }
  const setPrimary = (idx: number) => {
    setPhones((prev) => prev.map((p, i) => ({ ...p, is_primary: i === idx })))
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
          <div className="space-y-1.5">
            <Label htmlFor="gst_number" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">GST Number</Label>
            <Input
              id="gst_number"
              value={form.gst_number ?? ''}
              onChange={(e) => update('gst_number', e.target.value.toUpperCase())}
              placeholder="22AAAAA0000A1Z5"
              maxLength={15}
            />
            {gstError && <p className="text-xs text-destructive">{gstError}</p>}
          </div>
        </div>
      </FormSection>

      <FormSection icon={<Phone className="size-4" />} title="Phone numbers & contact" accent="violet">
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <p className="text-xs text-muted-foreground">
              Each phone has its own source, confidence and designation. Mark one as primary.
            </p>
            <Button type="button" variant="outline" size="sm" onClick={addPhone}>
              <Plus className="mr-1 size-3.5" /> Add Phone
            </Button>
          </div>
          {phones.map((phone, idx) => (
            <div key={idx} className={`rounded-lg border p-3 ${phone.is_primary ? 'border-primary/40 bg-primary/5' : 'bg-muted/30'}`}>
              <div className="grid gap-3 md:grid-cols-6">
                <div className="space-y-1 md:col-span-2">
                  <Label className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                    Phone Number {phone.is_primary && <Star className="inline size-3 fill-amber-400 text-amber-400 ml-0.5" />}
                  </Label>
                  <Input
                    value={phone.number_raw}
                    onChange={(e) => updatePhone(idx, 'number_raw', e.target.value)}
                    placeholder="9843012345"
                  />
                </div>
                <div className="space-y-1">
                  <Label className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">Source</Label>
                  <Select
                    value={phone.number_source}
                    onChange={(e) => updatePhone(idx, 'number_source', e.target.value)}
                  >
                    {NUMBER_SOURCES.map((s) => (
                      <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>
                    ))}
                  </Select>
                </div>
                <div className="space-y-1">
                  <Label className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">Confidence</Label>
                  <Select
                    value={phone.confidence ?? 'MEDIUM'}
                    onChange={(e) => updatePhone(idx, 'confidence', e.target.value)}
                  >
                    {CONFIDENCE_LEVELS.map((c) => (
                      <option key={c} value={c}>{c}</option>
                    ))}
                  </Select>
                </div>
                <div className="space-y-1">
                  <Label className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">Designation</Label>
                  <Input
                    value={phone.designation_override ?? ''}
                    onChange={(e) => updatePhone(idx, 'designation_override', e.target.value)}
                    placeholder="MD, CEO..."
                  />
                </div>
                <div className="flex items-end gap-1.5">
                  <Button
                    type="button"
                    variant={phone.is_primary ? 'default' : 'outline'}
                    size="sm"
                    className="text-xs flex-1"
                    onClick={() => setPrimary(idx)}
                  >
                    {phone.is_primary ? '★ Primary' : 'Set Primary'}
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="text-destructive hover:bg-destructive/10"
                    onClick={() => removePhone(idx)}
                    disabled={phones.length === 1}
                  >
                    <Trash2 className="size-4" />
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="mt-4 grid gap-4 md:grid-cols-2">
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
