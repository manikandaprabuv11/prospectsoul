import { useState } from 'react'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Sparkles, Loader2 } from 'lucide-react'
import { useEnrichCompany } from '../hooks'

interface EnrichButtonProps {
  companyId: string
  lastGoogleEnrich?: string | null
  lastWebsiteEnrich?: string | null
  lastPhoneEnrich?: string | null
}

const PROVIDERS = [
  { key: 'GOOGLE_PLACES', label: 'Google Places' },
  { key: 'WEBSITE', label: 'Website (Firecrawl)' },
  { key: 'PHONE', label: 'Phone (all)' },
] as const

function formatLastRun(date?: string | null): string {
  if (!date) return 'never'
  const d = new Date(date)
  const diffMs = Date.now() - d.getTime()
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  if (diffHours < 1) return 'just now'
  if (diffHours < 24) return `${diffHours}h ago`
  const diffDays = Math.floor(diffHours / 24)
  return `${diffDays}d ago`
}

export function EnrichButton({ companyId, lastGoogleEnrich, lastWebsiteEnrich, lastPhoneEnrich }: EnrichButtonProps) {
  const enrichMutation = useEnrichCompany(companyId)
  const [open, setOpen] = useState(false)

  const lastEnrichDates: Record<string, string | null | undefined> = {
    GOOGLE_PLACES: lastGoogleEnrich,
    WEBSITE: lastWebsiteEnrich,
    PHONE: lastPhoneEnrich,
  }

  function handleEnrich(providerKeys: string[]) {
    setOpen(false)
    enrichMutation.mutate({ provider_keys: providerKeys, force: false })
  }

  return (
    <DropdownMenu open={open} onOpenChange={setOpen}>
      <DropdownMenuTrigger asChild>
        <Button variant="outline" disabled={enrichMutation.isPending}>
          {enrichMutation.isPending ? <Loader2 className="animate-spin" /> : <Sparkles />}
          Enrich
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-64">
        {PROVIDERS.map((p) => (
          <DropdownMenuItem key={p.key} onClick={() => handleEnrich([p.key])}>
            <div className="flex w-full justify-between items-center">
              <span>{p.label}</span>
              <span className="text-xs text-muted-foreground">{formatLastRun(lastEnrichDates[p.key])}</span>
            </div>
          </DropdownMenuItem>
        ))}
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={() => handleEnrich(PROVIDERS.map((p) => p.key))}>
          <div className="flex w-full justify-between items-center">
            <span className="font-medium">Run all</span>
          </div>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
