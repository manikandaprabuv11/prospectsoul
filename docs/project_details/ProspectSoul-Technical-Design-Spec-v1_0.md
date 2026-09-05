# ProspectSoul — Technical Design Specification
**Version:** 1.0 · **Date:** July 2026 · **Companion to:** PRD v1.1, Domain Model v1.0
**Audience:** Developers / Claude Code. The PRD defines *what*; this defines *how*.

**Assumptions flagged for confirmation** (build proceeds on defaults; swap later is cheap):
- **A1:** Monorepo (backend + frontend in one repository)
- **A2:** UI component library = Ant Design (data-dense internal tooling). If Vyoog has a house library, substitute it — screen specs in the UI/UX doc are library-agnostic
- **A3:** Local dev uses Docker Compose (PostgreSQL, Keycloak, MinIO as S3 stand-in)

---

## 1. Repository & Project Structure

```
prospectsoul/
├── docker-compose.yml          # postgres, keycloak (with realm import), minio
├── backend/                    # Spring Boot 3.x, Java 21, Maven
│   └── src/main/java/com/vyoog/prospectsoul/
│       ├── config/             # security, S3, AI, async, OpenAPI
│       ├── common/             # errors, pagination, audit, normalization utils
│       ├── company/            # controller / service / repository / entity / dto per feature
│       ├── contact/
│       ├── imports/            # batches, rows, templates, async processing
│       ├── triage/             # duplicate candidates, merge
│       ├── activity/           # activities + attachments
│       ├── evidence/
│       ├── research/           # research queue, AI research capability
│       ├── icp/                # profiles, versions
│       ├── qualification/
│       ├── export/
│       ├── report/
│       ├── admin/              # reasons, settings, users mirror
│       └── ai/                 # provider-agnostic abstraction (Section 7)
│   └── src/main/resources/db/migration/   # Flyway: V1__baseline.sql, V2__..., etc.
└── frontend/                   # Vite + React 18 + TypeScript
    └── src/
        ├── api/                # typed client per resource (fetch wrappers)
        ├── auth/               # keycloak-js integration, role guards
        ├── components/         # shared: DataTable, EvidenceCard, StateBadge, TimelineItem
        ├── pages/              # one folder per screen (see UI/UX spec)
        └── lib/                # query client, formatting, constants
```

- Database migrations: **Flyway**, SQL files, never edit an applied migration.
- API documentation: springdoc-openapi; `/swagger-ui` enabled in non-prod.
- Frontend state/server-cache: **TanStack Query**; routing: **React Router**; no Redux.

---

## 2. Conventions

**Base path:** `/api/v1`
**IDs:** UUID everywhere. **Times:** UTC ISO-8601. **JSON:** snake_case (matches PRD field names).

**Pagination (all list endpoints):** request `?page=0&size=25&sort=created_at,desc` → response envelope:
```json
{ "content": [...], "page": 0, "size": 25, "total_elements": 1420, "total_pages": 57 }
```

**Errors:** RFC-7807 `application/problem+json`:
```json
{ "type": "https://prospectsoul/errors/validation", "title": "Validation failed",
  "status": 400, "detail": "phone must be 10 digits",
  "errors": [ { "field": "primary_phone", "message": "must be 10 digits" } ] }
```
Error catalogue: 400 validation · 401 unauthenticated · 403 role denied · 404 not found (alias IDs redirect, see §4.1) · 409 conflict (state transition not allowed, duplicate resolution already made) · 422 business rule (e.g. override without reason).

**Audit:** every mutating service method writes an `audit_log` row (entity_type, entity_id, actor from JWT, action, previous_state, new_state). Implemented as a service-level `AuditService.record(...)` call — explicit, not magic listeners.

---

## 3. Security & Authentication

- **Keycloak, OIDC.** Realm `vyoog`, client `prospectsoul-web` (public, PKCE) for the SPA; client `prospectsoul-api` (bearer-only) for the backend. Roles as realm roles: `PS_ANALYST`, `PS_SALES_LEAD`, `PS_ADMIN`, `PS_VIEWER`, `PS_COO`.
- Backend: Spring Security resource server (`spring-boot-starter-oauth2-resource-server`), JWT validation against Keycloak issuer; roles mapped from `realm_access.roles`; method security via `@PreAuthorize`.
- Frontend: `keycloak-js` — redirect login, silent refresh, token attached by the API client.
- **Role → capability matrix:**

| Capability | ANALYST | SALES_LEAD | ADMIN | VIEWER | COO |
|---|---|---|---|---|---|
| Import, triage, research, verify, qualify | ✓ | ✓ | ✓ | – | – |
| Tier override | ✓ | ✓ | ✓ | – | – |
| Create export | – | ✓ | ✓ | – | – |
| Configure ICP/templates/reasons/caps | – | – | ✓ | – | – |
| Read/search/reports | ✓ | ✓ | ✓ | ✓ | ✓ |

