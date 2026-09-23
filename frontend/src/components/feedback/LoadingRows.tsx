import { Skeleton } from "@/components/ui/skeleton"

/** Sensible default skeleton for row-shaped lists. */
export function LoadingRows({ count = 5, height = "h-12" }: { count?: number; height?: string }) {
  return (
    <div className="space-y-2" role="status" aria-label="Loading">
      {Array.from({ length: count }).map((_, i) => (
        <Skeleton key={i} className={`w-full ${height}`} />
      ))}
    </div>
  )
}
