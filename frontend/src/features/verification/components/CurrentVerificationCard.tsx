import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Skeleton } from '@/components/ui/skeleton'
import { ShieldCheck } from 'lucide-react'
import { Link } from 'react-router'
import type { VerificationBatch } from '../types'
import { isBatchRunning } from '../types'
import { formatDateTime, formatElapsed, formatPhone, problemDetail } from './format'
import { BatchStatusBadge } from './VerificationStatusBadge'

interface Props {
  batch: VerificationBatch | null | undefined
  isLoading: boolean
  error: unknown
}

/**
 * The "Current Verification" card.
 *
 * <p>Renders whatever `GET /api/v1/verifications/active` returned. It holds no
 * progress state of its own, which is exactly why the card reconstructs itself
 * after a route change, a browser refresh or a backend restart.
 */
export function CurrentVerificationCard({ batch, isLoading, error }: Props) {
  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Current Verification</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <Skeleton className="h-4 w-48" />
          <Skeleton className="h-3 w-full" />
          <Skeleton className="h-4 w-64" />
        </CardContent>
      </Card>
    )
  }

  if (error) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Current Verification</CardTitle>
        </CardHeader>
        <CardContent>
          <p role="alert" className="text-sm text-destructive">
            {problemDetail(error, 'Could not load the current verification.')}
          </p>
        </CardContent>
      </Card>
    )
  }

  if (!batch) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Current Verification</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col items-start gap-1 py-2">
            <div className="flex items-center gap-2 text-sm font-medium">
              <ShieldCheck className="size-4 text-muted-foreground" />
              No verification is running
            </div>
            <p className="text-sm text-muted-foreground">
              Pick unverified companies in <strong>Verify New Companies</strong> above and start a
              batch. Verification runs in the background, so you can leave this page.
            </p>
          </div>
        </CardContent>
      </Card>
    )
  }

  const processed = batch.verified_count + batch.failed_count + batch.skipped_count
  const running = isBatchRunning(batch)

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between gap-3 pb-3">
        <CardTitle>Current Verification</CardTitle>
        <BatchStatusBadge status={batch.status} />
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <div className="flex items-baseline justify-between">
            <p className="text-sm font-medium">
              {processed} / {batch.total_count} companies processed
            </p>
            <p className="text-sm font-semibold tabular-nums">{batch.progress_percent}%</p>
          </div>
          <Progress value={batch.progress_percent} label="Verification progress" />
        </div>

        <dl className="grid grid-cols-2 gap-3 sm:grid-cols-5">
          <Counter label="Verified" value={batch.verified_count} className="text-emerald-600" />
          <Counter label="Processing" value={batch.processing_count} className="text-amber-600" />
          <Counter label="Queued" value={batch.queued_count} />
          <Counter label="Failed" value={batch.failed_count} className="text-red-600" />
          <Counter label="Skipped" value={batch.skipped_count} />
        </dl>

        {batch.current_item ? (
          <div className="rounded-lg border bg-muted/30 p-3">
            <p className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
              Currently verifying
            </p>
            <p className="mt-1 text-sm font-medium">
              <Link to={`/companies/${batch.current_item.company_id}`} className="text-primary hover:underline">
                {batch.current_item.company_name ?? batch.current_item.company_id}
              </Link>
            </p>
            <p className="text-sm text-muted-foreground">
              {formatPhone(batch.current_item.phone_number)}
              {batch.current_item.provider ? ` · ${batch.current_item.provider} lookup` : ''}
            </p>
          </div>
        ) : running ? (
          <p className="text-sm text-muted-foreground">
            Waiting for the verification worker to pick up the next company.
          </p>
        ) : null}

        <div className="flex flex-wrap gap-x-6 gap-y-1 text-sm text-muted-foreground">
          <span>Started {formatDateTime(batch.started_at ?? batch.created_at)}</span>
          <span>
            {running ? 'Elapsed' : 'Took'} {formatElapsed(batch.elapsed_seconds)}
          </span>
          <span>Requested by {batch.requested_by_name ?? batch.requested_by}</span>
          <Link to={`/verify/${batch.id}`} className="text-primary hover:underline">
            View details
          </Link>
        </div>

        {/* Doc 13 §7: a Lookup proves the line is valid and mobile — not that
            anyone owns or answered the number. */}
        <p className="text-xs text-muted-foreground">
          Verification confirms that a number is valid and on a mobile line. It does not confirm
          ownership or that anyone answered.
        </p>
      </CardContent>
    </Card>
  )
}

function Counter({ label, value, className }: { label: string; value: number; className?: string }) {
  return (
    <div>
      <dt className="text-xs tracking-wide text-muted-foreground uppercase">{label}</dt>
      <dd className={`text-lg font-semibold tabular-nums ${className ?? ''}`}>{value}</dd>
    </div>
  )
}
