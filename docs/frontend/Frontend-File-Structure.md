# ProspectSoul Frontend — Professional File Structure

## Architecture Principle

The ProspectSoul frontend should use a **feature-first architecture with a shared/common layer**.

The primary rule is:

> **Organize frontend code by business feature first. Put genuinely reusable application code in `shared/`. Keep application composition in `app/`, API infrastructure in `services/`, and authentication concerns in `auth/`.**

The frontend should be a maintainable, scalable React + TypeScript application that can grow as ProspectSoul adds Company, Contact, Import, Triage, Activity, Evidence, Research, ICP, Qualification, Export, Reporting, Admin, and AI capabilities.

---

## Recommended Structure

```text
frontend/
│
├── public/
│   ├── favicon.ico
│   └── assets/
│
├── src/
│   │
│   ├── app/
│   │   ├── App.tsx
│   │   ├── main.tsx
│   │   │
│   │   ├── router/
│   │   │   ├── AppRouter.tsx
│   │   │   ├── ProtectedRoute.tsx
│   │   │   └── routes.tsx
│   │   │
│   │   ├── providers/
│   │   │   ├── AuthProvider.tsx
│   │   │   ├── QueryProvider.tsx
│   │   │   └── ThemeProvider.tsx
│   │   │
│   │   └── config/
│   │       ├── env.ts
│   │       └── appConfig.ts
│   │
│   ├── features/
│   │   ├── company/
│   │   ├── contact/
│   │   ├── imports/
│   │   ├── triage/
│   │   ├── activity/
│   │   ├── evidence/
│   │   ├── research/
│   │   ├── icp/
│   │   ├── qualification/
│   │   ├── export/
│   │   ├── reports/
│   │   ├── admin/
│   │   └── ai/
│   │
│   ├── shared/
│   │   ├── components/
│   │   │   ├── ui/
│   │   │   ├── data-display/
│   │   │   ├── forms/
│   │   │   ├── feedback/
│   │   │   └── navigation/
│   │   │
│   │   ├── hooks/
│   │   ├── utils/
│   │   ├── constants/
│   │   ├── types/
│   │   ├── schemas/
│   │   └── config/
│   │
│   ├── layouts/
│   │   ├── AppLayout.tsx
│   │   ├── AuthLayout.tsx
│   │   └── DashboardLayout.tsx
│   │
│   ├── services/
│   │   ├── api/
│   │   │   ├── apiClient.ts
│   │   │   ├── apiError.ts
│   │   │   └── apiTypes.ts
│   │   │
│   │   └── storage/
│   │       └── storageService.ts
│   │
│   ├── auth/
│   │   ├── authService.ts
│   │   ├── authTypes.ts
│   │   └── permissions.ts
│   │
│   ├── styles/
│   │   ├── globals.css
│   │   ├── variables.css
│   │   └── theme.css
│   │
│   └── assets/
│       ├── images/
│       ├── icons/
│       └── fonts/
│
├── tests/
│   ├── unit/
│   ├── integration/
│   └── e2e/
│
├── package.json
├── vite.config.ts
├── tsconfig.json
├── eslint.config.js
├── prettier.config.js
├── Dockerfile
└── CLAUDE.md
```

---

# 1. Architecture Rules

The frontend must follow these boundaries:

```text
app/
    Application composition and startup

features/
    Business-specific functionality

shared/
    Genuinely reusable code

layouts/
    Application page layouts

services/
    Shared technical services and API infrastructure

auth/
    Authentication and authorization infrastructure

styles/
    Global styling

assets/
    Static application assets
```

Do not mix these responsibilities.

---

# 2. `app/`

The `app/` directory contains application composition.

```text
app/
├── App.tsx
├── main.tsx
├── router/
├── providers/
└── config/
```

This directory should contain:

- application startup
- routing
- global providers
- application configuration

It should not contain business-specific Company, Contact, or Import logic.

---

# 3. `app/router/`

Routing belongs here.

```text
app/router/
├── AppRouter.tsx
├── ProtectedRoute.tsx
└── routes.tsx
```

The router maps URLs to feature pages.

Example:

```text
/companies
    → CompanyListPage

/companies/new
    → CompanyCreatePage

/companies/:id
    → CompanyDetailsPage
```

Do not place business logic inside the router.

---

# 4. `app/providers/`

Application-wide providers belong here.

Examples:

```text
AuthProvider
QueryProvider
ThemeProvider
```

Only providers required across the application should be placed here.

Feature-specific providers should remain inside the feature.

