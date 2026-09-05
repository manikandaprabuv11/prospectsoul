# ProspectSoul Backend — API Documentation & API Rules

## Purpose

This document defines the REST API contract, conventions, implementation rules, documentation requirements, and review standards for the ProspectSoul Spring Boot backend.

It is a persistent reference for developers and Claude Code.

The API must follow the project's DTO-first architecture:

```text
HTTP Request
    ↓
Request DTO
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
Entity / Database
```

Response flow:

```text
Database
    ↓
Entity
    ↓
Mapper
    ↓
Response DTO
    ↓
Controller
    ↓
HTTP Response
```

JPA entities must never be exposed directly through REST APIs. fileciteturn3file2L1-L20

---

# 1. API Design Principles

APIs must be:

- predictable
- consistent
- versioned
- DTO-first
- REST-oriented
- secure
- documented
- backward-compatible where required
- pagination-friendly
- validation-aware
- testable

Do not create a new API style for an individual feature.

Before adding an endpoint, inspect existing:

- routes
- HTTP methods
- DTO naming
- response formats
- pagination
- filtering
- authentication
- authorization
- error handling
- OpenAPI documentation

---

# 2. Base URL

The documented API base path is:

```text
/api/v1
```

All versioned application endpoints should follow this convention.

Example:

```text
/api/v1/companies
/api/v1/contacts
/api/v1/imports
```

Do not create feature endpoints outside the established API version unless explicitly required.

The project specification defines `/api/v1` as the API base path. fileciteturn3file6L1-L20

---

# 3. API Versioning

Use URL-based versioning:

```text
/api/v1
```

Do not silently introduce:

```text
/api/v2
```

or change an existing v1 contract without an explicit architectural/product decision.

Breaking changes require:

1. impact analysis;
2. consumer inspection;
3. migration strategy;
4. documentation update;
5. tests;
6. explicit approval.

---

# 4. Resource Naming

Use plural resource names.

Preferred:

```text
/companies
/contacts
/activities
/evidence
/imports
```

Avoid:

```text
/getCompanies
/createCompany
/companyList
```

HTTP methods express the operation.

---

# 5. HTTP Methods

Use HTTP methods consistently.

Typical conventions:

```text
GET     → retrieve
POST    → create
PUT     → full replacement/update
PATCH   → partial update
DELETE  → delete/deactivate where supported
```

Example:

```text
GET    /api/v1/companies
GET    /api/v1/companies/{id}
POST   /api/v1/companies
PUT    /api/v1/companies/{id}
PATCH  /api/v1/companies/{id}
DELETE /api/v1/companies/{id}
```

Do not use `POST` for ordinary reads merely because the query is complex unless there is a documented reason.

---

# 6. Path Parameters

Use path parameters to identify resources.

Example:

```text
GET /api/v1/companies/{id}
```

Use:

```text
GET /api/v1/companies/550e8400-e29b-41d4-a716-446655440000
```

Do not put the resource ID unnecessarily into the request body for normal resource operations.

---

# 7. UUIDs

The existing API specification defines UUID identifiers.

Use UUIDs consistently for externally exposed resource IDs.

Do not expose internal database implementation details when the API contract defines UUID identifiers.

---

# 8. Query Parameters

Use query parameters for:

- pagination
- filtering
- searching
- sorting
- date ranges
- status filters
- optional list criteria

Example:

```text
GET /api/v1/companies?page=0&size=25&sort=created_at,desc
```

Do not create separate endpoints for every simple filter.

---

# 9. Pagination

All list endpoints should be evaluated for pagination.

The documented convention is:

```text
?page=0&size=25&sort=created_at,desc
```

Response:

```json
{
  "content": [],
  "page": 0,
  "size": 25,
  "total_elements": 1420,
  "total_pages": 57
}
```

This pagination convention is defined by the existing ProspectSoul API specification. fileciteturn3file6L1-L20

Do not create a different pagination envelope for each feature.

---

# 10. Pagination Rules

Pagination must be server-side.

Do not:

```text
load entire table
    ↓
filter in Java
    ↓
return one page
```

Prefer:

```text
request
    ↓
repository query
    ↓
database filtering
    ↓
database pagination
    ↓
DTO mapping
    ↓
response
```

Avoid unbounded list APIs for potentially large datasets.

---

# 11. Page Size Limits

Do not allow clients to request unlimited page sizes.

The backend should enforce a configured maximum.

Example concept:

```text
default size: 25
maximum size: configured project limit
```

The exact limit should come from project configuration rather than being hard-coded independently in every controller.

---

