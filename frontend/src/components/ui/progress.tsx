import { cn } from "@/lib/utils"
import type * as React from "react"

interface ProgressProps extends React.ComponentProps<"div"> {
  /** Completion from 0 to 100. Values outside the range are clamped. */
  value: number
  /** Accessible name, e.g. "Verification progress". */
  label: string
}

/**
 * Determinate progress bar.
 *
 * <p>Carries the ARIA progressbar role and value so a screen reader announces
 * the percentage; the visible number next to it is not enough on its own.
 */
function Progress({ value, label, className, ...props }: ProgressProps) {
  const clamped = Math.min(100, Math.max(0, Math.round(value)))

  return (
    <div
      data-slot="progress"
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={clamped}
      className={cn("h-3 w-full overflow-hidden rounded-full bg-muted", className)}
      {...props}
    >
      <div
        data-slot="progress-indicator"
        className="h-full rounded-full bg-emerald-600 transition-[width] duration-500 ease-out"
        style={{ width: `${clamped}%` }}
      />
    </div>
  )
}

export { Progress }
