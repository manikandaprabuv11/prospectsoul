# CLAUDE.md

Engineering rules for the **ProspectSoul** monorepo (Spring Boot backend + React frontend).
This file is the single entry point Claude Code (and any developer) must read before making a
change. It routes each operation to the document that governs it and summarizes the
non-negotiable rules from each.

---

## 0. Document Precedence — Read This First

There are two kinds of documentation in `docs/`, and they answer different questions:

| Tier | Location | Answers | Authority |
|---|---|---|---|
| **1 — Coding standards** | `docs/backend/`, `docs/frontend/` | **HOW** to write code: layering, naming, DTOs, error format, component rules, state rules, UI tokens | **Always authoritative for conventions.** Follow these first, for every change. |
| **2 — Project details** | `docs/project_details/` | **WHAT** to build: product scope, domain model, invariants, endpoint contract, screens, fields, phases, sprints | Authoritative for product/domain/contract questions. Consult whenever a feature, field, endpoint, entity, screen, role, or business rule is **not covered** by Tier 1. |
| **3 — This file** | `CLAUDE.md` | Router + summary of both tiers | Lowest. If this file disagrees with a Tier 1 or Tier 2 doc, **the detail doc wins** and this file must be corrected. |

**The rule in one line:** follow the `docs/backend` + `docs/frontend` standards first; when
those standards do not describe the thing you need (a feature, an endpoint, a field, a state, a
screen, a business rule), take it from `docs/project_details/` — and implement it using the
Tier 1 conventions.

**Conflict resolution:**

1. **Coding-standard conflict → Tier 1 wins.** The project-details docs were written earlier and
   carry *flagged assumptions*, not decisions. Two known cases:
   - **UI library:** the Tech Spec assumes Ant Design (assumption A2). The project actually uses
     **shadcn/ui + Tailwind + Radix** per `docs/frontend/UI_DESIGN.md`, and that is what is
     installed. Use shadcn/ui. Screen specs in the UI/UX Spec are library-agnostic — build the
     specified screen with shadcn/ui.
   - **Frontend folder layout:** the Tech Spec §1 shows a flat `api/ pages/ components/ lib/`
     layout. `docs/frontend/Frontend-File-Structure.md` (feature-first: `app/ features/ shared/
     services/ layouts/ auth/`) is the standard and wins for **new** code. See §2.0 for how to
     handle the existing scaffold.
2. **Product/scope conflict → the newest project-details doc wins.**
   **PRD v2.0 supersedes PRD v1.1.** The companion docs (Domain Model v1.0, Tech Spec v1.0,
   UI/UX Spec v1.0, Implementation Roadmap v1.0) remain valid **except where amended by
   PRD v2.0 §14** — read that section before treating any companion doc as current.
3. **Anything not written in the docs is not in scope.** Propose it; never invent an endpoint,
   field, table, or screen silently.

---

## 1. Which File to Read for Which Operation

Read the matching document(s) **before** starting. Do not guess conventions that are documented.

### Coding standards (Tier 1)

| You are about to... | Read this file first |
|---|---|
| Add/change a REST endpoint, DTO, controller, error format, pagination, versioning | [`docs/backend/API_DOCS.md`](docs/backend/API_DOCS.md) |
| Create/alter a table, column, index, constraint, relationship, Flyway migration, JPA mapping | [`docs/backend/DATABASE.md`](docs/backend/DATABASE.md) |
| Decide where a new backend class/package belongs | [`docs/backend/Backend-File-Structure.md`](docs/backend/Backend-File-Structure.md) |
| Handle auth/authorization, secrets, logging of sensitive data on the backend | [`docs/backend/SECURITY.md`](docs/backend/SECURITY.md) *(placeholder — until filled in, follow §35–41 of API_DOCS.md, §44–45 of DATABASE.md, and §3 of the Tech Spec)* |
| Call the backend from React, create/modify an API service, hooks, error normalization, auth headers | [`docs/frontend/API_INTEGRATION.md`](docs/frontend/API_INTEGRATION.md) |
| Create/modify a React component, decide shared vs feature, props, forms, loading/empty/error states | [`docs/frontend/COMPONENTS.md`](docs/frontend/COMPONENTS.md) |
| Decide where new frontend code belongs | [`docs/frontend/Frontend-File-Structure.md`](docs/frontend/Frontend-File-Structure.md) |
| Add/modify state (local, server, global, URL, form) | [`docs/frontend/STATE_MANAGEMENT.md`](docs/frontend/STATE_MANAGEMENT.md) |
| Build/change visuals, layout, spacing, colors, typography, responsive/a11y, pick a UI component | [`docs/frontend/UI_DESIGN.md`](docs/frontend/UI_DESIGN.md) |

