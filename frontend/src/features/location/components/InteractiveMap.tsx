import L from 'leaflet'
import { useEffect, useMemo, useRef } from 'react'
import { MapContainer, Marker, Popup, TileLayer, Circle, useMap } from 'react-leaflet'
import { Link } from 'react-router'
import type { MapCompanyItem, MapNicCodeRef } from '../types'

const companyIcon = L.divIcon({
  className: '',
  html: `<div style="
    width: 28px; height: 28px;
    background: #3B82F6;
    border: 2.5px solid #fff;
    border-radius: 50%;
    box-shadow: 0 2px 6px rgba(0,0,0,0.35);
    display: flex; align-items: center; justify-content: center;
  ">
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
      <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
      <polyline points="9 22 9 12 15 12 15 22"/>
    </svg>
  </div>`,
  iconSize: [28, 28],
  iconAnchor: [14, 14],
  popupAnchor: [0, -16],
})

const centerIcon = L.divIcon({
  className: '',
  html: `<div style="
    width: 14px; height: 14px;
    background: #EF4444;
    border: 2px solid #fff;
    border-radius: 50%;
    box-shadow: 0 1px 4px rgba(0,0,0,0.3);
  "></div>`,
  iconSize: [14, 14],
  iconAnchor: [7, 7],
})

interface InteractiveMapProps {
  center: { lat: number; lng: number }
  radiusKm: number
  companies: MapCompanyItem[]
  matchedNicCodeIds: string[] | null
}

function FitBounds({ center, radiusKm, companies }: { center: { lat: number; lng: number }; radiusKm: number; companies: MapCompanyItem[] }) {
  const map = useMap()
  const prevKey = useRef('')

  useEffect(() => {
    const key = `${center.lat},${center.lng},${radiusKm},${companies.length}`
    if (key === prevKey.current) return
    prevKey.current = key

    if (companies.length === 0) {
      map.setView([center.lat, center.lng], zoomForRadius(radiusKm))
      return
    }

    const points: L.LatLngExpression[] = [
      [center.lat, center.lng],
      ...companies.map((c) => [Number(c.lat), Number(c.lng)] as [number, number]),
    ]
    const bounds = L.latLngBounds(points)
    map.fitBounds(bounds, { padding: [40, 40], maxZoom: 16 })
  }, [map, center, radiusKm, companies])

  return null
}

function zoomForRadius(km: number): number {
  if (km <= 2) return 15
  if (km <= 5) return 14
  if (km <= 10) return 13
  if (km <= 25) return 11
  return 10
}

function visibleNicCodes(nicCodes: MapNicCodeRef[], matchedIds: string[] | null): MapNicCodeRef[] {
  if (!matchedIds) return nicCodes
  const matched = new Set(matchedIds)
  return nicCodes.filter((n) => n.id != null && matched.has(n.id))
}

export function InteractiveMap({ center, radiusKm, companies, matchedNicCodeIds }: InteractiveMapProps) {
  const radiusMeters = radiusKm * 1000

  const markers = useMemo(
    () =>
      companies.map((c) => ({
        id: c.id,
        lat: Number(c.lat),
        lng: Number(c.lng),
        name: c.canonical_name,
        state: c.pipeline_state,
        nicCodes: visibleNicCodes(c.nic_codes ?? [], matchedNicCodeIds),
      })),
    [companies, matchedNicCodeIds],
  )

  return (
    <div className="rounded-xl overflow-hidden border border-border shadow-card bg-card">
      <MapContainer
        center={[center.lat, center.lng]}
        zoom={zoomForRadius(radiusKm)}
        className="h-[500px] w-full"
        scrollWheelZoom={true}
        zoomControl={true}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <FitBounds center={center} radiusKm={radiusKm} companies={companies} />

        <Marker position={[center.lat, center.lng]} icon={centerIcon}>
          <Popup>
            <span className="text-sm font-medium">Pincode centroid</span>
          </Popup>
        </Marker>

        <Circle
          center={[center.lat, center.lng]}
          radius={radiusMeters}
          pathOptions={{
            color: '#3B82F6',
            fillColor: '#3B82F6',
            fillOpacity: 0.06,
            weight: 1.5,
            dashArray: '6 4',
          }}
        />

        {markers.map((m) => (
          <Marker key={m.id} position={[m.lat, m.lng]} icon={companyIcon}>
            <Popup maxWidth={280} minWidth={200}>
              <div className="space-y-1.5">
                <Link
                  to={`/companies/${m.id}`}
                  className="font-semibold text-sm text-primary hover:underline block"
                >
                  {m.name}
                </Link>
                <div className="text-xs text-muted-foreground">
                  {m.state} &middot; {m.lat.toFixed(5)}, {m.lng.toFixed(5)}
                </div>
                {m.nicCodes.length > 0 && (
                  <div className="flex flex-wrap gap-1 pt-0.5">
                    {m.nicCodes.map((n) => (
                      <span
                        key={n.id ?? n.code}
                        title={n.description}
                        className="inline-block rounded bg-blue-50 px-1.5 py-0.5 text-[10px] font-medium text-blue-700"
                      >
                        {n.primary ? '★ ' : ''}{n.code}
                      </span>
                    ))}
                  </div>
                )}
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>

      <div className="flex items-center justify-between border-t border-border px-3 py-2 text-xs text-muted-foreground">
        <div className="flex items-center gap-4">
          <span className="inline-flex items-center gap-1.5">
            <span className="inline-block w-3 h-3 rounded-full bg-primary" />
            Companies ({companies.length})
          </span>
          <span className="inline-flex items-center gap-1.5">
            <span className="inline-block w-2.5 h-2.5 rounded-full bg-destructive" />
            Centroid
          </span>
          <span className="inline-flex items-center gap-1.5">
            <span className="inline-block w-3 h-3 rounded-full border border-primary/50 bg-primary/10" />
            Radius ({radiusKm} km)
          </span>
        </div>
        <span>OpenStreetMap</span>
      </div>
    </div>
  )
}
