import { PageHeader } from '@/components/layout/PageHeader'
import { usePermissions } from '@/auth/usePermissions'
import { Button } from '@/components/ui/button'
import { ChevronDown, ChevronUp } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { CurrentVerificationCard } from '../components/CurrentVerificationCard'
import { VerificationHistoryTable } from '../components/VerificationHistoryTable'
import { VerifiedCompaniesTable } from '../components/VerifiedCompaniesTable'
import { VerifyNewCompaniesPanel } from '../components/VerifyNewCompaniesPanel'
import { invalidateAfterBatchCompletion, useActiveVerification } from '../hooks'
import { isBatchRunning } from '../types'

export function VerifyPage() {
  const { canMutate } = usePermissions()
  const active = useActiveVerification()
  const running = isBatchRunning(active.data)

  const [selectionOpen, setSelectionOpen] = useState(canMutate)

  const previousStatus = useRef<string | null>(null)
  useEffect(() => {
    const status = active.data?.status ?? null
    if (previousStatus.current && previousStatus.current !== status) {
      const wasRunning = previousStatus.current === 'QUEUED' || previousStatus.current === 'PROCESSING'
      if (wasRunning) {
        invalidateAfterBatchCompletion()
      }
    }
    previousStatus.current = status
  }, [active.data?.status])

  return (
    <div className="space-y-6">
      <PageHeader
        title="Verify"
        description="Check prospect phone numbers through the verification provider and keep each company's verification state current."
        actions={
          canMutate ? (
            <Button
              variant={selectionOpen ? 'outline' : 'default'}
              onClick={() => setSelectionOpen((open) => !open)}
              aria-expanded={selectionOpen}
              aria-controls="verify-new-companies"
            >
              {selectionOpen ? (<><ChevronUp /> Hide selection</>) : (<><ChevronDown /> Verify new companies</>)}
            </Button>
          ) : null
        }
      />

      {selectionOpen && (
        <div id="verify-new-companies" className="animate-slide-up">
          <VerifyNewCompaniesPanel canStart={canMutate} batchRunning={running} />
        </div>
      )}

      <CurrentVerificationCard
        batch={active.data}
        isLoading={active.isLoading}
        error={active.error}
      />

      <VerificationHistoryTable />

      <VerifiedCompaniesTable />
    </div>
  )
}