### Project details (Tier 2)

| You need to know... | Read this file |
|---|---|
| Current product scope, Phase 1 vs 2 vs 3, import sources, NIC/ICP, scoring, geo search, call queue, new fields | [`docs/project_details/design-architecture/ProspectSoul-PRD-v2_0.md`](docs/project_details/design-architecture/ProspectSoul-PRD-v2_0.md) **(current PRD)** |
| The six-stage pipeline, evidence model, roles, search, reports, v1 data model, NFRs, out-of-scope list | [`docs/project_details/ProspectSoul-PRD-v1_1.md`](docs/project_details/ProspectSoul-PRD-v1_1.md) *(superseded where v2.0 differs)* |
| Business concepts, entity map, the three core flows, lifecycle state machine, **the 9 invariants**, vocabulary | [`docs/project_details/ProspectSoul-Domain-Model-v1_0.md`](docs/project_details/ProspectSoul-Domain-Model-v1_0.md) |
| Exact endpoint names/shapes, API conventions, normalization + dedup rules, background jobs, AI abstraction, file storage, config, test baseline | [`docs/project_details/ProspectSoul-Technical-Design-Spec-v1_0.md`](docs/project_details/ProspectSoul-Technical-Design-Spec-v1_0.md) |
| Screen-by-screen specs, routes, navigation map, shared components, UX rules | [`docs/project_details/ProspectSoul-UI-UX-Spec-v1_0.md`](docs/project_details/ProspectSoul-UI-UX-Spec-v1_0.md) |
| Sprint scope, acceptance criteria, seed data, Definition of Done, working rules | [`docs/project_details/ProspectSoul-Implementation-Roadmap-v1_0.md`](docs/project_details/ProspectSoul-Implementation-Roadmap-v1_0.md) |
| Architecture / workflow / lifecycle / ERD / screen-flow diagrams | [`docs/project_details/design-architecture/`](docs/project_details/design-architecture/) *(PNG diagrams)* |
| How to run the stack, ports, env vars, what is not configured yet | [`README.md`](README.md) |

Backend work almost always touches `API_DOCS.md` **and** `DATABASE.md` together, plus the Tech
Spec for the endpoint contract. Frontend work touches `Frontend-File-Structure.md` to place the
code, plus whichever of `COMPONENTS.md` / `API_INTEGRATION.md` / `STATE_MANAGEMENT.md` /
`UI_DESIGN.md` applies, plus the UI/UX Spec for the screen definition.

---

## 2. Repository Reality (verify before assuming)

Monorepo at the repo root:

```text
prospectsoul-backend/            # repo name; the product is "ProspectSoul"
├── backend/                     # Spring Boot 4.1.1, Java 21, Maven
│   └── src/main/java/com/vyoog/prospectsoul_backend/
├── frontend/                    # Vite + React 19 + TypeScript
├── docker/                      # keycloak/realm-export.json, postgres/init/
├── scripts/                     # start-dev.sh, stop-dev.sh
├── docker-compose.yml           # PostgreSQL, Keycloak, MinIO
├── docs/                        # Tier 1 + Tier 2 documentation
├── .env / .env.example          # infra + backend config
└── README.md
```

- **Backend base package is `com.vyoog.prospectsoul_backend`** (the docs write
  `com.vyoog.prospectsoul`). Follow the package that exists; do not rename it as a side effect
  of feature work.
