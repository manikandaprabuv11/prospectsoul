import { Button } from '@/components/ui/button'
import { StatCard } from '@/components/layout/StatCard'
import { useAuth } from '@/auth/useAuth'
import { usePermissions } from '@/auth/usePermissions'
import { cn } from '@/lib/utils'
import {
  ArrowRight, Building2, FileUp, MapPin, ShieldCheck, Users, ListTree,
  UserSquare2, Sparkles, TrendingUp,
} from 'lucide-react'
import { Link } from 'react-router'

/**
 * Dashboard — brand-forward landing screen.
 *
 *   • Hero card with the brand gradient, greeting and primary CTAs.
 *   • KPI stat row — accent-striped tiles in the feature palette.
 *   • Quick-action grid — coloured icon chips per module, hover shifts
 *     the card up and shows a chevron.
 */
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
        <div className="absolute -top-16 -right-16 size-64 rounded-full bg-white/10 blur-3xl" aria-hidden="true" />
        <div className="absolute -bottom-16 -left-16 size-64 rounded-full bg-white/10 blur-3xl" aria-hidden="true" />
        <div className="relative px-6 py-8 sm:px-10 sm:py-10 flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div className="min-w-0 space-y-2">
            <div className="inline-flex items-center gap-2 rounded-full bg-white/15 backdrop-blur px-2.5 py-1 text-xs font-semibold uppercase tracking-wider">
              <Sparkles className="size-3.5" /> {primaryRole} · ProspectSoul
            </div>
            <h1 id="hero-title" className="text-3xl sm:text-4xl font-bold tracking-tight">
              {greeting}{first ? `, ${first}` : ''}
            </h1>
            <p className="text-white/85 max-w-2xl leading-relaxed">
              A single view of every prospect. Filter, verify, enrich and export — with a
              full audit trail behind every mutation.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {permissions.canRead ? (
              <Button variant="secondary" className="bg-white text-primary hover:bg-white/90 shadow-md" asChild>
                <Link to="/companies"><Building2 /> Browse companies</Link>
              </Button>
            ) : null}
            {permissions.canMutate ? (
              <Button className="bg-black/25 hover:bg-black/35 text-white backdrop-blur border border-white/25" asChild>
                <Link to="/imports/new"><FileUp /> Start import</Link>
              </Button>
            ) : null}
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
            icon={<Building2 className="size-4" />}
            accent="teal"
          />
          <StatCard
            label="Import batches"
            value="—"
            hint="Uploaded this month"
            icon={<FileUp className="size-4" />}
            accent="violet"
          />
          <StatCard
            label="Verified"
            value="—"
            hint="Phones confirmed by Twilio"
            icon={<ShieldCheck className="size-4" />}
            accent="emerald"
          />
          <StatCard
            label="NIC codes"
            value="—"
            hint="Master data seeded"
            icon={<ListTree className="size-4" />}
            accent="amber"
          />
        </div>
      </section>

      {/* Quick actions */}
      <section aria-labelledby="quick-heading" className="space-y-4">
        <div className="flex items-end justify-between gap-3">
          <div>
            <h2 id="quick-heading" className="text-lg font-semibold tracking-tight">Quick actions</h2>
            <p className="text-sm text-muted-foreground">Every module at your fingertips.</p>
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
        "accent-stripe group relative flex flex-col justify-between gap-4 rounded-xl border border-border/70 bg-card p-5 shadow-sm transition-all hover:shadow-md hover:-translate-y-0.5",
        cls,
      )}
    >
      <div className="space-y-3">
        <div className={cn("accent-chip-bg flex size-11 items-center justify-center rounded-lg", cls)}>
          {icon}
        </div>
        <div>
          <h3 className="text-base font-semibold tracking-tight">{title}</h3>
          <p className="text-sm text-muted-foreground leading-relaxed mt-1">{description}</p>
        </div>
      </div>
      <span className="inline-flex items-center gap-1 text-sm font-medium text-primary group-hover:gap-1.5 transition-all">
        Open <ArrowRight className="size-4 group-hover:translate-x-0.5 transition-transform" />
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