# 12. Sorting

Use query parameters for sorting.

Example:

```text
?sort=created_at,desc
```

Support only approved sortable fields.

Do not concatenate arbitrary request parameters into SQL.

Example concept:

```text
sort=created_at,desc
```

must be mapped against a known field allowlist.

---

# 13. Search

Use a clear search parameter for free-text search where appropriate.

Example:

```text
GET /api/v1/companies?search=abc
```

Search behavior must be documented per resource when it is not obvious.

Do not create inconsistent parameters such as:

```text
?q=
?keyword=
?term=
?searchText=
```

for the same purpose across different endpoints.

---

# 14. Filtering

Use query parameters for structured filters.

Example:

```text
GET /api/v1/companies?status=QUALIFIED&source=INDIAMART
```

For multiple filters, keep parameter naming consistent.

Complex dynamic filtering should be implemented through the service/repository/specification layer, not inside controllers. fileciteturn3file7L1-L20

---

# 15. Date Filters

Use ISO-8601 date/time formats consistently.

Examples:

```text
created_from
created_to
updated_from
updated_to
```

Example:

```text
GET /api/v1/companies?created_from=2026-01-01T00:00:00Z
```

Use UTC for API timestamps unless the contract explicitly requires another timezone.

---

# 16. JSON Naming

The existing API specification defines:

```text
snake_case
```

for JSON.

Example:

```json
{
  "company_name": "ABC Pumps",
  "created_at": "2026-08-26T09:30:00Z"
}
```

Do not mix:

```text
company_name
companyName
CompanyName
```

within the same API contract.

---

# 17. Date and Time

API timestamps should use:

```text
UTC ISO-8601
```

Example:

```text
2026-08-26T09:30:00Z
```

Do not return ambiguous timestamps such as:

```text
26/08/2026 09:30
```

for machine-readable API fields.

The existing API specification explicitly defines UTC ISO-8601 timestamps. fileciteturn3file6L1-L20

---

# 18. Request DTO Rules

Controllers must accept request DTOs.

Example:

```java
public record CreateCompanyRequest(
    @NotBlank String companyName,
    String website
) {}
```

Never accept:

```java
Company
```

directly from a REST request.

Request DTOs should expose only fields the client is allowed to provide.

---

# 19. Create DTOs

Use dedicated create DTOs when creation rules differ from update rules.

Example:

```text
CompanyCreateRequest
```

Do not reuse a response DTO as a create request.

Server-controlled fields must not be writable.

Examples:

```text
id
created_at
created_by
updated_at
updated_by
audit fields
system-generated status
system-generated identifiers
```

The project's DTO rules explicitly require separate create/update/response DTOs where appropriate. fileciteturn3file2L1-L20

---

# 20. Update DTOs

Use a dedicated update DTO when update rules differ.

Example:

```text
CompanyUpdateRequest
```

Do not automatically allow clients to modify every database field.

Protected fields must be controlled by the service/domain layer.

---

# 21. PATCH

If PATCH is supported:

- distinguish omitted fields from explicit null where required;
- update only allowed fields;
- enforce business rules;
- prevent modification of protected fields.

Do not accidentally implement PATCH as unrestricted replacement.

---

# 22. PUT

If PUT is supported, define replacement semantics clearly.

PUT should not silently behave like PATCH.

Required fields must be validated according to the project's contract.

---

# 23. Response DTOs

Controllers must return response DTOs.

Example:

```java
public record CompanyResponse(
    UUID id,
    String companyName,
    String industry,
    Instant createdAt
) {}
```

Never return:

```java
Company
```

directly.

Response DTOs define the public API contract.

They must not expose:

- JPA internals
- Hibernate proxies
- database-only fields
- secrets
- sensitive internal metadata

The existing DTO rules explicitly prohibit entity exposure. fileciteturn3file7L1-L20

---

# 24. Detail vs Summary DTOs

Use different DTOs when payload requirements differ.

Examples:

```text
CompanyResponse
CompanyDetailResponse
CompanySummaryResponse
```

Use summary DTOs for nested resources when full details are unnecessary.

Example:

```java
public record CompanyResponse(
    UUID id,
    String companyName,
    SourceSummaryResponse source
) {}
```

Do not nest complete entities inside responses.

---

# 25. Response DTO Mapping

Use a dedicated mapper.

Preferred flow:

```text
Entity
    ↓
Mapper
    ↓
Response DTO
```

Mapping must be:

- deterministic
- readable
- testable
- free of business logic

Use MapStruct if approved by the project; otherwise use explicit mapper classes. fileciteturn3file2L1-L20

