# ProspectSoul Backend — Recommended File Structure

## Architecture Principle

Organize the backend by **business feature first, and technical layer second**.

Instead of one global set of `controller/`, `service/`, `repository/`, and `entity/` packages, each business capability owns its related layers.

This is a feature-first modular monolith structure.

---

## Recommended Structure

```text
backend/
│
├── pom.xml
├── Dockerfile
├── CLAUDE.md
│
├── src/
│   │
│   ├── main/
│   │   │
│   │   ├── java/
│   │   │   └── com/vyoog/prospectsoul/
│   │   │       │
│   │   │       ├── ProspectSoulApplication.java
│   │   │       │
│   │   │       ├── config/
│   │   │       │
│   │   │       ├── common/
│   │   │       │   ├── exception/
│   │   │       │   ├── dto/
│   │   │       │   ├── audit/
│   │   │       │   ├── pagination/
│   │   │       │   └── validation/
│   │   │       │
│   │   │       ├── company/
│   │   │       │   ├── controller/
│   │   │       │   ├── service/
│   │   │       │   ├── repository/
│   │   │       │   ├── entity/
│   │   │       │   ├── dto/
│   │   │       │   │   ├── request/
│   │   │       │   │   └── response/
│   │   │       │   ├── mapper/
│   │   │       │   └── specification/
│   │   │       │
│   │   │       ├── contact/
│   │   │       ├── imports/
│   │   │       ├── triage/
│   │   │       ├── activity/
│   │   │       ├── evidence/
│   │   │       ├── research/
│   │   │       ├── icp/
│   │   │       ├── qualification/
│   │   │       ├── export/
│   │   │       ├── report/
│   │   │       ├── admin/
│   │   │       └── ai/
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/
│   │           └── migration/
│   │
│   └── test/
│       └── java/
│           └── com/vyoog/prospectsoul/
│               ├── company/
│               ├── contact/
│               ├── imports/
│               ├── triage/
│               ├── activity/
│               ├── evidence/
│               ├── research/
│               ├── icp/
│               ├── qualification/
│               ├── export/
│               └── ai/
```

---

## Core Rule

> **Organize the backend by business feature first, and by technical layer second.**

Example:

```text
company/
    controller/
    service/
    repository/
    entity/
    dto/
    mapper/

contact/
    controller/
    service/
    repository/
    entity/
    dto/
    mapper/

imports/
    controller/
    service/
    repository/
    entity/
    dto/
    mapper/
```

Do not use a large global structure such as:

```text
controller/
service/
repository/
entity/
```

for the entire application.

---

## 1. config/

Application-wide Spring configuration belongs here.

Examples:

```text
config/
├── JpaConfig.java
├── SecurityConfig.java
├── OpenApiConfig.java
├── JacksonConfig.java
└── StorageConfig.java
```

Keep business-specific logic outside `config/`.

---

## 2. common/

Only genuinely shared backend infrastructure belongs here.

Recommended:

```text
common/
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── BusinessException.java
│
├── dto/
│   ├── PageResponse.java
│   └── ErrorResponse.java
│
├── audit/
├── pagination/
└── validation/
```

Do not turn `common/` into a dumping ground.

Avoid generic classes such as:

```text
Utils.java
Helper.java
CommonService.java
```

unless their responsibility is genuinely shared and well-defined.

---

## 3. Feature Modules

Each business feature should own its code.

Example:

```text
company/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
│   ├── request/
│   └── response/
├── mapper/
└── specification/
```

The same pattern can be used for:

```text
contact/
imports/
triage/
activity/
evidence/
research/
icp/
qualification/
export/
report/
admin/
ai/
```

Do not force every module to contain every folder.

For example, a report module that only performs queries may not need its own JPA entity.

---

## 4. DTO Structure

Use separate request and response DTOs:

```text
dto/
├── request/
└── response/
```

Examples:

```text
CompanyCreateRequest
CompanyUpdateRequest
CompanySearchRequest

CompanyResponse
CompanyDetailResponse
CompanySummaryResponse
```

Do not expose JPA entities directly through REST APIs.

