import { Button } from '@/components/ui/button'
import { PageHeader } from '@/components/layout/PageHeader'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useMapCompanies, usePincode, usePlacesSearch } from '../hooks'
import type { MapCompanyItem } from '../types'

/**
 * `/companies/map` — UI/UX Addendum §5.
 *
 * Google Maps JS is optional: set VITE_MAPS_JS_API_KEY in
 * frontend/.env.local (origin-restricted) to render the real map. When it
 * is absent, the page falls back to an OpenStreetMap iframe centred on the
 * pincode centroid, so the geographic context is still visible without any
 * paid dependency.
 *
 * Owned companies (solid teal) and external Google results (dashed amber)
 * always render as two separated lists per UX Rule 7.
 */

const MAPS_KEY = import.meta.env.VITE_MAPS_JS_API_KEY as string | undefined

export function CompanyMapPage() {
  const [sp, setSp] = useSearchParams()
  const pincode = sp.get('pincode') ?? ''
  const radiusKm = Number(sp.get('radius_km') ?? '5')
  const [tab, setTab] = useState<'owned' | 'external'>('owned')

  const centroid = usePincode(pincode)
  const owned = useMapCompanies(pincode, radiusKm)
  const external = usePlacesSearch(pincode, radiusKm * 1000, tab === 'external')

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

  return (
    <div className="space-y-6">
      <PageHeader
        title="Company Map"
        description="Enter any Indian pincode. Owned companies plot instantly; external Places results come live from OpenStreetMap or Google."
      />

      <div className="flex flex-wrap items-end gap-3">
        <div>
          <label className="text-xs text-muted-foreground">Pincode</label>
          <Input
            className="w-32"
            value={pincode}
            onChange={(e) => update('pincode', e.target.value.replace(/[^0-9]/g, '').slice(0, 6))}
            placeholder="e.g. 641001"
          />
        </div>
        <div>
          <label className="text-xs text-muted-foreground">Radius</label>
          <Select value={String(radiusKm)} onChange={(e) => update('radius_km', e.target.value)}>
            <option value="2">2 km</option>
            <option value="5">5 km</option>
            <option value="10">10 km</option>
            <option value="25">25 km</option>
            <option value="50">50 km</option>
          </Select>
        </div>
        {centroid.data ? (
          <p className="text-sm text-muted-foreground">
            → {centroid.data.area_name}
            {centroid.data.state ? `, ${centroid.data.state}` : ''}
          </p>
        ) : null}
      </div>

      {unknown ? (
        <p className="rounded bg-amber-50 border border-amber-300 p-2 text-xs text-amber-800">
          Pincode <strong>{pincode}</strong> is not in the master centroid table yet — the map is
          centred at India for now, but the companies list below still shows every record whose
          pincode matches. Add this pincode to <code>pincode_centroids</code> (or import an India
          Post PIN file) to get an accurate centre.
        </p>
      ) : null}

      {pincode && center ? (
        <MapView
          center={center}
          radiusKm={radiusKm}
          owned={owned.data?.content ?? []}
          externalMarkers={tab === 'external' ? (external.data?.results ?? []).map((r) => ({
            id: r.place_id,
            lat: Number(r.lat),
            lng: Number(r.lng),
            name: r.name,
          })) : []}
        />
      ) : (
        <p className="text-sm text-muted-foreground">Enter a 6-digit pincode to load the map.</p>
      )}

      <div className="flex gap-2">
        <Button size="sm" variant={tab === 'owned' ? 'default' : 'outline'} onClick={() => setTab('owned')}>
          In ProspectSoul ({owned.data?.content.length ?? 0})
        </Button>
        <Button size="sm" variant={tab === 'external' ? 'default' : 'outline'} onClick={() => setTab('external')}>
          {externalLabel(external.data?.source)} ({external.data?.results.length ?? 0})
        </Button>
      </div>

      {tab === 'owned' ? (
        <section className="space-y-1">
          {owned.isLoading ? (
            <p className="text-sm text-muted-foreground">Loading…</p>
          ) : owned.data && owned.data.content.length === 0 ? (
            <p className="text-sm text-muted-foreground">No owned companies in this area.</p>
          ) : (
            <ul className="text-sm space-y-1">
              {owned.data?.content.map((c) => (
                <li key={c.id} className="rounded border p-2 flex items-center justify-between">
                  <div>
                    <div className="font-medium">{c.canonical_name}</div>
                    <div className="text-xs text-muted-foreground">
                      {c.pipeline_state} · {Number(c.lat).toFixed(4)}, {Number(c.lng).toFixed(4)}
                    </div>
                  </div>
                  <Link to={`/companies/${c.id}`} className="text-primary text-xs">
                    view →
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      ) : (
        <section className="space-y-2">
          {external.isLoading ? (
            <p className="text-sm text-muted-foreground">Loading external results…</p>
          ) : external.isError ? (
            <p className="text-sm text-destructive">
              {String((external.error as Error)?.message ?? 'External lookup failed')}
            </p>
          ) : external.data && external.data.results.length === 0 ? (
            <p className="text-sm text-muted-foreground">No external results.</p>
          ) : (
            <ul className="text-sm space-y-1">
              {external.data?.results.map((r) => (
                <li
                  key={r.place_id}
                  className="rounded border border-dashed border-amber-400 p-2 flex items-center justify-between"
                >
                  <div>
                    <div className="font-medium">{r.name}</div>
                    <div className="text-xs text-muted-foreground">
                      {r.formatted_address} · not yet in ProspectSoul
                    </div>
                  </div>
                  <Link
                    to={{
                      pathname: '/companies/new',
                      search: new URLSearchParams({
                        source: 'GOOGLE_PLACES',
                        canonical_name: r.name,
                        pincode,
                        address_line: r.formatted_address,
                        primary_phone: r.phone ?? '',
                      }).toString(),
                    }}
                    className="rounded border px-2 py-1 text-xs"
                  >
                    + Add
                  </Link>
                </li>
              ))}
            </ul>
          )}
          {external.data ? (
            <p className="text-xs text-muted-foreground">
              Quota remaining today: {external.data.quota_remaining}
            </p>
          ) : null}
        </section>
      )}
    </div>
  )
}

interface MapViewProps {
  center: { lat: number; lng: number }
  radiusKm: number
  owned: MapCompanyItem[]
  externalMarkers: { id: string; lat: number; lng: number; name: string }[]
}

/**
 * Renders a Google Maps JS iframe when VITE_MAPS_JS_API_KEY is set; falls
 * back to an OpenStreetMap iframe otherwise so the page always shows a
 * visible map instead of just text lists.
 */
function MapView({ center, radiusKm, owned, externalMarkers }: MapViewProps) {
  // Rough BBox around the centre for the OSM fallback iframe. 0.045 deg
  // ≈ 5 km at Indian latitudes — scales roughly with the radius.
  const delta = 0.01 * Math.max(radiusKm, 2)
  const bbox = [center.lng - delta, center.lat - delta, center.lng + delta, center.lat + delta]

  if (MAPS_KEY) {
    // Google Maps Embed API. The key MUST be origin-restricted in the
    // Google Cloud Console — never a server-side key.
    const src = `https://www.google.com/maps/embed/v1/view?key=${MAPS_KEY}&center=${center.lat},${center.lng}&zoom=13`
    return (
      <div className="rounded border overflow-hidden">
        <iframe
          src={src}
          title="Company map"
          className="w-full h-[400px] border-0"
          loading="lazy"
          referrerPolicy="no-referrer-when-downgrade"
        />
        <MapLegend ownedCount={owned.length} externalCount={externalMarkers.length} />
      </div>
    )
  }

  const osmSrc = `https://www.openstreetmap.org/export/embed.html?bbox=${bbox.join(',')}&layer=mapnik&marker=${center.lat},${center.lng}`
  return (
    <div className="rounded border overflow-hidden">
      <iframe
        src={osmSrc}
        title="Company map (OpenStreetMap fallback)"
        className="w-full h-[400px] border-0"
        loading="lazy"
        referrerPolicy="no-referrer-when-downgrade"
      />
      <div className="border-t px-3 py-2 text-xs text-muted-foreground">
        Showing an OpenStreetMap fallback. Set <code>VITE_MAPS_JS_API_KEY</code> in
        <code> frontend/.env.local</code> (origin-restricted) to render Google Maps here.
      </div>
      <MapLegend ownedCount={owned.length} externalCount={externalMarkers.length} />
    </div>
  )
}

function MapLegend({ ownedCount, externalCount }: { ownedCount: number; externalCount: number }) {
  return (
    <div className="flex gap-4 border-t px-3 py-2 text-xs text-muted-foreground">
      <span className="inline-flex items-center gap-1">
        <span className="inline-block w-3 h-3 rounded-full bg-teal-500" /> Owned ({ownedCount})
      </span>
      <span className="inline-flex items-center gap-1">
        <span className="inline-block w-3 h-3 rounded-full border-2 border-dashed border-amber-500" />
        External ({externalCount})
      </span>
    </div>
  )
}

function externalLabel(source: string | undefined) {
  switch (source) {
    case 'google_places':          return 'Found on Google'
    case 'openstreetmap_overpass': return 'Found on OpenStreetMap'
    case 'stub':                   return 'Found (stub)'
    default:                       return 'Found externally'
  }
}
