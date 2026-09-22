# ADR-0009: Companies carry latitude / longitude as first-class columns
Date: 2026-09-21 · Status: Accepted

## Context
Doc 21 §7.1 specifies the map endpoint response with per-company `lat`/`lng` fields:
```
GET /api/v1/map/companies?pincode=641001&radius_km=5
  → { center, content: [ { id, canonical_name, lat, lng, tier, pipeline_state, primary_nic } ] }
```
The company field extension in doc 21 §4.1 does NOT include coordinates. Without coordinates, every company in a pincode plots on the same centroid and `radius_km` degrades to "pincodes whose centroid falls within the radius" — useless for the map screen's stated intent of showing geographic spread inside a city.

The alternative — deterministic jitter around the centroid — falsifies location data and hides real proximity signals from the user; it also makes future geocoding a lossy migration.

## Decision
Add two additional nullable columns to the V10 additive migration:
```
latitude   NUMERIC(9,6)
longitude  NUMERIC(9,6)
```
A partial index `idx_company_lat_lng` covers the not-null case for radius queries. When a company has no coordinates, the map service falls back to the pincode centroid so the row still appears — the user can then edit the record to attach real coordinates (via `PATCH /api/v1/companies/{id}` — see the extended `CompanyUpdateRequest`). No automatic geocoding is done in this scope; a follow-up track owns the batch geocoder.

Also captured here for symmetry: the `tier` field named in doc 21 §7.1 is **omitted** from the response because the qualification module has not been built. The map response contract instead exposes `pipeline_state` and `primary_nic_code_id`; adding `tier` when qualification lands is an additive shape change.

## Consequences
**Easy:** radius queries are meaningful the day companies get coordinates; the field is nullable so nothing breaks on rows that don't have them; the pincode-centroid fallback keeps the map non-empty in the meantime.
**Hard:** two more nullable columns on a wide table. Cheap.
**Given up:** the pretence that map behaviour is fully specified by doc 21 §7.1; the ADR is where the gap is recorded.

## Affected
- `backend/src/main/resources/db/migration/V10__company_sales_fields.sql` (latitude/longitude columns + `idx_company_lat_lng` partial index)
- `Company` entity + DTOs
- `CompanyMapService` (haversine over lat/lng; centroid fallback)
- Map response shape: `tier` omitted until qualification module ships.
