import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { useEffect, useState } from 'react'
import type { IndustryType, NicCodeResponse } from '../types'

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSave: (payload: {
    code: string
    description: string
    industry_type: IndustryType
    is_primary: boolean
    active: boolean
  }) => Promise<void> | void
  initial?: NicCodeResponse | null
  saving?: boolean
  errorMessage?: string
}

export function NicCodeDialog({
  open,
  onOpenChange,
  onSave,
  initial,
  saving,
  errorMessage,
}: Props) {
  const [code, setCode] = useState('')
  const [description, setDescription] = useState('')
  const [industryType, setIndustryType] = useState<IndustryType>('Manufacturing')
  const [isPrimary, setIsPrimary] = useState(false)
  const [active, setActive] = useState(true)

  useEffect(() => {
    if (!open) return
    setCode(initial?.code ?? '')
    setDescription(initial?.description ?? '')
    setIndustryType(initial?.industry_type ?? 'Manufacturing')
    setIsPrimary(initial?.is_primary ?? false)
    setActive(initial?.active ?? true)
  }, [open, initial])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{initial ? 'Edit NIC code' : 'Add NIC code'}</DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="nic-code" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Code</Label>
            <Input
              id="nic-code"
              value={code}
              onChange={(e) => setCode(e.target.value.trim())}
              placeholder="e.g. 28132"
              disabled={!!initial}
              maxLength={5}
            />
            <p className="text-[11px] text-muted-foreground">1–5 digits. Level is derived from length.</p>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="nic-description" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Description</Label>
            <Input
              id="nic-description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="nic-industry" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Industry type</Label>
            <Select
              id="nic-industry"
              value={industryType}
              onChange={(e) => setIndustryType(e.target.value as IndustryType)}
            >
              <option value="Manufacturing">Manufacturing</option>
              <option value="Service">Service</option>
              <option value="Unknown">Unknown</option>
            </Select>
          </div>

          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input
              type="checkbox"
              checked={isPrimary}
              onChange={(e) => setIsPrimary(e.target.checked)}
              className="size-4 rounded accent-primary"
            />
            Primary — surface first in filter pickers
          </label>

          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input
              type="checkbox"
              checked={active}
              onChange={(e) => setActive(e.target.checked)}
              disabled={!initial}
              className="size-4 rounded accent-primary"
            />
            Active
          </label>

          {errorMessage ? (
            <div className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm font-medium text-destructive">
              {errorMessage}
            </div>
          ) : null}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={saving}>
            Cancel
          </Button>
          <Button
            onClick={() =>
              onSave({
                code,
                description,
                industry_type: industryType,
                is_primary: isPrimary,
                active,
              })
            }
            disabled={saving || !code || !description}
          >
            {saving ? 'Saving…' : 'Save'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
