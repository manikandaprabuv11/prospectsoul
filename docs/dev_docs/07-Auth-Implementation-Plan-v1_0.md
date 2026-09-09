<!--
Document: 07-Auth-Implementation-Plan-v1.0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Scope: Authentication + RBAC + User/Role Management + Seeding
Location: docs/dev-docs/07-Auth-Implementation-Plan-v1_0.md
Audience: Product, Engineering, QA, Claude Code
-->
# ProspectSoul — Authentication & RBAC Implementation Plan

**Version:** 1.0  
**Execution model:** vertical slice  
**Owner:** Engineering / Claude Code

## Phase 0 — Repository and specification audit
Tasks:
- read CLAUDE.md
- read all files in docs/dev-docs/ (05 through 12)
- read PRD v1.1 §2, §9
- read Technical Design Spec v1.0 §2, §3
- read UI/UX Spec v1.0 §1
- read Implementation Roadmap Sprint 1
- read Developer Guide §15, §16
- inspect existing backend: security config, controllers, services, entities
- inspect existing frontend: auth code, routing, app shell, components
- inspect existing Docker Compose for Keycloak service
- inspect existing Flyway migrations — identify next migration version number
- inspect existing audit infrastructure
- inspect existing users/roles tables
- inspect design template files (10, 11, 12) and extract design tokens (colors, fonts, spacing, component patterns)
- produce a short gap assessment

Exit criteria:
- no uncertainty about existing infrastructure
- clear list of what exists vs what must be built
- design tokens extracted from templates and ready for frontend implementation

