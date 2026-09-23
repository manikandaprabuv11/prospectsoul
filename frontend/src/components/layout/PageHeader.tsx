import * as React from "react"
import { cn } from "@/lib/utils"

interface PageHeaderProps {
  title: React.ReactNode
  description?: React.ReactNode
  breadcrumb?: React.ReactNode
  actions?: React.ReactNode
  className?: string
}

/**
 * Standard page header. Puts a consistent frame around every screen —
 * breadcrumb, title, description on the left, actions on the right.
 * Wraps to a single column on small screens.
 */
export function PageHeader({ title, description, breadcrumb, actions, className }: PageHeaderProps) {
  return (
    <div className={cn("relative flex flex-col gap-3 pb-5", className)}>
      {breadcrumb ? (
        <nav aria-label="Breadcrumb" className="text-xs text-muted-foreground">{breadcrumb}</nav>
      ) : null}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0 space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight leading-tight text-foreground">
            {title}
          </h1>
          {description ? (
            <p className="text-sm text-muted-foreground max-w-2xl">{description}</p>
          ) : null}
        </div>
        {actions ? (
          <div className="flex flex-wrap items-center gap-2 shrink-0">{actions}</div>
        ) : null}
      </div>
      <div aria-hidden="true" className="absolute inset-x-0 bottom-0 h-px bg-gradient-to-r from-transparent via-border to-transparent" />
      <div aria-hidden="true" className="absolute inset-x-0 bottom-0 h-px bg-brand-gradient opacity-40 [mask-image:linear-gradient(90deg,transparent,black_25%,black_75%,transparent)]" />
    </div>
  )
}
