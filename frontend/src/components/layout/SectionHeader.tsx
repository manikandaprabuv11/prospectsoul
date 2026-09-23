import * as React from "react"
import { cn } from "@/lib/utils"

interface SectionHeaderProps {
  title: React.ReactNode
  description?: React.ReactNode
  actions?: React.ReactNode
  className?: string
}

/** Reusable section header used inside cards and page sections. */
export function SectionHeader({ title, description, actions, className }: SectionHeaderProps) {
  return (
    <div className={cn("flex flex-wrap items-start justify-between gap-2 mb-3", className)}>
      <div className="min-w-0 space-y-0.5">
        <h2 className="text-sm font-semibold tracking-tight leading-none">{title}</h2>
        {description ? (
          <p className="text-xs text-muted-foreground">{description}</p>
        ) : null}
      </div>
      {actions ? <div className="flex items-center gap-1.5 shrink-0">{actions}</div> : null}
    </div>
  )
}
