import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { useState } from 'react'

interface PaginationProps {
  /** Zero-indexed current page, matching the backend envelope's `page` field. */
  page: number
  totalPages: number
  totalElements?: number
  /** Noun used in the summary text, e.g. "companies", "jobs". Defaults to "items". */
  itemLabel?: string
  onPageChange: (page: number) => void
  className?: string
}

/**
 * Shared pagination bar: Previous / Next, numbered page buttons (windowed
 * with ellipses once there are many pages) and a jump-to-page input for
 * long lists. Every list table in the app should use this instead of
 * hand-rolling its own Previous/Next pair, so page navigation looks and
 * behaves the same everywhere.
 */
export function Pagination({ page, totalPages, totalElements, itemLabel = 'items', onPageChange, className }: PaginationProps) {
  const [jumpValue, setJumpValue] = useState('')
  const clampedTotalPages = Math.max(1, totalPages)

  if (clampedTotalPages <= 1) return null

  function goTo(target: number) {
    const next = Math.min(Math.max(target, 0), clampedTotalPages - 1)
    if (next !== page) onPageChange(next)
  }

  function submitJump() {
    const parsed = Number.parseInt(jumpValue, 10)
    if (Number.isFinite(parsed) && parsed >= 1 && parsed <= clampedTotalPages) {
      goTo(parsed - 1)
    }
    setJumpValue('')
  }

  const pageNumbers = buildPageWindow(page, clampedTotalPages)
  const showJump = clampedTotalPages > 7

  return (
    <div className={`flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between text-sm ${className ?? ''}`}>
      <p className="text-muted-foreground" data-tabular="true">
        Page <span className="font-medium text-foreground">{page + 1}</span> of{' '}
        <span className="font-medium text-foreground">{clampedTotalPages}</span>
        {typeof totalElements === 'number' && (
          <>
            {' · '}
            <span className="font-medium text-foreground">{totalElements.toLocaleString()}</span> {itemLabel}
          </>
        )}
      </p>

      <nav aria-label="Pagination" className="flex flex-wrap items-center gap-1.5">
        <Button
          variant="outline"
          size="sm"
          disabled={page === 0}
          onClick={() => goTo(page - 1)}
        >
          <ChevronLeft className="size-4" />
          Previous
        </Button>

        <div className="flex items-center gap-1">
          {pageNumbers.map((entry, index) =>
            entry === 'ellipsis' ? (
              <span key={`ellipsis-${index}`} className="px-1.5 text-muted-foreground select-none">
                …
              </span>
            ) : (
              <Button
                key={entry}
                variant={entry === page ? 'default' : 'outline'}
                size="icon-sm"
                onClick={() => goTo(entry)}
                aria-label={`Page ${entry + 1}`}
                aria-current={entry === page ? 'page' : undefined}
              >
                {entry + 1}
              </Button>
            ),
          )}
        </div>

        <Button
          variant="outline"
          size="sm"
          disabled={page >= clampedTotalPages - 1}
          onClick={() => goTo(page + 1)}
        >
          Next
          <ChevronRight className="size-4" />
        </Button>

        {showJump && (
          <form
            className="flex items-center gap-1.5 pl-1"
            onSubmit={(event) => {
              event.preventDefault()
              submitJump()
            }}
          >
            <label htmlFor="pagination-jump" className="text-xs text-muted-foreground">
              Go to
            </label>
            <Input
              id="pagination-jump"
              type="number"
              min={1}
              max={clampedTotalPages}
              value={jumpValue}
              onChange={(event) => setJumpValue(event.target.value)}
              className="h-8 w-16 px-2 text-center"
              aria-label="Jump to page"
            />
          </form>
        )}
      </nav>
    </div>
  )
}

/**
 * Windows the page numbers around the current page (2 either side) plus the
 * first and last page, collapsing gaps into an ellipsis marker. Keeps the
 * control usable at 200+ pages instead of rendering every button.
 */
function buildPageWindow(current: number, totalPages: number): (number | 'ellipsis')[] {
  const window = 2
  const pages = new Set<number>()
  pages.add(0)
  pages.add(totalPages - 1)
  for (let i = current - window; i <= current + window; i++) {
    if (i >= 0 && i < totalPages) pages.add(i)
  }
  const sorted = [...pages].sort((a, b) => a - b)
  const result: (number | 'ellipsis')[] = []
  for (let i = 0; i < sorted.length; i++) {
    const value = sorted[i]
    if (value === undefined) continue
    if (i > 0) {
      const prev = sorted[i - 1]
      if (prev !== undefined && value - prev > 1) result.push('ellipsis')
    }
    result.push(value)
  }
  return result
}
