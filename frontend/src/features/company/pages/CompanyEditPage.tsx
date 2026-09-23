import { Card, CardContent } from '@/components/ui/card'
import { PageHeader } from '@/components/layout/PageHeader'
import { LoadingRows } from '@/components/feedback/LoadingRows'
import { ErrorState } from '@/components/feedback/ErrorState'
import { ArrowLeft } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router'
import { CompanyForm } from '../components/CompanyForm'
import { useCompany, useUpdateCompany } from '../hooks'

export function CompanyEditPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: company, isLoading } = useCompany(id)
  const updateMutation = useUpdateCompany(id ?? '')

  if (isLoading) {
    return (
      <div className="space-y-4">
        <LoadingRows count={1} height="h-16" />
        <LoadingRows count={1} height="h-80" />
      </div>
    )
  }
  if (!company) return <ErrorState title="Company not found" />

  return (
    <div className="space-y-6">
      <PageHeader
        breadcrumb={
          <Link to={`/companies/${id}`} className="inline-flex items-center gap-1 hover:text-foreground transition-colors">
            <ArrowLeft className="size-3.5" /> Back to company
          </Link>
        }
        title={`Edit · ${company.canonical_name}`}
        description="Changes are audited and stamp the actor on the record."
      />
      <Card>
        <CardContent className="p-6">
          <CompanyForm
            initialData={company}
            onSubmit={(data) => {
              updateMutation.mutate(data, { onSuccess: () => navigate(`/companies/${id}`) })
            }}
            isPending={updateMutation.isPending}
            submitLabel="Save changes"
          />
        </CardContent>
      </Card>
    </div>
  )
}
