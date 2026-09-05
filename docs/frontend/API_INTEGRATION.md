# Frontend API Integration Rules

## Purpose

This document defines the core rules for integrating the React frontend with the Spring Boot backend.

Before creating or modifying API integration code, Claude Code must follow these rules.

---

## 1. API Architecture

Use this flow:

```text
React Component
      ↓
Custom Hook
      ↓
Feature API Service
      ↓
Central HTTP Client
      ↓
Spring Boot REST API
```

Do not call `fetch()` or `axios` directly from React components.

---

## 2. API Base URL

Use the backend API version:

```text
/api/v1
```

Configure the complete API URL through environment variables.

Example:

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

Never hard-code environment-specific URLs in application code.

---

## 3. Central HTTP Client

Create one shared HTTP client responsible for:

- Base URL
- Authentication headers
- Common headers
- Timeout
- Response handling
- Error handling

Example:

```text
src/services/api/
├── client.js
├── companiesApi.js
├── contactsApi.js
└── ...
```

Do not create separate HTTP clients for individual features.

---

## 4. Feature API Services

Each feature should have its own API service.

Example:

```text
companiesApi.js
contactsApi.js
activitiesApi.js
importsApi.js
```

API services contain:

- Endpoint paths
- HTTP methods
- Query parameters
- Request payloads
- API-specific response handling

They must not contain UI logic.

---

## 5. React Hooks

Components should consume APIs through custom hooks.

Example:

```text
CompanyPage
    ↓
useCompanies()
    ↓
companiesApi.getCompanies()
    ↓
apiClient.get()
```

Hooks handle appropriate server state such as:

- loading
- data
- error
- refetching
- mutations

---

## 6. Request and Response Contract

The frontend must follow the backend API contract.

Do not invent:

- endpoint paths
- request fields
- response fields
- status codes
- error formats

Before changing an API integration, check the backend API documentation.

---

## 7. JSON Naming

The backend API uses:

```text
snake_case
```

Example:

```json
{
  "company_name": "ABC Pumps",
  "created_at": "2026-08-26T09:30:00Z"
}
```

Do not randomly mix API field naming conventions.

If the frontend uses camelCase internally, perform conversion at the API boundary rather than throughout UI components.

---

## 8. HTTP Methods

Follow standard REST methods:

```text
GET     → Read
POST    → Create
PUT     → Full update
PATCH   → Partial update
DELETE  → Delete/deactivate
```

---

## 9. Query Parameters

Use query parameters for:

- Search
- Filtering
- Sorting
- Pagination
- Date ranges

Example:

```text
GET /api/v1/companies?page=0&size=25&search=abc
```

Do not manually construct query strings throughout components.

---

## 10. Pagination

Use server-side pagination.

Standard parameters:

```text
page
size
sort
```

Example:

```text
?page=0&size=25&sort=created_at,desc
```

Use backend pagination metadata for the UI.

Do not load an entire large dataset just to paginate in React.

---

## 11. Search

Use the backend's standard search parameter:

```text
search
```

For search inputs that trigger API requests:

- debounce requests
- avoid duplicate requests
- cancel stale requests where appropriate

---

## 12. Filtering and Sorting

Keep filters in structured state.

Example:

```javascript
{
  search: '',
  status: 'QUALIFIED',
  page: 0,
  size: 25,
  sort: 'created_at,desc'
}
```

Use only backend-supported filter and sort fields.

---

## 13. Loading and Empty States

Every API-driven feature must distinguish:

```text
Loading
Success
Empty
Error
```

Do not show an empty state while the API is still loading.

---

## 14. Error Handling

Use one centralized API error handling strategy.

The backend uses:

```text
application/problem+json
```

Frontend code must normalize API errors before passing them to UI components.

Handle at least:

```text
400 → Validation error
401 → Authentication required
403 → Permission denied
404 → Resource not found
409 → Conflict
422 → Business rule failure
500 → Server error
```

Do not expose backend stack traces or internal errors to users.

---

## 15. Authentication

Authenticated requests must use the application's configured authentication mechanism.

Use:

```http
Authorization: Bearer <access-token>
```

through the centralized HTTP client.

Do not manually attach tokens inside individual components.

---

## 16. Authorization

Frontend authorization is for UI behavior only.

Example:

```text
Hide/disable button
```

does not provide security.

The backend must always enforce authorization.

---

## 17. Token Refresh

If token refresh is supported:

```text
Request
   ↓
401
   ↓
Refresh token
   ↓
Retry once
```

Never create an infinite refresh/retry loop.

Concurrent requests should use a centralized refresh mechanism.

---

## 18. API Performance

Avoid:

- Duplicate requests
- Unnecessary refetches
- Unbounded API responses
- Large unnecessary payloads
- N+1 frontend request patterns
- Repeated API logic

Prefer:

- Pagination
- Caching where appropriate
- Request deduplication
- Query invalidation
- Debounced search
- Parallel requests when independent

---

## 19. Mutations

For:

```text
Create
Update
Delete
```

use the project's standard mutation mechanism.

After a successful mutation, update or invalidate affected server-state queries.

Example:

```text
Create Company
      ↓
Invalidate Companies Query
      ↓
Refresh Company List
```

---

## 20. File Uploads

Use:

```text
FormData
```

for multipart uploads when required by the backend.

Follow the backend's documented:

```text
field names
content type
endpoint
response
error format
```

Do not manually set multipart boundaries.

---

## 21. API Security

Never put secrets in frontend code.

Never expose:

```text
Database passwords
Private API keys
Client secrets
Server credentials
```

Remember that frontend environment variables are visible in the built application.

---

## 22. API Logging

Do not log:

```text
Access tokens
Authorization headers
Passwords
API keys
Sensitive user data
```

Log only safe request information when debugging or monitoring is required.

---

## 23. API Testing

API integration should be tested for:

```text
Successful requests
Validation errors
Authentication failures
Authorization failures
Not found
Conflict
Server errors
Pagination
Search
Filtering
Mutations
```

Do not rely only on UI tests for API behavior.

---

## 24. API Change Workflow

Before adding or changing an API:

```text
Check backend API contract
        ↓
Update API service
        ↓
Update types/models if required
        ↓
Update React hook
        ↓
Update UI
        ↓
Update tests
        ↓
Verify error/loading states
```

Never change an endpoint contract silently.

---

## 25. Core Rules

> **Components must not call APIs directly.**

> **Use a centralized HTTP client.**

> **Use feature-specific API services.**

> **Use hooks between components and API services.**

> **Follow the backend API contract exactly.**

> **Use server-side pagination, filtering, and sorting.**

> **Centralize authentication and error handling.**

> **Never expose secrets in the frontend.**

> **Keep API integration logic separate from UI logic.**
