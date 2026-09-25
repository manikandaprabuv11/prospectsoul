import { AlertCircle } from "lucide-react"
import * as React from "react"
import { cn } from "@/lib/utils"

interface ErrorStateProps {
  title?: React.ReactNode
  message?: React.ReactNode
  action?: React.ReactNode
  className?: string
}

export function ErrorState({
  title = "Something went wrong",
  message,
  action,
  className,
}: ErrorStateProps) {
  return (
    <div
      role="alert"
      className={cn(
        "rounded-xl border border-destructive/20 bg-destructive/5 p-4 flex items-start gap-3",
        className,
      )}
    >
      <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-destructive/10">
        <AlertCircle className="size-4 text-destructive" aria-hidden="true" />
      </div>
      <div className="min-w-0 flex-1 pt-0.5">
        <p className="text-sm font-semibold text-destructive">{title}</p>
        {message ? <p className="text-sm text-destructive/80 mt-0.5">{message}</p> : null}
        {action ? <div className="mt-3">{action}</div> : null}
      </div>
    </div>
  )
}