- Migrations live in `backend/src/main/resources/db/migration/`; Hibernate runs
  `ddl-auto: validate`, so **Flyway is the only path to schema change**. No migrations exist yet.
- Backend config is `backend/src/main/resources/application.yaml` + root `.env` overrides.
- Commands (run them the way a developer would, from the right directory):
  `docker compose up -d` · `cd backend && mvn spring-boot:run` · `cd backend && mvn clean test` ·
  `cd frontend && npm run dev | npm run build | npm run lint`.
- Keycloak must be running before the backend starts (issuer URI resolved at start-up).

**Current state (foundation only — no product features yet):** no Flyway migrations, no domain
entities/endpoints, no application UI, no `PS_*` realm roles in `docker/keycloak/realm-export.json`
(only a `dev` user), no springdoc/Swagger UI (no release compatible with Spring Boot 4.1.1 yet),
and no frontend auth wiring (`frontend/src/api/client.ts` exposes `setAuthTokenProvider()`).
Re-check the README and the tree rather than trusting this paragraph.

### 2.0 Installed stack — use what is there

- **Backend:** Spring Boot 4.1.1 · Java 21 · Spring Web MVC, Data JPA, Validation, Security +
  OAuth2 Resource Server, Actuator · Flyway + PostgreSQL · Lombok · Testcontainers.
  `spring-ai-starter-model-openai` is on the classpath but **nothing AI is wired** — see §4.7.
- **Frontend:** React 19 · Vite · TypeScript (strict, `noUncheckedIndexedAccess`) ·
  TanStack Query (server state) · TanStack Table (data tables) · React Router 7 ·
  Tailwind v4 + shadcn/ui (Radix + CVA) · React Hook Form + Zod · Recharts · oxlint.
  **No Redux** — do not add a global store library.
- Import with the `@/` alias (`@/features/...`, `@/shared/...`), never deep relative paths.
- The existing frontend scaffold (`src/api/client.ts`, `src/lib/query-client.ts`, `src/routes/`,
  `src/pages/SetupCheckPage.tsx`, `src/components/ui/button.tsx`) predates the feature-first
  standard. **Do not build a second parallel structure and do not create a second HTTP client or
  query client.** Place new feature code per `Frontend-File-Structure.md`
  (`src/features/<name>/{api,components,hooks,pages,schemas,types}`), reuse the existing central
  client, and relocate scaffold files toward the standard layout only when the change you are
  already making requires it.

---

## 3. Backend Rules

### 3.1 Architecture (`Backend-File-Structure.md`)

- **Feature-first, technical layer second.** Each business capability (`company/`, `contact/`,
  `imports/`, `triage/`, `activity/`, `verification/`, `evidence/`, `research/`, `icp/`,
  `qualification/`, `export/`, `report/`, `admin/`, `ai/`) owns its own `controller/`, `service/`, `repository/`,
  `entity/`, `dto/{request,response}/`, `mapper/`, `specification/`. Never build one global
  `controller/`/`service/`/`repository/` for the whole app.
- `config/` holds app-wide Spring config only. `common/` holds genuinely shared infrastructure
  only (`exception/`, `dto/`, `audit/`, `pagination/`, `validation/`) — not a dumping ground; no
  generic `Utils.java`/`Helper.java`.
- `ai/` integrations stay isolated behind a capability/provider interface; never couple business
  services to a vendor SDK.
- Don't pre-create empty packages "to complete the tree" — create a subpackage when the feature
  actually needs it.

### 3.2 API Design (`API_DOCS.md` + Tech Spec §2, §4)

- Flow is strict: `Request DTO → Controller → Service → Repository → Entity/DB` in,
  `DB → Entity → Mapper → Response DTO → Controller` out.
- **Never expose or accept JPA entities through REST.** Use purpose-specific DTOs
  (`XCreateRequest`, `XUpdateRequest`, `XResponse`, `XDetailResponse`, `XSummaryResponse`).
