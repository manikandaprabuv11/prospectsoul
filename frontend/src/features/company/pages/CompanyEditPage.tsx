import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
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
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96 w-full" />
      </div>
    )
  }

  if (!company) {
    return (
      <div className="rounded-lg border border-destructive/50 p-6 text-center text-sm text-destructive">
        Company not found
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" asChild>
          <Link to={`/companies/${id}`}><ArrowLeft className="size-4" /></Link>
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Edit: {company.canonical_name}</h1>
      </div>

      <Card>
        <CardHeader><CardTitle>Company Details</CardTitle></CardHeader>
        <CardContent>
          <CompanyForm
            initialData={company}
            onSubmit={(data) => {
              updateMutation.mutate(data, {
                onSuccess: () => navigate(`/companies/${id}`),
              })
            }}
            isPending={updateMutation.isPending}
            submitLabel="Save Changes"
          />
        </CardContent>
      </Card>
    </div>
  )
}