Do not accept JPA entities directly from REST requests.

DTOs define the API boundary.

---

## 5. Controller

Controllers handle HTTP/API concerns only.

Flow:

```text
HTTP Request
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

Controllers should not contain business logic or direct repository access.

---

## 6. Service

Services contain business/application logic.

Services coordinate:

- repositories
- business rules
- validation
- transactions
- external integrations

Services should not contain HTTP-specific logic.

---

## 7. Repository

Repositories handle persistence.

Use Spring Data JPA repositories such as:

```java
JpaRepository
```

Repositories may contain:

- persistence queries
- specifications
- projections
- query methods

Business logic should remain in services/domain logic.

---

## 8. Entity

Entities represent persistence/domain state.

Do not expose entities directly through REST.

Be deliberate with JPA relationships:

- fetch type
- cascade
- orphan removal
- ownership
- foreign keys

Avoid blindly using:

```java
CascadeType.ALL
```

or:

```java
FetchType.EAGER
```

---

## 9. Mapper

Keep entity-to-DTO mapping separate from controllers and entities.

Examples:

```text
CompanyMapper
ContactMapper
ImportMapper
```

Use MapStruct or explicit mapper classes according to the project convention.

---

## 10. Specification

Use feature-specific specifications for complex dynamic filtering.

Example:

```text
company/specification/CompanySpecification.java
```

This is particularly useful for:

- pagination
- search
- filtering
- sorting
- optional criteria

Do not put complex filtering logic inside controllers.

---

## 11. Database

Database migrations belong here:

```text
src/main/resources/db/migration/
```

Example:

```text
V1__initial_schema.sql
V2__create_company_table.sql
V3__add_company_indexes.sql
```

Never modify an already-applied migration.

Create a new migration for schema changes.

---

## 12. Tests

Tests should mirror the feature structure.

Example:

```text
src/test/java/com/vyoog/prospectsoul/

├── company/
│   ├── controller/
│   ├── service/
│   └── repository/
│
├── contact/
├── imports/
├── triage/
├── activity/
├── evidence/
├── research/
├── icp/
├── qualification/
├── export/
└── ai/
```

Example test names:

```text
CompanyServiceTest.java
CompanyControllerTest.java
CompanyRepositoryIntegrationTest.java
```

---

## 13. AI Module

Keep AI integrations isolated from core business modules.

Recommended:

```text
ai/
├── capability/
├── provider/
│   ├── AiProvider.java
│   └── ...
├── dto/
└── config/
```

Preferred conceptual flow:

```text
Business Logic
      ↓
AI Capability
      ↓
AI Provider
      ↓
External AI Provider
```

Avoid tightly coupling core business services directly to a vendor SDK.

---

## 14. Imports Module

Import processing should remain isolated because it can contain parsing, validation, normalization, and batch-processing logic.

Possible structure:

```text
imports/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
│   ├── request/
│   └── response/
├── mapper/
├── parser/
└── normalization/
```

---

## 15. Do Not Over-Create Packages

The structure is a guideline, not a requirement to create empty folders.

Create a package when the feature actually needs it.

For example:

```text
report/
├── controller/
├── service/
└── dto/
```

may be completely valid if reports do not require persistence.

Avoid creating dozens of empty packages simply to make the tree look complete.

---

## 16. Recommended Architectural Flow

For a normal CRUD operation:

```text
Controller
    ↓
Request DTO
    ↓
Service
    ↓
Repository
    ↓
Entity
    ↓
Database
```

For the response:

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

The backend should follow this separation consistently.

---

## 17. Final Recommendation

For ProspectSoul, use a **feature-first modular monolith**.

The primary structure should be:

```text
com.vyoog.prospectsoul/
├── config/
├── common/
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
├── report/
├── admin/
└── ai/
```

Each feature owns its technical layers.

This provides:

- clear ownership
- easier navigation
- better maintainability
- lower coupling
- easier testing
- easier future extraction into services if required
- cleaner development with Claude Code
- better separation of business capabilities

Most importantly:

> Do not create the entire final structure at project initialization. Create the top-level feature modules, then add technical subpackages only when the feature requires them.
