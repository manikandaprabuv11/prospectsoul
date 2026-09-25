import { cn } from "@/lib/utils"
import type * as React from "react"

interface ProgressProps extends React.ComponentProps<"div"> {
  value: number
  label: string
}

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
      className={cn("h-2.5 w-full overflow-hidden rounded-full bg-surface-1", className)}
      {...props}
    >
      <div
        data-slot="progress-indicator"
        className="h-full rounded-full bg-brand-gradient transition-[width] duration-500 ease-out"
        style={{ width: `${clamped}%` }}
      />
    </div>
  )
}

export { Progress }