---

# 26. HTTP Status Codes

Use meaningful HTTP status codes.

Typical conventions:

```text
200 OK
201 Created
202 Accepted
204 No Content
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
500 Internal Server Error
```

Do not return `200 OK` for every possible outcome.

---

# 27. POST Responses

Successful resource creation should normally return:

```text
201 Created
```

where appropriate.

The response should contain the API representation defined by the contract.

If the operation is asynchronous, consider:

```text
202 Accepted
```

only when the architecture actually treats the operation as asynchronous.

---

# 28. DELETE Responses

Use:

```text
204 No Content
```

when a successful deletion has no response body and the contract permits it.

If ProspectSoul uses soft deletion or state transitions instead of physical deletion, follow the domain contract rather than automatically implementing SQL deletion.

---

# 29. Error Format

Use:

```text
application/problem+json
```

with RFC-7807-style problem details.

The documented example is:

```json
{
  "type": "https://prospectsoul/errors/validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "phone must be 10 digits",
  "errors": [
    {
      "field": "primary_phone",
      "message": "must be 10 digits"
    }
  ]
}
```

The project specification explicitly defines RFC-7807 `application/problem+json`. fileciteturn3file6L1-L20

---

# 30. Error Catalogue

Use the documented meanings:

```text
400 → validation / malformed request
401 → unauthenticated
403 → role/permission denied
404 → resource not found
409 → conflict
422 → business rule violation
500 → unexpected server error
```

Examples:

```text
400 → invalid phone format
401 → missing/invalid authentication
403 → insufficient role
404 → company does not exist
409 → duplicate/conflicting state
422 → business rule prevents requested operation
```

Do not randomly change the meaning of status codes between modules.

---

# 31. Validation Errors

Validation errors should identify the relevant field where possible.

Example:

```json
{
  "type": "https://prospectsoul/errors/validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "Request validation failed",
  "errors": [
    {
      "field": "company_name",
      "message": "must not be blank"
    }
  ]
}
```

Request DTO validation should happen at the API boundary.

Business validation belongs in the service/domain layer.

---

# 32. Business Errors

Do not use generic validation errors for business rule failures.

Example:

```text
422 Unprocessable Entity
```

may represent:

```text
requested state transition is not allowed
override requires a reason
qualification cannot occur without required evidence
```

Business invariants must also be enforced in application code.

---

# 33. Conflict Errors

Use:

```text
409 Conflict
```

when the requested operation conflicts with current resource state.

Examples:

```text
duplicate resolution already completed
resource version conflict
duplicate unique business identifier
invalid concurrent state change
```

---

# 34. Global Exception Handling

Use a centralized exception handler.

Recommended:

```text
common/exception/
└── GlobalExceptionHandler.java
```

Controllers should not contain repeated try/catch blocks for standard API errors.

The global handler should translate known exceptions into the standard error contract.

---

# 35. Authentication

The backend is the authoritative security boundary.

The existing architecture specifies Keycloak/OIDC and Spring Security resource-server JWT validation.

Conceptually:

```text
Client
    ↓
Bearer JWT
    ↓
Spring Security
    ↓
Keycloak issuer validation
    ↓
Controller
    ↓
Service
```

Do not trust frontend authorization alone.

---

# 36. Authorization

Authorization must be enforced server-side.

Use the project's configured role/capability model.

Where appropriate, use method security such as:

```java
@PreAuthorize(...)
```

Do not protect an endpoint only by hiding its frontend button.

---

# 37. Resource-Level Authorization

Role checks alone may not always be enough.

Where the domain requires it, verify:

```text
user
    ↓
role/capability
    ↓
resource access
    ↓
business rule
```

Do not assume that possessing a general role means access to every resource.

---

# 38. Audit Logging

Every mutation must be auditable where required by the product architecture.

The existing specification defines an explicit service-level:

```text
AuditService.record(...)
```

rather than relying on hidden/magic listeners. fileciteturn3file15L1-L20

A mutation audit record should capture the required information, such as:

```text
entity_type
entity_id
actor
action
previous_state
new_state
```

Do not silently bypass audit requirements for new mutation endpoints.

---

# 39. Idempotency

Consider idempotency for operations that may be retried by clients or infrastructure.

Especially consider it for:

- imports
- external callbacks
- payment-like operations if ever introduced
- asynchronous commands
- expensive mutations

Do not add idempotency keys everywhere without a requirement.

Where required, define:

```text
idempotency key
scope
storage
expiration
duplicate behavior
```

---

# 40. API Security

Never expose:

