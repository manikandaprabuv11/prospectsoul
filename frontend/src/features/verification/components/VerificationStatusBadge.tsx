import { Badge } from '@/components/ui/badge'
import type { VerificationBatchStatus, VerificationItemStatus } from '../types'

const BATCH_LABELS: Record<VerificationBatchStatus, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' | 'success' | 'warning' }> = {
  QUEUED: { label: 'Queued', variant: 'secondary' },
  PROCESSING: { label: 'Processing', variant: 'warning' },
  COMPLETED: { label: 'Completed', variant: 'success' },
  COMPLETED_WITH_ERRORS: { label: 'Completed with errors', variant: 'warning' },
  FAILED: { label: 'Failed', variant: 'destructive' },
  CANCELLED: { label: 'Cancelled', variant: 'outline' },
}

const ITEM_LABELS: Record<VerificationItemStatus, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' | 'success' | 'warning' }> = {
  QUEUED: { label: 'Queued', variant: 'secondary' },
  PROCESSING: { label: 'Processing', variant: 'warning' },
  VERIFIED: { label: 'Verified', variant: 'success' },
  FAILED: { label: 'Failed', variant: 'destructive' },
  SKIPPED: { label: 'Skipped', variant: 'outline' },
}

export function BatchStatusBadge({ status }: { status: VerificationBatchStatus }) {
  const entry = BATCH_LABELS[status]
  return <Badge variant={entry?.variant ?? 'outline'}>{entry?.label ?? status}</Badge>
}

export function ItemStatusBadge({ status }: { status: VerificationItemStatus }) {
  const entry = ITEM_LABELS[status]
  return <Badge variant={entry?.variant ?? 'outline'}>{entry?.label ?? status}</Badge>
}
