# CLAUDE.md

Engineering rules for the ProspectSoul codebase (Spring Boot backend + React frontend). This file is the single entry point Claude Code (and any developer) must read before making a change. It routes each type of operation to the detailed rule document that governs it, and summarizes the non-negotiable rules from each of those documents.

The detailed rule documents live under `docs/backend/` and `docs/frontend/` and remain the authoritative source. This file must never contradict them — if this summary and a detail doc disagree, the detail doc wins and this file should be corrected.

---

## 0. Which File to Read for Which Operation

Read the matching document(s) **before** starting the operation. Do not guess conventions that are already documented.

| You are about to... | Read this file first |
|---|---|
| Add/change a REST endpoint, DTO, controller, error format, pagination, versioning | [`docs/backend/API_DOCS.md`](docs/backend/API_DOCS.md) |
| Create/alter a table, column, index, constraint, relationship, Flyway migration, JPA mapping | [`docs/backend/DATABASE.md`](docs/backend/DATABASE.md) |
| Decide where a new backend class/package belongs (controller/service/repository/entity/dto/mapper/specification) | [`docs/backend/Backend-File-Structure.md`](docs/backend/Backend-File-Structure.md) |
| Handle authentication/authorization, secrets, logging of sensitive data on the backend | [`docs/backend/SECURITY.md`](docs/backend/SECURITY.md) *(placeholder — also follow §35–41 of API_DOCS.md and §44–45 of DATABASE.md until filled in)* |
| Call the backend from React, create/modify an API service, hooks, error normalization, auth headers | [`docs/frontend/API_INTEGRATION.md`](docs/frontend/API_INTEGRATION.md) |
| Create/modify a React component, decide shared vs feature component, props, forms, loading/empty/error states | [`docs/frontend/COMPONENTS.md`](docs/frontend/COMPONENTS.md) |
| Decide where new frontend code belongs (`app/`, `features/`, `shared/`, `services/`, `auth/`, `layouts/`) | [`docs/frontend/Frontend-File-Structure.md`](docs/frontend/Frontend-File-Structure.md) |
| Add/modify state (local, server, global, URL, form state) | [`docs/frontend/STATE_MANAGEMENT.md`](docs/frontend/STATE_MANAGEMENT.md) |
| Build/change a screen's visuals, layout, spacing, colors, typography, responsive/accessibility behavior, pick a UI library component | [`docs/frontend/UI_DESIGN.md`](docs/frontend/UI_DESIGN.md) |
| Understand overall system architecture | [`docs/project_details/ARCHITECTURE.md`](docs/project_details/ARCHITECTURE.md) *(placeholder)* |
| Set up a local dev environment | [`docs/project_details/SETUP.md`](docs/project_details/SETUP.md) *(placeholder)* |
| Follow contribution/PR conventions | [`docs/project_details/CONTRIBUTING.md`](docs/project_details/CONTRIBUTING.md) *(placeholder)* |

Backend work almost always touches both `API_DOCS.md` and `DATABASE.md` together — a new endpoint that persists data must satisfy both. Frontend work touches `Frontend-File-Structure.md` to place the code, plus whichever of `COMPONENTS.md` / `API_INTEGRATION.md` / `STATE_MANAGEMENT.md` / `UI_DESIGN.md` matches what's being built.

---

## 1. Backend Rules

### 1.1 Architecture (`Backend-File-Structure.md`)

- **Feature-first, technical layer second.** Each business capability (`company/`, `contact/`, `imports/`, `triage/`, `activity/`, `evidence/`, `research/`, `icp/`, `qualification/`, `export/`, `report/`, `admin/`, `ai/`) owns its own `controller/`, `service/`, `repository/`, `entity/`, `dto/{request,response}/`, `mapper/`, `specification/`. Never build one global `controller/`/`service/`/`repository/` for the whole app.
- `config/` holds app-wide Spring config only. `common/` holds genuinely shared infrastructure only (`exception/`, `dto/`, `audit/`, `pagination/`, `validation/`) — it is not a dumping ground; avoid generic `Utils.java`/`Helper.java`.
- `ai/` integrations stay isolated behind a Provider interface; never couple business services directly to a vendor SDK.
- Don't pre-create empty packages/folders "to complete the tree" — create a subpackage only when the feature actually needs it.

### 1.2 API Design (`API_DOCS.md`)