- Base path `/api/v1`; plural resource names (`/companies`, not `/getCompanies`); UUIDs for all
  external IDs; standard HTTP verbs/status codes.
- JSON is `snake_case`; timestamps are UTC ISO-8601.
- List endpoints: server-side pagination `?page=0&size=25&sort=created_at,desc` returning the
  envelope `{ content, page, size, total_elements, total_pages }`; enforced max page size;
  allowlisted sort fields — never load-then-filter-in-Java.
- Errors: `application/problem+json` (RFC-7807) with `type`, `title`, `status`, `detail`, and a
  field-level `errors[]` for validation. Status catalogue: **400** validation · **401**
  unauthenticated · **403** role denied · **404** not found (alias IDs resolve instead, see §4.3)
  · **409** conflict (illegal state transition, duplicate already resolved) · **422** business
  rule (override without reason, cap exceeded) · **500**. Centralize in
  `GlobalExceptionHandler`; no repeated try/catch in controllers.
- **Endpoint names come from Tech Spec §4 plus the PRD v2.0 §14 additions — implement them
  exactly as named.** If a needed endpoint is missing, propose it; do not invent one.
- Authorization is enforced **server-side only** (Keycloak/OIDC JWT + `@PreAuthorize`) — a hidden
  frontend button is never a security control.
- **Every mutating service method writes an audit row** via an explicit
  `AuditService.record(entity_type, entity_id, actor, action, previous_state, new_state)` call —
  service-level and explicit, not magic entity listeners.
- Controllers: accept/return DTOs, validate, delegate — no business logic, no repository access,
  no vendor SDK calls. Services: business logic, transactions, orchestration, audit. Repositories:
  persistence only.
- Every public endpoint needs OpenAPI docs (once springdoc is installable — see §2) and
  controller/integration tests: success, validation failure, 404, 401, 403, 409, business-rule
  failure, pagination/filter/sort, and for mutations DB state + audit row.
- Never silently break an existing contract (field rename/removal/type change, status code
  change, pagination/error shape change) without impact analysis and a migration plan.

### 3.3 Database (`DATABASE.md` + PRD §10 / PRD v2.0 §5)

- Stack: PostgreSQL + Spring Data JPA/Hibernate + Flyway (`pg_trgm` for fuzzy search;
  `earthdistance`/`cube` or PostGIS for radius search; pgvector present but unused). Don't
  introduce another DB technology without an explicit decision.
- `snake_case` naming; UUID primary keys for externally exposed entities; real foreign keys for
  real relationships (`<referenced_table>_id`).
- `NOT NULL`/`UNIQUE` constraints must reflect actual business rules — enforce uniqueness in the
  DB, not via check-then-insert in Java.
- Index foreign keys, frequently filtered/sorted columns, and high-selectivity search fields —
  not every column. Radius queries must stay under 2s at 200k companies, so index accordingly.
- Never blanket `FetchType.EAGER` or `CascadeType.ALL` to paper over an N+1 or serialization
  problem; use fetch joins/EntityGraph/projections, and cascade only when the child's lifecycle
  is genuinely owned by the parent.
- `@Transactional` lives at the service layer, never in controllers, and must never stay open
  across a slow external/AI call.
- **Migrations are the only path to schema change:** `backend/src/main/resources/db/migration/`,
  `V<version>__<description>.sql`. **Never edit an applied migration** — add a new one. Treat
  `DROP TABLE`/`DROP COLUMN`/large `DELETE`/`ALTER COLUMN TYPE` as high-risk: inspect dependencies
  and data first, prefer phased (add → backfill → cutover → remove) on production tables.
- Before any DB change, inspect the existing schema, migrations, entities, repositories and DTOs —
  never guess or duplicate schema objects. The table inventory in PRD v1.1 §10 and the deltas in
  PRD v2.0 §5 are the intended shape; the ERD PNG in `design-architecture/` is the picture.
- Never commit secrets (DB passwords, connection strings) — env vars only.

---

