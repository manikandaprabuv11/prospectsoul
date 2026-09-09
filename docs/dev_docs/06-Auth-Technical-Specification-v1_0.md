<!--
Document: 06-Auth-Technical-Specification-v1.0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Scope: Authentication + RBAC + User/Role Management + Seeding
Location: docs/dev-docs/06-Auth-Technical-Specification-v1_0.md
Audience: Product, Engineering, QA, Claude Code
-->
# ProspectSoul — Authentication & RBAC Technical Specification

**Version:** 1.0  
**Status:** Implementation contract for this scope

## 1. Architecture

### Backend packages
```text
com.vyoog.prospectsoul/
  config/
    SecurityConfig.java              # resource server, CORS, CSRF, JWT converter
    WebConfig.java                   # general web config
    KeycloakAdminConfig.java         # Keycloak Admin Client bean for user management
  common/
    audit/
      AuditService.java
      AuditEntity.java
      AuditRepository.java
    security/
      RoleConstants.java             # role strings + composite @PreAuthorize expressions
      CurrentUser.java               # JWT actor extraction utility
      PreAuthorizeExpressions.java   # reusable SpEL constants
    errors/
      ProblemJsonExceptionHandler.java
      AuthExceptionHandler.java      # 401/403 → problem+json
  admin/
    users/
      UserController.java            # /api/v1/admin/users
      UserService.java               # local mirror + Keycloak Admin sync
      UserRepository.java
      UserEntity.java
      UserDto.java
      UserCreateRequest.java
      UserUpdateRequest.java
      UserMapper.java
    roles/
      RoleController.java            # /api/v1/admin/roles
      RoleService.java
      RoleRepository.java
      RoleEntity.java
      RoleDto.java
      RoleMapper.java
    userroles/
      UserRoleEntity.java
      UserRoleRepository.java
```

### Frontend structure
```text
frontend/src/
  auth/
    keycloak.ts                      # keycloak-js init + config
    AuthProvider.tsx                  # React context, wraps app
    RequireRole.tsx                   # route guard
    RequireAuth.tsx                   # auth gate (redirects to login)
    guards.ts                        # canMutate, canExport, canConfigure, canManageUsers
    useAuth.ts                       # hook: user, roles, logout, hasRole
    usePermissions.ts                # hook: computed permission booleans
  pages/
    auth/
      LoginPage.tsx                  # custom branded login
      LoginPage.module.css           # login styles (from design template)
      ForgotPasswordPage.tsx         # forgot password step 1
      ForgotPasswordPage.module.css
      ResetConfirmation.tsx          # "check your email" step 2
    admin/
      UsersPage.tsx                  # user list + CRUD
      UsersPage.module.css
      UserDetailModal.tsx            # create/edit user modal
      RolesPage.tsx                  # role list + assignment
      RolesPage.module.css
      RoleDetailDrawer.tsx           # role detail + assign users
  components/
    UserMenu.tsx                     # header user dropdown
    Sidebar.tsx                      # role-filtered nav (update existing)
    RoleBadge.tsx                    # colored role indicator
    StatusBadge.tsx                  # active/inactive indicator
    PermissionTag.tsx                # permission display chip
  api/
    client.ts                        # fetch wrapper with bearer token
    authApi.ts                       # login, logout, forgot-password calls
    usersApi.ts                      # /admin/users endpoints
    rolesApi.ts                      # /admin/roles endpoints
  lib/
    constants.ts                     # role names, permission labels
```

## 2. Keycloak realm configuration

### Realm
```text
Realm:              vyoog
Login theme:        prospectsoul (custom theme matching design templates)
Email theme:        keycloak (default — custom email templates deferred)
Token lifespan:     access = 5 min, refresh = 30 min, SSO session = 10 hours
```

### Clients

#### prospectsoul-web
```text
Client ID:              prospectsoul-web
Access Type:            public
Standard Flow:          enabled
PKCE:                   S256
Direct Access Grants:   enabled (for custom login page — internal tool only)
Valid Redirect URIs:    http://localhost:5173/*, ${PROD_URL}/*
Web Origins:            http://localhost:5173, ${PROD_URL}
```

#### prospectsoul-api
```text
Client ID:              prospectsoul-api
Access Type:            bearer-only
```