- Flow is strict: `Request DTO → Controller → Service → Repository → Entity/DB` in, `DB → Entity → Mapper → Response DTO → Controller` out.
- **Never expose or accept JPA entities through REST.** Always use purpose-specific DTOs (`XCreateRequest`, `XUpdateRequest`, `XResponse`, `XDetailResponse`, `XSummaryResponse`).
- Base path is `/api/v1`; plural resource names (`/companies`, not `/getCompanies`); UUIDs for external IDs; standard HTTP verbs/status codes.
- JSON is `snake_case`; timestamps are UTC ISO-8601.
- List endpoints: server-side pagination (`?page=&size=&sort=`), enforced max page size, allowlisted sort fields — never load-then-filter-in-Java.
- Errors: `application/problem+json` (RFC-7807). Use the documented status catalogue (400/401/403/404/409/422/500) consistently; centralize in `GlobalExceptionHandler`, no repeated try/catch in controllers.
- Authentication/authorization is enforced **server-side only** (Keycloak/OIDC JWT + `@PreAuthorize`) — never trust a hidden frontend button as security.
- Mutations must be auditable via `AuditService.record(...)` where required.
- Controllers: accept/return DTOs, validate, delegate, no business logic, no direct repository access, no vendor SDK calls. Services: business logic, transactions, orchestration, audit calls. Repositories: persistence only.
- Every public endpoint needs OpenAPI (springdoc) docs and controller/integration tests (success, validation failure, 404, 401, 403, 409, business-rule failure, pagination/filter/sort, and for mutations: DB state + audit).
- Never invent an endpoint not in the approved spec — propose it instead. Never silently break an existing contract (field rename/removal/type change, status code change, pagination/error format change) without impact analysis and migration planning.

### 1.3 Database (`DATABASE.md`)

- Stack: PostgreSQL + Spring Data JPA/Hibernate + Flyway. Don't introduce another DB technology without an explicit decision.
- `snake_case` naming; UUID primary keys preferred for new externally-exposed entities; real foreign keys for real relationships (`<referenced_table>_id`).
- `NOT NULL` and `UNIQUE` constraints must reflect actual business rules — enforce uniqueness in the DB, not just via a check-then-insert in application code.
- Index foreign keys, frequently filtered/sorted columns, and high-selectivity search fields — not every column.
- Never blindly set `FetchType.EAGER` or `CascadeType.ALL` to solve an N+1 or serialization problem; use fetch joins/EntityGraph/projections and only cascade when the child's lifecycle is genuinely owned by the parent.
- Transactions live at the service layer (`@Transactional`), never in controllers, and must never stay open across slow external/AI calls.
- **Migrations are the only path to schema change.** Location: `src/main/resources/db/migration/`, named `V<version>__<description>.sql`. **Never edit an applied migration** — always add a new one. Treat `DROP TABLE`/`DROP COLUMN`/large `DELETE`/`ALTER COLUMN TYPE` as high-risk: inspect dependencies and data first, prefer phased (add → backfill → cutover → remove) for production tables.
- Before any DB change: inspect existing schema, migrations, entities, repositories, and DTOs first — never guess or duplicate schema objects.
- Never commit secrets (DB passwords, connection strings); use environment variables / the project secret manager.

---

## 2. Frontend Rules

### 2.1 Architecture (`Frontend-File-Structure.md`)

- Feature-first with a shared layer: `app/` (composition/router/providers/config), `features/<name>/` (business capability — `api/`, `components/`, `hooks/`, `pages/`, `schemas/`, `types/`), `shared/` (only genuinely reusable code), `layouts/`, `services/` (API client, storage), `auth/`, `styles/`, `assets/`.
- Frontend `features/` map 1:1 to backend feature modules (`features/company/` ↔ backend `company/`, etc.) — keep the boundary aligned.
- Put code in `shared/` only after it's genuinely reused across features (rule of thumb: first use feature-local, second use evaluate, third genuine use extract to `shared/`). Don't create a catch-all `common/`/`utils.ts` dumping ground.
- A feature must not reach into another feature's internals — expose a deliberate `index.ts` if cross-feature access is needed.
- Use path aliases (`@/features/...`, `@/shared/...`) instead of deep relative imports; avoid circular deps.
- TypeScript strictly; avoid `any` without a documented reason; never silence type errors just to build.

### 2.2 API Integration (`API_INTEGRATION.md`)

- Strict flow: `Component → Custom Hook → Feature API Service → Central HTTP Client → Spring Boot API`. **Components must never call `fetch`/`axios` directly.**
- One central HTTP client (`services/api/apiClient.ts`) owns base URL, auth headers, timeout, and error normalization. Base URL comes from `VITE_API_BASE_URL`, never hard-coded.
- Follow the backend contract exactly — same `/api/v1` paths, `snake_case` JSON (convert to camelCase at the boundary if needed), same pagination params (`page`, `size`, `sort`), same `search` param name, same RFC-7807 error shape.
- Normalize/handle all backend error codes (400/401/403/404/409/422/500) centrally; never show a raw stack trace to the user.
- Auth token attached only by the central client (`Authorization: Bearer ...`); token refresh must be centralized with no infinite retry loop.
- After a mutation, invalidate/update the affected query — don't hand-maintain duplicate copies of server data.
- Never put secrets in frontend code or env vars — anything shipped to the browser is public.

