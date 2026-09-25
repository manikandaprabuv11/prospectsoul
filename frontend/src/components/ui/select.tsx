import * as React from "react"
import { cn } from "@/lib/utils"
import { ChevronDown } from "lucide-react"

function Select({ className, children, ...props }: React.ComponentProps<"select">) {
  return (
    <div className="relative w-full">
      <select
        data-slot="select"
        className={cn(
          "peer appearance-none flex h-9 w-full rounded-lg border border-input bg-background pl-3 pr-8 py-1.5 text-sm shadow-xs transition-colors duration-200",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40 focus-visible:border-ring",
          "hover:border-border-strong",
          "disabled:cursor-not-allowed disabled:opacity-50 disabled:bg-muted",
          "placeholder:text-muted-foreground/50",
          className,
        )}
        {...props}
      >
        {children}
      </select>
      <ChevronDown
        aria-hidden="true"
        className="pointer-events-none absolute right-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground peer-disabled:opacity-40"
      />
    </div>
  )
}

export { Select }
