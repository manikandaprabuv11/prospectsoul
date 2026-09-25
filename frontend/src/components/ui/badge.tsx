import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"

const badgeVariants = cva(
  "inline-flex items-center gap-1 rounded-md border px-2 py-0.5 text-xs font-semibold transition-colors " +
  "focus:outline-none focus-visible:ring-2 focus-visible:ring-ring/50",
  {
    variants: {
      variant: {
        default:     "border-primary/20 bg-primary/10 text-primary",
        secondary:   "border-transparent bg-secondary text-secondary-foreground",
        outline:     "border-border text-foreground bg-background",
        success:     "border-accent-emerald/20 bg-accent-emerald/10 text-accent-emerald",
        warning:     "border-accent-amber/20 bg-accent-amber/10 text-accent-amber",
        destructive: "border-accent-rose/20 bg-accent-rose/10 text-accent-rose",
        info:        "border-accent-sky/20 bg-accent-sky/10 text-accent-sky",
        neutral:     "border-border bg-surface-1 text-muted-foreground",
      },
      size: {
        default: "text-[11px] px-2 py-0.5",
        sm:      "text-[10px] px-1.5 py-0.5",
        lg:      "text-xs px-2.5 py-1",
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