## 4. Product Rules That Are Code Rules (from `docs/project_details/`)

These are not optional product colour: they are invariants the code must enforce. A UI-only check
is **not** enforcement (Roadmap §0.4).

### 4.1 The nine domain invariants (Domain Model §5)

1. **One company = one real-world entity.** Duplicates are merged, never left coexisting; merged
   IDs become aliases that still resolve.
2. **AI never changes state.** No auto-merge, no auto-qualify, no auto-disqualify. AI drafts,
   humans decide.
3. **No evidence, no claim.** An AI assertion without source URL + excerpt + capture date is
   persisted as `not_found` — enforced in code, not by trusting the model.
4. **Qualifications and merges are append-only.** History is never rewritten or overwritten.
5. **Every terminal decision carries a recorded reason** (disqualify, reject, override).
6. **Every record has lineage** — any field traces back to the import batch and row that
   introduced it; rows are persisted raw before processing, and nothing is silently dropped.
7. **The timeline is composed at query time** from the real tables (import rows, merges,
   activities, qualifications, audit log, exports). Never add a second event store to drift.
8. **Research activities only.** Sales outreach belongs in the CRM.
9. **Configuration over code.** ICP criteria, NIC ontology, import mappings, export layouts,
   reason lists, queue caps, score weights, staleness threshold — Admin-editable and versioned
   where they affect decisions. Never hard-code them.

### 4.2 Company lifecycle (enforce server-side)

```text
IMPORTED → TRIAGE → RESEARCH → QUALIFICATION → READY → EXPORTED
DISQUALIFIED / ARCHIVED — terminal, reachable from any state, always with a reason,
                          always still searchable
```

Exactly one state per company at all times. Allowed shortcut: `TRIAGE → QUALIFICATION`. Illegal
transitions return **409**; a terminal transition without a reason returns **422**. A previously
disqualified company re-surfacing from a new import must show its full timeline first.

### 4.3 Identity, dedup and normalization (Tech Spec §5, PRD v2.0 §4.1)

- Normalization is deterministic and exhaustively unit-tested — phone (strip `+91`/`0091`/leading
  `0`/punctuation → 10 digits; invalid kept + flagged), website → registered domain, name (trim,
  collapse, case-fold, strip configured stop-suffixes), city/state against the seeded reference
  list, NIC codes → array, pincode extracted from address, industrial-estate extraction.
- Dedup match order (first hit wins): **① CIN exact ② normalized phone ③ website domain
  ④ normalized name + pincode ⑤ normalized name + district**. `NOT_DUPLICATE` suppressed pairs
  are checked before flagging and must never be re-flagged.
- Merges write `merges` + `company_aliases`; `GET /api/v1/companies/{id}` on an alias returns
  **200** with the survivor and a `resolved_from` field — never 404.
- **Absolute rule:** rows matching an existing client or an open quotation merge into that record
  and are excluded from the Call Queue.

### 4.4 Roles and permissions (Tech Spec §3, PRD v2.0 §2)

Keycloak realm `vyoog`; SPA client `prospectsoul-web` (public + PKCE), API client
`prospectsoul-api` (bearer-only). Realm roles: `PS_ANALYST`, `PS_SALES_LEAD`, `PS_ADMIN`,
`PS_VIEWER`, `PS_COO`, `PS_TELECALLER`.

| Capability | ANALYST | SALES_LEAD | ADMIN | VIEWER | COO | TELECALLER |
|---|---|---|---|---|---|---|
| Import, triage, research, verify, qualify | ✓ | ✓ | ✓ | – | – | – |
| Tier override | ✓ | ✓ | ✓ | – | – | – |
| Create export | – | ✓ | ✓ | – | – | – |
| Configure ICP / templates / reasons / caps | – | – | ✓ | – | – | – |
| Read / search / reports | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ (own queue + detail) |
| Log call / visit outcomes | ✓ | ✓ | ✓ | – | – | ✓ |

Every role restriction needs a test that asserts the **403** path.

### 4.5 Qualification and export gates

