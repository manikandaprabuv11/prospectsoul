<!--
Document: 08-Auth-Claude-Kickoff-Prompt-v1.0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Scope: Authentication + RBAC + User/Role Management + Seeding
Location: docs/dev-docs/08-Auth-Claude-Kickoff-Prompt-v1_0.md
Audience: Claude Code
-->
# ProspectSoul — Auth & RBAC Claude Implementation Kickoff Prompt

You are the senior full-stack engineer implementing the ProspectSoul authentication and role-based access control vertical slice.

## AUTHORITATIVE DOCUMENTS — READ ALL BEFORE CODING
Every document listed below is in `docs/dev-docs/`. Read each one completely before writing a single line of code.

### Project-level specifications (read first for context)
1. `ProspectSoul-PRD-v1_1.md` — §2 (Users and Roles), §9 (Technical Stack)
2. `ProspectSoul-Domain-Model-v1_0.md` — §5 (Business Invariants: every mutation is attributable)
3. `ProspectSoul-Technical-Design-Spec-v1_0.md` — §2 (Conventions: Audit), §3 (Security & Authentication)
4. `ProspectSoul-UI-UX-Spec-v1_0.md` — §1 (Navigation Map: role-filtered sidebar, Keycloak logout)
5. `ProspectSoul-Implementation-Roadmap-v1_0.md` — Sprint 1 (acceptance criteria 1 and 6)

### Auth-specific documents (your implementation contract)
6. `docs/dev-docs/05-Auth-Requirements-v1_0.md` — what to build
7. `docs/dev-docs/06-Auth-Technical-Specification-v1_0.md` — how to build it (code patterns, SQL, API contracts)
8. `docs/dev-docs/07-Auth-Implementation-Plan-v1_0.md` — build sequence (Phase 0 through Phase 17)

### Design templates (your visual implementation reference)
9. `docs/dev-docs/10-Auth-Login-Template.html` — login page design
10. `docs/dev-docs/11-Auth-ForgotPassword-Template.html` — forgot-password flow design
11. `docs/dev-docs/12-Auth-UserRole-Management-Template.html` — user and role admin panel design

### Also read
12. `CLAUDE.md` — repository conventions
13. `ProspectSoul_TechnicalTeam_Developer_Guide.pdf` — §15 (Keycloak Authentication), §16 (Role Boundaries)

Document hierarchy must be respected. If documents conflict, do not silently choose one. Identify the conflict.

## OBJECTIVE
Implement a complete authentication and role-based access control system including:
- Keycloak OIDC integration (realm, clients, roles, seed users)
- Custom-branded login page matching the design template
- Custom-branded forgot-password flow matching the design template
- Logout with full session cleanup
- JWT validation and role-based method-level authorization on all endpoints
- User management admin panel (CRUD + activate/deactivate) matching the design template
- Role management admin panel (view, edit, assign) matching the design template
- Frontend route guards and role-filtered navigation
- Flyway migrations for all auth tables
- Fake data seeding (7 users, 5 roles)
- Audit actor attribution from validated JWTs
- Comprehensive tests

This is a real end-to-end implementation across:
- Spring Boot backend
- React frontend
- PostgreSQL database
- Docker Compose environment

## FIRST: INSPECT THE REPOSITORY
Before changing any code:
- inspect the full repository structure
- inspect existing security configuration
- inspect existing Docker Compose (is Keycloak already there?)
- inspect existing Keycloak realm exports
- inspect existing frontend auth code (keycloak-js, AuthProvider, guards)
- inspect existing Flyway migrations — identify the next version number
- inspect existing audit infrastructure (AuditService, audit_log table)
- inspect existing users/roles tables or entities
- inspect existing API controllers for @PreAuthorize annotations
- inspect existing error handling (problem+json handlers)
- inspect existing frontend routing, app shell, sidebar
- inspect CLAUDE.md for project conventions

Return a **short gap assessment** before implementation listing:
- what already exists and can be reused
- what needs to be created
- what needs to be modified
- any conflicts between existing code and the specifications

## DESIGN TEMPLATE INTEGRATION — CRITICAL
The three HTML design template files (10, 11, 12) are the **pixel-accurate visual reference** for the frontend implementation.

Before building any frontend page:
1. **Open the template file** and study its structure
2. **Extract the design tokens** into your shared CSS/theme:
   - Colors: `--ps-navy: #162032`, `--ps-copper: #c27a3e`, `--ps-copper-glow: #d4944f`, etc.
   - Fonts: `'Fraunces'` for display/headings, `'Inter'` for body/UI
   - Spacing, border-radius, shadows — all defined as CSS variables in the templates
   - Component patterns: field styling, buttons, badges, cards, modals, table rows
