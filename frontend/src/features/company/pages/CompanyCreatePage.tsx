import { Card, CardContent } from '@/components/ui/card'
import { PageHeader } from '@/components/layout/PageHeader'
import { ArrowLeft } from 'lucide-react'
import { Link, useNavigate } from 'react-router'
import { CompanyForm } from '../components/CompanyForm'
import { useCreateCompany } from '../hooks'

export function CompanyCreatePage() {
  const navigate = useNavigate()
  const create = useCreateCompany()

  return (
    <div className="space-y-6">
      <PageHeader
        breadcrumb={
          <Link to="/companies" className="inline-flex items-center gap-1 hover:text-foreground transition-colors">
            <ArrowLeft className="size-3.5" /> Back to companies
          </Link>
        }
        title="New company"
        description="Add a prospect manually. Every mutation is audited server-side."
      />
      <Card>
        <CardContent className="p-6">
          <CompanyForm
            isPending={create.isPending}
            submitLabel="Create company"
            onSubmit={(data) =>
              create.mutate(data, {
                onSuccess: (company) => navigate(`/companies/${company.id}`),
              })
            }
          />
        </CardContent>
      </Card>
    </div>
  )
}
