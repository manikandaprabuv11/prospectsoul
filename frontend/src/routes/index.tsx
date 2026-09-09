import { RequireRole } from '@/auth/RequireRole'
import { RolesPage } from '@/features/admin/pages/RolesPage'
import { UsersPage } from '@/features/admin/pages/UsersPage'
import { CompanyCreatePage } from '@/features/company/pages/CompanyCreatePage'
import { CompanyDetailPage } from '@/features/company/pages/CompanyDetailPage'
import { CompanyEditPage } from '@/features/company/pages/CompanyEditPage'
import { CompanyListPage } from '@/features/company/pages/CompanyListPage'
import { DashboardPage } from '@/features/dashboard/pages/DashboardPage'
import { ImportDetailPage } from '@/features/imports/pages/ImportDetailPage'
import { ImportListPage } from '@/features/imports/pages/ImportListPage'
import { ImportWizardPage } from '@/features/imports/pages/ImportWizardPage'
import { AppLayout } from '@/layouts/AppLayout'
import { createBrowserRouter } from 'react-router'

const MUTATE_ROLES = ['PS_ANALYST', 'PS_SALES_LEAD', 'PS_ADMIN']
const ADMIN_ROLES = ['PS_ADMIN']

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { path: '/', element: <DashboardPage /> },
      { path: '/companies', element: <CompanyListPage /> },
      {
        path: '/companies/new',
        element: (
          <RequireRole roles={MUTATE_ROLES}>
            <CompanyCreatePage />
          </RequireRole>
        ),
      },
      { path: '/companies/:id', element: <CompanyDetailPage /> },
      {
        path: '/companies/:id/edit',
        element: (
          <RequireRole roles={MUTATE_ROLES}>
            <CompanyEditPage />
          </RequireRole>
        ),
      },
      {
        path: '/imports',
        element: (
          <RequireRole roles={MUTATE_ROLES}>
            <ImportListPage />
          </RequireRole>
        ),
      },
      {
        path: '/imports/new',
        element: (
          <RequireRole roles={MUTATE_ROLES}>
            <ImportWizardPage />
          </RequireRole>
        ),
      },
      {
        path: '/imports/:id',
        element: (
          <RequireRole roles={MUTATE_ROLES}>
            <ImportDetailPage />
          </RequireRole>
        ),
      },
      {
        path: '/settings/users',
        element: (
          <RequireRole roles={ADMIN_ROLES}>
            <UsersPage />
          </RequireRole>
        ),
      },
      {
        path: '/settings/roles',
        element: (
          <RequireRole roles={ADMIN_ROLES}>
            <RolesPage />
          </RequireRole>
        ),
      },
    ],
  },
])