### Realm roles
```text
PS_ANALYST
PS_SALES_LEAD
PS_ADMIN
PS_VIEWER
PS_COO
```

### Seed users (7 users)
```text
Username      Password       Role           UUID (fixed for mirror sync)            Full Name          Email
analyst       analyst123     PS_ANALYST     a1000000-0000-0000-0000-000000000001    Priya Sharma       priya@vyoog.com
analyst2      analyst123     PS_ANALYST     a1000000-0000-0000-0000-000000000002    Deepa Krishnan     deepa@vyoog.com
saleslead     saleslead123   PS_SALES_LEAD  b2000000-0000-0000-0000-000000000001    Kumar Rajan        kumar@vyoog.com
admin         admin123       PS_ADMIN       c3000000-0000-0000-0000-000000000001    Ravi Chandran      ravi@vyoog.com
viewer        viewer123      PS_VIEWER      d4000000-0000-0000-0000-000000000001    Meera Natarajan    meera@vyoog.com
viewer2       viewer123      PS_VIEWER      d4000000-0000-0000-0000-000000000002    Arun Prakash       arun@vyoog.com
coo           coo123         PS_COO         e5000000-0000-0000-0000-000000000001    Senthil Kumar      senthil@vyoog.com
```

UUIDs are deterministic so Flyway seed data matches the realm export.

## 3. Database schema (Flyway migrations)

### Migration: V{N}__auth_tables.sql
```sql
-- Roles table
CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(50)  NOT NULL UNIQUE,
    display_name    VARCHAR(100) NOT NULL,
    description     TEXT,
    permissions     JSONB        NOT NULL DEFAULT '[]',
    active          BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_roles_name ON roles(name);
CREATE INDEX idx_roles_active ON roles(active);

-- Users table (mirrors Keycloak)
CREATE TABLE users (
    id              UUID PRIMARY KEY,  -- matches Keycloak user UUID
    username        VARCHAR(255) NOT NULL UNIQUE,
    full_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT true,
    last_login_at   TIMESTAMP WITH TIME ZONE,
    avatar_url      VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(active);

-- User-role assignments (local tracking for admin UI and audit)
CREATE TABLE user_roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    role_id         UUID NOT NULL REFERENCES roles(id),
    assigned_by     UUID REFERENCES users(id),
    assigned_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE(user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);
```

### Migration: V{N+1}__seed_roles.sql
```sql
INSERT INTO roles (id, name, display_name, description, permissions) VALUES
('10000000-0000-0000-0000-000000000001', 'PS_ANALYST', 'Research Analyst',
 'Imports files, resolves duplicates, runs AI research batches, verifies evidence, logs activities, confirms qualifications. The primary daily user.',
 '["import","triage","research","verify","qualify","read","search","reports"]'),

('10000000-0000-0000-0000-000000000002', 'PS_SALES_LEAD', 'Sales Lead',
 'Reviews qualified pool, approves exports, overrides tiers with reason, requests re-research.',
 '["import","triage","research","verify","qualify","export","override","read","search","reports"]'),

('10000000-0000-0000-0000-000000000003', 'PS_ADMIN', 'Administrator',
 'Configures ICP profiles, import templates, disqualification reasons, activity types, user roles. Full system access.',
 '["import","triage","research","verify","qualify","export","override","configure","manage_users","manage_roles","read","search","reports"]'),

('10000000-0000-0000-0000-000000000004', 'PS_VIEWER', 'Viewer',
 'Read-only access to search, timelines, and reports. No editing or workflow actions.',
 '["read","search","reports"]'),

('10000000-0000-0000-0000-000000000005', 'PS_COO', 'COO',
 'Weekly operational dashboard — pipeline throughput, source analytics, data quality. No record-level work.',
 '["read","search","reports","dashboard"]');
```

