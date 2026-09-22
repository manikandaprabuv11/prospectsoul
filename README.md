# ProspectSoul

Monorepo for the ProspectSoul application: a Spring Boot backend, a React
frontend, and the Docker Compose stack they develop against.

## Requirements

| Tool           | Version                                    |
| -------------- | ------------------------------------------ |
| Java           | 21 (the build targets 21; older JDKs fail) |
| Maven          | 3.9+                                       |
| Node.js        | 20 LTS or newer (see `frontend/.nvmrc`)    |
| npm            | ships with Node.js                         |
| Docker         | with the Compose v2 plugin                 |
| Git            | any recent version                         |

This project uses [SDKMAN](https://sdkman.io/) to manage Java versions. A
`.sdkmanrc` at the repository root pins the JDK to `21.0.4-tem`. If you have
`sdkman_auto_env=true` (the default on this machine), SDKMAN sets `JAVA_HOME`
automatically when you `cd` into the project. Otherwise run `sdk env` once after
cloning.

## Project structure

```
.
├── backend/                 Spring Boot 4.1.1 service (Java 21, Maven)
│   ├── pom.xml
│   └── src/{main,test}/
├── frontend/                Vite + React 19 + TypeScript application
│   ├── package.json
│   ├── vite.config.ts
│   ├── vitest.config.ts
│   └── src/
├── docker/
│   ├── keycloak/            realm-export.json, imported on first start
│   └── postgres/init/       creates the Keycloak database
├── scripts/
│   ├── start-dev.sh         prerequisite checks + docker compose up
│   └── stop-dev.sh          docker compose down (volumes preserved)
├── docker-compose.yml       PostgreSQL, Keycloak, MinIO
├── .sdkmanrc                pins JDK to 21.0.4-tem via SDKMAN
├── .env.example
└── README.md
```

## Quick start

```bash
cp .env.example .env
cp frontend/.env.example frontend/.env.local
./scripts/start-dev.sh        # starts PostgreSQL, Keycloak, MinIO
cd backend && mvn spring-boot:run &
cd frontend && npm install && npm run dev &
```

`start-dev.sh` verifies the prerequisites, creates the `.env` files if missing,
and waits for every container to report healthy. **Keycloak must be running
before the backend starts** (the issuer URI is resolved at startup).

## Running each part

### Infrastructure

```bash
docker compose up -d        # or ./scripts/start-dev.sh
docker compose ps
docker compose down         # or ./scripts/stop-dev.sh — volumes are kept
```

To fully reset Keycloak and re-import the realm:
```bash
docker compose down -v      # destroys volumes
docker compose up -d
```

### Backend

```bash
cd backend
mvn spring-boot:run
```

Run the tests (Testcontainers starts its own PostgreSQL, so Docker must be
running):

```bash
cd backend
mvn clean test
```

### Frontend

```bash
cd frontend
npm install
npm run dev       # dev server on http://localhost:5173
npm run build     # type-check + production build
npm run lint      # oxlint
npm run test      # vitest — all frontend tests
```

## Authentication and roles

### How it works

The backend is a stateless OAuth2 resource server. Keycloak issues the tokens;
Spring Security validates them against `KEYCLOAK_ISSUER_URI`. The
`SecurityConfig` extracts realm roles from the `realm_access.roles` JWT claim,
filters for `PS_*` roles, and maps them to Spring authorities as
`ROLE_PS_<name>`. Method-level authorization uses `@PreAuthorize` with composite
SpEL expressions defined in `RoleConstants`.

The frontend uses `keycloak-js` with `onLoad: 'check-sso'` and a custom login
page (Direct Access Grants). Token refresh is handled via the Keycloak token
endpoint. On a 401 from the API, the frontend clears tokens and returns to the
login page.

### Keycloak realm

The realm at `docker/keycloak/realm-export.json` defines:
- **Realm:** `vyoog`
- **SPA client:** `prospectsoul-web` (public, PKCE, direct access grants)
- **API client:** `prospectsoul-api` (bearer-only)
- **5 realm roles:** PS_ANALYST, PS_SALES_LEAD, PS_ADMIN, PS_VIEWER, PS_COO
- **7 seed users** with role assignments (imported on first start)

### Seed users

All seed users have password: **`password`**

| Username    | Full Name        | Email              | Role          | Capabilities                          |
| ----------- | ---------------- | ------------------ | ------------- | ------------------------------------- |
| `analyst`   | Priya Sharma     | priya@vyoog.com    | PS_ANALYST    | Import, triage, research, verify, read |
| `analyst2`  | Deepa Krishnan   | deepa@vyoog.com    | PS_ANALYST    | Import, triage, research, verify, read |
| `saleslead` | Kumar Rajan      | kumar@vyoog.com    | PS_SALES_LEAD | All analyst + export, override         |
| `admin`     | Ravi Chandran    | ravi@vyoog.com     | PS_ADMIN      | Full access including user/role mgmt   |
| `viewer`    | Meera Natarajan  | meera@vyoog.com    | PS_VIEWER     | Read-only (search, reports)            |
| `viewer2`   | Arun Prakash     | arun@vyoog.com     | PS_VIEWER     | Read-only (search, reports)            |
| `coo`       | Senthil Kumar    | senthil@vyoog.com  | PS_COO        | Read-only (dashboard, reports)         |

### Login walkthrough

1. Start the infrastructure: `docker compose up -d`
2. Start the backend: `cd backend && mvn spring-boot:run`
3. Start the frontend: `cd frontend && npm run dev`
4. Open http://localhost:5173
5. Sign in with any seed user (e.g. `admin` / `password`)
6. The sidebar shows navigation filtered by role:
   - **Admin:** Dashboard, Companies, Imports, Users & Roles
   - **Analyst/Sales Lead:** Dashboard, Companies, Imports
   - **Viewer/COO:** Dashboard, Companies
7. User menu (top-right) shows name, role badge, and sign-out

### Role permission matrix

| Capability      | SpEL constant    | ANALYST | SALES_LEAD | ADMIN | VIEWER | COO |
| --------------- | ---------------- | ------- | ---------- | ----- | ------ | --- |
| Read/search     | `HAS_READ`       | ✓       | ✓          | ✓     | ✓      | ✓   |
| Mutate (CRUD)   | `HAS_MUTATE`     | ✓       | ✓          | ✓     | —      | —   |
| Export           | `HAS_EXPORT`     | —       | ✓          | ✓     | —      | —   |
| Configure/Admin  | `HAS_CONFIGURE`  | —       | —          | ✓     | —      | —   |

### User management walkthrough (Admin only)

1. Sign in as `admin` / `password`
2. Navigate to **Users & Roles** in the sidebar
3. **Users tab:** list, search, filter by role/status, paginate
4. **Add user:** click "Add user" — enter username, full name, email, password (min 8 chars), role. Creates in both Keycloak and local DB.
5. **Edit user:** click the edit icon on a row — change name, email, or role
6. **Activate/Deactivate:** click the toggle icon — disables the user in both Keycloak and local DB
7. **Roles tab:** view role cards with permissions and user counts. Click a card to open the detail drawer.
8. **Role detail drawer:** edit description, view permissions, assign/unassign users

### Keycloak Admin Client usage

The backend uses the `keycloak-admin-client` library to synchronize user
management operations with Keycloak. Configuration:

```yaml
keycloak:
  admin:
    server-url: ${KEYCLOAK_ADMIN_SERVER_URL:http://localhost:8081}
    realm: master
    client-id: admin-cli
    username: ${KEYCLOAK_ADMIN:admin}
    password: ${KEYCLOAK_ADMIN_PASSWORD:admin}
  target-realm: vyoog
```

Operations synchronized with Keycloak:
- **Create user:** creates in Keycloak with temporary password → assigns realm role → saves to local DB
- **Update user:** updates name/email in Keycloak → updates local DB
- **Change role:** removes old realm role, adds new one in Keycloak → updates local DB + user_roles
- **Activate/Deactivate:** enables/disables user in Keycloak → updates local DB
- **Rollback:** if role assignment fails after user creation, the Keycloak user is deleted

## API endpoints

All endpoints require a valid JWT bearer token unless noted as public.
Errors return `application/problem+json` (RFC 7807).

### Auth (`/api/v1/auth`) — public

| Method | Path                              | Description              |
| ------ | --------------------------------- | ------------------------ |
| POST   | `/api/v1/auth/forgot-password`    | Request password reset   |

### Companies (`/api/v1/companies`)

| Method | Path                                | Authorization   | Description                  |
| ------ | ----------------------------------- | --------------- | ---------------------------- |
| POST   | `/api/v1/companies`                 | HAS_MUTATE      | Create company               |
| GET    | `/api/v1/companies`                 | HAS_READ        | List companies (paginated)   |
| GET    | `/api/v1/companies/{id}`            | HAS_READ        | Get company detail           |
| PATCH  | `/api/v1/companies/{id}`            | HAS_MUTATE      | Update company               |
| POST   | `/api/v1/companies/{id}/verify`     | HAS_MUTATE      | Verify company               |

### Imports (`/api/v1/imports`)

| Method | Path                                     | Authorization   | Description                  |
| ------ | ---------------------------------------- | --------------- | ---------------------------- |
| POST   | `/api/v1/imports`                        | HAS_MUTATE      | Upload CSV/Excel             |
| GET    | `/api/v1/imports`                        | HAS_READ        | List import batches          |
| GET    | `/api/v1/imports/{id}`                   | HAS_READ        | Get batch detail             |
| GET    | `/api/v1/imports/{id}/rows`              | HAS_READ        | Get batch rows (paginated)   |
| GET    | `/api/v1/imports/{id}/mappings/suggest`  | HAS_MUTATE      | Get mapping suggestions      |
| POST   | `/api/v1/imports/{id}/mappings/confirm`  | HAS_MUTATE      | Confirm column mappings      |
| GET    | `/api/v1/imports/{id}/preview`           | HAS_MUTATE      | Preview import results       |
| POST   | `/api/v1/imports/{id}/process`           | HAS_MUTATE      | Process the import           |

### Import Templates (`/api/v1/import-templates`)

| Method | Path                                | Authorization   | Description                  |
| ------ | ----------------------------------- | --------------- | ---------------------------- |
| GET    | `/api/v1/import-templates`          | HAS_READ        | List templates               |
| POST   | `/api/v1/import-templates`          | HAS_MUTATE      | Create template              |
| GET    | `/api/v1/import-templates/aliases`  | HAS_READ        | List column aliases          |

### Admin — Users (`/api/v1/admin/users`) — HAS_CONFIGURE (Admin only)

| Method | Path                                     | Description                  |
| ------ | ---------------------------------------- | ---------------------------- |
| GET    | `/api/v1/admin/users`                    | List users (paginated, searchable) |
| GET    | `/api/v1/admin/users/{id}`               | Get user detail              |
| POST   | `/api/v1/admin/users`                    | Create user                  |
| PATCH  | `/api/v1/admin/users/{id}`               | Update user                  |
| POST   | `/api/v1/admin/users/{id}/activate`      | Activate user                |
| POST   | `/api/v1/admin/users/{id}/deactivate`    | Deactivate user              |

### Admin — Roles (`/api/v1/admin/roles`) — HAS_CONFIGURE (Admin only)

| Method | Path                                       | Description                  |
| ------ | ------------------------------------------ | ---------------------------- |
| GET    | `/api/v1/admin/roles`                      | List all roles               |
| GET    | `/api/v1/admin/roles/{id}`                 | Get role detail + users      |
| PATCH  | `/api/v1/admin/roles/{id}`                 | Update role description      |
| GET    | `/api/v1/admin/roles/{id}/users`           | Get assigned users           |
| POST   | `/api/v1/admin/roles/{id}/users`           | Assign user to role          |
| DELETE | `/api/v1/admin/roles/{id}/users/{userId}`  | Unassign user from role      |

### NIC Master (`/api/v1/nic-codes`) — Sales-Intelligence

| Method | Path                                       | Authorization   | Description                                   |
| ------ | ------------------------------------------ | --------------- | --------------------------------------------- |
| GET    | `/api/v1/nic-codes`                        | HAS_READ        | Paginated NIC listing (search/level/type)     |
| GET    | `/api/v1/nic-codes/{id}`                   | HAS_READ        | Get one NIC code                              |
| GET    | `/api/v1/nic-codes/{id}/children`          | HAS_READ        | Direct children                               |
| GET    | `/api/v1/nic-codes/tree?root_id=&depth=`   | HAS_READ        | Subtree (in-memory cached at root)            |
| GET    | `/api/v1/nic-codes/primary`                | HAS_READ        | Primary-flagged codes only                    |
| POST   | `/api/v1/nic-codes`                        | HAS_CONFIGURE   | Create code — parent must be a prefix         |
| PATCH  | `/api/v1/nic-codes/{id}`                   | HAS_CONFIGURE   | Update / deactivate (422 if in use)           |
| POST   | `/api/v1/nic-codes/{id}/toggle-primary`    | HAS_CONFIGURE   | Toggle primary flag                           |
| POST   | `/api/v1/admin/nic-codes/import`           | HAS_CONFIGURE   | Reference-data import (no `import_batches`)   |

### Contact Roles (`/api/v1/contact-roles`) — Sales-Intelligence

| Method | Path                                       | Authorization   | Description                                   |
| ------ | ------------------------------------------ | --------------- | --------------------------------------------- |
| GET    | `/api/v1/contact-roles`                    | HAS_READ        | List roles (`include_inactive=true` optional) |
| POST   | `/api/v1/contact-roles`                    | HAS_CONFIGURE   | Create role                                   |
| PATCH  | `/api/v1/contact-roles/{id}`               | HAS_CONFIGURE   | Update / deactivate (never delete)            |

### Contacts (multi-per-company) — Sales-Intelligence

| Method | Path                                                | Authorization   | Description                                   |
| ------ | --------------------------------------------------- | --------------- | --------------------------------------------- |
| GET    | `/api/v1/companies/{companyId}/contacts`            | HAS_READ        | List contacts                                 |
| POST   | `/api/v1/companies/{companyId}/contacts`            | HAS_MUTATE      | Create contact (role_id required)             |
| PATCH  | `/api/v1/contacts/{contactId}`                      | HAS_MUTATE      | Update contact                                |
| POST   | `/api/v1/contacts/{contactId}/make-primary`         | HAS_MUTATE      | Promote to primary (atomic; DB-enforced)      |

### Multi-NIC per company — Sales-Intelligence

| Method | Path                                                       | Authorization   | Description                              |
| ------ | ---------------------------------------------------------- | --------------- | ---------------------------------------- |
| GET    | `/api/v1/companies/{companyId}/nic-codes`                  | HAS_READ        | List join rows                           |
| POST   | `/api/v1/companies/{companyId}/nic-codes`                  | HAS_MUTATE      | Attach a NIC code (raw or resolved)      |
| POST   | `/api/v1/companies/{companyId}/nic-codes/{rowId}/make-primary` | HAS_MUTATE | Swap primary (atomic; DB-enforced)       |
| DELETE | `/api/v1/companies/{companyId}/nic-codes/{rowId}`          | HAS_MUTATE      | Detach one row                           |

### Location Intelligence (`/api/v1/map`, `/api/v1/external`) — Sales-Intelligence

| Method | Path                                        | Authorization   | Description                                                  |
| ------ | ------------------------------------------- | --------------- | ------------------------------------------------------------ |
| GET    | `/api/v1/map/pincode/{pincode}`             | HAS_READ        | Offline pincode centroid (no Google call)                    |
| GET    | `/api/v1/map/companies?pincode=&radius_km=` | HAS_READ        | Owned companies inside the radius (haversine)                |
| GET    | `/api/v1/external/places-search?pincode=`   | HAS_READ        | Live Places lookup — never persisted, quota-tracked          |

### Companies List — Download (`/api/v1/companies/download`) — Sales-Intelligence

| Method | Path                                | Authorization   | Description                                                              |
| ------ | ----------------------------------- | --------------- | ------------------------------------------------------------------------ |
| POST   | `/api/v1/companies/download`        | HAS_READ        | Filter-scoped CSV/XLSX. **Does NOT** change pipeline state (ADR-0005).   |

### Companies List — extended filters (Sales-Intelligence)

`GET /api/v1/companies` now accepts:

| Query param                     | Meaning                                                          |
| ------------------------------- | ---------------------------------------------------------------- |
| `region`, `district`, `pincode` | Sales-Intelligence facets                                        |
| `turnover_min`, `turnover_max`  | Range filter on `turnover`                                       |
| `employee_min`, `employee_max`  | Range filter on `employee_count`                                 |
| `gst_present=true|false`        | GST presence facet                                               |
| `nic_code_id=<uuid>`            | Exact NIC classification match                                   |
| `nic_parent_id=<uuid>`          | Descendant expansion via recursive CTE (default on)              |
| `nic_include_descendants`       | Turn descendant expansion off                                    |
| `has_contact_role_id=<uuid>`    | Companies that have at least one contact with the given role     |
| `view=grouped_by_nic`           | Returns `{root_node, groups[], total_companies}` (needs `nic_parent_id`) |

## Sales-Intelligence extension

Docs 19–24 (`docs/dev_docs/19-*` … `docs/dev_docs/24-*`) describe the Sales-Intelligence extension.
Implementation ships in five tracks (C1–C5) — see [`docs/dev_docs/23-Sales-Intelligence-Implementation-Plan-v1_0.md`](docs/dev_docs/23-Sales-Intelligence-Implementation-Plan-v1_0.md).
Behaviour changes to existing code are recorded as ADRs in [`docs/dev_docs/adr/`](docs/dev_docs/adr/) — ADR-0006 through ADR-0009 apply to this build.

Notable operating notes:

- **NIC master import is reference-data**: `POST /api/v1/admin/nic-codes/import` does NOT create `import_batches` / `import_rows` (Kickoff constraint 6).
- **Places API key is server-side only**: set `prospectsoul.places.daily-quota` and provide a Google Places client bean to swap out the built-in stub. Never put the key in `frontend/.env.local` or a `VITE_*` variable.
- **Download vs Export**: `POST /api/v1/companies/download` is a filter-scoped file dump; it does not change any company's `pipeline_state`. The pipeline `POST /api/v1/exports` is unimplemented in this scope (deferred until the qualification module lands).

### Verifications (`/api/v1/verifications`)

Company phone verification through Twilio Lookup v2 Line Type Intelligence.
Starting a batch returns `202 Accepted` immediately; a DB-backed worker does the
provider calls in the background.

| Method | Path                                          | Authorization | Description                                        |
| ------ | --------------------------------------------- | ------------- | -------------------------------------------------- |
| POST   | `/api/v1/verifications`                       | HAS_MUTATE    | Start a batch (**202**; body: `company_ids`, `added_by`, `date_from`, `date_to`) |
| GET    | `/api/v1/verifications/active`                | HAS_READ      | The running batch, or **204** when nothing is running |
| GET    | `/api/v1/verifications`                       | HAS_READ      | Previous jobs (`status`, `requested_by`, `from`, `to`, paginated) |
| GET    | `/api/v1/verifications/{id}`                  | HAS_READ      | Job detail with counters and progress              |
| GET    | `/api/v1/verifications/{id}/items`            | HAS_READ      | Per-company results (`status`, `q`, paginated)     |
| GET    | `/api/v1/verifications/eligible`              | HAS_READ      | Selection candidates — **never** a VERIFIED company (`added_by`, `date_from`, `date_to`, `verification_status`, `q`) |
| GET    | `/api/v1/verifications/companies`             | HAS_READ      | Verified companies **only** (`q`, `verified_by`, `verified_from`, `verified_to`, `added_by`) |
| GET    | `/api/v1/verifications/added-by-options`      | HAS_READ      | Options for the Added By filter                    |

Verify-specific status codes, on top of the table below:

| Status | When                                                                        |
| ------ | --------------------------------------------------------------------------- |
| 202    | Batch accepted; queue rows committed, no provider call made yet             |
| 204    | `GET /active` — no batch is running (a normal state, not an error)           |
| 400    | `date_from` after `date_to`; unknown status filter; `verification_status=VERIFIED` on `/eligible` |
| 409    | A selected company is already queued or processing in another batch          |
| 422    | No eligible company in the selection, or more companies than the configured cap |

**What verification proves.** A successful lookup means the number is valid and
on a **mobile** line. It does not prove ownership and does not mean anyone
answered. Successful items set `companies.verification_status = VERIFIED`; every
other outcome records the reason on the item and leaves the company unverified.

### Error responses

All errors use `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "You do not have permission to access this resource"
}
```

| Status | Meaning                | When                                         |
| ------ | ---------------------- | -------------------------------------------- |
| 400    | Bad Request            | Validation failure (field errors in `errors[]`) |
| 401    | Unauthorized           | Missing, expired, or malformed JWT           |
| 403    | Forbidden              | Valid JWT but insufficient role               |
| 404    | Not Found              | Entity not found by ID                       |
| 409    | Conflict               | Duplicate username/email, duplicate assignment |
| 422    | Unprocessable Entity   | Business rule violation (invalid role, etc.)  |

## Frontend routes

| Path               | Component          | Guard            | Description                |
| ------------------ | ------------------ | ---------------- | -------------------------- |
| `/`                | DashboardPage      | authenticated    | Dashboard                  |
| `/companies`       | CompanyListPage    | authenticated    | Company list               |
| `/companies/new`   | CompanyCreatePage  | MUTATE_ROLES     | Create company             |
| `/companies/:id`   | CompanyDetailPage  | authenticated    | Company detail             |
| `/companies/:id/edit` | CompanyEditPage | MUTATE_ROLES     | Edit company               |
| `/imports`         | ImportListPage     | MUTATE_ROLES     | Import batch list          |
| `/imports/new`     | ImportWizardPage   | MUTATE_ROLES     | New import wizard          |
| `/imports/:id`     | ImportDetailPage   | MUTATE_ROLES     | Import batch detail        |
| `/verify`          | VerifyPage         | authenticated    | Verify workspace           |
| `/verify/:id`      | VerificationDetailPage | authenticated | Verification job detail  |
| `/settings/users`  | UsersPage          | ADMIN_ROLES      | User management            |
| `/settings/roles`  | RolesPage          | ADMIN_ROLES      | Role management            |

## Database migrations

Flyway manages the schema. Hibernate runs with `ddl-auto: validate`. Migrations
live in `backend/src/main/resources/db/migration/`.

| Version | Description              |
| ------- | ------------------------ |
| V1      | Create audit_log table   |
| V2      | Create companies table   |
| V3      | Create import tables     |
| V4      | Auth tables (users, roles, user_roles) |
| V5      | Seed 5 roles             |
| V6      | Seed 7 users + 7 role assignments |
| V7      | Create activities table (PRD v1.1 §10; VERIFICATION activities compose into the Company Timeline) |
| V8      | Create verification_batches + verification_batch_items (the DB-backed verification queue) |

## Design template integration

The login, forgot-password, user management, and role management pages use a
custom design system based on the project's design templates:

- **Fonts:** Fraunces (display headings), Inter (body text)
- **Colors:** Copper `#c27a3e` (primary accent), Navy `#162032` (brand panel),
  Sage `#f7f8f6` (backgrounds), Slate `#5e6360` (muted text)
- **Components:** Custom CSS for login shell (split brand/form layout), data
  tables, filter toolbars, modals, role cards, detail drawers, skeleton loading,
  empty states
- **Not using shadcn/ui** for auth pages — they follow the design template
  directly. App shell pages (dashboard, companies, imports) use shadcn/ui
  components with Tailwind.

## URLs

| Service          | URL                                          | Credentials                     |
| ---------------- | -------------------------------------------- | ------------------------------- |
| Frontend         | http://localhost:5173                        | see seed users above            |
| Backend          | http://localhost:8080                        | bearer token from Keycloak      |
| Backend health   | http://localhost:8080/actuator/health        | public                          |
| PostgreSQL       | localhost:5432                               | `prospectsoul` / `prospectsoul` |
| Keycloak         | http://localhost:8081                        | `admin` / `admin`               |
| Keycloak realm   | http://localhost:8081/realms/vyoog           | see seed users above            |
| MinIO API        | http://localhost:9000                        | `prospectsoul` / `prospectsoul` |
| MinIO console    | http://localhost:9001                        | `prospectsoul` / `prospectsoul` |

**Every credential above is a development-only default from `.env.example`.**
They exist so a fresh checkout runs without configuration. Never reuse them
outside a developer machine.

> **Port conflicts.** Every published port is overridable in `.env`
> (`POSTGRES_PORT`, `KEYCLOAK_PORT`, `MINIO_PORT`, `MINIO_CONSOLE_PORT`,
> `SERVER_PORT`, `VITE_DEV_SERVER_PORT`). If something already listens on
> 5432, set `POSTGRES_PORT` to a free port and update `DATABASE_URL` to match.

## Environment variables

Infrastructure and backend variables live in the root `.env` (see
`.env.example`). Frontend variables live in `frontend/.env.local` (see
`frontend/.env.example`).

### Infrastructure — root `.env`

| Variable                                    | Purpose                       |
| ------------------------------------------- | ----------------------------- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` | PostgreSQL container |
| `KEYCLOAK_DB`                               | database Keycloak stores its own schema in |
| `KEYCLOAK_PORT`, `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD` | Keycloak container |
| `MINIO_PORT`, `MINIO_CONSOLE_PORT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` | MinIO container |

### Backend — root `.env`

| Variable                | Default                                          |
| ----------------------- | ------------------------------------------------ |
| `SERVER_PORT`           | `8080`                                           |
| `DATABASE_URL`          | `jdbc:postgresql://localhost:5432/prospectsoul`  |
| `DATABASE_USERNAME`     | `prospectsoul`                                   |
| `DATABASE_PASSWORD`     | `prospectsoul`                                   |
| `KEYCLOAK_ISSUER_URI`   | `http://localhost:8081/realms/vyoog`             |
| `KEYCLOAK_CLIENT_ID`    | `prospectsoul-web`                               |
| `KEYCLOAK_ADMIN_SERVER_URL` | `http://localhost:8081`                      |
| `MINIO_ENDPOINT`        | `http://localhost:9000`                          |
| `MINIO_ACCESS_KEY`      | `prospectsoul`                                   |
| `MINIO_SECRET_KEY`      | `prospectsoul`                                   |
| `MINIO_BUCKET`          | `prospectsoul`                                   |
| `CORS_ALLOWED_ORIGINS`  | `http://localhost:5173`                          |
| `OPENAI_API_KEY`        | `not-configured` (placeholder, see below)        |
| `TWILIO_ACCOUNT_SID`    | empty (see "Twilio setup" below)                 |
| `TWILIO_AUTH_TOKEN`     | empty (see "Twilio setup" below)                 |
| `TWILIO_LOOKUP_BASE_URL`| `https://lookups.twilio.com`                     |
| `VERIFICATION_WORKER_ENABLED` | `true`                                     |
| `VERIFICATION_POLL_INTERVAL_MS` | `5000`                                   |
| `VERIFICATION_BATCH_SIZE` | `10` (items claimed per worker pass)           |
| `VERIFICATION_MAX_RETRIES` | `3` (attempts per item)                       |
| `VERIFICATION_PROVIDER` | `twilio`                                         |
| `VERIFICATION_MAX_BATCH_COMPANIES` | `1000`                                |
| `VERIFICATION_STALE_ITEM_TIMEOUT_MS` | `300000` (abandoned-item recovery)  |
| `VERIFICATION_DEFAULT_COUNTRY_CODE` | `+91`                                |

### Frontend — `frontend/.env.local`

| Variable                   | Default                            |
| -------------------------- | ---------------------------------- |
| `VITE_API_BASE_URL`        | `http://localhost:8080/api/v1`     |
| `VITE_KEYCLOAK_URL`        | `http://localhost:8081`            |
| `VITE_KEYCLOAK_REALM`      | `vyoog`                            |
| `VITE_KEYCLOAK_CLIENT_ID`  | `prospectsoul-web`                 |
| `VITE_DEV_SERVER_PORT`     | `5173`                             |

Anything prefixed `VITE_` is embedded in the browser bundle. Never put a
secret, API key, or database password there.

## How the pieces fit together

**Security.** The backend is a stateless OAuth2 resource server. Keycloak
issues the tokens; Spring Security validates them against
`KEYCLOAK_ISSUER_URI`. Only `/actuator/health`, `/actuator/info`, and
`/api/v1/auth/**` are anonymous — every other path requires an authenticated
principal.

**CORS.** Allowed origins come from `CORS_ALLOWED_ORIGINS` — an explicit list,
never a wildcard. The development default is the Vite dev server.

**Database.** Flyway owns the schema and Hibernate runs with
`ddl-auto: validate`. Six migrations exist (V1–V6), creating the audit log,
companies, import tables, auth tables, and seeding roles/users.

**Networking.** Containers address each other by service name (`postgres`,
`keycloak`, `minio`) on the `prospectsoul` bridge network. The host reaches
them on the published ports listed above. Backend and frontend run on the host
during development, not in containers.

**Spring AI.** The `spring-ai-starter-model-openai` dependency is on the
classpath and refuses to start without a credential, so `OPENAI_API_KEY`
defaults to the placeholder `not-configured`. No AI feature is wired up; set a
real key only when that work begins.

## Verify module (company phone verification)

The Verify workspace at `/verify` selects unverified companies, checks their
phone numbers through Twilio Lookup, and maintains each company's record-level
verification state. It is a background-processing feature, so the interesting
parts are operational.

### How a batch runs

```text
/verify → filter (Added By, date range) → select → confirm → POST /verifications (202)
        → verification_batch_items rows committed
        → worker claims with FOR UPDATE SKIP LOCKED
        → Twilio Lookup v2 (Line Type Intelligence)
        → item result persisted, company updated, VERIFICATION activity + audit row
        → batch counters recomputed → COMPLETED / COMPLETED_WITH_ERRORS
```

There is no message broker. The queue is the `verification_batch_items` table,
the same DB-backed pattern the import pipeline uses. That is what makes progress
survive a browser refresh and a backend restart: the only place work-in-progress
is recorded is PostgreSQL, and the UI reads it back from
`GET /api/v1/verifications/active`.

### Twilio setup

The **backend** calls Twilio. Never copy these values into
`frontend/.env.local` or any `VITE_` variable — everything shipped to the
browser is public.

1. In the Twilio console, take the **Account SID** and **Auth Token**.
2. Put them in the root `.env`:

   ```bash
   TWILIO_ACCOUNT_SID=AC...
   TWILIO_AUTH_TOKEN=...
   TWILIO_LOOKUP_BASE_URL=https://lookups.twilio.com
   ```

3. Restart the backend.

**Working without credentials.** Leave them empty and the whole module still
works end to end — selection, the queue, live progress, history, per-item
reasons. Every item simply fails permanently with `PROVIDER_NOT_CONFIGURED`,
which is visible on the item in the UI rather than buried in a log. No test
makes a live Twilio call.

**Credential hygiene.** Any credential that has been pasted into a chat, a
ticket, a code sample or an AI-assistant session must be treated as
**compromised** and rotated before production use. Credentials are never logged,
never returned in a DTO and never sent to the frontend.

### Result rules

| Provider result                 | Item     | Company            |
| ------------------------------- | -------- | ------------------ |
| valid **and** line type mobile  | VERIFIED | VERIFIED           |
| valid but not a mobile line     | FAILED   | unchanged          |
| not a valid number              | FAILED   | unchanged          |
| timeout / 429 / temporary 5xx   | retried, then FAILED as `MAX_ATTEMPTS_EXCEEDED` | unchanged |
| auth or configuration failure   | FAILED   | unchanged          |
| no usable phone                 | SKIPPED (`NO_PHONE`) | unchanged |
| already verified                | SKIPPED (`ALREADY_VERIFIED`) | unchanged |
| outside the selected filters    | SKIPPED (`FILTER_MISMATCH`) | unchanged |

One company's failure never stops the batch. Each item is processed in its own
transaction, and the provider call happens with no transaction open.

### Actor recording

`companies.verified_by` records the **user who requested the batch** — the
person accountable for the decision to verify. The fact that the check itself
was performed automatically by a provider is recorded separately:

- the `VERIFICATION` activity content carries `automated: true`, the provider,
  the provider reference, the line type, `proves: PHONE_VALIDITY_AND_LINE_TYPE`
  and `ownership_verified: false`;
- the audit action is `VERIFY_AUTOMATED`, distinct from the `VERIFY` action the
  manual `POST /api/v1/companies/{id}/verify` writes.

### Troubleshooting

| Symptom | Cause and fix |
| ------- | ------------- |
| Every item fails with `PROVIDER_NOT_CONFIGURED` | `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` are unset or still a placeholder. Set them in the root `.env` and restart. |
| Every item fails with `PROVIDER_AUTH_ERROR` | Twilio rejected the credentials (HTTP 401/403). The token was rotated or the SID belongs to another account. |
| Items stay `QUEUED` and the progress bar never moves | The worker is off. Check `VERIFICATION_WORKER_ENABLED=true` and that the backend is running. Nothing is lost — the rows are still in the queue and are picked up when it starts. |
| A batch is stuck at 99% with one item `PROCESSING` | The JVM died mid-item. The worker re-queues it once `VERIFICATION_STALE_ITEM_TIMEOUT_MS` has passed (5 min by default) and the attempt budget still applies. |
| `409` when starting a batch | One of the selected companies already has a queued or processing item in another batch. The selection table marks those rows "Being verified" and makes them unselectable. |
| `422 No eligible companies` | Everything selected is already verified, has no usable phone, or falls outside the Added By / date filters. |
| A verified company shows an empty line type and carrier | It was verified through the manual `POST /api/v1/companies/{id}/verify`, which performs no provider lookup. |
| Items fail with `NO_PHONE` although a phone is visible | The stored number does not normalise to 10 digits. `PhoneNormalizer` is the single source of truth; the Verify module only prefixes `VERIFICATION_DEFAULT_COUNTRY_CODE` to build the E.164 form Lookup requires. |
| Rate limiting under a large batch | Lower `VERIFICATION_BATCH_SIZE` or raise `VERIFICATION_POLL_INTERVAL_MS`. `RATE_LIMITED` is retryable, so items are re-queued rather than lost. |

### Reading a batch straight from the database

```sql
-- current state of a batch
SELECT status, total_count, queued_count, processing_count,
       verified_count, failed_count, skipped_count
FROM verification_batches ORDER BY created_at DESC LIMIT 1;

-- why individual companies failed
SELECT c.canonical_name, i.status, i.line_type, i.failure_code, i.attempt_count
FROM verification_batch_items i
JOIN companies c ON c.id = i.company_id
WHERE i.batch_id = '<batch-uuid>' AND i.status <> 'VERIFIED';
```

## Not yet configured

- **Springdoc / Swagger UI** is not installed. The latest release (2.8.6)
  targets Spring Boot 3.x; no version compatible with Spring Boot 4.1.1 has
  been published. Security annotations are documented in the API endpoints table
  above. Add Springdoc once a compatible version is available.
- **Verify module OpenAPI** — for the same reason there is no generated
  specification for `/api/v1/verifications`. Its request/response shapes,
  authorization and status codes are documented in the API endpoints table
  above, and pinned by `VerificationControllerIntegrationTest`.
