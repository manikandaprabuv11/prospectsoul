import { Info, AlertTriangle, CheckCircle2 } from "lucide-react"
import * as React from "react"
import { cn } from "@/lib/utils"

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
      ? "border-accent-amber/30 bg-accent-amber/5 text-foreground"
      : tone === "success"
        ? "border-accent-emerald/30 bg-accent-emerald/5 text-foreground"
        : "border-accent-sky/30 bg-accent-sky/5 text-foreground"
  const Icon =
    tone === "warning" ? AlertTriangle
      : tone === "success" ? CheckCircle2
        : Info
  const iconColor =
    tone === "warning" ? "text-accent-amber"
      : tone === "success" ? "text-accent-emerald"
        : "text-accent-sky"
  return (
    <div
      className={cn(
        "flex items-start gap-2.5 rounded-xl border px-4 py-3 text-sm leading-relaxed",
        styles,
        className,
      )}
    >
      <Icon className={cn("size-4 shrink-0 mt-0.5", iconColor)} aria-hidden="true" />
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  )
}
