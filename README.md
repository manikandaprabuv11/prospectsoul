# ProspectSoul

Monorepo for the ProspectSoul application: a Spring Boot backend, a React
frontend, and the Docker Compose stack they develop against.

This repository currently contains the **development foundation only**. No
product features, domain entities, business endpoints, or application UI have
been implemented yet.

## Requirements

| Tool           | Version                                    |
| -------------- | ------------------------------------------ |
| Java           | 21 (the build targets 21; older JDKs fail) |
| Maven          | 3.9+                                       |
| Node.js        | 20 LTS or newer (see `frontend/.nvmrc`)    |
| npm            | ships with Node.js                         |
| Docker         | with the Compose v2 plugin                 |
| Git            | any recent version                         |

If `java -version` reports anything below 21, point `JAVA_HOME` at a JDK 21
before running Maven:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

## Project structure

```
.
├── backend/                 Spring Boot 4.1.1 service (Java 21, Maven)
│   ├── pom.xml
│   └── src/{main,test}/
├── frontend/                Vite + React + TypeScript application
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
├── docker/
│   ├── keycloak/            realm-export.json, imported on first start
│   └── postgres/init/       creates the Keycloak database
├── scripts/
│   ├── start-dev.sh         prerequisite checks + docker compose up
│   └── stop-dev.sh          docker compose down (volumes preserved)
├── docker-compose.yml       PostgreSQL, Keycloak, MinIO
├── .env.example
└── README.md
```

## Quick start

```bash
cp .env.example .env
cp frontend/.env.example frontend/.env.local
./scripts/start-dev.sh
```

`start-dev.sh` verifies the prerequisites, creates the `.env` files if missing,
and waits for every container to report healthy.

## Running each part

### Infrastructure

```bash
docker compose up -d        # or ./scripts/start-dev.sh
docker compose ps
docker compose down         # or ./scripts/stop-dev.sh — volumes are kept
```

### Backend

```bash
cd backend
mvn spring-boot:run
```

The backend imports the repository-root `.env` on start-up, so the ports and
credentials Docker Compose uses apply without exporting anything by hand. Real
environment variables take precedence over `.env`, and the defaults in
`application.yaml` apply when neither is set.

`mvn` needs `JAVA_HOME` pointing at a JDK 21. If you see *"The JAVA_HOME
environment variable is not defined correctly"*, it is set to a path that does
not exist:

```bash
echo "$JAVA_HOME" && ls -d "$JAVA_HOME"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
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
npm run dev       # dev server
npm run build     # type-check + production build
npm run lint      # oxlint
```

## URLs

| Service          | URL                                          | Credentials                     |
| ---------------- | -------------------------------------------- | ------------------------------- |
| Frontend         | http://localhost:5173                        | —                               |
| Backend          | http://localhost:8080                        | bearer token from Keycloak      |
| Backend health   | http://localhost:8080/actuator/health        | public                          |
| PostgreSQL       | localhost:5432                               | `prospectsoul` / `prospectsoul` |
| Keycloak         | http://localhost:8081                        | `admin` / `admin`               |
| Keycloak realm   | http://localhost:8081/realms/vyoog           | dev user `dev` / `dev`          |
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
`KEYCLOAK_ISSUER_URI`. Only `/actuator/health` and `/actuator/info` are
anonymous — every other path requires an authenticated principal. Because the
issuer URI is resolved at start-up, **Keycloak must be running before the
backend starts**.

The realm at `docker/keycloak/realm-export.json` defines realm `vyoog` with two
clients: `prospectsoul-web` (public, authorization code + PKCE, for the
browser) and `prospectsoul-api` (bearer-only, the API audience). It is imported
on the first Keycloak start; later edits require
`docker compose down -v` or a manual import.

**CORS.** Allowed origins come from `CORS_ALLOWED_ORIGINS` — an explicit list,
never a wildcard. The development default is the Vite dev server.

**Database.** Flyway owns the schema and Hibernate runs with
`ddl-auto: validate`. Migrations belong in
`backend/src/main/resources/db/migration` as `V<n>__<description>.sql`; none
exist yet.

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
  been published. Add it once one is available.
- **Frontend authentication** is not implemented. `src/api/client.ts` exposes
  `setAuthTokenProvider()` for the auth layer to register a token source; until
  it does, requests are sent unauthenticated.
# prospectsoul