---

# 5. `app/config/`

Application configuration belongs here.

Examples:

```text
env.ts
appConfig.ts
```

Environment-specific configuration must not be hardcoded throughout the application.

---

# 6. `features/`

`features/` is the most important frontend directory.

Each business capability should own its frontend implementation.

ProspectSoul features include:

```text
features/
├── company/
├── contact/
├── imports/
├── triage/
├── activity/
├── evidence/
├── research/
├── icp/
├── qualification/
├── export/
├── reports/
├── admin/
└── ai/
```

A feature owns its:

- API integration
- components
- hooks
- pages
- types
- validation schemas
- feature-specific state

---

# 7. Feature Structure

A typical feature should use:

```text
features/company/
│
├── api/
│   └── companyApi.ts
│
├── components/
│   ├── CompanyTable.tsx
│   ├── CompanyFilters.tsx
│   ├── CompanyForm.tsx
│   ├── CompanyDetails.tsx
│   └── CompanyStatus.tsx
│
├── hooks/
│   ├── useCompanies.ts
│   ├── useCompany.ts
│   ├── useCreateCompany.ts
│   └── useUpdateCompany.ts
│
├── pages/
│   ├── CompanyListPage.tsx
│   ├── CompanyCreatePage.tsx
│   └── CompanyDetailsPage.tsx
│
├── schemas/
│   └── companySchema.ts
│
├── types/
│   └── companyTypes.ts
│
└── index.ts
```

Not every feature must contain every directory.

Only create directories when they are actually required.

---

# 8. Feature `api/`

Feature-specific API operations belong inside the feature.

Example:

```text
features/company/api/companyApi.ts
```

Possible operations:

```text
getCompanies()
getCompany()
createCompany()
updateCompany()
deleteCompany()
searchCompanies()
```

These functions must use the centralized API client.

Preferred flow:

```text
Component
    ↓
Feature Hook
    ↓
Feature API
    ↓
Shared API Client
    ↓
Spring Boot Backend
```

Do not make raw HTTP requests directly from UI components.

---

# 9. Feature `components/`

Feature-specific components belong inside the feature.

Example:

```text
features/company/components/
├── CompanyTable.tsx
├── CompanyFilters.tsx
├── CompanyForm.tsx
├── CompanyDetails.tsx
└── CompanyStatus.tsx
```

A Company-specific component should not be placed in:

```text
shared/components/
```

unless it is genuinely reusable outside Company.

---

# 10. Feature `hooks/`

Feature-specific React hooks belong inside the feature.

Example:

```text
features/company/hooks/
├── useCompanies.ts
├── useCompany.ts
├── useCreateCompany.ts
└── useUpdateCompany.ts
```

Global reusable hooks belong in:

```text
shared/hooks/
```

Do not put feature-specific hooks in the global shared hooks directory.

---

# 11. Feature `pages/`

Feature pages belong inside the feature.

Example:

```text
features/company/pages/
├── CompanyListPage.tsx
├── CompanyCreatePage.tsx
└── CompanyDetailsPage.tsx
```

The page should primarily compose:

```text
feature hooks
+
feature components
+
shared UI
```

Avoid putting all business logic inside page components.

---

# 12. Feature `types/`

Feature-specific TypeScript types belong inside the feature.

Example:

```text
features/company/types/companyTypes.ts
```

Possible types:

```text
Company
CompanySummary
CompanySearchParams
CompanyCreateRequest
CompanyUpdateRequest
CompanyResponse
```

Keep frontend types aligned with backend API DTO contracts.

Do not expose or depend on backend JPA entities as frontend contracts.

---

# 13. Feature `schemas/`

Feature-specific validation schemas belong inside the feature.

Example:

```text
features/company/schemas/companySchema.ts
```

Possible schemas:

```text
companyCreateSchema
companyUpdateSchema
companySearchSchema
```

Frontend validation improves user experience.

However:

> Frontend validation must never replace backend validation.

The backend remains the authoritative validation boundary.

---

# 14. `shared/`

`shared/` is the common reusable layer.

This is where common frontend code should live.

```text
shared/
├── components/
├── hooks/
├── utils/
├── constants/
├── types/
├── schemas/
└── config/
```

The most important rule is:

> Only put code in `shared/` when it is genuinely reusable across multiple features.

Do not use `shared/` as a dumping ground.

---

# 15. `shared/components/`

Common UI components belong here.

```text
shared/components/
├── ui/
├── data-display/
├── forms/
├── feedback/
└── navigation/
```