- Qualification draft (`GET .../qualification/draft`) computes and persists **nothing**.
- `POST .../qualifications` is append-only and stamped with the ICP profile **version**; editing an
  ICP creates a new version and old qualifications keep displaying the version they were decided
  under.
- **422** when `final_tier ≠ computed_tier` without `override_reason`, and when `DISQUALIFIED`
  arrives without a `disqualification_reason_id`.
- Only Tier A/B/C companies reach the Ready pool; exporting records the snapshot and moves the
  companies to `EXPORTED`; exports are re-downloadable and byte-identical.
- Disqualification reasons are **deactivate-only** — never delete a row referenced by history.

### 4.6 Background work (Tech Spec §6) — no message broker

DB-backed queues + Spring scheduling, deliberately. Import processing is `@Async` with per-row
transactions so **one bad row never fails the batch**. Queue workers claim rows with
`FOR UPDATE SKIP LOCKED`, isolate per-item failures (mark `FAILED` with the error, keep going),
respect Admin-configured batch size and daily caps (**422** over cap), and expose visible progress.
A nightly `@Scheduled` job recomputes staleness flags against the configured threshold.

### 4.7 AI — Phase 2, not Phase 1

**PRD v2.0 moves the whole AI layer (Tech Spec §7) to Phase 2: zero LLM calls in Phase 1.** Do not
scaffold AI packages, providers, prompts or capabilities during Phase 1 work, and do not wire the
`spring-ai-starter-model-openai` starter that sits unused on the classpath. When Phase 2 starts:
capabilities are named and provider-agnostic (`AiCapability<I,O>`), routing (capability → provider
→ model → prompt version) is DB-backed configuration, **the backend fetches website text itself**
(Jsoup, timeouts, page allowlist, robots.txt) so every claim cites a real URL, output is parsed
defensively, every call is logged (capability, provider, model, prompt version, tokens, duration,
outcome), a `MockAiProvider` serves dev/CI, and **no live AI call ever runs in CI**.

### 4.8 Phase discipline and sprint discipline

- **Phase 1** (build now): import & merge from government/association sources · NIC ontology &
  ICP classification · `turnover_slab` / `phone_status` as CSV-updated fields · estate extraction ·
  **geographic search with adjustable radius** · two computed scores + `priority_rank` ·
  Call Queue · structured call/visit outcome capture · timeline · search · exports · admin config ·
  reports. **Phase 2:** GST/HLR automation, AI research + review queue. **Phase 3:** feedback
  loop, relationships, similarity, CRM sync. Anything in "Parked" is not to be built.
- **Never start the next sprint before the current sprint's acceptance criteria all pass** — no
  "I'll just scaffold it." Build **vertical slices** (backend + frontend + tests, demoable
  end-to-end), not layers.
- Build against shipped defaults (default ICP v1, default export template, generic CSV mapping,
  seeded reason list) instead of blocking on open items — real values arrive as configuration.
- **Definition of Done (every sprint):** acceptance criteria demonstrably pass against seed data ·
  unit tests for the business logic · a Testcontainers integration test for the sprint's main
  flow · audit rows for every new mutation · role restrictions enforced **and** tested ·
  OpenAPI matches reality · README updated · no TODOs referencing that sprint's scope.
- **Test priorities:** normalization, dedup matching, tier computation, evidence enforcement and
  the state machine are *the product* — test them exhaustively, every sprint.

### 4.9 Vocabulary — use these words everywhere (Domain Model §6)

Company (a prospect organization, not a customer) · Contact (a person, not a CRM lead) · Activity
(a research/verification touch, not sales outreach) · Evidence (claim + source + excerpt + date,
never an unsourced opinion) · Qualification (a tier decision under an ICP version, not a sales
stage) · Tier (A/B/C per the playbook) · Export (the handoff moment) · Batch (one import event
with lineage). Name entities, DTOs, endpoints, screens and UI labels with these terms.

---

## 5. Frontend Rules

### 5.1 Architecture (`Frontend-File-Structure.md`)

