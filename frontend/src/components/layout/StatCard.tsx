import * as React from "react"
import { cn } from "@/lib/utils"

type Accent = "indigo" | "teal" | "violet" | "emerald" | "amber" | "rose" | "sky"

interface StatCardProps {
  label: React.ReactNode
  value: React.ReactNode
  hint?: React.ReactNode
  icon?: React.ReactNode
  accent?: Accent
  className?: string
}

const accentClass: Record<Accent, string> = {
  indigo:  "accent-indigo",
  teal:    "accent-teal",
  violet:  "accent-violet",
  emerald: "accent-emerald",
  amber:   "accent-amber",
  rose:    "accent-rose",
  sky:     "accent-sky",
}

/**
 * KPI tile with a coloured accent stripe on the top edge and a matching
 * chip behind the icon. All hues live in `index.css` at the same
 * OKLCH lightness, so a row of tiles feels varied without becoming noisy.
 */
export function StatCard({ label, value, hint, icon, accent = "indigo", className }: StatCardProps) {
  return (
    <div
      className={cn(
        "accent-stripe relative rounded-xl border border-border/70 bg-card shadow-sm p-4 flex items-start justify-between gap-3 transition-all hover:shadow-md hover:-translate-y-0.5",
        accentClass[accent],
        className,
      )}
    >
      <div className="min-w-0 space-y-1">
        <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">{label}</p>
        <p data-tabular="true" className="text-2xl font-bold tracking-tight leading-none text-foreground">
          {value}
        </p>
        {hint ? <p className="text-xs text-muted-foreground">{hint}</p> : null}
      </div>
      {icon ? (
        <div className={cn("accent-chip-bg flex size-10 shrink-0 items-center justify-center rounded-lg", accentClass[accent])}>
          {icon}
        </div>
      ) : null}
    </div>
  )
}