---

# 16. `shared/components/ui/`

Reusable primitive UI components.

Examples:

```text
Button.tsx
Input.tsx
Select.tsx
Checkbox.tsx
Dialog.tsx
Modal.tsx
Dropdown.tsx
Badge.tsx
Card.tsx
Tabs.tsx
Tooltip.tsx
Spinner.tsx
```

These components should not know anything about Company, Contact, Import, etc.

---

# 17. `shared/components/data-display/`

Common data-display components belong here.

Examples:

```text
DataTable.tsx
Pagination.tsx
SortableHeader.tsx
EmptyState.tsx
DataGrid.tsx
```

This is particularly important for ProspectSoul because many features will need:

- search
- filtering
- sorting
- pagination
- table views
- empty states

Do not duplicate these components in every feature.

---

# 18. `shared/components/forms/`

Common form components belong here.

Examples:

```text
FormField.tsx
SearchInput.tsx
DatePicker.tsx
FileUpload.tsx
FormActions.tsx
```

Feature-specific forms remain inside their feature.

For example:

```text
features/company/components/CompanyForm.tsx
```

can use:

```text
shared/components/forms/FormField.tsx
```

---

# 19. `shared/components/feedback/`

Common feedback components:

```text
LoadingSpinner.tsx
LoadingState.tsx
ErrorState.tsx
EmptyState.tsx
SuccessMessage.tsx
ConfirmDialog.tsx
```

Every data-driven feature should handle loading, error, empty, and success states appropriately.

---

# 20. `shared/components/navigation/`

Common navigation components:

```text
Breadcrumbs.tsx
Pagination.tsx
Tabs.tsx
SidebarItem.tsx
```

Only genuinely reusable navigation belongs here.

---

# 21. `shared/hooks/`

Global reusable hooks belong here.

Examples:

```text
useDebounce.ts
useMediaQuery.ts
useLocalStorage.ts
usePrevious.ts
```

A Company-specific hook belongs in:

```text
features/company/hooks/
```

not here.

---

# 22. `shared/utils/`

Shared technical utilities belong here.

Avoid creating one giant:

```text
utils.ts
```

when the application grows.

Prefer focused utilities such as:

```text
dateUtils.ts
formatUtils.ts
stringUtils.ts
numberUtils.ts
validationUtils.ts
```

Utilities must remain generic and side-effect aware.

Do not place business logic here.

---

# 23. `shared/constants/`

Global application constants belong here.

Examples:

```text
routes.ts
pagination.ts
dateFormats.ts
appConstants.ts
```

Feature-specific constants should remain inside the feature.

---

# 24. `shared/types/`

Only globally shared types belong here.

Examples:

```text
api.ts
pagination.ts
common.ts
```

Feature-specific types must remain inside their feature.

Avoid creating a massive global type file.

---

# 25. `shared/schemas/`

Only genuinely shared validation schemas belong here.

Feature-specific schemas remain in:

```text
features/<feature>/schemas/
```

Do not duplicate schemas.

---

# 26. `layouts/`

Application-level layouts belong here.

Recommended:

```text
layouts/
├── AppLayout.tsx
├── AuthLayout.tsx
└── DashboardLayout.tsx
```

Layouts define page structure.

For example:

```text
DashboardLayout
├── Sidebar
├── Header
└── Page Content
```

Layouts should not contain feature-specific business logic.

---

# 27. `services/`

`services/` contains shared technical services.

It should not become another feature directory.

Recommended:

```text
services/
├── api/
│   ├── apiClient.ts
│   ├── apiError.ts
│   └── apiTypes.ts
│
└── storage/
    └── storageService.ts
```

---

# 28. API Client

The API client is the single common HTTP boundary.

Example:

```text
services/api/apiClient.ts
```

Responsibilities may include:

- base URL
- headers
- authentication
- request configuration
- response parsing
- common error handling
- timeout handling

Feature APIs should use this client.

Example:

```text
companyApi.ts
    ↓
apiClient.ts
    ↓
Spring Boot API
```

Do not use `fetch()` or `axios()` randomly throughout components.

---

# 29. API Types

Shared API infrastructure types can live in:

```text
services/api/apiTypes.ts
```

Examples:

```text
ApiError
PageResponse
PaginationMetadata
ApiResponse
```

Feature-specific API types remain in the feature.

---

# 30. `auth/`

Authentication and authorization infrastructure belongs here.

```text
auth/
├── authService.ts
├── authTypes.ts
└── permissions.ts
```

Responsibilities include:

