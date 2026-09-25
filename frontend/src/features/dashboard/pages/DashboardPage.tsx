import { Button } from '@/components/ui/button'
import { StatCard } from '@/components/layout/StatCard'
import { useAuth } from '@/auth/useAuth'
import { usePermissions } from '@/auth/usePermissions'
import { cn } from '@/lib/utils'
import {
  ArrowRight, Building2, FileUp, MapPin, ShieldCheck, Users, ListTree,
  UserSquare2, Sparkles, TrendingUp, Layers,
} from 'lucide-react'
import { Link } from 'react-router'

export function DashboardPage() {
  const { user, roles } = useAuth()
  const permissions = usePermissions()
  const greeting = timeGreeting()
  const first = firstName(user?.fullName ?? user?.username)
  const primaryRole = ROLE_LABELS[roles?.[0] ?? ''] ?? 'Team member'

  return (
    <div className="space-y-8">
      {/* Hero */}
      <section
        aria-labelledby="hero-title"
        className="relative overflow-hidden rounded-2xl bg-brand-gradient text-white shadow-primary"
      >
        {/* Decorative orbs */}
        <div className="absolute -top-20 -right-20 size-72 rounded-full bg-white/8 blur-3xl" aria-hidden="true" />
        <div className="absolute -bottom-20 -left-20 size-64 rounded-full bg-white/6 blur-3xl" aria-hidden="true" />
        <div className="absolute top-1/2 left-1/3 size-48 rounded-full bg-white/5 blur-3xl" aria-hidden="true" />

        <div className="relative px-6 py-10 sm:px-10 sm:py-12 flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div className="min-w-0 space-y-3">
            <div className="inline-flex items-center gap-2 rounded-full bg-white/12 backdrop-blur-sm px-3 py-1.5 text-[11px] font-semibold uppercase tracking-wider border border-white/10">
              <Sparkles className="size-3.5" /> {primaryRole}
            </div>
            <h1 id="hero-title" className="text-3xl sm:text-4xl font-bold tracking-tight leading-[1.1]">
              {greeting}{first ? `, ${first}` : ''}
            </h1>
            <p className="text-white/75 max-w-xl leading-relaxed text-[15px]">
              Your single view of every prospect. Filter, verify, enrich and export — with a
              full audit trail behind every mutation.
            </p>
          </div>
          <div className="flex flex-wrap gap-2.5">
            {permissions.canRead && (
              <Button variant="secondary" className="bg-white/95 text-primary hover:bg-white shadow-md border-0 font-semibold" asChild>
                <Link to="/companies"><Building2 /> Browse companies</Link>
              </Button>
            )}
            {permissions.canMutate && (
              <Button className="bg-white/15 hover:bg-white/25 text-white backdrop-blur-sm border border-white/20 shadow-none" asChild>
                <Link to="/imports/new"><FileUp /> Start import</Link>
              </Button>
            )}
          </div>
        </div>
      </section>

      {/* KPIs */}
      <section aria-labelledby="kpi-heading">
        <h2 id="kpi-heading" className="sr-only">Pipeline overview</h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard
            label="Prospect companies"
            value="—"
            hint={<span className="inline-flex items-center gap-1"><TrendingUp className="size-3" /> Managed in ProspectSoul</span>}
            icon={<Building2 className="size-4.5" />}
            accent="teal"
          />
          <StatCard
            label="Import batches"
            value="—"
            hint="Uploaded this month"
            icon={<FileUp className="size-4.5" />}
            accent="violet"
          />
          <StatCard
            label="Verified"
            value="—"
            hint="Phones confirmed by Twilio"
            icon={<ShieldCheck className="size-4.5" />}
            accent="emerald"
          />
          <StatCard
            label="NIC codes"
            value="—"
            hint="Master data seeded"
            icon={<ListTree className="size-4.5" />}
            accent="amber"
          />
        </div>
      </section>

      {/* Quick actions */}
      <section aria-labelledby="quick-heading" className="space-y-5">
        <div className="flex items-end justify-between gap-3">
          <div>
            <h2 id="quick-heading" className="text-lg font-bold tracking-tight">Quick actions</h2>
            <p className="text-sm text-muted-foreground mt-0.5">Every module at your fingertips.</p>
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {permissions.canRead && (
            <ActionCard
              to="/companies"
              icon={<Building2 className="size-5" />}
              title="Companies"
              description="Search, filter and drill into every prospect. Radius, NIC and role facets built in."
              accent="teal"
            />
          )}
          {permissions.canRead && (
            <ActionCard
              to="/companies/map"
              icon={<MapPin className="size-5" />}
              title="Map view"
              description="Plot companies on a live map, search external Places, add nearby businesses in one click."
              accent="amber"
            />
          )}
          {permissions.canMutate && (
            <ActionCard
              to="/imports"
              icon={<FileUp className="size-5" />}
              title="Import data"
              description="Upload Excel or CSV. Live progress, skip-on-error and idempotent re-imports."
              accent="violet"
            />
          )}
          {permissions.canRead && (
            <ActionCard
              to="/verify"
              icon={<ShieldCheck className="size-5" />}
              title="Verify phones"
              description="Batch-verify contact numbers through Twilio Lookup with a live queue."
              accent="emerald"
            />
          )}
          {permissions.canRead && (
            <ActionCard
              to="/enrichment/jobs"
              icon={<Layers className="size-5" />}
              title="Enrichment"
              description="Run enrichment against Google Places, website and phone providers."
              accent="sky"
            />
          )}
          {permissions.canConfigure && (
            <ActionCard
              to="/settings/nic-codes"
              icon={<ListTree className="size-5" />}
              title="NIC master"
              description="Manage the hierarchical NIC classification. Import, tree view, primary flag."
              accent="sky"
            />
          )}
          {permissions.canConfigure && (
            <ActionCard
              to="/settings/contact-roles"
              icon={<UserSquare2 className="size-5" />}
              title="Contact roles"
              description="Configure the role vocabulary for company contacts. Add, deactivate, reorder."
              accent="rose"
            />
          )}
          {permissions.canConfigure && (
            <ActionCard
              to="/settings/users"
              icon={<Users className="size-5" />}
              title="Users & roles"
              description="Provision users, assign roles, mirror to Keycloak."
              accent="indigo"
            />
          )}
        </div>
      </section>
    </div>
  )
}