## Phase 1 — Keycloak realm and Docker Compose
Tasks:
- create `docker/keycloak/realm-export.json`:
  - realm `vyoog`
  - client `prospectsoul-web` (public, PKCE, direct access grants enabled)
  - client `prospectsoul-api` (bearer-only)
  - 5 realm roles (PS_ANALYST, PS_SALES_LEAD, PS_ADMIN, PS_VIEWER, PS_COO)
  - 7 seed users with deterministic UUIDs and role assignments
  - forgot-password action enabled
  - email settings for local dev (use Keycloak's built-in email for reset flow)
- update `docker-compose.yml`:
  - Keycloak service with `--import-realm`
  - healthcheck for startup dependency
  - correct ports and environment variables
- verify `docker compose up` starts Keycloak with all roles and users
- verify each seed user can log in via Keycloak admin console

Exit criteria:
- `docker compose up` creates a working Keycloak with 5 roles and 7 users
- realm-export.json committed to docker/keycloak/

## Phase 2 — Flyway migrations and seed data
Tasks:
- identify next available migration version number
- create migration: auth tables (roles, users, user_roles with indexes and constraints)
- create migration: seed 5 roles with display names, descriptions, permissions JSON
- create migration: seed 7 users with UUIDs matching Keycloak realm export
- create migration: seed 7 user_roles linking users to roles
- verify migrations run from empty database
- verify migrations run on top of existing migrations without conflict

Exit criteria:
- `./mvnw flyway:migrate` succeeds from clean state
- roles table has 5 rows with correct data
- users table has 7 rows with UUIDs matching Keycloak
- user_roles table has 7 rows linking correctly
- existing migrations remain untouched

## Phase 3 — Backend security configuration
Tasks:
- add spring-boot-starter-oauth2-resource-server dependency
- add keycloak-admin-client dependency
- create SecurityConfig:
  - CSRF disabled
  - CORS for local/prod profiles
  - session STATELESS
  - permit: swagger, health, /api/v1/auth/**
  - authenticate: all other requests
  - JWT resource server with custom converter
  - problem+json 401 entry point
  - problem+json 403 handler
- create RoleConstants with all role strings and composite expressions
- create CurrentUser utility (id, name, roles from JWT)
- create KeycloakAdminConfig bean
- create AuthExceptionHandler for problem+json 401/403 responses
- configure application.yml with KEYCLOAK_ISSUER_URI and admin client settings
- verify backend starts and validates JWTs from local Keycloak

Exit criteria:
- backend starts without errors
- request without token → 401 problem+json
- request with invalid token → 401 problem+json
- request with valid token → passes to controller
- roles extracted correctly from JWT

## Phase 4 — Audit service and actor attribution
Tasks:
- inspect existing AuditService / audit_log table
- create or update AuditService:
  - record(entityType, entityId, actorId, action, previousState, newState)
  - Propagation.MANDATORY
  - JSON serialization for state
- create AuditEntity, AuditRepository if not present
- add audit_log migration if not already present
- verify actorId always comes from CurrentUser.id(auth)

Exit criteria:
- AuditService records mutations with JWT actor
- audit_log rows have correct actor UUIDs
- AuditService refuses to run outside a transaction

## Phase 5 — User management backend
Tasks:
- create UserEntity, UserRepository, UserDto, UserCreateRequest, UserUpdateRequest, UserMapper
- create UserService:
  - list with search, filter (role, active), pagination, sorting
  - getById with role and audit summary
  - create: validate fields → create in Keycloak → create local mirror → assign role → audit
  - update: validate → update Keycloak → update mirror → handle role change → audit
  - activate: enable in Keycloak → update mirror → audit
  - deactivate: disable in Keycloak → update mirror → audit
  - updateLastLogin: called on token validation (optional)
- create UserController with @PreAuthorize(HAS_CONFIGURE) on all methods
- handle Keycloak Admin Client errors gracefully (user already exists, connection failure)
- all mutations call AuditService.record()

Exit criteria:
- CRUD operations work end-to-end
- Keycloak and local mirror stay in sync
- non-Admin roles get 403
- all mutations audited

## Phase 6 — Role management backend
Tasks:
- create RoleEntity, RoleRepository, RoleDto, RoleMapper
- create RoleService:
  - list with user counts
  - getById with permissions and assigned users
  - update metadata (display_name, description only — name is immutable)
  - assignUser: add to user_roles + assign Keycloak realm role → audit
  - unassignUser: remove from user_roles + remove Keycloak realm role → audit
- create RoleController with @PreAuthorize(HAS_CONFIGURE)
- create UserRoleEntity, UserRoleRepository

Exit criteria:
- role list returns 5 roles with correct user counts
- assign/unassign syncs to Keycloak
- non-Admin roles get 403
- all mutations audited

## Phase 7 — Auth API endpoint (forgot-password)
Tasks:
- create AuthController:
  - POST /api/v1/auth/forgot-password (public, no JWT required)
  - accepts { email } → calls Keycloak Admin Client to trigger password reset email
  - returns 200 regardless of whether email exists (prevent enumeration)
- configure Keycloak to send reset emails (or log to console in dev)

Exit criteria:
- forgot-password endpoint returns 200
- Keycloak triggers reset action for valid email
- no user enumeration possible
- no JWT required

## Phase 8 — Method-level authorization on all existing endpoints
Tasks:
- add @PreAuthorize to every existing controller method using RoleConstants:
  - Read endpoints: HAS_READ
  - Mutation endpoints: HAS_MUTATE
  - Export endpoints: HAS_EXPORT
  - Admin endpoints: HAS_CONFIGURE
- update all mutating service methods to accept Authentication and pass actor to audit
- verify no endpoint is unprotected

Exit criteria:
- every controller method has @PreAuthorize
- every mutation passes JWT actor to AuditService
- no unprotected endpoints remain

## Phase 9 — Frontend: Keycloak integration and auth provider
Tasks:
- install keycloak-js
- create auth/keycloak.ts with initialization config
- create public/silent-check-sso.html
- create auth/AuthProvider.tsx:
  - init keycloak on mount
  - provide user, roles, token, logout, hasRole
  - loading state during init
  - redirect on auth failure
- create auth/useAuth.ts hook
- create auth/usePermissions.ts hook
- create api/client.ts with bearer token and auto-refresh
- wrap App with AuthProvider
- verify login redirect works end-to-end

Exit criteria:
- unauthenticated user redirected to Keycloak
- authenticated user sees app with correct identity
- silent refresh works
- API calls include bearer token

## Phase 10 — Frontend: Login page (design template integration)
Tasks:
- read design from docs/dev-docs/10-Auth-Login-Template.html
- extract all design tokens: colors, fonts, spacing, shadows, border-radius, layout
- create pages/auth/LoginPage.tsx matching the design pixel-for-pixel
- create pages/auth/LoginPage.module.css with extracted tokens
- implement: split layout (brand panel + form panel), logo, tagline, form fields
- Keycloak redirect flow: the login page is shown when keycloak-js redirects to Keycloak
  - Option A: customize Keycloak theme to match the design template
  - Option B: if using direct access grants, render the custom form and call token endpoint
- implement error states: invalid credentials, server error
- implement "Remember me" checkbox
- responsive: centered form on mobile, split on desktop

Exit criteria:
- login page matches design template visually
- all interactive states work (focus, error, loading)
- responsive layout works

## Phase 11 — Frontend: Forgot-password page (design template integration)
Tasks:
- read design from docs/dev-docs/11-Auth-ForgotPassword-Template.html
- create pages/auth/ForgotPasswordPage.tsx matching design
- create pages/auth/ForgotPasswordPage.module.css
- create pages/auth/ResetConfirmation.tsx (success screen)
- implement: email input, submit, loading, confirmation
- call POST /api/v1/auth/forgot-password
- "Back to login" link
- same branding as login page

Exit criteria:
- forgot-password page matches design template
- submit shows confirmation regardless of email existence
- back-to-login works

## Phase 12 — Frontend: Route guards and role-filtered UI
Tasks:
- create auth/RequireRole.tsx route guard
- create auth/RequireAuth.tsx auth gate
- wrap routes with appropriate guards:
  - /settings/*: PS_ADMIN
  - /ready, /exports: PS_SALES_LEAD + PS_ADMIN
  - /imports, /triage, /research: PS_ANALYST + PS_SALES_LEAD + PS_ADMIN
  - /, /companies, /reports: all authenticated
- update Sidebar to filter navigation by role
- create UserMenu component: name, role badge, logout
- update action buttons to check permissions (canMutate, canExport, canConfigure)

Exit criteria:
- each role sees only permitted navigation
- unauthorized route access redirects to home
- action buttons hidden for unauthorized roles
- UserMenu shows correct identity

## Phase 13 — Frontend: User management admin panel (design template integration)
Tasks:
- read design from docs/dev-docs/12-Auth-UserRole-Management-Template.html (Users tab)
- create pages/admin/UsersPage.tsx matching design
- create pages/admin/UsersPage.module.css
- create pages/admin/UserDetailModal.tsx (create + edit)
- create components/RoleBadge.tsx, StatusBadge.tsx
- create api/usersApi.ts (list, get, create, update, activate, deactivate)
- implement:
  - user data table with search, role filter, status filter, pagination, sorting
  - create user: modal form with name, email, username, password, role select
  - edit user: modal form pre-filled, role change
  - activate/deactivate toggle with confirmation
  - empty state and loading state
- route: /settings/users (Admin only)

Exit criteria:
- user list matches design template
- CRUD operations work end-to-end
- role filter and search work
- activate/deactivate syncs to backend
- non-Admin cannot access page

## Phase 14 — Frontend: Role management admin panel (design template integration)
Tasks:
- read design from docs/dev-docs/12-Auth-UserRole-Management-Template.html (Roles tab)
- create pages/admin/RolesPage.tsx matching design
- create pages/admin/RolesPage.module.css
- create pages/admin/RoleDetailDrawer.tsx
- create components/PermissionTag.tsx
- create api/rolesApi.ts (list, get, update, assigned users, assign, unassign)
- implement:
  - role cards with name, description, user count, permission tags
  - click card → side drawer with full detail
  - assigned users list in drawer
  - assign/unassign user from role
  - edit role description
- route: /settings/roles (Admin only)

Exit criteria:
- role list matches design template
- role detail drawer shows permissions and users
- assign/unassign works end-to-end
- non-Admin cannot access page

## Phase 15 — Error handling integration
Tasks:
- verify 401 returns problem+json with meaningful detail
- verify 403 returns problem+json with meaningful detail
- frontend displays problem detail verbatim
- login page shows "Invalid username or password" for auth failures
- handle network errors gracefully (Keycloak down, API down)

Exit criteria:
- all auth error paths return problem+json
- frontend never shows generic "something went wrong" for auth errors

## Phase 16 — Testing
Tasks:
- unit tests:
  - JWT role extraction
  - CurrentUser utilities
  - RoleConstants evaluation per role
  - Permission hooks per role
  - UserService validation
  - RoleService immutability check
- integration tests (Testcontainers):
  - each seed role obtains token
  - Viewer → POST /companies → 403
  - Analyst → POST /exports → 403
  - COO → POST /companies → 403
  - Analyst → POST /admin/users → 403
  - Admin → full user CRUD cycle
  - Admin → role assignment cycle
  - missing/expired/malformed token → 401
  - mutation → audit row with JWT actor
  - Flyway migrations clean run
  - seed data verification (7 users, 5 roles, 7 assignments)
- frontend tests:
  - LoginPage renders and handles errors
  - ForgotPasswordPage flow
  - RequireRole/RequireAuth behavior
  - Sidebar role filtering
  - UserMenu display
  - UsersPage renders for Admin
  - API client bearer token attachment

Exit criteria:
- all unit tests pass
- all integration tests pass
- all frontend tests pass

## Phase 17 — Documentation and handoff
Tasks:
- update OpenAPI with security annotations on every endpoint
- update README:
  - Docker Compose startup (including Keycloak)
  - seed user credentials table
  - login walkthrough per role
  - user management walkthrough
  - environment variable reference
- document design template integration approach
- document Keycloak Admin Client usage

Final report:
- changed files
- migrations created
- APIs implemented
- frontend routes/components
- design template integration status
- tests and results
- acceptance criteria PASS/FAIL
- specification deviations
- remaining work

## Release gate
Do not mark complete until:
- Docker Compose starts with Keycloak + 5 roles + 7 users
- login page matches design template
- each seed user logs in and sees correct Dashboard
- sidebar is role-filtered
- logout works completely
- forgot-password initiates reset flow
- Admin can manage users (create, edit, activate, deactivate)
- Admin can view and manage role assignments
- Non-Admin gets 403 on admin endpoints
- Viewer gets 403 on all mutations
- 401/403 return problem+json
- all audit rows have JWT actor
- Flyway runs cleanly from empty
- all tests pass
- OpenAPI has security annotations
- README complete

## Scope control
Do not implement:
- service-to-service client-credentials grant
- MFA configuration
- user self-registration
- session management UI
- custom Keycloak email templates
- OAuth2 social login
- API key management
- any Company/Import/Research/Qualification business logic beyond what's needed to test auth endpoints
