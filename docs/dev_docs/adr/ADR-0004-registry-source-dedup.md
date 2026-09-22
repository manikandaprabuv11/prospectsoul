# ADR-0004: Extended dedup rules for registry sources
Date: 2026-07 · Status: Accepted

## Context
Existing dedup (docs 01–18, Tech Spec §5) uses three rules in order: ① normalized phone ② website domain ③ normalized name + city.

The Kanchipuram MSME registry has zero phones, zero websites, and — because the whole file is one district — "city" is nearly constant. That collapses dedup to name-only matching against a file with 15,002 duplicated proprietor-style names ("MILK BUSINESS" × 119, "MANIKANDAN" × 109). Importing as-is would generate tens of thousands of false duplicate candidates and bury the Triage Queue.

## Decision
Add dedup rule ⓪ (highest priority): match on `source + source_reference` (the registry's own identity — for Udyam: `LG_ST_Code-LG_DT_Code-pincode-<hash(name+regdate)>`). This catches true re-imports of the same registry record with zero false positives.

Rules ① ② ③ continue to run for records without a `source_reference` match.

For sources where rule ③ is unreliable (registry sources in the same district), the rule tightens to `normalized name + pincode + address similarity ≥ threshold`. Threshold is Admin-configurable.

## Consequences
**Easy:** re-importing the same Kanchipuram file is idempotent; no false triage queue explosion; per-source dedup behavior is tunable.
**Hard:** dedup logic branches by source type — more code paths, more tests.
**Given up:** one universal dedup algorithm — replaced by source-aware dedup.

## Affected
- `imports/` — dedup service now consults source type
- New `companies.source_reference` column (ADR-0003)
- New Admin setting: address similarity threshold
- Test suite — new fixtures per source type
