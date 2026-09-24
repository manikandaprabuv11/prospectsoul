# ADR-0010: Enrichment Framework Architecture

**Status:** Accepted  
**Date:** 2026-09-23  
**Deciders:** Engineering  

## Context

ProspectSoul needs to enrich company and contact data from multiple external
sources (Google Places, website scraping, phone validation). The enrichment
system must:

1. Support multiple providers with a common interface (SPI pattern)
2. Never silently overwrite human-entered data
3. Maintain full evidence lineage (every fact traces to a source)
4. Be idempotent — repeated requests within a configurable window return cached results
5. Handle provider failures gracefully with retry and backoff
6. Track costs per provider call
7. Keep external API calls outside database transactions

## Decision

### Provider SPI

All enrichment providers implement `EnrichmentProvider` (key, capabilities, execute).
Providers return a `ProviderResult` containing facts, candidates, raw payload,
cost, and status. The framework handles persistence, evidence recording,
and fact application uniformly.

### Transaction boundary

The orchestrator separates the external call from DB writes:
1. **Transaction 1** — create the job row (RUNNING) + audit
2. **No transaction** — call `provider.execute()` (may be slow/external)
3. **Transaction 2** — record evidence, apply facts, update job, write activity

This prevents holding a DB connection open during slow external HTTP calls
(CLAUDE.md §3.3 rule: `@Transactional` must never stay open across external calls).

### Human-overwrite protection

When a provider returns a fact for a field that already has a value AND that value
was set by a human (not a previous enrichment), the fact is stored as an
`enrichment_candidate` with status PENDING instead of being applied directly. A
human must accept or reject the candidate through the UI.

### Idempotency

Each job stores an `input_hash` (SHA-256 of provider key + entity ID + sorted input).
Before creating a new job, the framework checks for a recent job with the same hash
within the provider's configured `idempotency_window_hours`. Callers can bypass
this with `force: true`.

### Evidence recording

Every successful provider result is recorded as an `evidence` row with the raw
JSON payload, source URL, and provider attribution. This satisfies domain
invariant #5: "No evidence, no claim."

### Cost tracking

Each provider config defines `cost_per_call_usd`. The actual cost (if returned by
the provider) or the configured default is recorded on every job.

## Deviations from vyoog-Diagnostic

- **Google Places:** Not found in vyoog-Diagnostic source. Implemented from
  doc 26 §5 specifications.
- **Phone validation:** Not found in vyoog-Diagnostic. Using libphonenumber-java
  per doc 25 §13.
- **Website scraping:** Only the crawl/fetch logic is ported from vyoog's
  `firecrawl.server.ts` and `website-intel.functions.ts`. The AI extraction
  (Gemini-based) is Phase 2 scope and not included.

## Consequences

- New tables: `enrichment_jobs`, `evidence`, `enrichment_candidates`, `provider_configs`
- New entity fields: 25 on `companies`, 8 on `contacts`
- Migrations V17–V22 are additive (no existing table modifications beyond ALTER ADD COLUMN)
- `ActivityType.ENRICHMENT` added to the existing enum
