<!--
Document: 02-Company-Technical-Specification-v1.0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Scope: Company Management + Excel/CSV Import
Audience: Product, Engineering, QA, Claude Code
-->
# ProspectSoul — Company Management Technical Specification

**Version:** 1.0  
**Status:** Implementation contract for this scope

## 1. Architecture
Use the existing ProspectSoul modular-monolith architecture:
- Spring Boot backend
- React frontend
- PostgreSQL
- Keycloak/OIDC
- Flyway
- existing object storage abstraction where required

Backend is feature-first:
```text
com.vyoog.prospectsoul/
  config/
  common/
  company/
  contact/
  imports/
  ...
```

Frontend:
```text
frontend/src/
  api/
  auth/
  components/
  pages/
  lib/
```

Use the repository's actual package names if they already differ; do not restructure unrelated modules.

## 2. Backend layering
Company:
```text
company/
  controller/
  service/
  repository/
  entity/
  dto/
  mapper/
```

Import:
```text
imports/
  controller/
  service/
  repository/
  entity/
  dto/
  mapper/
  normalization/
  mapping/
```

Use DTO-first APIs:
Request DTO → Controller → Service → Repository → Entity
Entity → Service → Response DTO → Controller

Controllers must not accept or return JPA entities.

## 3. Database
Use new Flyway migrations only. Never edit an applied migration.

Company table must support:
```text
id UUID PK
canonical_name
normalized_name
website_domain
primary_phone_normalized
email
city
state
cluster
industry
size_band
tags
source
pipeline_state
completeness_score
verification_status
verified_by
verified_at
created_by
created_at
updated_by
updated_at
```

Use UUIDs and appropriate foreign keys to the users mirror where that table exists.

Recommended indexes:
- normalized_name
- website_domain
- primary_phone_normalized
- email
- city
- state
- cluster
- industry
- pipeline_state
- verification_status
- created_at
- updated_at

Do not add speculative tables that duplicate documented domain concepts.

## 4. Import data model
Use the existing documented import entities/tables:
- import_batches
- import_rows
- import_templates
- import_template_mappings
- audit_log

Every source row is persisted before processing.

If the existing repository already has these tables/entities, extend them rather than duplicating them.

## 5. Mapping model
The mapping implementation must support:
- source header
- normalized source header
- target Company field
- alias set
- active flag
- mapping type/confidence as needed

If a required mapping field is not present in the existing Technical Design, treat the smallest compatible addition as a documented implementation proposal.

Do not invent a second unrelated mapping architecture.

## 6. Header normalization service
Implement a reusable service, for example:
```text
HeaderNormalizer
```

Responsibilities:
- trim
- lowercase/case-fold
- normalize whitespace
- normalize separators
- remove irrelevant punctuation
- produce stable comparison key

Example:
```text
"Company Name"
"company_name"
"COMPANY-NAME"
"CompanyName"
```
must resolve to a comparable key.

## 7. Alias registry
Implement a reusable configurable alias registry.

Initial aliases must cover at least:
```text
canonical_name:
Company
Company Name
Company_Name
CompanyName
Organization
Organization Name
Organisation
Firm
Firm Name
Business Name
Legal Name

primary_phone_normalized:
Phone
Phone Number
Phone No
Phone No.
Mobile
Mobile Number
Mobile No
Contact Number
Contact No
Telephone
Telephone Number
Business Phone
Office Phone
Contact Phone

email:
Email
Email ID
Email Address
E-mail
E-mail ID
Mail
Company Email
Business Email

website_domain:
Website
Website URL
Web
URL
Company Website
Web Address
Domain

city:
City
Town
Location
Company City
Business Location

state:
State
State Name
Province
Region
Company State

industry:
Industry
Business Type
Sector
Business Sector
Industry Type
```

Make this extensible.

## 8. Mapping suggestion algorithm
Order:
1. exact normalized alias
2. configured alias match
3. safe fuzzy suggestion

Return:
- source column
- target field
- confidence
- reason/match type
- ambiguous flag

Low-confidence mappings must require user confirmation.

## 9. Import processing
Recommended flow:
```text
Upload
  ↓
Create import_batch
  ↓
Persist import_rows raw
  ↓
Detect/map headers
  ↓
Preview/validate
  ↓
Async process
  ↓
Normalize values
  ↓
Duplicate check
  ↓
Create company OR flag duplicate OR reject
  ↓
Update row outcome
  ↓
Complete batch
```

Do not drop rows.

## 10. Normalization
Phone:
- strip documented country prefix forms
- remove spaces/punctuation
- produce comparable value
- classify where existing rules require it
- invalid values remain stored and flagged

Website:
- remove scheme
- remove www where applicable
- remove path/query
- normalize registered domain

Company name:
- case-fold
- strip punctuation
- collapse whitespace
- strip configurable legal suffixes

Location:
- normalize against existing reference data.

## 11. Duplicate detection
Use deterministic matching:
```text
normalized phone
OR
website domain
OR
normalized name + city
```

Persist a duplicate candidate using the existing model if present.
Do not auto-merge.

## 12. APIs
Preserve documented Company APIs where they already exist:
```text
GET   /api/v1/companies
GET   /api/v1/companies/{id}
POST  /api/v1/companies
PATCH /api/v1/companies/{id}
POST  /api/v1/companies/{id}/verify
GET   /api/v1/companies/{id}/timeline
```

Preserve documented import APIs where they already exist:
```text
POST /api/v1/imports
GET  /api/v1/imports
GET  /api/v1/imports/{id}
GET  /api/v1/imports/{id}/rows
GET  /api/v1/import-templates
POST /api/v1/import-templates
PATCH /api/v1/import-templates/{id}
```

If an endpoint required for mapping preview/confirmation is absent:
- document it as a proposed endpoint,
- use /api/v1,
- UUIDs,
- snake_case JSON,
- documented pagination,
- RFC-7807 errors,
- server-side authorization,
- audit mutation,
- OpenAPI update.

## 13. Frontend
Use existing project stack and component library.

Required routes should be aligned to the existing routing conventions. At minimum:
- Companies list
- Company create/edit/detail
- Imports list/detail/wizard

Do not create duplicate layout components if an app shell exists.

## 14. Company table
Recommended visible columns:
- Company Name
- Phone
- Email
- Website
- City
- State
- Industry
- Cluster
- Source
- Pipeline State
- Verification
- Created At
- Actions

## 15. Import wizard
Steps:
1. Upload
2. Detect Columns
3. Map
4. Preview
5. Validate
6. Import
7. Processing
8. Report

Mapping UI must allow:
- suggested mapping
- manual field selection
- Ignore Column
- confidence/status
- preview

## 16. Error handling
Use existing problem+json/RFC-7807 conventions.
Expose row-level validation errors without leaking stack traces.

## 17. Security
Enforce permissions on backend endpoints.
Test forbidden paths.

## 18. Testing
Unit:
- header normalization
- aliases
- mapping confidence
- phone normalization
- website normalization
- company name normalization
- duplicate detection
- validation

Integration/Testcontainers:
- create company
- update company
- verify company
- upload file
- persist rows
- process rows
- create companies
- duplicate candidate
- rejected row
- batch completion
- authorization
- audit

## 19. Non-functional targets
Follow the existing v1 envelope:
- batches up to 10k rows
- 5k-row batch processed asynchronously within minutes
- search sub-second for standard lookups
- no silent data loss
- auditability
