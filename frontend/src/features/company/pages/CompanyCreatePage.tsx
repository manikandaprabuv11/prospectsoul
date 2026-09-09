import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ArrowLeft } from 'lucide-react'
import { Link, useNavigate } from 'react-router'
import { CompanyForm } from '../components/CompanyForm'
import { useCreateCompany } from '../hooks'

export function CompanyCreatePage() {
  const navigate = useNavigate()
  const createMutation = useCreateCompany()

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" asChild>
          <Link to="/companies"><ArrowLeft className="size-4" /></Link>
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">New Company</h1>
      </div>

      <Card>
        <CardHeader><CardTitle>Company Details</CardTitle></CardHeader>
        <CardContent>
          <CompanyForm
            onSubmit={(data) => {
              createMutation.mutate(data, {
                onSuccess: (company) => navigate(`/companies/${company.id}`),
              })
            }}
            isPending={createMutation.isPending}
            submitLabel="Create Company"
          />
        </CardContent>
      </Card>
    </div>
  )
}
