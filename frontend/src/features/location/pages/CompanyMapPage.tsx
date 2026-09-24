import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { PageHeader } from '@/components/layout/PageHeader'
import { EmptyState } from '@/components/feedback/EmptyState'
import { LoadingRows } from '@/components/feedback/LoadingRows'
import { InlineTip } from '@/components/feedback/InlineTip'
import { useNicPrimary, useResolveNicByCode } from '@/features/nic/hooks'
import { MapPin } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useMapCompanies, usePincode } from '../hooks'
import type { MapCompanyItem, MapNicCodeRef } from '../types'

/**
 * `/companies/map` — owned companies plotted around a pincode centroid.
 * External Places search (Overpass / Google) has been removed by product
 * decision; the tab, the API call, and the "+ Add" flow are gone. What
 * remains is a pincode search box, a radius selector, a map render and
 * the list of ProspectSoul companies at that pincode.
 */
export function CompanyMapPage() {
  const [sp, setSp] = useSearchParams()
  const pincode = sp.get('pincode') ?? ''
  const radiusKm = Number(sp.get('radius_km') ?? '5')
  const nicParentId = sp.get('nic_parent_id') ?? ''
  const nicIncludeDescendants = sp.get('nic_include_descendants') !== 'false'

  const nicPrimary = useNicPrimary()
  const centroid = usePincode(pincode)
  const owned = useMapCompanies(pincode, radiusKm, {
    nic_parent_id: nicParentId || undefined,
    nic_include_descendants: nicParentId ? nicIncludeDescendants : undefined,
  })

  // Manual NIC code entry (map page only) — a text-input alternative to the
  // dropdown. Local UI state: the in-progress text and its resolution error
  // are transient and belong here, not in the URL; the RESOLVED filter still
  // lives in `nic_parent_id` like the dropdown, so the URL shape is unchanged.
  const [manualNicCode, setManualNicCode] = useState('')
  const [manualNicError, setManualNicError] = useState<string | null>(null)
  const resolveNic = useResolveNicByCode()

  function handleManualNicChange(value: string) {
    setManualNicCode(value)
    setManualNicError(null)
    // Typing clears the dropdown selection — only one input drives the
    // active filter at a time.
    if (nicParentId) update('nic_parent_id', '')
  }

  function submitManualNic() {
    const trimmed = manualNicCode.trim()
    if (!trimmed) return
    resolveNic.mutate(trimmed, {
      onSuccess: (nic) => {
        setManualNicError(null)
        update('nic_parent_id', nic.id)
      },
      onError: (err) => {
        setManualNicError(err instanceof Error ? err.message : `No NIC found for '${trimmed}'`)
      },
    })
  }

  function handleDropdownNicChange(value: string) {
    update('nic_parent_id', value)
    // Picking from the dropdown clears the manual input.
    setManualNicCode('')
    setManualNicError(null)
  }

  const matchedNicCodeIds = owned.data?.matched_nic_code_ids ?? null
  function visibleNicCodes(nicCodes: MapNicCodeRef[]): MapNicCodeRef[] {
    if (!matchedNicCodeIds) return nicCodes
    const matched = new Set(matchedNicCodeIds)
    return nicCodes.filter((n) => n.id != null && matched.has(n.id))
  }

  const center = useMemo(() => {
    if (centroid.data)
      return { lat: Number(centroid.data.centroid.lat), lng: Number(centroid.data.centroid.lng) }
    return null
  }, [centroid.data])

  function update(key: string, value: string) {
    const next = new URLSearchParams(sp)
    if (!value) next.delete(key)
    else next.set(key, value)
    setSp(next)
  }

  const unknown = owned.data?.unknown_pincode
  const list: MapCompanyItem[] = owned.data?.content ?? []

  return (
    <div className="space-y-6">
      <PageHeader
        title="Company Map"
        description="Plot ProspectSoul companies around any Indian pincode. The centroid is resolved on demand and cached."
      />

      <div className="rounded-xl border border-border/70 bg-card p-4 shadow-sm">
        <div className="flex flex-wrap items-end gap-3">
          <div className="min-w-[8rem]">
            <label htmlFor="map-pincode" className="text-xs font-medium text-muted-foreground">Pincode</label>
            <Input
              id="map-pincode"
              className="mt-1"
              value={pincode}
              onChange={(e) => update('pincode', e.target.value.replace(/[^0-9]/g, '').slice(0, 6))}
              placeholder="e.g. 641006"
              inputMode="numeric"
            />
          </div>
          <div className="min-w-[8rem]">
            <label htmlFor="map-radius" className="text-xs font-medium text-muted-foreground">Radius</label>
            <Select id="map-radius" className="mt-1" value={String(radiusKm)} onChange={(e) => update('radius_km', e.target.value)}>
              <option value="2">2 km</option>
              <option value="5">5 km</option>
              <option value="10">10 km</option>
              <option value="25">25 km</option>
              <option value="50">50 km</option>
            </Select>
          </div>
          <div className="min-w-[10rem]">
            <label htmlFor="map-nic" className="text-xs font-medium text-muted-foreground">NIC (with sub-codes)</label>
            <Select
              id="map-nic"
              className="mt-1"
              value={nicParentId}
              onChange={(e) => handleDropdownNicChange(e.target.value)}
            >
              <option value="">Any NIC</option>
              {nicPrimary.data?.map((n) => (
                <option key={n.id} value={n.id}>{n.code} · {n.description}</option>
              ))}
            </Select>
          </div>
          <div className="min-w-[9rem]">
            <label htmlFor="map-nic-manual" className="text-xs font-medium text-muted-foreground">Or type NIC code</label>
            <Input
              id="map-nic-manual"
              className="mt-1"
              value={manualNicCode}
              onChange={(e) => handleManualNicChange(e.target.value)}
              onBlur={submitManualNic}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault()
                  submitManualNic()
                }
              }}
              placeholder="e.g. 22199"
              aria-invalid={manualNicError ? true : undefined}
              aria-describedby={manualNicError ? 'map-nic-manual-error' : undefined}
            />
            {manualNicError ? (
              <p id="map-nic-manual-error" className="mt-1 text-xs text-destructive">{manualNicError}</p>
            ) : null}
          </div>
          {nicParentId ? (
            <label className="flex h-9 items-center gap-2 pb-1.5 text-sm text-foreground">
              <input
                type="checkbox"
                checked={nicIncludeDescendants}
                onChange={(e) => update('nic_include_descendants', e.target.checked ? '' : 'false')}
              />
              Include descendants
            </label>
          ) : null}
          {centroid.data ? (
            <div className="text-sm text-muted-foreground pb-1.5">
              → <span className="font-medium text-foreground">{centroid.data.area_name}</span>
              {centroid.data.state ? `, ${centroid.data.state}` : ''}
            </div>
          ) : null}
        </div>

        {unknown ? (
          <InlineTip tone="warning" className="mt-3">
            Pincode <strong>{pincode}</strong> isn't in the master centroid table yet — the map is
            centred at India for now, but the companies list below still shows every ProspectSoul
            record whose pincode matches.
          </InlineTip>
        ) : null}
      </div>

      {pincode && center ? (
        <MapView center={center} radiusKm={radiusKm} ownedCount={list.length} />
      ) : (
        <EmptyState
          icon={<MapPin className="size-6" />}
          title="Enter a pincode"
          description="Type a 6-digit Indian pincode to load the map and see companies in ProspectSoul in that area."
        />
      )}

      <section className="space-y-3">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
          In ProspectSoul ({list.length})
        </h2>
        {owned.isLoading ? (
          <LoadingRows count={3} height="h-14" />
        ) : list.length === 0 ? (
          <EmptyState
            title="No companies in this area"
            description="Try a larger radius, or import companies for this pincode from the Imports page."
          />
        ) : (
          <ul className="grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
            {list.map((c) => {
              const chips = visibleNicCodes(c.nic_codes ?? [])
              return (
                <li key={c.id} className="rounded-lg border border-border/70 bg-card p-3 shadow-sm hover:shadow-md transition-shadow flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <Link to={`/companies/${c.id}`} className="font-medium text-foreground hover:text-primary transition-colors block truncate">
                      {c.canonical_name}
                    </Link>
                    <div className="text-xs text-muted-foreground mt-0.5">
                      {c.pipeline_state} · {Number(c.lat).toFixed(4)}, {Number(c.lng).toFixed(4)}
                    </div>
                    {chips.length > 0 ? (
                      <div className="mt-1.5 flex flex-wrap gap-1">
                        {chips.map((n) => (
                          <span
                            key={n.id ?? n.code}
                            title={n.description}
                            className={
                              'inline-flex items-center gap-0.5 rounded-md px-1.5 py-0.5 text-[11px] font-medium ' +
                              (n.primary
                                ? 'bg-primary/10 text-primary border border-primary/20'
                                : 'bg-muted text-foreground border border-border')
                            }
                          >
                            {n.primary ? <span className="text-amber-500">★</span> : null}
                            {n.code}
                          </span>
                        ))}
                      </div>
                    ) : null}
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </section>
    </div>
  )
}

const MAPS_KEY = import.meta.env.VITE_MAPS_JS_API_KEY as string | undefined

function MapView({ center, radiusKm, ownedCount }: { center: { lat: number; lng: number }; radiusKm: number; ownedCount: number }) {
  const delta = 0.01 * Math.max(radiusKm, 2)
  const bbox = [center.lng - delta, center.lat - delta, center.lng + delta, center.lat + delta]

  const src = MAPS_KEY
    ? `https://www.google.com/maps/embed/v1/view?key=${MAPS_KEY}&center=${center.lat},${center.lng}&zoom=13`
    : `https://www.openstreetmap.org/export/embed.html?bbox=${bbox.join(',')}&layer=mapnik&marker=${center.lat},${center.lng}`

  return (
    <div className="rounded-xl overflow-hidden border border-border/70 shadow-sm bg-card">
      <iframe
        src={src}
        title="Company map"
        className="w-full h-[400px] border-0"
        loading="lazy"
        referrerPolicy="no-referrer-when-downgrade"
      />
      <div className="flex items-center justify-between border-t border-border/70 px-3 py-2 text-xs text-muted-foreground">
        <span className="inline-flex items-center gap-2">
          <span className="inline-block w-3 h-3 rounded-full bg-teal-500" />
          Owned in ProspectSoul ({ownedCount})
        </span>
        {!MAPS_KEY ? (
          <span>OpenStreetMap fallback — set <code>VITE_MAPS_JS_API_KEY</code> for Google Maps.</span>
        ) : null}
      </div>
    </div>
  )
}
