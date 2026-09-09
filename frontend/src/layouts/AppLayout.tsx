import { Footer } from '@/shared/components/Footer'
import { Header } from '@/shared/components/Header'
import { Sidebar } from '@/shared/components/Sidebar'
import { Outlet } from 'react-router'

export function AppLayout() {
  return (
    <div className="flex h-svh flex-col">
      <Header />
      <div className="flex flex-1 overflow-hidden">
        <Sidebar />
        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </div>
      <Footer />
    </div>
  )
}
