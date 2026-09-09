import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Building2, FileUp } from 'lucide-react'
import { Link } from 'react-router'

export function DashboardPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
        <p className="text-sm text-muted-foreground">Welcome to ProspectSoul</p>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Building2 className="size-5" /> Companies
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <p className="text-sm text-muted-foreground">
              Manage prospect companies, search, filter, and verify records.
            </p>
            <Button asChild>
              <Link to="/companies">View Companies</Link>
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <FileUp className="size-5" /> Import Data
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <p className="text-sm text-muted-foreground">
              Upload Excel or CSV files with flexible column mapping.
            </p>
            <Button asChild>
              <Link to="/imports/new">Start Import</Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
