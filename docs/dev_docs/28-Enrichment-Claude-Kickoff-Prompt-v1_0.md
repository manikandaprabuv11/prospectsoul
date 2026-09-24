<!--
Document: 28-Enrichment-Claude-Kickoff-Prompt-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Audience: Claude Code
-->
# ProspectSoul — Phase 1 Enrichment: Claude Kickoff Prompt

You are the senior full-stack engineer implementing the ProspectSoul Phase 1 Enrichment scope.

---

## AUTHORITATIVE DOCUMENTS

Before coding, read in order:

1. ProspectSoul PRD v1.1 (and PRD v2.0 amendments where applicable)
2. ProspectSoul Domain Model v1.0
3. ProspectSoul Technical Design Specification v1.0
4. ProspectSoul UI/UX Specification v1.0
5. ProspectSoul Implementation Roadmap v1.0
6. Technical-Team Developer Guide / CLAUDE.md
7. Docs 01–18 (Company Management vertical slice — already implemented)
8. Docs 19–22 (Sales Intelligence Extension — implemented, with the current-state deltas listed in doc 25 §3)
9. **Doc 25 — Enrichment Requirements**
10. **Doc 26 — Enrichment Technical Specification**
11. **Doc 27 — Enrichment Implementation Plan**

Document hierarchy must be respected. If documents conflict, **do not silently choose one — identify the conflict, stop, and ask.**

---

## TWO CODEBASES YOU MUST INSPECT

Unlike prior scopes where you inspected only the ProspectSoul target repo, this scope requires inspecting **both**:

**SOURCE** — `vyoog-Diagnostic` zip (Lovable + Supabase application):
- Firecrawl integration — file paths, options passed, extraction patterns
- Google Places / Business integration — API surface used, matching logic, field selection
- Phone validation — which library or API, format handling, status semantics
- API secret management
- Error handling patterns

**TARGET** — ProspectSoul repository (Spring Boot + PostgreSQL + React):
- Current `companies` and `contacts` tables (which fields from doc 26 §3.4 / §3.5 already exist — do not re-add)
- Current `evidence` table (confirm shape before extending)
- Current `research/` module and its queue (must remain working through a bridging shim)
- Current Company Timeline composition
- Current authentication / Keycloak roles
- Existing tests
- Object-storage abstraction

---

## THE CENTRAL DIRECTIVE

From product:

> "I wanna do the company details scraping like we did in the file code base."

This is a **port**, not a redesign. The scraping behavior in vyoog-Diagnostic is the target behavior. Your job is to relocate that behavior into ProspectSoul's architecture (Spring Boot modules, Postgres, JPA, Flyway, React, Keycloak) while preserving what it does and how it decides.

Preserving functionality means:
- If vyoog's Google matcher tries `name+pincode` then `name+city`, your port does the same in the same order
- If vyoog's Firecrawl call uses specific options, your port passes the same options
- If vyoog's phone validator classifies certain formats as VALID and others as INVALID, your port classifies them the same way
- Any behavioral deviation requires an ADR with justification

