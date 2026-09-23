import { Info } from "lucide-react"
import * as React from "react"
import { cn } from "@/lib/utils"

/**
 * Small inline callout for hints and micro-explanations. Uses the info
 * palette so it never competes with real errors.
 */
export function InlineTip({
  children,
  className,
  tone = "info",
}: {
  children: React.ReactNode
  className?: string
  tone?: "info" | "warning" | "success"
}) {
  const styles =
    tone === "warning"
      ? "border-amber-300/60 bg-amber-50 text-amber-800 dark:bg-amber-500/10 dark:text-amber-200"
      : tone === "success"
        ? "border-emerald-300/60 bg-emerald-50 text-emerald-800 dark:bg-emerald-500/10 dark:text-emerald-200"
        : "border-blue-300/60 bg-blue-50 text-blue-800 dark:bg-blue-500/10 dark:text-blue-200"
  return (
    <div
      className={cn(
        "flex items-start gap-2 rounded-md border px-3 py-2 text-xs",
        styles,
        className,
      )}
    >
      <Info className="size-4 shrink-0 mt-0.5" aria-hidden="true" />
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  )
}
