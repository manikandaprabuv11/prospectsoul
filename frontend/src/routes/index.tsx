import { SetupCheckPage } from '@/pages/SetupCheckPage'
import { createBrowserRouter } from 'react-router'

/**
 * Routing foundation. The ProspectSoul route tree is added in later work; for
 * now `/` renders the setup verification page.
 */
export const router = createBrowserRouter([
  {
    path: '/',
    element: <SetupCheckPage />,
  },
])