3. **Create a shared theme file** (`frontend/src/styles/theme.css` or equivalent) containing all tokens
4. **Implement each page to match the template exactly**: layout, typography, colors, spacing, component shapes, interactive states (hover, focus, error, loading, empty)
5. **Responsive behavior**: split layout on desktop, single column on mobile (brand panel hidden)

The design tokens are consistent across all three templates — extract them once and reuse everywhere.

Role badge colors per the design template:
- Analyst: blue (#e8f0fc / #2d5fa3)
- Sales Lead: purple (#ede8fc / #5a44a3)
- Admin: copper (#f8ead5 / #96592b)
- Viewer: green (#e8f5ee / #1e6b44)
- COO: rose (#fce8ed / #963b4a)

Avatar colors per role: Analyst #4a7fb5, Sales Lead #6a5fb5, Admin copper, Viewer #5fb59e, COO #b55f6a.

## KEYCLOAK REALM
Create `docker/keycloak/realm-export.json` per Technical Spec §2:

Realm: `vyoog`
Clients: `prospectsoul-web` (public, PKCE, direct access grants), `prospectsoul-api` (bearer-only)
Roles: PS_ANALYST, PS_SALES_LEAD, PS_ADMIN, PS_VIEWER, PS_COO

Seed users with deterministic UUIDs:
```
analyst    / analyst123    → PS_ANALYST    / a1000000-0000-0000-0000-000000000001 / Priya Sharma
analyst2   / analyst123    → PS_ANALYST    / a1000000-0000-0000-0000-000000000002 / Deepa Krishnan
saleslead  / saleslead123  → PS_SALES_LEAD / b2000000-0000-0000-0000-000000000001 / Kumar Rajan
admin      / admin123      → PS_ADMIN      / c3000000-0000-0000-0000-000000000001 / Ravi Chandran
viewer     / viewer123     → PS_VIEWER     / d4000000-0000-0000-0000-000000000001 / Meera Natarajan
viewer2    / viewer123     → PS_VIEWER     / d4000000-0000-0000-0000-000000000002 / Arun Prakash
coo        / coo123        → PS_COO        / e5000000-0000-0000-0000-000000000001 / Senthil Kumar
```

Docker Compose must auto-import this realm on startup.

## FLYWAY MIGRATIONS
Create migrations for (use next available version numbers):
1. Auth tables: `roles`, `users`, `user_roles` — exact schema in Tech Spec §3
2. Seed roles: 5 roles with display names, descriptions, permissions JSON
3. Seed users: 7 users with UUIDs matching Keycloak realm export
4. Seed user-role assignments: 7 links

UUIDs in Flyway seed data MUST match the Keycloak realm export so JWT subjects resolve correctly.

Never modify existing migrations.

## BACKEND SECURITY
Implement per Tech Spec §4:
- SecurityConfig: CSRF off, CORS, STATELESS, JWT resource server, role mapping from `realm_access.roles`
- RoleConstants: PS_ANALYST through PS_COO + composite expressions (HAS_MUTATE, HAS_EXPORT, HAS_CONFIGURE, HAS_READ)
- CurrentUser: actorId from JWT sub, name from preferred_username, roles from authorities
- KeycloakAdminConfig: admin client bean for user management
- problem+json responses for 401 and 403

## BACKEND ENDPOINTS
Auth (public):
```
POST /api/v1/auth/forgot-password   { "email": "..." }
```

User management (Admin only):
```
GET    /api/v1/admin/users
GET    /api/v1/admin/users/{id}
POST   /api/v1/admin/users
PATCH  /api/v1/admin/users/{id}
POST   /api/v1/admin/users/{id}/activate
POST   /api/v1/admin/users/{id}/deactivate
```

Role management (Admin only):
```
GET    /api/v1/admin/roles
GET    /api/v1/admin/roles/{id}
PATCH  /api/v1/admin/roles/{id}
GET    /api/v1/admin/roles/{id}/users
POST   /api/v1/admin/roles/{id}/users
DELETE /api/v1/admin/roles/{id}/users/{userId}
```

All mutations call AuditService.record() with actor from JWT.

## @PreAuthorize ON ALL ENDPOINTS
Apply to every controller method per Tech Spec §10:
- Read: HAS_READ (all authenticated roles)
- Mutate: HAS_MUTATE (Analyst + Sales Lead + Admin)
- Export: HAS_EXPORT (Sales Lead + Admin)
- Configure + User/Role management: HAS_CONFIGURE (Admin only)

## FRONTEND — AUTH FLOW
- keycloak-js with login-required, PKCE S256, silent check SSO
- AuthProvider wrapping the entire app
- useAuth hook: user, roles, token, logout, hasRole
- usePermissions hook: canMutate, canExport, canConfigure, canManageUsers
- API client with auto bearer token + refresh before expiry
- 401 from API → redirect to login

## FRONTEND — PAGES (match design templates exactly)
1. **LoginPage** → match `10-Auth-Login-Template.html`
2. **ForgotPasswordPage** → match `11-Auth-ForgotPassword-Template.html`
3. **ResetConfirmation** → match confirmation state in template 11
4. **UsersPage** (/settings/users) → match Users tab in `12-Auth-UserRole-Management-Template.html`
5. **UserDetailModal** → match Create User modal in template 12
6. **RolesPage** (/settings/roles) → match Roles tab in template 12
7. **RoleDetailDrawer** → role detail with assigned users

## FRONTEND — ROUTE GUARDS
```
/login                → public (unauthenticated only)
/forgot-password      → public
/                     → all authenticated
/companies            → all authenticated
/reports              → all authenticated
/imports              → Analyst + Sales Lead + Admin
/triage               → Analyst + Sales Lead + Admin
/research/*           → Analyst + Sales Lead + Admin
/ready                → Sales Lead + Admin
/exports              → Sales Lead + Admin
/settings/*           → Admin only
/settings/users       → Admin only
/settings/roles       → Admin only
```

## FRONTEND — ROLE-FILTERED SIDEBAR
Per UI/UX Spec §1:
- Settings / Users & Roles: Admin only
- Ready Pool / Exports: Sales Lead + Admin
- Import / Triage / Research: Analyst + Sales Lead + Admin
- Dashboard / Companies / Reports: all authenticated

## TESTS
Unit:
- JWT role extraction and mapping
- CurrentUser utilities
- RoleConstants expressions per role
- Permission hooks per role
- UserService validation
- RoleService immutability

Integration (Testcontainers):
- Each seed role obtains valid token
- Viewer → POST /companies → 403
- Analyst → POST /exports → 403
- COO → POST /companies → 403
- Analyst → POST /admin/users → 403
- Admin → full user CRUD
- Admin → role assignment
- Missing/expired/malformed token → 401 problem+json
- Mutation → audit row with JWT actor UUID
- Flyway clean run from empty DB
- Seed data: 7 users, 5 roles present

Frontend:
- LoginPage renders, handles errors
- ForgotPasswordPage flow
- RequireRole/RequireAuth behavior
- Sidebar role filtering
- UsersPage for Admin
- API client bearer token

## DEFINITION OF DONE
Do not claim completion until:
- Docker Compose starts with Keycloak + realm + 5 roles + 7 users
- Login page matches design template (10)
- Forgot-password page matches design template (11)
- User management page matches design template (12)
- Role management page matches design template (12)
- Each seed user logs in and sees correct role-filtered Dashboard
- Sidebar filters by role correctly
- UserMenu shows name, role badge, logout
- Logout clears session completely
- Admin can CRUD users, view/manage roles
- Non-Admin → 403 on admin endpoints
- Viewer → 403 on all mutations
- 401/403 return problem+json
- All audit rows have JWT actor
- Flyway runs clean from empty DB
- Seed data verified (7 users, 5 roles, 7 assignments)
- All unit tests pass
- All integration tests pass
- All frontend tests pass
- OpenAPI has security annotations
- README documents: Docker setup, seed credentials, login walkthrough, user management walkthrough

## FINAL RESPONSE FORMAT
Report:
1. Files created/changed (full list)
2. Database migrations (version numbers, what each does)
3. Backend implementation (packages, classes, endpoints)
4. Frontend implementation (pages, components, routes, auth integration)
5. Design template integration (tokens extracted, pages matched, deviations if any)
6. Docker Compose / Keycloak realm
7. Tests and exact results
8. Acceptance criteria PASS/FAIL/BLOCKED with evidence
9. Specification deviations (with justification)
10. Remaining work

Do not claim PASS without evidence.

## SCOPE CONTROL
Do not implement:
- service-to-service client-credentials grant (deferred)
- MFA (Keycloak admin concern)
- user self-registration
- session management UI
- custom Keycloak email templates
- OAuth2 social login
- API key management
- custom Keycloak login theme (use direct access grants from custom React page for v1)
- any Company/Import/Research/Qualification business logic beyond minimal endpoint stubs needed to test auth

Start by inspecting the repository and reading all authoritative documents.