- Dev realm JSON (`docker/keycloak/realm-export.json`) with the five roles and one seed user per role ships in the repo.

---

## 4. REST API Contract

Endpoints Claude Code must implement exactly as named. Request/response shapes follow the PRD data model; representative bodies shown where non-obvious.

### 4.1 Companies & Contacts
```
GET    /api/v1/companies                 # q= free text (name/phone/email/domain/contact), + filters:
                                         # state, tier, cluster, city, verification, stale, tag, source
GET    /api/v1/companies/{id}            # alias IDs return 200 with the survivor + "resolved_from" field
POST   /api/v1/companies                 # manual entry — creates a batch of one internally
PATCH  /api/v1/companies/{id}            # editable fields only; core-field edit drops verification
POST   /api/v1/companies/{id}/verify     # marks record VERIFIED (actor, timestamp)
POST   /api/v1/companies/{id}/state      # { "state": "DISQUALIFIED"|"ARCHIVED", "reason_id": ..., "note": ... }
GET    /api/v1/companies/{id}/timeline   # composed stream, newest first, paginated
                                         # item: { at, type, actor, summary, ref: {entity, id}, payload }
GET    /api/v1/companies/{id}/contacts
POST   /api/v1/companies/{id}/contacts
PATCH  /api/v1/contacts/{id}             # includes re-linking: { "company_id": newId } closes old association
```

### 4.2 Imports
```
POST   /api/v1/imports                   # multipart: file + template_id (+source enum) → 202 {batch_id}
GET    /api/v1/imports                   # batch list with counts
GET    /api/v1/imports/{id}              # status: PROCESSING|COMPLETE|FAILED + row counts
GET    /api/v1/imports/{id}/rows?outcome=rejected|merged|created|pending
GET/POST/PATCH /api/v1/import-templates  # column-mapping config: { source, mappings: [{column, field}] }
```
Processing is async (§6). Rows are persisted raw before processing (invariant 6).

### 4.3 Triage
```
GET    /api/v1/triage/queue              # duplicate candidates + unreviewed clean records
POST   /api/v1/duplicates/{id}/resolve
       { "action": "MERGE"|"NOT_DUPLICATE"|"REJECT",
         "field_decisions": { "canonical_name": "existing", "website_domain": "incoming", ... },
         "reason": "..." }               # reason required for REJECT
```
MERGE writes `merges` + `company_aliases`; NOT_DUPLICATE persists the suppressed pair.

### 4.4 Research & Activities
```
POST   /api/v1/research/queue            # { "company_ids": [...] } → 202; enforces daily cap (422 if exceeded)
GET    /api/v1/research/queue?status=queued|processing|complete|failed
GET    /api/v1/review/queue              # companies with unverified AI research
GET    /api/v1/companies/{id}/activities
POST   /api/v1/companies/{id}/activities # { type: MANUAL_NOTE|CALL|VISIT|VERIFICATION, content, ... }
POST   /api/v1/activities/{id}/attachments   # multipart → S3, returns attachment record
POST   /api/v1/activities/{id}/verify    # analyst accepts an AI_RESEARCH activity (optionally with corrections)
GET    /api/v1/activities/{id}/evidence
```

### 4.5 ICP & Qualification
```
GET/POST        /api/v1/icp-profiles
POST            /api/v1/icp-profiles/{id}/versions    # editing = new version
GET             /api/v1/companies/{id}/qualification/draft
   # returns: hard-filter results, per-criterion AI suggestion { answer, rationale, evidence_ids[] },
   # computed_tier — nothing persisted
POST            /api/v1/companies/{id}/qualifications
   { "icp_profile_version_id": ..., 
     "criterion_results": [ { "criterion_key", "answer", "evidence_ids": [] } ],
     "final_tier": "A"|"B"|"C"|"DISQUALIFIED",
     "override_reason": null, "disqualification_reason_id": null }
   # 422 if final_tier ≠ computed_tier and override_reason absent
   # 422 if DISQUALIFIED without reason_id
GET             /api/v1/companies/{id}/qualifications  # full append-only history
```

### 4.6 Export
```
GET    /api/v1/ready-pool                # READY companies + filters; stale flagged
POST   /api/v1/exports                   # { company_ids | filter_snapshot, destination_label, template_id }
                                         # → export record + CSV download URL; companies → EXPORTED
GET    /api/v1/exports                   # log
GET    /api/v1/exports/{id}/file         # re-download
GET/POST/PATCH /api/v1/export-templates  # column layout config; ships with "default" template
```