Changes that ARE expected:
- Location of the code (`enrichment/` module in ProspectSoul, not vyoog's src/)
- Architectural shape (provider abstraction, jobs table, evidence trail)
- Persistence (PostgreSQL via Flyway, not Supabase)
- Frontend (existing React shell, not Lovable UI)
- Authentication (Keycloak, not Supabase auth)

---

## OBJECTIVE

Implement Phase 1 across the existing:
- Spring Boot backend
- React frontend
- PostgreSQL database

Six sequential tracks:

- **E1** — Enrichment framework (module, jobs table, evidence extension, orchestrator, bridging shim)
- **E2** — Google Places provider
- **E3** — Website (Firecrawl) provider
- **E4** — Phone provider
- **E5** — Company Detail Enrich UI + timeline + evidence viewer + candidates panel
- **E6** — Bulk Enrich + jobs list + Admin provider config

Full scope, criteria, and required tests in doc 27.

**Never start track N+1 until track N's acceptance criteria all pass.**

---

## FIRST: PHASE 0 AUDIT

Before changing any code, produce three artifacts:

1. **Gap assessment against target** — table per planned change: new / partially present / already present
2. **Migration map against source** — table per vyoog capability: source files, target location, disposition (Port / Discard / Defer), notes
3. **Functional-equivalence harness plan** — how you'll prove per-provider behavior parity with vyoog

Do not begin Track E1 until these three artifacts are reviewed.

**Reuse existing code wherever appropriate.** Do not duplicate the app shell, audit service, Keycloak wiring, timeline composition, or object-storage abstraction.

---

## TWELVE CRITICAL CONSTRAINTS

Break any of these and the work is not accepted:

1. **Do not recreate `companies`, `contacts`, `evidence`, or any other existing table.** Additive `ALTER TABLE` migrations only. Never modify an applied migration.
2. **Existing `research/` AI flow continues to work unchanged.** Bridging shim required. Migration to `enrichment_jobs` is a Phase 1.5 follow-up, not Phase 1.
3. **Company Map stays owned-only.** Do NOT reintroduce Google Places / OpenStreetMap / Overpass to the map. Google Places is enrichment-only.
4. **Enrichment is a first-class module.** External API calls do NOT live inside `CompanyService` or `research/`. They live in `enrichment/<provider>/` behind the `EnrichmentProvider` SPI.
5. **A fact must trace to at least one evidence row.** No fact without provenance.
6. **Enrichment never silently overwrites human-entered data.** Human-set field → provider result becomes a candidate in `enrichment_candidates`. Empty field → provider result may become the fact silently.
7. **Extract deterministically only.** No AI interpretation of website content in Phase 1 (pain points, positioning, sentiment — all Phase 2).
8. **No silent contact creation from website scraping.** Emails and phones detected on a website land as `enrichment_candidates`, not new `contacts` rows.
9. **API keys are server-side only.** `GOOGLE_PLACES_API_KEY`, `FIRECRAWL_API_KEY`, `PHONE_PROVIDER_API_KEY` — never in a frontend bundle or network response to the browser.
10. **Preserve vyoog-Diagnostic behavior.** Deviations require an ADR. Reproduce Firecrawl options, Google matching ladder, phone validator choice, extraction regexes as they exist in source.
11. **Do not port Phase 2 capabilities** from vyoog — benchmarks, pain library, observations, scenarios, product modifiers, narrative reports, PDF, diagnostic UI, diagnostic scoring, diagnostic auth, Supabase migrations, Lovable UI shell. Even if it looks easy, defer.
12. **Every behavior change to existing code requires an ADR.** Generalizing `research_queue` → `enrichment_jobs` (via bridging shim) is one such change; ADR-0001 is written as part of Track E1. New code that follows the specs as written does not need an ADR.

---

## BACKEND RULES

- Feature-first modular architecture: `controller / service / repository / entity / dto / mapper / spi`
- Controllers never expose JPA entities
- UUID ids, `/api/v1`, snake_case JSON, RFC-7807 errors
- Server-side authorization; test 403 paths for non-authorized roles on every mutation
- `AuditService` / `audit_log` for every mutation
- Flyway migrations, immutable once applied
- Rate limiting backed by database counters (survives restarts)
- Idempotency backed by database, not in-memory

---

## ADR ON DEMAND

**Do not create `/adr` at project setup.**

Create it **when you actually change existing code, existing flow, or deviate from docs 01–28 during implementation.** At that moment:

1. Create `/adr` at repository root if it does not exist
2. Create `/adr/README.md` with the template below on first creation
3. Add one ADR file per behavior-changing decision, named `ADR-NNNN-<short-slug>.md`
4. Number sequentially starting at `0001`

**When to write an ADR:**
- You change existing code paths, existing flows, or existing behavior
- You deviate from any spec in docs 01–28
- You introduce a new invariant or business rule not already in the specs
- You reject or override a specified approach for a documented reason
- You deviate from vyoog-Diagnostic behavior when porting a provider

**Not required for:**
- New code that follows the specs as written
- Cosmetic refactors within a single new module
- Bug fixes in your own new code

**ADR template:**

```markdown
# ADR-NNNN: <Title>
Date: YYYY-MM-DD
Status: Proposed | Accepted | Superseded by ADR-XXXX

## Context
What existed before, and what forced a decision.

## Decision
What you're doing.

## Consequences
What this makes easy. What it makes hard. What was given up.

## Affected
Files / migrations / APIs / specs touched.
```

**Expected ADRs for Phase 1:**
- **ADR-0001** — Generalize `research_queue` → `enrichment_jobs`; bridging shim keeps existing AI flow working; full migration deferred to Phase 1.5. **Required for Track E1.**
- Any other ADR emerges from deviations discovered in Phase 0 or during a track.

---

## TESTS REQUIRED

### Unit
- `ProviderRegistry` resolves providers by key
- `IdempotencyService` stable input hashes; cache hit/miss
- `RateLimiter` token-bucket behavior including database persistence
- `EvidenceRecorder` inline JSONB vs object-storage split by size
- `FactUpdater` respects the human-overwrite rule
- Google Places field mapper: full response, missing fields
- Website extractors: regex patterns on fixture pages
- Phone validator: E.164 normalization, IN + international
- Cost tracker accumulation

### Integration (Testcontainers + mocked providers)
- Single company × 3 providers → 3 jobs, 3 evidence rows, N facts, 3 timeline events
- Idempotency: within window → 0 API calls, cached returned
- `force: true` bypasses idempotency
- Human-set field → candidate, not silent overwrite
- Candidate accept → fact updated, new evidence row
- Bulk enrich with filter → correct set, cap enforced, rate limits respected
- Provider transient failure → retried per policy
- Provider permanent failure → FAILED, no partial writes
- 403 paths for non-authorized roles
- **Existing `research/` AI flow continues to work** (regression assertion — critical)

### Functional-equivalence tests (vs vyoog-Diagnostic)
- Fixture company × Google Places → same fact set as vyoog output
- Fixture URL × Firecrawl → same fact + candidate set
- Fixture phones × phone provider → same normalization + status

### AI
- No changes; existing MockAiProvider baseline unchanged

---

## DOCUMENTATION

Update:
- OpenAPI (all new endpoints)
- README (enrichment section: how to enrich, how to view results, how to configure providers)
- `/adr` if any existing-flow changes were made (ADR-0001 at minimum)

---

## DEFINITION OF DONE

Do not claim completion until every criterion in doc 27's Release Gate passes, with evidence per criterion.

---

## FINAL RESPONSE FORMAT

1. Phase 0 artifacts: gap assessment, migration map, equivalence harness plan
2. Files created / changed
3. Database migrations added
4. Backend implementation summary per track (E1 → E6)
5. Frontend implementation summary per relevant track (E5, E6)
6. Provider equivalence results (per provider: PASS with fact-set match, or DIFFER with ADR)
7. Tests and exact results
8. Acceptance criteria PASS / FAIL / BLOCKED per criterion, per track
9. ADRs written (one-line summary each) — ADR-0001 always expected
10. Existing `research/` AI flow regression status
11. Specification deviations (flag with reason if any)
12. Remaining work

**Do not claim PASS without evidence.**

---

## SCOPE CONTROL — HARD LIMIT

Do not implement in Phase 1:
- Any Phase 2 capability (benchmarks, pain library, observations, scenarios, product modifiers, reports, PDF, diagnostic UI, diagnostic scoring)
- AI interpretation of website content
- Automatic contact creation from website scraping
- Scheduled/automated enrichment sweeps
- Providers beyond Google Places, Website, Phone
- Adding external results back to Company Map
- Migrating `research/` onto `enrichment_jobs` (Phase 1.5)
- Changes to Company Default Filter Settings, Companies List rendering, import processing, ICP Qualification, Tier engine, or scoring
- Company relationships

Start by inspecting both codebases and the authoritative documents. Then produce the Phase 0 artifacts. Then start Track E1.