```text
database credentials
API provider keys
internal stack traces
JWT secrets
private configuration
```

through API responses.

Do not log access tokens or sensitive request payloads.

---

# 41. API Logging

Log enough information to diagnose requests without exposing secrets.

Avoid logging:

```text
Authorization header
access tokens
passwords
API keys
sensitive personal data
```

Use correlation/request IDs when the application's observability architecture supports them.

---

# 42. API Contract Stability

Once an endpoint is consumed by the frontend or another integration, treat its contract as stable.

Before changing:

```text
field names
field types
required fields
status codes
error format
pagination format
endpoint path
HTTP method
```

inspect consumers and documentation.

Do not make silent breaking changes.

---

# 43. Backward Compatibility

Prefer additive changes when possible.

Safer:

```text
add optional response field
```

Riskier:

```text
rename existing field
remove field
change field type
change meaning
```

Breaking changes require explicit migration planning.

---

# 44. OpenAPI Documentation

All public REST endpoints must be documented through the project's OpenAPI tooling.

Document:

- endpoint
- HTTP method
- summary
- description
- authentication requirement
- required roles where appropriate
- path parameters
- query parameters
- request body
- response body
- status codes
- error responses

The existing project specification calls for OpenAPI documentation through springdoc. fileciteturn3file15L1-L20

---

# 45. OpenAPI Quality

Do not generate meaningless documentation such as:

```text
GET /companies
Returns companies.
```

Document actual behavior:

```text
GET /api/v1/companies

Returns a paginated list of companies matching the supplied
search, filter, and sorting criteria.
```

Use meaningful schemas and examples where helpful.

---

# 46. Swagger UI

The project specification indicates Swagger UI should be available in non-production environments.

Do not expose development-oriented API documentation publicly in production unless explicitly approved.

---

# 47. API Tests

Every meaningful endpoint must have API/controller tests.

Test at minimum:

```text
success
validation failure
not found
unauthorized
forbidden
conflict
business rule failure
pagination
filtering
sorting
```

For mutation endpoints also test:

```text
database state
audit behavior
business invariants
```

---

# 48. Integration Tests

Important APIs should have integration coverage against the real application stack where practical.

Verify:

```text
HTTP
    ↓
Security
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

Do not rely only on mocked controller tests for persistence-sensitive behavior.

---

# 49. API Performance

Avoid:

- unbounded queries
- N+1 queries
- huge response payloads
- unnecessary entity graphs
- unnecessary remote calls
- duplicate queries
- excessive serialization

Use:

```text
pagination
projections
appropriate indexes
optimized queries
```

for data-heavy APIs.

---

# 50. External APIs and AI

External service integrations must remain behind appropriate service/provider abstractions.

Preferred:

```text
Business Service
    ↓
Capability / Integration Service
    ↓
Provider Interface
    ↓
External API
```

Do not tightly couple every domain service directly to a vendor SDK.

External API calls must consider:

- timeout
- retry
- failure handling
- rate limits
- credentials
- observability
- cost

Never hard-code provider credentials.

---

# 51. RAG / AI API Boundaries

When exposing RAG/AI capabilities through REST:

```text
API Request
    ↓
Request DTO
    ↓
AI Capability Service
    ↓
Retrieval
    ↓
Context Construction
    ↓
Provider
    ↓
Structured Response DTO
```

Do not put prompt construction directly inside controllers.

Do not put core business invariants only inside prompts.

Application code must enforce important business rules independently of AI output.

---

# 52. API File Organization

Follow the feature-first backend structure.

Example:

```text
company/
├── controller/
│   └── CompanyController.java
├── service/
│   └── CompanyService.java
├── repository/
│   └── CompanyRepository.java
├── entity/
│   └── Company.java
├── dto/
│   ├── request/
│   │   ├── CompanyCreateRequest.java
│   │   ├── CompanyUpdateRequest.java
│   │   └── CompanySearchRequest.java
│   └── response/
│       ├── CompanyResponse.java
│       ├── CompanyDetailResponse.java
│       └── CompanySummaryResponse.java
├── mapper/
│   └── CompanyMapper.java
└── specification/
    └── CompanySpecification.java
```

The project backend structure explicitly follows feature-first organization with controllers, services, repositories, entities, DTOs, mappers, and specifications inside the relevant feature. fileciteturn3file3L1-L20

---

# 53. Controller Responsibilities

Controllers MUST:

- accept DTOs
- validate request input
- use configured security
- delegate to services
- return DTOs
- use standard HTTP status codes
- use standard error handling
- follow documented endpoint conventions

Controllers MUST NOT:

- access repositories directly
- contain business rules
- manipulate database entities as API contracts
- contain complex query construction
- perform long-running business workflows
- call vendor SDKs directly

---

# 54. Service Responsibilities

Services contain:

- business logic
- business validation
- transactions
- orchestration
- repository coordination
- integration coordination
- audit calls where required

Typical flow:

```text
Controller
    ↓
