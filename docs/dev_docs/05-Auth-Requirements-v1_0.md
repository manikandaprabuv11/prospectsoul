<!--
Document: 05-Auth-Requirements-v1.0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Scope: Authentication + RBAC + User/Role Management + Seeding
Location: docs/dev-docs/05-Auth-Requirements-v1_0.md
Audience: Product, Engineering, QA, Claude Code
-->
# ProspectSoul — Authentication & RBAC Requirements

**Version:** 1.0  
**Status:** Ready for implementation  
**Scope:** Keycloak OIDC integration, custom login/logout/forgot-password UI, role-based access control, user and role management admin panel, Flyway migrations, fake data seeding

## 1. Purpose
Implement a complete authentication and role-based access control system for ProspectSoul across the existing Spring Boot backend and React frontend.

The system must deliver:
- Custom-branded login, logout, and forgot-password screens in the React frontend (not Keycloak's default theme)
- Keycloak OIDC as the identity provider behind these screens
- JWT validation, role extraction, and method-level authorization on every API endpoint
- A full user management admin panel (list, create, edit, activate/deactivate, assign roles)
- A full role management admin panel (list roles, view permissions, assign users)
- Frontend route guards and role-filtered navigation
- Users mirror table with Flyway migrations
- Comprehensive fake data seeding for development and testing
- Audit actor attribution from validated JWTs

This is a foundational vertical slice. Every subsequent feature depends on authentication being complete, not stubbed.

## 2. Authoritative project context
The existing ProspectSoul documentation hierarchy is:
1. PRD v1.1 — §2 (Users and Roles), §9 (Technical Stack: Keycloak)
2. Domain Model v1.0 — §5 (Business Invariant: every mutation is attributable)
3. Technical Design Specification v1.0 — §3 (Security & Authentication), §2 (Audit)
4. UI/UX Specification v1.0 — §1 (Navigation Map: role-filtered sidebar, Keycloak logout)
5. Implementation Roadmap v1.0 — Sprint 1 (acceptance criteria 1 and 6)
6. Technical-Team Developer Guide — §15 (Keycloak Authentication), §16 (Role Boundaries)

Design reference files (in `docs/dev-docs/`):
- `10-Auth-Login-Template.html` — login page design
- `11-Auth-ForgotPassword-Template.html` — forgot-password flow design
- `12-Auth-UserRole-Management-Template.html` — user and role admin panel design

Do not silently resolve conflicts. The implementation must preserve the documented architecture and scope.

## 3. Identity provider
Keycloak is the single identity provider:
- Realm: `vyoog`
- SPA client: `prospectsoul-web` — public client, Authorization Code + PKCE
- API client: `prospectsoul-api` — bearer-only resource server
- Roles are Keycloak realm roles in `realm_access.roles`

ProspectSoul never stores passwords. Keycloak owns authentication. The custom login UI calls Keycloak's token endpoint — it does not implement its own credential store.

## 4. Roles
Five roles, defined in PRD §2:

| Role | Keycloak realm role | Who |
|---|---|---|
| Research Analyst | PS_ANALYST | Inside-sales / research team |
| Sales Lead | PS_SALES_LEAD | Head of sales |
| Admin | PS_ADMIN | Ops/tech owner |
| Viewer | PS_VIEWER | Founders, marketing |
| COO | PS_COO | Senthil |

## 5. Capability matrix

| Capability | Analyst | Sales Lead | Admin | Viewer | COO |
|---|---|---|---|---|---|
| Import, triage, research, verify, qualify | ✓ | ✓ | ✓ | — | — |
| Tier override (mandatory reason) | ✓ | ✓ | ✓ | — | — |
| Create export | — | ✓ | ✓ | — | — |
| Configure ICP / templates / reasons / caps | — | — | ✓ | — | — |
| Manage users and roles | — | — | ✓ | — | — |
| Read / search / reports | ✓ | ✓ | ✓ | ✓ | ✓ |

## 6. Login
### Requirements
- Custom-branded login page rendered by the React frontend
- The design must match `docs/dev-docs/10-Auth-Login-Template.html` exactly
- Form fields: username/email, password, "Remember me" checkbox
- "Forgot password?" link navigates to the forgot-password screen
- On submit: call Keycloak token endpoint via Authorization Code + PKCE flow
- On success: store tokens in memory (keycloak-js adapter), redirect to Dashboard
- On failure: display error message inline — "Invalid username or password", never a stack trace
- Keycloak's default login page must NOT be shown; the SPA handles the entire login experience visually, with keycloak-js handling the redirect-based OIDC flow behind the scenes

### Behavior notes
- The custom login screen is the SPA's landing state when unauthenticated. keycloak-js with `onLoad: 'login-required'` redirects to Keycloak, which renders the login. To achieve the custom-branded experience, the Keycloak login theme must be customized to match the design template OR the SPA intercepts the flow with a custom page and uses Keycloak's REST endpoints for direct grant (appropriate for internal tools).
- Recommended approach for v1 internal tool: use Keycloak's direct access grant (Resource Owner Password Credentials) from the custom login page for the simplest integration, with a note that this should migrate to standard PKCE redirect if the tool ever becomes external-facing.

## 7. Logout
### Requirements
- Logout action in the user menu (visible on every authenticated page)
- On logout: revoke tokens, clear in-memory session, call Keycloak logout endpoint
- Redirect to the login page after logout
- Session is fully cleared — refreshing the browser after logout must not restore the session

## 8. Forgot password
### Requirements
- Accessible from the login page via "Forgot password?" link
- The design must match `docs/dev-docs/11-Auth-ForgotPassword-Template.html`
- Step 1: enter email/username → submit
- Backend triggers Keycloak's "forgot password" action (Keycloak sends the reset email)
- Step 2: confirmation screen — "Check your email for a reset link"
- Step 3: Keycloak handles the actual password reset via its built-in reset flow
- The forgot-password page is branded to match ProspectSoul, not Keycloak defaults

## 9. User management (Admin panel)
### Requirements
- Accessible only to PS_ADMIN role
- The design must match the Users tab in `docs/dev-docs/12-Auth-UserRole-Management-Template.html`
- User list: table with name, email, username, role, status (active/inactive), last login, created date
- Search users by name, email, or username
- Sort by any column
- Pagination
- Create user: form with full name, email, username, role assignment → creates user in Keycloak + local mirror
- Edit user: change name, email, role assignment → syncs to Keycloak + local mirror
- Activate/deactivate user: toggle active status (soft disable, not delete)
- Never hard-delete a user — deactivation only (audit trail must preserve actor references)
- View user detail: shows assigned role, activity summary (audit count), status, timestamps

### Data model — users mirror
```
users:
  id: UUID (matches Keycloak user UUID)
  username: VARCHAR NOT NULL UNIQUE
  full_name: VARCHAR NOT NULL
  email: VARCHAR NOT NULL
  role: VARCHAR NOT NULL (PS_ANALYST | PS_SALES_LEAD | PS_ADMIN | PS_VIEWER | PS_COO)
  active: BOOLEAN NOT NULL DEFAULT true
  last_login_at: TIMESTAMP nullable
  avatar_url: VARCHAR nullable
  created_at: TIMESTAMP NOT NULL
  updated_at: TIMESTAMP NOT NULL
```

## 10. Role management (Admin panel)
### Requirements
- Accessible only to PS_ADMIN role
- The design must match the Roles tab in `docs/dev-docs/12-Auth-UserRole-Management-Template.html`
- Role list: cards or table showing each role with name, description, user count, permission summary
- View role detail: description, full permission list, assigned users
- Edit role metadata: description and display name (actual Keycloak role names are immutable)
- Assign/unassign users to roles
- Roles are not created or deleted through the UI in v1 — the five roles are fixed. The UI displays and manages assignments only.

### Data model — roles
```
roles:
  id: UUID
  name: VARCHAR NOT NULL UNIQUE (PS_ANALYST etc.)
  display_name: VARCHAR NOT NULL ("Research Analyst" etc.)
  description: TEXT
  permissions: JSONB (capability list for display)
  active: BOOLEAN NOT NULL DEFAULT true
  created_at: TIMESTAMP NOT NULL
  updated_at: TIMESTAMP NOT NULL

user_roles:
  id: UUID
  user_id: UUID FK → users
  role_id: UUID FK → roles
  assigned_by: UUID FK → users nullable
  assigned_at: TIMESTAMP NOT NULL
```

Note: user_roles is the local management table. The actual authority comes from Keycloak realm_access.roles in the JWT. The local table mirrors assignments for display, audit, and admin UI purposes.

## 11. Flyway migrations
Required migrations (in order, appended after existing migrations):

1. **Auth tables migration**: users, roles, user_roles, and any supporting indexes/constraints
2. **Seed roles migration**: insert the five defined roles with display names, descriptions, and permission summaries
3. **Seed users migration**: insert development seed users matching the Keycloak realm export UUIDs
4. **Seed user-role assignments**: link seed users to their roles

Migrations must be idempotent where possible and must never modify previously applied migrations.

## 12. Fake data seeding
### Seed users (development)
```
Username        Password        Role            Full Name           Email
analyst         analyst123      PS_ANALYST      Priya Sharma        priya@vyoog.com
analyst2        analyst123      PS_ANALYST      Deepa Krishnan      deepa@vyoog.com
saleslead       saleslead123    PS_SALES_LEAD   Kumar Rajan         kumar@vyoog.com
admin           admin123        PS_ADMIN        Ravi Chandran       ravi@vyoog.com
viewer          viewer123       PS_VIEWER       Meera Natarajan     meera@vyoog.com
viewer2         viewer123       PS_VIEWER       Arun Prakash        arun@vyoog.com
coo             coo123          PS_COO          Senthil Kumar       senthil@vyoog.com
```

Seven seed users (two analysts, two viewers to demonstrate list behavior) with Indian names matching the Vyoog context.

### Seed roles
```
Role             Display Name        Description
PS_ANALYST       Research Analyst    Imports files, resolves duplicates, runs AI research, verifies evidence, confirms qualifications
PS_SALES_LEAD    Sales Lead          Reviews qualified pool, approves exports, overrides tiers with reason
PS_ADMIN         Administrator       Configures ICP profiles, templates, reasons, caps, manages users and roles
PS_VIEWER        Viewer              Read-only access to search, timelines, and reports
PS_COO           COO                 Operational dashboard — pipeline throughput, source analytics, data quality
```

### Keycloak realm export
`docker/keycloak/realm-export.json` must contain all seven seed users with matching UUIDs so the local mirror resolves correctly.

## 13. Backend authorization
- Spring Security resource server with JWT validation
- Roles extracted from `realm_access.roles`, mapped to Spring GrantedAuthority
- Method-level `@PreAuthorize` on every controller method
- Actor identity from JWT subject for audit attribution
- 401 for missing/expired/malformed tokens
- 403 for insufficient role
- Both return `application/problem+json`

## 14. Frontend authorization
- keycloak-js integration with redirect login and silent refresh
- AuthProvider context: user, roles, token, logout, hasRole
- RequireRole route guard component
- Role-filtered sidebar navigation
- Role-filtered action buttons
- API client with automatic bearer token attachment

## 15. Error handling
- 401/403 return `application/problem+json` with meaningful `detail`
- Frontend displays `detail` verbatim — no generic error messages
- Login failures show "Invalid username or password"
- Session expiry triggers re-authentication, not a silent failure

## 16. Scope boundaries
This scope covers:
- Custom login page (matching design template)
- Logout with full session cleanup
- Forgot-password flow (Keycloak-backed)
- User management admin panel (CRUD + activate/deactivate)
- Role management admin panel (view, edit metadata, assign users)
- Flyway migrations for all auth tables
- Fake data seeding (7 users, 5 roles)
- Keycloak realm export with seed data
- JWT validation and role mapping
- @PreAuthorize on all endpoints
- Frontend route guards and role-filtered UI
- Audit actor attribution
- problem+json error handling

This scope does NOT cover:
- User self-registration (internal tool, Admin creates users)
- MFA setup (Keycloak realm admin concern, not application code)
- Service-to-service client-credentials grant (deferred)
- Custom Keycloak email templates (use Keycloak defaults for reset emails)
- Session management UI (no "active sessions" screen)
- OAuth2 social login providers
- API key management

## 17. Acceptance criteria
The feature is complete when:
- Keycloak realm loads automatically in Docker Compose with 5 roles and 7 seed users
- Custom login page renders matching the design template
- Each seed user can log in and see the Dashboard
- Logout clears session and returns to login
- Forgot-password initiates Keycloak reset flow
- Sidebar is role-filtered per capability matrix
- UserMenu shows current user name, role badge, and logout
- Admin can list/create/edit/deactivate users through the admin panel
- Admin can view roles and manage user-role assignments
- Non-Admin receives 403 on user/role management endpoints
- Viewer receives 403 on all mutating endpoints
- Analyst receives 403 on export endpoints
- COO receives 403 on mutation endpoints
- Expired/missing token returns 401 problem+json
- Every audit_log row has actor_id from JWT subject
- Flyway migrations run cleanly from empty database
- Seed data populates all 7 users and 5 roles
- All unit tests pass
- All integration tests pass (including 403 paths)
- All frontend auth tests pass
- Design templates are implemented pixel-accurately in the React frontend
- OpenAPI has security annotations on every endpoint
- README documents seed credentials and complete login-to-dashboard walkthrough
