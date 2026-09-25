import { Skeleton } from "@/components/ui/skeleton"

export function LoadingRows({ count = 5, height = "h-12" }: { count?: number; height?: string }) {
  return (
    <div className="space-y-3" role="status" aria-label="Loading">
      {Array.from({ length: count }).map((_, i) => (
        <Skeleton
          key={i}
          className={`w-full rounded-xl ${height}`}
          style={{ animationDelay: `${i * 80}ms` }}
        />
      ))}
    </div>
  )
}