### 2.3 Components (`COMPONENTS.md`)

- One component, one responsibility (render UI / handle interaction / compose children) — no business logic, no direct API calls inside components.
- Shared components (`shared/components/`) stay domain-independent (no `Company`/`Contact` awareness); feature-specific UI stays inside `features/<name>/components/`.
- PascalCase names matching filenames; no vague names (`Thing.jsx`, `Helper.jsx`).
- Every API-driven component must distinguish **Loading / Success / Empty / Error** — never render an empty state while still loading, never leak backend stack traces.
- Stable `key` (use real IDs, not array index) for lists; extract complex JSX into subcomponents rather than deep nested ternaries.
- Search for an existing/shared component before creating a new one; don't optimize (`useMemo`/`useCallback`/`React.memo`) without a measured reason.

### 2.4 State Management (`STATE_MANAGEMENT.md`)

- Classify state first: **Local UI**, **Server**, **Global**, **URL**, **Form** — don't dump everything into one global store.
- Local UI state (`useState`) for things like modal open/closed, selected tab. Server state (companies, contacts, etc.) goes through the project's server-state tool (TanStack Query where available) — don't hand-roll fetch/cache/loading with raw `useEffect`+`useState`.
- Global state only for genuinely app-wide data (auth user, theme, preferences). Shareable/bookmarkable state (search, page, filters, sort) belongs in the URL.
- Never mutate state directly (`state.items.push`) — always immutable updates. Never store derived values that can be computed from existing state.
- Persist only what has a clear requirement, and never persist secrets/tokens/passwords in storage.

### 2.5 UI Design (`UI_DESIGN.md`)

- **Inspect the existing screen and reuse existing patterns/components before changing anything.** Don't redesign unrelated parts of the app; change only what the requirement asks for.
- Component library default: **shadcn/ui** (Tailwind + Radix). Check shadcn/ui first, then existing custom components, and only build new as a last resort.
- Design tokens are fixed — reuse them, don't invent new values:
  - Colors: Primary `#3B82F6`, Success `#10B981`, Warning `#F59E0B`, Error `#EF4444`, Info `#6366F1`, Neutral `#6B7280`
  - Font: Inter, sizes 12/14/16/18/20/24/30/36/48px, weights 400/500/600/700
  - Spacing scale: 0,4,8,12,16,20,24,32,40,48,56,64,80,96,112,128px
  - Radius: none/2/4/8/12/16/full; Shadows: sm/md/lg/xl/2xl as documented in the file
- Every new screen must handle Loading / Empty / Error states, be responsive (desktop/tablet/mobile), and be accessible (semantic HTML, labels, keyboard nav, visible focus, contrast) — no clickable `<div>` in place of `<button>`.
- Implement provided designs accurately (layout, hierarchy, spacing, typography, colors, interactions) rather than approximating.

---

## 3. Cross-Cutting Rules (apply to both frontend and backend)

- **The backend is the sole authority for security.** Frontend hiding a button or route is a UX nicety, never a security control — every protected operation must also be enforced server-side.
- **Never expose secrets** (DB credentials, API keys, JWT secrets, client secrets) in code, logs, or frontend bundles/env vars.
- **Never log** access tokens, `Authorization` headers, passwords, or other sensitive payloads on either side.
- **Follow the existing contract, don't invent one.** Frontend types/requests must match backend DTOs exactly (same fields, same `snake_case` JSON, same pagination/error shape); propose new backend endpoints instead of silently adding ad hoc ones.
- **Inspect before you build.** For any change: read the relevant doc(s) from the routing table above, inspect existing code/schema/components for the pattern already in use, and reuse it instead of introducing a new one.
- **Make the smallest change that satisfies the requirement** — no unrelated refactors, no speculative abstractions, no premature optimization.
- **Nothing is "done" until it's verified**: tests pass (backend: unit + controller/integration; frontend: relevant unit/integration tests, typecheck, build), loading/empty/error states are handled, no secrets committed, and the existing behavior elsewhere isn't broken.

---

## 4. Standard Workflow for Any Change

```text
1. Identify the operation type (API, DB, component, state, UI, structure)
2. Look it up in the routing table (§0) and read the matching doc(s)
3. Inspect existing code for the established pattern (backend feature module /
   frontend feature folder, existing DTOs/components/hooks)
4. Confirm the change is within an approved requirement/spec — propose,
   don't invent, missing endpoints or contracts
5. Implement following that doc's rules
6. Add/update tests
7. Update OpenAPI docs (backend) / verify build & typecheck (frontend)
8. Review against that doc's checklist before calling the change complete
```