### Migration: V{N+2}__seed_users.sql
```sql
INSERT INTO users (id, username, full_name, email, role, active) VALUES
('a1000000-0000-0000-0000-000000000001', 'analyst',   'Priya Sharma',     'priya@vyoog.com',   'PS_ANALYST',     true),
('a1000000-0000-0000-0000-000000000002', 'analyst2',  'Deepa Krishnan',   'deepa@vyoog.com',   'PS_ANALYST',     true),
('b2000000-0000-0000-0000-000000000001', 'saleslead', 'Kumar Rajan',      'kumar@vyoog.com',   'PS_SALES_LEAD',  true),
('c3000000-0000-0000-0000-000000000001', 'admin',     'Ravi Chandran',    'ravi@vyoog.com',    'PS_ADMIN',       true),
('d4000000-0000-0000-0000-000000000001', 'viewer',    'Meera Natarajan',  'meera@vyoog.com',   'PS_VIEWER',      true),
('d4000000-0000-0000-0000-000000000002', 'viewer2',   'Arun Prakash',     'arun@vyoog.com',    'PS_VIEWER',      true),
('e5000000-0000-0000-0000-000000000001', 'coo',       'Senthil Kumar',    'senthil@vyoog.com', 'PS_COO',         true);

-- Link users to roles
INSERT INTO user_roles (user_id, role_id, assigned_at) VALUES
('a1000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', now()),
('a1000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', now()),
('b2000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', now()),
('c3000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003', now()),
('d4000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000004', now()),
('d4000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000004', now()),
('e5000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000005', now());
```

## 4. Backend security configuration

### SecurityConfig.java
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()  // login, forgot-password
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter()))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(problemJson401EntryPoint())
                .accessDeniedHandler(problemJson403Handler())
            );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null) return Collections.emptyList();
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles == null) return Collections.emptyList();
            return roles.stream()
                .filter(r -> r.startsWith("PS_"))
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());
        });
        return converter;
    }
}
```

### RoleConstants.java
```java
public final class RoleConstants {
    private RoleConstants() {}

    public static final String ANALYST     = "PS_ANALYST";
    public static final String SALES_LEAD  = "PS_SALES_LEAD";
    public static final String ADMIN       = "PS_ADMIN";
    public static final String VIEWER      = "PS_VIEWER";
    public static final String COO         = "PS_COO";

    public static final String HAS_MUTATE    = "hasAnyRole('PS_ANALYST','PS_SALES_LEAD','PS_ADMIN')";
    public static final String HAS_EXPORT    = "hasAnyRole('PS_SALES_LEAD','PS_ADMIN')";
    public static final String HAS_CONFIGURE = "hasRole('PS_ADMIN')";
    public static final String HAS_READ      = "hasAnyRole('PS_ANALYST','PS_SALES_LEAD','PS_ADMIN','PS_VIEWER','PS_COO')";
}
```

### CurrentUser.java
```java
public final class CurrentUser {
    private CurrentUser() {}