```text
login
logout
current user
authentication state
permission checks
```

Do not place feature business logic here.

The backend remains the authoritative security boundary.

---

# 31. Authentication Boundary

The frontend may:

- hide unauthorized UI
- protect routes
- show permission-based navigation
- manage authentication state

But:

> Frontend authorization must never be treated as sufficient security.

Every protected operation must also be enforced by the backend.

---

# 32. `styles/`

Global styling belongs here.

```text
styles/
├── globals.css
├── variables.css
└── theme.css
```

Use the project's selected styling approach consistently.

Do not mix multiple styling systems without a clear architectural reason.

---

# 33. `assets/`

Static application assets belong here.

```text
assets/
├── images/
├── icons/
└── fonts/
```

Do not store API data or business logic here.

---

# 34. `public/`

Use `public/` for assets that must be served directly without bundling.

Examples:

```text
favicon.ico
robots.txt
static public assets
```

Prefer imported assets under `src/assets/` when they are part of the application bundle.

---

# 35. State Management

Separate state by responsibility.

### Local UI state

Use component state for local concerns:

```text
useState
useReducer
```

Examples:

```text
modal open/closed
selected tab
temporary form state
```

### Server state

Use the project's selected server-state solution, such as TanStack Query, for API/server data where appropriate.

### Global application state

Only genuinely global state should be global.

Examples:

```text
authentication state
theme
application-wide preferences
```

Do not put every API response into a global store.

---

# 36. Server State Rule

Avoid unnecessary duplication:

```text
API response
    ↓
Global store
    ↓
Local state
```

Prefer a dedicated server-state solution when appropriate.

Keep server state separate from UI state.

---

# 37. Tables, Search, Filter, Sort, Pagination

ProspectSoul will have data-heavy screens.

Common capabilities should be reusable.

Use shared components for:

```text
DataTable
Pagination
SearchInput
FilterPanel
SortableHeader
ColumnSelector
EmptyState
LoadingState
```

Feature-specific filtering logic belongs to the feature.

For example:

```text
shared/components/data-display/DataTable.tsx

features/company/components/CompanyFilters.tsx
```

This keeps common UI reusable while keeping business rules feature-specific.

---

# 38. Large Data Sets

Do not load huge datasets into the browser unnecessarily.

For large collections such as Companies:

- use server-side pagination
- use server-side search
- use server-side filtering
- use server-side sorting where appropriate

The frontend should request only the data needed for the current view.

---

# 39. Forms

Forms should support:

- typed inputs
- validation
- loading state
- server errors
- success handling
- accessible labels
- disabled submission while processing

Common form infrastructure should be shared.

Feature-specific form definitions belong inside the feature.

Example:

```text
shared/components/forms/FormField.tsx

features/company/components/CompanyForm.tsx
features/company/schemas/companySchema.ts
```

---

# 40. Loading, Error, Empty, and Success States

Every API-driven screen should consider:

```text
Loading
Success
Empty
Error
```

Do not leave users with a blank screen when an API call fails.

Use shared feedback components where appropriate.

---

# 41. API Error Handling

The frontend should understand standardized backend errors.

At minimum account for:

```text
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Validation Error
500 Internal Server Error
```

Technical backend details must not be exposed directly to users.

Convert errors into useful UI messages.

---

# 42. TypeScript Rules

Use TypeScript strictly.

Prefer:

```text
explicit types
interfaces/types
generics
type-safe API responses
discriminated unions
```

Avoid:

```typescript
any
```

unless there is a documented reason.

Do not suppress TypeScript errors simply to make the build pass.

---

# 43. Naming Conventions

Use clear and consistent naming.

Components:

```text
CompanyTable.tsx
CompanyFilters.tsx
CompanyDetails.tsx
```

Hooks:

```text
useCompanies.ts
useCompany.ts
```

API:

```text
companyApi.ts
contactApi.ts
```

Types:

```text
companyTypes.ts
```

Schemas:

```text
companySchema.ts
```

Avoid vague names:

```text
Data.tsx
Manager.tsx
Helper.ts
Common.ts
Thing.tsx
```

---

# 44. Import Rules

Use configured path aliases where appropriate.

Example:

```text
@/features/company
@/shared/components
@/services/api
```

Avoid excessive relative imports such as:

```text
../../../../components/...
```

Avoid circular dependencies.

---

# 45. Feature Boundary Rules

A feature should not reach into another feature's internal implementation.

Avoid:

```text
features/company/
    ↓
features/contact/components/internal/
```