- Feature-first with a shared layer: `app/` (composition/router/providers/config),
  `features/<name>/` (`api/`, `components/`, `hooks/`, `pages/`, `schemas/`, `types/`), `shared/`
  (only genuinely reusable code), `layouts/`, `services/` (API client, storage), `auth/`,
  `styles/`, `assets/`. See §2.0 for the existing scaffold.
- Frontend `features/` map 1:1 to backend feature modules (`features/company/` ↔ backend
  `company/`) — keep the boundary aligned.
- Promote to `shared/` only on genuine reuse (first use feature-local, second evaluate, third
  extract). No catch-all `common/` or `utils.ts` dumping ground.
- A feature must not reach into another feature's internals — expose a deliberate `index.ts` when
  cross-feature access is needed. Use path aliases; avoid circular deps.
- TypeScript strictly; no `any` without a documented reason; never silence a type error to make
  the build pass.

### 5.2 API Integration (`API_INTEGRATION.md`)

- Strict flow: `Component → Custom Hook → Feature API Service → Central HTTP Client → Spring Boot`.
  **Components never call `fetch`/`axios` directly.**
- One central HTTP client owns base URL, auth header, timeout and error normalization. Base URL
  comes from `VITE_API_BASE_URL` — never hard-coded.
- Follow the backend contract exactly: same `/api/v1` paths, `snake_case` JSON (convert at the
  boundary if needed), same pagination params and response envelope, same `q`/filter param names,
  same RFC-7807 error shape.
- Normalize every backend status (400/401/403/404/409/422/500) centrally; the user sees the
  problem+json `detail`, never a stack trace and never a generic "something went wrong".
- The token is attached only by the central client (`Authorization: Bearer …`) via the auth layer's
  registered provider; refresh is centralized with no infinite retry loop.
- After a mutation, invalidate/update the affected query — don't hand-maintain duplicate copies of
  server data.
- Never put a secret in frontend code or a `VITE_` variable — everything shipped is public.

### 5.3 Components (`COMPONENTS.md`)

- One component, one responsibility (render / handle interaction / compose). No business logic and
  no API calls inside components.
- `shared/components/` stays domain-independent (no `Company`/`Contact` awareness); domain UI lives
  in `features/<name>/components/`.
- PascalCase names matching filenames; no vague names (`Thing.tsx`, `Helper.tsx`).
- Every API-driven view distinguishes **Loading / Success / Empty / Error** — never render an empty
  state while loading.
- Stable `key` from real IDs, never the array index. Extract complex JSX into subcomponents rather
  than nesting ternaries. Don't add `useMemo`/`useCallback`/`React.memo` without a measured reason.
- Search for an existing shared component before creating a new one. The UI/UX Spec §3 names the
  shared components this product needs: `StateBadge`, `TierBadge`, `EvidenceChip`/`EvidenceCard`,
  `TimelineItem`, `DataTable`, `StaleFlag`, `ReasonModal` — build and reuse these, don't
  re-implement variants per screen.

### 5.4 State Management (`STATE_MANAGEMENT.md`)

- Classify state first: **Local UI**, **Server**, **Global**, **URL**, **Form**. Never dump
  everything into one global store.
- `useState` for local UI (modal open, active tab). All server data goes through TanStack Query —
  never hand-roll fetch/cache/loading with raw `useEffect` + `useState`.
- Global state only for genuinely app-wide data (auth user, theme, preferences). Shareable state
  (search text, page, filters, sort, radius centre) belongs in the **URL** — `/companies/{id}` is
  THE company URL and every list screen must be deep-linkable.
- Never mutate state in place; never store derived values that can be computed.
- Persist only what has a stated requirement, and never persist tokens or secrets.

### 5.5 UI Design (`UI_DESIGN.md` + UI/UX Spec)

- **Inspect the existing screen and reuse its patterns before changing anything.** Change only
  what the requirement asks; don't redesign unrelated parts.
- Component library: **shadcn/ui** (Tailwind + Radix). Check shadcn/ui first, then existing custom
  components, and build new only as a last resort.