    public static String id(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getSubject();
        }
        throw new IllegalStateException("Expected JWT authentication");
    }

    public static String name(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getClaimAsString("preferred_username");
        }
        return "unknown";
    }

    public static List<String> roles(Authentication auth) {
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith("ROLE_PS_"))
            .map(a -> a.substring(5))
            .toList();
    }
}
```

## 5. Auth API endpoints (public, no JWT required)
```text
POST /api/v1/auth/forgot-password     # { "email": "..." } → triggers Keycloak reset
```

Login and token exchange are handled by keycloak-js redirect flow (frontend-only). No custom login endpoint on the backend.

## 6. User management API endpoints (Admin only)
```text
GET    /api/v1/admin/users                    # list, search, paginate, sort
GET    /api/v1/admin/users/{id}               # detail with role + audit summary
POST   /api/v1/admin/users                    # create user in Keycloak + local mirror
PATCH  /api/v1/admin/users/{id}               # update user profile + role
POST   /api/v1/admin/users/{id}/activate      # reactivate
POST   /api/v1/admin/users/{id}/deactivate    # soft disable
```

### User list request
```text
GET /api/v1/admin/users?q=priya&role=PS_ANALYST&active=true&page=0&size=25&sort=full_name,asc
```

### User list response
```json
{
  "content": [
    {
      "id": "a1000000-0000-0000-0000-000000000001",
      "username": "analyst",
      "full_name": "Priya Sharma",
      "email": "priya@vyoog.com",
      "role": "PS_ANALYST",
      "role_display_name": "Research Analyst",
      "active": true,
      "last_login_at": "2026-07-15T10:30:00Z",
      "created_at": "2026-07-01T00:00:00Z"
    }
  ],
  "page": 0,
  "size": 25,
  "total_elements": 7,
  "total_pages": 1
}
```

### Create user request
```json
{
  "username": "newanalyst",
  "full_name": "Kavitha Sundaram",
  "email": "kavitha@vyoog.com",
  "password": "tempPassword123",
  "role": "PS_ANALYST"
}
```

The service creates the user in Keycloak (via Keycloak Admin Client), assigns the realm role, then creates the local mirror record. If Keycloak creation fails, the local record is not created (transactional).

### Update user request
```json
{
  "full_name": "Kavitha S.",
  "email": "kavitha.s@vyoog.com",
  "role": "PS_SALES_LEAD"
}
```

Role changes update both Keycloak (remove old role, add new) and the local mirror.

## 7. Role management API endpoints (Admin only)
```text
GET    /api/v1/admin/roles                    # list all roles with user counts
GET    /api/v1/admin/roles/{id}               # detail with permission list + assigned users
PATCH  /api/v1/admin/roles/{id}               # update description/display_name only
GET    /api/v1/admin/roles/{id}/users         # users assigned to this role
POST   /api/v1/admin/roles/{id}/users         # assign user to role
DELETE /api/v1/admin/roles/{id}/users/{userId} # unassign user from role
```

### Role list response
```json
{
  "content": [
    {
      "id": "10000000-0000-0000-0000-000000000001",
      "name": "PS_ANALYST",
      "display_name": "Research Analyst",
      "description": "Imports files, resolves duplicates...",
      "permissions": ["import","triage","research","verify","qualify","read","search","reports"],
      "user_count": 2,
      "active": true
    }
  ]
}
```

## 8. Keycloak Admin Client integration
For user management, the backend uses the Keycloak Admin Client SDK:

```java
@Configuration
public class KeycloakAdminConfig {

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    @Bean
    public Keycloak keycloakAdmin() {
        return KeycloakBuilder.builder()
            .serverUrl(serverUrl)
            .realm("master")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .build();
    }
}
```

UserService uses this to:
- Create users in Keycloak
- Update user profiles
- Enable/disable users
- Assign/remove realm roles
- Trigger forgot-password emails

## 9. Frontend implementation

### Login page
- Matches `docs/dev-docs/10-Auth-Login-Template.html` design exactly
- Left panel: ProspectSoul branding, tagline, decorative element
- Right panel: login form (username, password, remember me, forgot password link)
- Calls keycloak-js `login()` which redirects to Keycloak — but the Keycloak theme must match the design template so the experience is visually seamless
- Error states: inline validation, API error display
- Responsive: form centered on mobile, split layout on desktop

### Forgot password page
- Matches `docs/dev-docs/11-Auth-ForgotPassword-Template.html` design exactly
- Single field: email address
- Submit calls `POST /api/v1/auth/forgot-password`
- Success: shows confirmation message ("Check your email")
- Back to login link
- Same branding as login page

### User management page (/settings/users)
- Matches Users tab in `docs/dev-docs/12-Auth-UserRole-Management-Template.html`
- Data table: name, email, role (with badge), status, last login, actions
- Search bar, role filter, status filter
- Create button → modal form
- Edit button → modal form (pre-filled)
- Activate/deactivate toggle
- Pagination and sorting

### Role management page (/settings/roles)
- Matches Roles tab in `docs/dev-docs/12-Auth-UserRole-Management-Template.html`
- Role cards: name, description, user count, permission tags
- Click card → drawer with full detail + assigned users list
- Assign/unassign users from role

### User menu
- Displays in the app header
- Shows: avatar placeholder (initials), full name, role badge
- Dropdown: profile summary, logout button
- Logout calls keycloak.logout() and redirects to login

## 10. Frontend auth integration patterns

### API client with token
```typescript
import { keycloak } from '../auth/keycloak';

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  try {
    await keycloak.updateToken(30);
  } catch {
    keycloak.login();
    throw new Error('Session expired');
  }

  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${keycloak.token}`,
      ...options.headers,
    },
  });

  if (response.status === 401) {
    keycloak.login();
    throw new Error('Unauthenticated');
  }

  if (!response.ok) {
    const problem = await response.json();
    throw new ApiError(problem);
  }

  return response.json();
}
```