type Accent = "indigo" | "teal" | "violet" | "emerald" | "amber" | "rose" | "sky"

const accentClassMap: Record<Accent, string> = {
  indigo:  "accent-indigo",
  teal:    "accent-teal",
  violet:  "accent-violet",
  emerald: "accent-emerald",
  amber:   "accent-amber",
  rose:    "accent-rose",
  sky:     "accent-sky",
}

function ActionCard({
  to, icon, title, description, accent,
}: { to: string; icon: React.ReactNode; title: string; description: string; accent: Accent }) {
  const cls = accentClassMap[accent]
  return (
    <Link
      to={to}
      className={cn(
        "accent-stripe group relative flex flex-col justify-between gap-4 rounded-xl border border-border bg-card p-5",
        "shadow-card transition-all duration-200 hover:shadow-card-hover hover:-translate-y-1",
        cls,
      )}
    >
      <div className="space-y-3">
        <div className={cn("accent-chip-bg flex size-11 items-center justify-center rounded-xl transition-transform duration-200 group-hover:scale-110", cls)}>
          {icon}
        </div>
        <div>
          <h3 className="text-[15px] font-semibold tracking-tight">{title}</h3>
          <p className="text-[13px] text-muted-foreground leading-relaxed mt-1">{description}</p>
        </div>
      </div>
      <span className="inline-flex items-center gap-1.5 text-sm font-medium text-primary group-hover:gap-2 transition-all duration-200">
        Open <ArrowRight className="size-4 group-hover:translate-x-0.5 transition-transform duration-200" />
      </span>
    </Link>
  )
}

const ROLE_LABELS: Record<string, string> = {
  PS_ADMIN: 'Administrator',
  PS_ANALYST: 'Research Analyst',
  PS_SALES_LEAD: 'Sales Lead',
  PS_VIEWER: 'Viewer',
  PS_COO: 'COO',
  PS_TELECALLER: 'Telecaller',
}

function timeGreeting() {
  const h = new Date().getHours()
  if (h < 12) return 'Good morning'
  if (h < 17) return 'Good afternoon'
  return 'Good evening'
}

function firstName(display: string | undefined | null) {
  if (!display) return ''
  const clean = display.trim().split(/[.\s@]/)[0]
  return clean ? clean.charAt(0).toUpperCase() + clean.slice(1) : ''
}
