import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { Skeleton } from '@/components/ui/skeleton'
import { ShieldCheck, Loader2 } from 'lucide-react'
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
          <div role="alert" className="rounded-xl border border-destructive/30 bg-destructive/5 px-4 py-3 text-sm font-medium text-destructive">
            {problemDetail(error, 'Could not load the current verification.')}
          </div>
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
          <div className="flex flex-col items-start gap-2 py-2">
            <div className="flex items-center gap-2.5 text-sm font-semibold">
              <div className="flex size-8 items-center justify-center rounded-lg bg-surface-1 text-muted-foreground">
                <ShieldCheck className="size-4" />
              </div>
              No verification is running
            </div>
            <p className="text-sm text-muted-foreground leading-relaxed">
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
        <div className="flex items-center gap-2">
          {running && (
            <span className="flex items-center gap-1.5 text-xs font-semibold text-primary">
              <span className="relative flex size-2">
                <span className="absolute inline-flex size-full animate-ping rounded-full bg-primary opacity-50" />
                <span className="relative inline-flex size-2 rounded-full bg-primary" />
              </span>
              Live
            </span>
          )}
          <BatchStatusBadge status={batch.status} />
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <div className="flex items-baseline justify-between">
            <p className="text-sm font-medium">
              {processed} / {batch.total_count} companies processed
            </p>
            <p className="text-sm font-bold tabular-nums">{batch.progress_percent}%</p>
          </div>
          <Progress value={batch.progress_percent} label="Verification progress" />
        </div>

        <dl className="grid grid-cols-2 gap-3 sm:grid-cols-5">
          <Counter label="Verified" value={batch.verified_count} className="text-accent-emerald" />
          <Counter label="Processing" value={batch.processing_count} className="text-accent-amber" />
          <Counter label="Queued" value={batch.queued_count} />
          <Counter label="Failed" value={batch.failed_count} className="text-accent-rose" />
          <Counter label="Skipped" value={batch.skipped_count} />
        </dl>

        {batch.current_item ? (
          <div className="rounded-xl border border-border bg-surface-1/50 p-3">
            <div className="flex items-center gap-2">
              <Loader2 className="size-3.5 animate-spin text-primary" />
              <p className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                Currently verifying
              </p>
            </div>
            <p className="mt-1.5 text-sm font-semibold">
              <Link to={`/companies/${batch.current_item.company_id}`} className="text-primary hover:underline transition-colors">
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
          <Link to={`/verify/${batch.id}`} className="text-primary font-medium hover:underline transition-colors">
            View details
          </Link>
        </div>

        <p className="text-xs text-muted-foreground leading-relaxed">
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
      <dt className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">{label}</dt>
      <dd className={`text-lg font-bold tabular-nums ${className ?? ''}`}>{value}</dd>
    </div>
  )
}