Service
    ↓
Repository / Provider
```

Do not move business rules into controllers merely to reduce service code.

---

# 55. Repository Responsibilities

Repositories contain persistence operations.

They may provide:

- query methods
- specifications
- projections
- persistence-specific queries

Repositories must not become the location for business workflows.

---

# 56. DTO Rules

DTOs must be:

- explicit
- typed
- stable
- purpose-specific
- validated
- free from persistence internals

Avoid one universal DTO such as:

```text
CompanyDTO
```

when create, update, summary, detail, and search contracts have different requirements.

---

# 57. API Naming Checklist

Before adding an endpoint:

```text
[ ] Resource name is correct
[ ] HTTP method is correct
[ ] /api/v1 prefix used
[ ] Path parameters are appropriate
[ ] Query parameters are consistent
[ ] Request DTO exists
[ ] Response DTO exists
[ ] Validation exists
[ ] Authorization exists
[ ] Service method exists
[ ] Repository/query exists if required
[ ] Pagination exists if list endpoint
[ ] Standard error handling exists
[ ] OpenAPI documentation exists
[ ] API tests exist
[ ] Audit behavior reviewed
```

---

# 58. Adding a New API

When adding an API:

1. Confirm it is required by the PRD/roadmap.
2. Check whether the endpoint already exists in the Technical Design.
3. Follow `/api/v1`.
4. Use UUIDs.
5. Use snake_case JSON.
6. Use documented pagination conventions.
7. Use RFC-7807 errors.
8. Enforce authorization server-side.
9. Apply business invariants in the service/domain layer.
10. Record mutations in `audit_log` where required.
11. Add unit tests.
12. Add integration/API tests.
13. Update OpenAPI.
14. Update API documentation.

If the required endpoint is missing from the approved specification:

> **Propose it. Do not invent it silently.**

This workflow is explicitly defined in the existing project guidance. fileciteturn3file8L1-L20

---

# 59. API Change Workflow

Use:

```text
Requirement
    ↓
PRD / Domain / Technical Design
    ↓
Existing API inspection
    ↓
API contract design
    ↓
Request DTO
    ↓
Response DTO
    ↓
Controller
    ↓
Service
    ↓
Repository / Provider
    ↓
Tests
    ↓
OpenAPI
    ↓
Documentation
```

Do not start by writing controller code before understanding the contract.

---

# 60. Definition of Done

An API change is complete only when:

```text
[ ] Requirement confirmed
[ ] Existing API inspected
[ ] Endpoint contract defined
[ ] HTTP method correct
[ ] /api/v1 used
[ ] Request DTO implemented
[ ] Response DTO implemented
[ ] Entity not exposed
[ ] Validation implemented
[ ] Service logic implemented
[ ] Authorization implemented
[ ] Standard errors implemented
[ ] Pagination/filter/search implemented where required
[ ] Audit behavior reviewed
[ ] API tests added
[ ] Integration tests added where appropriate
[ ] OpenAPI updated
[ ] Documentation updated
[ ] Backward compatibility reviewed
[ ] No silent contract changes
```

---

# 61. Most Important API Rules

> **Never expose JPA entities through REST.**

> **Never accept JPA entities from REST requests.**

> **Never put business logic in controllers.**

> **Never create an endpoint without checking the approved specification first.**

> **Never silently change an existing API contract.**

> **Never create a different pagination or error format for one feature.**

> **Always use DTOs, standard errors, server-side authorization, tests, and OpenAPI documentation.**

---

## Final API Architecture

```text
                         REST CLIENT
                              │
                              ▼
                    /api/v1/{resource}
                              │
                              ▼
                       Controller
                              │
                       Request DTO
                              │
                              ▼
                          Service
                     ┌────────┴────────┐
                     ▼                 ▼
                 Repository       Integration
                     │                 │
                     ▼                 ▼
                   Entity          External API
                     │
                     ▼
                 PostgreSQL
                     │
                     ▼
                   Mapper
                     │
                     ▼
               Response DTO
                     │
                     ▼
                HTTP Response
```

The ProspectSoul API must remain:

**DTO-first + versioned + secure + consistent + paginated + documented + testable + backward-compatible.**