If cross-feature interaction is required, expose a deliberate public interface through:

```text
features/contact/index.ts
```

or an appropriate shared abstraction.

---

# 46. Shared Code Rule

Use this decision process before placing code:

### Feature-specific?

Put it in:

```text
features/<feature>/
```

### Reusable across multiple features?

Consider:

```text
shared/
```

### Application composition?

Put it in:

```text
app/
```

### Application layout?

Put it in:

```text
layouts/
```

### Shared technical service?

Put it in:

```text
services/
```

### Authentication infrastructure?

Put it in:

```text
auth/
```

This rule should prevent the frontend from becoming disorganized.

---

# 47. Avoid Premature Abstraction

Do not move something to `shared/` merely because it looks reusable.

A good approach is:

```text
First use:
    feature-specific

Second real use:
    evaluate reuse

Multiple genuine uses:
    extract to shared/
```

Avoid creating a complicated design system before there is a real need.

---

# 48. Component Rules

Components should have clear responsibilities.

Avoid giant components containing:

- API calls
- business logic
- validation
- table implementation
- modal implementation
- filtering
- pagination
- navigation

Prefer:

```text
Page
  ↓
Feature Hook
  ↓
Feature Component
  ↓
Shared UI Component
```

---

# 49. Page Rules

Pages should primarily compose the feature.

Example:

```text
CompanyListPage
    ↓
CompanyFilters
    ↓
CompanyTable
    ↓
Pagination
```

The page should not become a dumping ground for business logic.

---

# 50. Routing Rules

Keep route definitions centralized.

Feature pages should be registered in:

```text
app/router/routes.tsx
```

Use protected routes where required.

Use lazy loading/code splitting for larger application areas when appropriate.

Do not define duplicate routes across unrelated components.

---

# 51. Environment Variables

Do not hardcode environment-specific configuration.

Example:

```text
VITE_API_BASE_URL
```

Never put secrets into frontend environment variables.

Anything exposed to the browser can be inspected.

Therefore:

```text
API keys
private credentials
secret tokens
database credentials
```

must never be treated as frontend secrets.

---

# 52. Accessibility

All UI should consider accessibility.

Use:

- semantic HTML
- accessible labels
- keyboard navigation
- visible focus states
- appropriate ARIA attributes
- meaningful error messages
- accessible dialogs
- accessible tables

Do not use a `<div>` as a button when a `<button>` is appropriate.

---

# 53. Performance

Do not optimize without evidence.

Preferred process:

```text
Build correctly
    ↓
Measure
    ↓
Identify bottleneck
    ↓
Optimize
    ↓
Verify
```

Pay attention to:

- unnecessary renders
- large lists
- expensive calculations
- duplicate API calls
- oversized bundles
- unnecessary global state

Use memoization only when it provides measurable or clear value.

---

# 54. Testing

Tests should focus on meaningful behavior.

Recommended categories:

```text
tests/
├── unit/
├── integration/
└── e2e/
```

Test important:

- components
- hooks
- feature behavior
- forms
- validation
- API behavior
- user workflows

Do not write tests only to increase coverage numbers.

---

# 55. Test Organization

Feature-specific tests may also live close to their source code when that is the project convention.

Example:

```text
features/company/components/
├── CompanyTable.tsx
└── CompanyTable.test.tsx
```

For broader tests:

```text
tests/
├── unit/
├── integration/
└── e2e/
```

Use one consistent convention.

---

# 56. Security Rules

Never trust the frontend for authorization.

The frontend may:

```text
hide UI
protect routes
show permissions
```

But the backend must enforce:

```text
authentication
authorization
resource access
business security rules
```

Never expose secrets in frontend code.

---

# 57. Backend API Contract

The frontend and backend should communicate through explicit API contracts.

Recommended flow:

```text
React Feature
    ↓
Feature API
    ↓
Shared API Client
    ↓
REST API
    ↓
Spring Boot Controller
    ↓
DTO
```

Do not couple frontend code to JPA entity internals.

Frontend models should represent API contracts.

---

# 58. Recommended Full-Stack Alignment

The frontend and backend should use matching business boundaries.

```text
BACKEND                         FRONTEND

company/                        features/company/
contact/                        features/contact/
imports/                        features/imports/
triage/                         features/triage/
activity/                       features/activity/
evidence/                       features/evidence/
research/                       features/research/
icp/                            features/icp/
qualification/                  features/qualification/
export/                         features/export/
report/                         features/reports/
admin/                          features/admin/
ai/                             features/ai/
```