### Permission hooks
```typescript
export function usePermissions() {
  const { roles } = useAuth();
  return {
    canMutate:      roles.some(r => ['PS_ANALYST','PS_SALES_LEAD','PS_ADMIN'].includes(r)),
    canExport:      roles.some(r => ['PS_SALES_LEAD','PS_ADMIN'].includes(r)),
    canConfigure:   roles.includes('PS_ADMIN'),
    canManageUsers: roles.includes('PS_ADMIN'),
    canRead:        true,
  };
}
```

## 11. Environment configuration

### Backend (application.yml)
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8080/realms/vyoog}

keycloak:
  admin:
    server-url: ${KEYCLOAK_ADMIN_URL:http://localhost:8080}
    realm: vyoog
    client-id: ${KEYCLOAK_ADMIN_CLIENT_ID:admin-cli}
    client-secret: ${KEYCLOAK_ADMIN_CLIENT_SECRET:}
```

### Frontend (.env.local)
```text
VITE_KEYCLOAK_URL=http://localhost:8080
VITE_KEYCLOAK_REALM=vyoog
VITE_KEYCLOAK_CLIENT_ID=prospectsoul-web
VITE_API_BASE_URL=http://localhost:8081/api/v1
```

### Docker Compose
```yaml
keycloak:
  image: quay.io/keycloak/keycloak:25.0
  command: start-dev --import-realm
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
  volumes:
    - ./docker/keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json
  ports:
    - "8080:8080"
  healthcheck:
    test: ["CMD-SHELL", "exec 3<>/dev/tcp/localhost/8080"]
    interval: 10s
    timeout: 5s
    retries: 10
```

## 12. Audit integration
Every mutating service method calls:
```java
auditService.record(
    "USER",                          // entityType
    userId,                          // entityId
    CurrentUser.id(authentication),  // actorId — from JWT, never from request
    "USER_CREATED",                  // action
    null,                            // previousState
    objectMapper.writeValueAsString(userDto)  // newState
);
```

`Propagation.MANDATORY` on AuditService.record() — must run inside an existing transaction.

## 13. Testing

### Unit tests
- JWT realm_access.roles → GrantedAuthority mapping (PS_ filter, ROLE_ prefix)
- CurrentUser.id() extracts sub claim
- CurrentUser.roles() extracts only PS_ roles
- RoleConstants expressions evaluate correctly per role
- Permission hooks (canMutate, canExport, canConfigure) per role
- UserService validates required fields on create
- RoleService rejects role name changes

### Integration tests (Testcontainers)
- Full login flow: obtain token, call protected endpoint
- Viewer → POST /api/v1/companies → 403
- Analyst → POST /api/v1/exports → 403
- COO → POST /api/v1/companies → 403
- Analyst → POST /api/v1/admin/users → 403
- Admin → POST /api/v1/admin/users → 201
- Admin → PATCH /api/v1/admin/users/{id} → 200
- Admin → POST /api/v1/admin/users/{id}/deactivate → 200
- Missing token → 401 problem+json
- Expired token → 401 problem+json
- Malformed token → 401 problem+json
- Mutation → audit row with correct JWT actor UUID
- Create user → Keycloak user exists + local mirror exists
- Deactivate user → Keycloak user disabled + local mirror updated
- Role assignment → user_roles row created + Keycloak role assigned
- Flyway migrations run cleanly from empty database
- Seed data: 7 users, 5 roles, 7 user_roles present after migration

### Frontend tests
- LoginPage renders form fields
- LoginPage shows error on failed auth
- ForgotPasswordPage submits email and shows confirmation
- RequireRole renders/redirects based on role
- RequireAuth redirects unauthenticated users
- Sidebar hides Settings for non-Admin
- Sidebar hides Exports for Viewer/COO
- UserMenu shows user name and role
- UsersPage renders user list for Admin
- API client attaches Authorization header
- API client redirects to login on 401

## 14. Non-functional targets
- Token validation <10ms per request (JWKS caching)
- User list page loads in <1s for up to 100 users
- Keycloak Admin Client calls complete in <2s
- Silent refresh completes before expiry
- No plaintext production secrets in source code
- CORS restricts origins in production profile