- Design tokens are fixed — reuse, don't invent:
  - Colors: Primary `#3B82F6`, Success `#10B981`, Warning `#F59E0B`, Error `#EF4444`,
    Info `#6366F1`, Neutral `#6B7280`
  - Font: Inter; sizes 12/14/16/18/20/24/30/36/48px; weights 400/500/600/700
  - Spacing: 0,4,8,12,16,20,24,32,40,48,56,64,80,96,112,128px
  - Radius none/2/4/8/12/16/full; shadows sm/md/lg/xl/2xl as documented
- Every screen handles Loading / Empty / Error, is accessible (semantic HTML, labels, keyboard
  nav, visible focus, contrast — no clickable `<div>` where a `<button>` belongs), and is
  responsive. Note the product bias: this is a **data-dense desktop-first internal tool**;
  analysts work on laptops, so responsive layout is required but mobile is not the primary target.
- **Build the named screens from the UI/UX Spec, not generic CRUD pages.** Routes are specified:
  `/` `/companies` `/companies/{id}` (Timeline is the default tab) `/companies/{id}/qualify`
  `/imports/new` `/imports/{id}` `/triage` `/research/queue` `/research/review` `/ready`
  `/exports` `/reports` `/settings`, plus the PRD v2.0 additions (Call Queue, Log Call/Visit,
  Estate Density, radius controls on Companies).
- UX rules that are requirements: terminal/destructive actions confirm **and** capture a reason ·
  empty states teach and offer the next action · errors show the problem+json `detail` verbatim ·
  queue screens show position ("4 of 37") and support keyboard advance · AI content is visually
  marked unverified until a human verifies it · skeletons on first load, silent background refresh.

---

## 6. Cross-Cutting Rules

- **The backend is the sole authority for security.** Hiding a button or route in the frontend is
  UX, never a control — every protected operation is enforced server-side too.
- **Never expose secrets** (DB credentials, API keys, JWT/client secrets) in code, logs, or
  frontend bundles/`VITE_` vars. The credentials in `.env.example` are development-only defaults
  and must never be reused anywhere else.
- **Never log** access tokens, `Authorization` headers, passwords, or PII payloads on either side.
- **Follow the existing contract, don't invent one.** Frontend types must match backend DTOs
  exactly (same fields, same `snake_case`, same pagination/error shape). Backend endpoints must
  match Tech Spec §4 + PRD v2.0 §14 exactly. Propose additions instead of adding them silently.
- **Inspect before you build.** Read the routed doc(s), then read the existing code/schema/
  components for the pattern already in use, and reuse it.
- **Make the smallest change that satisfies the requirement** — no unrelated refactors, no
  speculative abstractions, no premature optimization.
- **Ask when ambiguous.** Assume nothing about scope that is not written in the Tier 1 + Tier 2
  documents.
- **Nothing is "done" until it's verified:** backend unit + controller/integration tests pass;
  frontend typecheck, lint and build pass; loading/empty/error states handled; audit rows written;
  403/409/422 paths tested; no secrets committed; existing behavior elsewhere unbroken.

---

## 7. Standard Workflow for Any Change

```text
1. Identify the operation type (API, DB, component, state, UI, structure)
2. Route it via §1 and read the matching Tier 1 standard(s)
3. Look up WHAT to build in the Tier 2 project details (current PRD = v2.0, plus the
   Tech Spec contract, Domain Model invariants, UI/UX screen spec, sprint scope)
4. Inspect the existing code/schema/components for the established pattern and reuse it
5. Confirm the change is inside an approved requirement and the current phase/sprint —
   propose, don't invent, missing endpoints, fields or screens
6. Implement per the Tier 1 rules, enforcing the domain invariants in code
7. Add/update tests (incl. role 403, 409/422 business-rule, and problem+json shape)
8. Update OpenAPI + README (backend) / verify typecheck, lint and build (frontend)
9. Review against the routed doc's checklist and the sprint Definition of Done before
   calling the change complete
```