This makes the codebase much easier to navigate.

---

# 59. Do Not Duplicate Common Functionality

Do not create:

```text
company/components/Pagination.tsx
contact/components/Pagination.tsx
imports/components/Pagination.tsx
```

Instead use:

```text
shared/components/data-display/Pagination.tsx
```

Likewise:

```text
SearchInput
DataTable
Modal
ConfirmDialog
LoadingState
ErrorState
EmptyState
FormField
```

should be shared when they are genuinely generic.

---

# 60. Do Not Create a Giant `Common` Folder

Avoid:

```text
common/
    Everything.tsx
```

or:

```text
shared/
    utils.ts
```

with unrelated functionality.

Organize shared code by responsibility:

```text
shared/
├── components/
├── hooks/
├── utils/
├── constants/
├── types/
├── schemas/
└── config/
```

---

# 61. Do Not Over-Create Files

Do not create files merely because the architecture diagram contains a possible category.

For example, do not create:

```text
CompanyManager.ts
CompanyHelper.ts
CompanyProcessor.ts
CompanyUtils.ts
CompanyFactory.ts
```

unless each has a real responsibility.

The architecture should support the code, not force unnecessary abstractions.

---

# 62. Do Not Over-Create Features

Create a feature directory when there is an actual business capability.

The initial ProspectSoul feature boundaries can follow:

```text
company
contact
imports
triage
activity
evidence
research
icp
qualification
export
reports
admin
ai
```

New capabilities can be introduced later without restructuring the entire application.

---

# 63. Recommended Development Flow

For a new feature:

```text
1. Define the business capability
        ↓
2. Create feature directory
        ↓
3. Define API types
        ↓
4. Implement feature API
        ↓
5. Implement feature hooks
        ↓
6. Implement feature components
        ↓
7. Implement feature pages
        ↓
8. Register routes
        ↓
9. Reuse shared components
        ↓
10. Add tests
        ↓
11. Verify build
```

---

# 64. Before Every Task

Before modifying frontend code:

1. Read `frontend/CLAUDE.md`.
2. Inspect the relevant feature.
3. Inspect existing shared components.
4. Inspect existing hooks.
5. Inspect API integration.
6. Inspect types.
7. Inspect routing.
8. Inspect relevant tests.
9. Follow existing project conventions.
10. Make the smallest clean change necessary.

Do not modify unrelated features.

---

# 65. Definition of Done

A frontend change is complete when:

- TypeScript checks pass.
- The production build succeeds.
- Relevant tests pass.
- Loading states are handled.
- Error states are handled.
- Empty states are handled where applicable.
- API errors are handled.
- Accessibility is considered.
- No secrets are committed.
- No unnecessary global state is introduced.
- Feature boundaries are respected.
- Existing functionality is not accidentally broken.

Never claim completion if verification failed.

---

# 66. Final Architecture

The final architecture should be understood as:

```text
src/
│
├── app/                    # Application composition
│
├── features/               # Business capabilities
│   ├── company/
│   ├── contact/
│   ├── imports/
│   ├── triage/
│   ├── activity/
│   ├── evidence/
│   ├── research/
│   ├── icp/
│   ├── qualification/
│   ├── export/
│   ├── reports/
│   ├── admin/
│   └── ai/
│
├── shared/                 # Reusable common code
│   ├── components/
│   ├── hooks/
│   ├── utils/
│   ├── constants/
│   ├── types/
│   ├── schemas/
│   └── config/
│
├── layouts/                # Application layouts
│
├── services/               # Technical services/API
│
├── auth/                   # Authentication infrastructure
│
├── styles/                 # Global styles
│
└── assets/                 # Static application assets
```

The most important boundaries are:

```text
Business functionality
        ↓
    features/

Reusable functionality
        ↓
     shared/

Application composition
        ↓
       app/

Technical services
        ↓
     services/

Authentication
        ↓
       auth/

Page structure
        ↓
     layouts/
```

---

# 67. Most Important Rule

> **Do not guess and do not introduce a new pattern when an existing project pattern already exists.**

Before implementing anything:

1. Read the relevant documentation.
2. Inspect the existing code.
3. Identify the correct feature boundary.
4. Reuse existing shared components.
5. Reuse existing API infrastructure.
6. Follow established naming and state-management conventions.
7. Make the smallest maintainable change.
8. Verify the result.

The frontend should remain **feature-oriented, reusable, type-safe, accessible, testable, and easy for another developer or Claude Code to understand and modify.**
