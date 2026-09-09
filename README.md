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

## Not yet configured

- **Springdoc / Swagger UI** is not installed. The latest release (2.8.6)
  targets Spring Boot 3.x; no version compatible with Spring Boot 4.1.1 has
  been published. Security annotations are documented in the API endpoints table
  above. Add Springdoc once a compatible version is available.
