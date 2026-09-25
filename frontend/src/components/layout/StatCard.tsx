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

export function StatCard({ label, value, hint, icon, accent = "indigo", className }: StatCardProps) {
  return (
    <div
      className={cn(
        "accent-stripe group relative rounded-xl border border-border bg-card p-5 flex items-start justify-between gap-3",
        "shadow-card transition-all duration-200 hover:shadow-card-hover hover:-translate-y-0.5",
        accentClass[accent],
        className,
      )}
    >
      <div className="min-w-0 space-y-1.5">
        <p className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">{label}</p>
        <p data-tabular="true" className="text-2xl font-bold tracking-tight leading-none text-foreground">
          {value}
        </p>
        {hint ? <p className="text-xs text-muted-foreground">{hint}</p> : null}
      </div>
      {icon ? (
        <div className={cn(
          "accent-chip-bg flex size-11 shrink-0 items-center justify-center rounded-xl transition-transform duration-200 group-hover:scale-105",
          accentClass[accent],
        )}>
          {icon}
        </div>
      ) : null}
    </div>
  )
}
