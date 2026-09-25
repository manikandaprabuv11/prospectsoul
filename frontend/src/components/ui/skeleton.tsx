import { cn } from "@/lib/utils"

function Skeleton({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="skeleton"
      className={cn(
        "rounded-xl bg-gradient-to-r from-surface-1 via-surface-2 to-surface-1 bg-[length:200%_100%] animate-shimmer",
        className,
      )}
      {...props}
    />
  )
}

export { Skeleton }