### 4.7 Reports & Admin
```
GET  /api/v1/reports/source-funnel?from=&to=     # per source: imported→deduped→qualified(by tier)→exported
GET  /api/v1/reports/pipeline                    # counts by state, weekly movement, aging, queue depths
GET  /api/v1/reports/quality                     # completeness distribution, verified %, stale count
GET  /api/v1/reports/qualification-outcomes      # tier distribution, reasons ranked, override rate
GET/POST/PATCH /api/v1/admin/disqualification-reasons
GET/PATCH      /api/v1/admin/settings            # research batch size, daily AI cap, staleness threshold
GET            /api/v1/admin/users               # Keycloak mirror
```

---

## 5. Normalization Rules (Deterministic, Unit-Tested)

| Field | Rule |
|---|---|
| Phone | strip `+91`, `0091`, leading `0`, spaces, `-()`; valid = 10 digits; mobile = starts 9/8/7/6; invalid kept + flagged |
| Website | lowercase; strip scheme, `www.`, path/query → registered domain |
| Name | trim, collapse whitespace, case-fold, strip punctuation, strip configurable stop-suffixes (`pvt ltd`, `private limited`, `llp`, `industries`, `enterprises`, …) |
| City/State | match against seeded reference list (aliases: `Madras→Chennai`); unmatched kept + flagged |

**Dedup match rules (in order, first hit wins):** ①  same normalized phone ② same website domain ③ same normalized name + city. Suppressed pairs (`NOT_DUPLICATE`) are checked before flagging.

---

## 6. Background Jobs (No Message Broker)

DB-backed queues + Spring scheduling. Deliberately simple; a broker is not justified at this scale (NFR: 100k companies).

| Job | Trigger | Behavior |
|---|---|---|
| **Import processor** | `@Async` on batch upload | Row-by-row normalize + dedup-check inside per-row transactions; batch status/progress updated; one bad row never fails the batch |
| **Research worker** | `@Scheduled` poll of `research_queue` (every 30s) | Claims up to `batch_size` queued rows (`FOR UPDATE SKIP LOCKED`), calls AI capability per company, writes AI_RESEARCH activity + evidence; per-company failure isolation → status `FAILED` with error, others proceed; respects daily cap counter |
| **Staleness flagger** | `@Scheduled` nightly | Recomputes stale flags against configured threshold |

---

## 7. AI Abstraction (ADR-008 Pattern, Same as CustSoul)

```java
public interface AiCapability<I, O> { O execute(I input); String capabilityName(); }

// v1 capabilities:
CompanyResearchCapability   // input: company + fetched website text
                            // output: ResearchDraft { summary, cluster_guess, size_signals,
                            //   trader_or_manufacturer, claims: [ {claim, source_url, excerpt} ] }
QualificationSuggestCapability  // input: company + verified evidence + ICP version criteria
                            // output: per-criterion { answer, rationale, evidence_ids }
```
- Routing: `AiProviderRegistry` maps capability → provider+model+prompt_version from configuration (DB-backed settings, not code). v1 provider: Anthropic Claude via REST.
- **Website fetching is done by the backend**, not the AI: `WebsiteFetchService` (Jsoup, 15s timeout, max N pages: home/about/products/contact/certifications, robots.txt respected). Fetched text + source URLs are passed to the AI so every claim can cite a real URL Claude Code stores verbatim.
- AI returns structured JSON (prompted, parsed defensively). A claim without a matching source URL + excerpt is downgraded to `not_found` before persistence — invariant 3 enforced in code, not by trust.
- Every AI call logged: capability, provider, model, prompt_version, tokens, duration, success/failure.

---

## 8. File Storage

- S3 (Mumbai) in prod; MinIO locally, same client (AWS SDK, endpoint override).
- Key scheme: `attachments/{company_id}/{attachment_id}/{filename}`; export files: `exports/{export_id}.csv`.
- Allowed types: pdf, png, jpg, xlsx, docx; max 20 MB (both configurable). Downloads via short-lived pre-signed URLs.

---

## 9. Configuration & Environments

- `application.yml` + env overrides: `DB_URL`, `KEYCLOAK_ISSUER_URI`, `S3_*`, `ANTHROPIC_API_KEY`, `AI_DAILY_CAP`, `RESEARCH_BATCH_SIZE`, `STALENESS_DAYS`.
- Profiles: `local` (compose stack, MinIO, permissive CORS), `prod`.
- Seed migration (`V2__seed.sql`): default ICP profile v1 (playbook criteria as JSON), default export template, disqualification-reason starter list, city reference list, generic + IndiaMART import templates.

## 10. Testing Baseline

- Unit: normalization rules, dedup matcher, tier computation, evidence-enforcement — exhaustive, these are the product.
- Integration: Testcontainers (PostgreSQL); full import→triage→research(mock AI)→qualify→export flow test.
- AI capabilities tested against recorded fixtures; a `MockAiProvider` ships for local/dev and CI.
- Detailed per-sprint test strategy lives in the Implementation Roadmap.
