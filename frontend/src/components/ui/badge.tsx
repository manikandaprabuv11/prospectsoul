import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"

const badgeVariants = cva(
  "inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-medium transition-colors " +
  "focus:outline-none focus-visible:ring-2 focus-visible:ring-ring/50",
  {
    variants: {
      variant: {
        default:     "border border-primary/20 bg-primary/10 text-primary",
        secondary:   "border-transparent bg-secondary text-secondary-foreground",
        outline:     "border-border text-foreground bg-background",
        success:     "border border-emerald-200/60 bg-emerald-50 text-emerald-700 dark:border-emerald-500/25 dark:bg-emerald-500/15 dark:text-emerald-300",
        warning:     "border border-amber-200/60 bg-amber-50 text-amber-800 dark:border-amber-500/25 dark:bg-amber-500/15 dark:text-amber-200",
        destructive: "border border-red-200/60 bg-red-50 text-red-700 dark:border-red-500/25 dark:bg-red-500/15 dark:text-red-300",
        info:        "border border-blue-200/60 bg-blue-50 text-blue-700 dark:border-blue-500/25 dark:bg-blue-500/15 dark:text-blue-300",
        neutral:     "border border-slate-200 bg-slate-100 text-slate-700 dark:border-slate-500/25 dark:bg-slate-500/20 dark:text-slate-200",
      },
      size: {
        default: "text-xs px-2 py-0.5",
        sm:      "text-[10px] px-1.5 py-0.5",
        lg:      "text-sm px-2.5 py-1",
      },
    },
    defaultVariants: { variant: "default", size: "default" },
  }
)

function Badge({
  className, variant, size, ...props
}: React.ComponentProps<"div"> & VariantProps<typeof badgeVariants>) {
  return <div className={cn(badgeVariants({ variant, size }), className)} {...props} />
}

export { Badge, badgeVariants }
