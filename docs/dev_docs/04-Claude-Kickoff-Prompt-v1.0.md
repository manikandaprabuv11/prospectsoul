<!--
Document: 04-Claude-Kickoff-Prompt-v1.0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Scope: Company Management + Excel/CSV Import
Audience: Product, Engineering, QA, Claude Code
-->
# ProspectSoul — Claude Implementation Kickoff Prompt

You are the senior full-stack engineer implementing a ProspectSoul vertical slice.

## AUTHORITATIVE DOCUMENTS
Before coding, read:
1. ProspectSoul PRD v1.1
2. ProspectSoul Domain Model v1.0
3. ProspectSoul Technical Design Specification v1.0
4. ProspectSoul UI/UX Specification v1.0
5. ProspectSoul Implementation Roadmap v1.0
6. CLAUDE.md
7. Technical-Team Developer Guide

Document hierarchy must be respected. If documents conflict, do not silently choose one. Identify the conflict.

## OBJECTIVE
Implement a complete Company Master and Excel/CSV import workflow across the existing:
- Spring Boot backend
- React frontend
- PostgreSQL database

This is a real end-to-end implementation, not a UI mock and not a backend-only task.

## FIRST: INSPECT
Before changing code:
- inspect repository
- inspect current Company code
- inspect current Import code
- inspect migrations
- inspect API contracts
- inspect frontend routes
- inspect app shell
- inspect authentication/Keycloak
- inspect tests
- inspect CLAUDE.md

Return a short gap assessment before implementation.

Reuse existing code wherever appropriate.

## COMPANY FIELDS
Support:
id
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

verified_by, verified_at, created_by and updated_by may initially be NULL where no authenticated actor value is available.

## COMPANY CRUD
Implement:
- create
- read/list
- detail
- update
- search
- sorting
- filtering
- pagination
- verify

Do not add destructive hard-delete as a normal Company action.

Follow the documented lifecycle model.

## BACKEND RULES
Use feature-first modular architecture.

Use:
controller
service
repository
entity
dto
mapper

Controllers must never expose JPA entities.

Use DTO-first requests/responses.

Use:
- UUID
- /api/v1
- snake_case JSON
- RFC-7807 errors
- server-side authorization
- AuditService/audit_log
- Flyway migrations

Never modify an already-applied migration.

## COMMON UI SHELL
Use/reuse one:
- Header
- Sidebar
- Footer

Functional current-scope navigation:
- Dashboard
- Companies
- Imports

Do not duplicate the shell inside individual pages.

## COMPANY UI
Company list:
- search
- sort
- filters
- pagination
- create
- edit
- view
- verify

Suggested columns:
Company Name
Phone
Email
Website
City
State
Industry
Cluster
Source
Pipeline State
Verification
Created At
Actions

## IMPORT
Support:
- .xlsx
- .csv

Follow:
Upload
→ Detect Headers
→ Map
→ Preview
→ Validate
→ Import
→ Processing
→ Report

Every row must be persisted before processing.

Never silently discard an input row.

## FLEXIBLE HEADER MAPPING — CRITICAL
Users will provide Excel files with different column names.

Do not require exact names.

Examples that must map to Company Name:
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

Examples that must map to Phone:
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

Examples that must map to Email:
Email
Email ID
Email Address
E-mail
E-mail ID
Mail
Company Email
Business Email

Examples that must map to Website:
Website
Website URL
Web
URL
Company Website
Web Address
Domain

Examples that must map to City:
City
Town
Location
Company City
Business Location

Examples that must map to State:
State
State Name
Province
Region
Company State

Examples that must map to Industry:
Industry
Business Type
Sector
Business Sector
Industry Type

Implement a reusable configurable alias mechanism.

## HEADER NORMALIZATION
Normalize:
- case
- whitespace
- underscores
- hyphens
- punctuation
- joined forms

Example:
Company Name
company_name
COMPANY-NAME
CompanyName

must compare equivalently.

## AMBIGUITY RULE
Use:
1. exact alias
2. normalized alias
3. safe suggestion

If confidence is low:
- show the suggestion
- show confidence/reason
- require user confirmation
- allow manual target selection
- allow Ignore Column

Never silently map ambiguous data to the wrong field.

## VALUE NORMALIZATION
Follow the existing ProspectSoul rules.

Phone:
- normalize country prefix forms
- remove spaces/punctuation
- produce comparable phone
- retain invalid values and flag them

Website:
- normalize to registered domain

Company name:
- case-fold
- strip punctuation
- collapse whitespace
- remove configured legal suffixes

Location:
- use configured reference normalization

## DUPLICATES
Use documented deterministic matching:
1. normalized phone
2. website domain
3. normalized company name + city

Duplicate matches are candidates.

Never automatically merge.

Human decision remains authoritative.

## IMPORT DATA MODEL
Reuse:
- import_batches
- import_rows
- import_templates
- import_template_mappings
- audit_log

Do not duplicate existing concepts.

If a mapping capability is missing from the current schema/API:
1. identify the gap,
2. propose the smallest compatible addition,
3. document it,
4. implement it consistently.

Do not silently invent an unrelated API.

## ASYNC PROCESSING
Follow the existing import architecture:
- upload
- persist batch
- persist raw rows
- process asynchronously
- expose batch status
- expose row outcomes
- expose report

## AUTHORIZATION
Enforce roles server-side.
Viewer must receive 403 for Company mutation/import operations.

Add tests for forbidden paths.

## AUDIT
Use the existing audit mechanism for:
- company create
- company update
- company verify
- import mutation
- mapping mutation where applicable

Do not build a second audit system.

## TESTS
Unit test:
- header normalization
- aliases
- mapping confidence
- phone normalization
- website normalization
- company-name normalization
- duplicate detection
- validation

Integration/Testcontainers test:
- create
- update
- verify
- upload
- raw row persistence
- mapping
- normalization
- duplicate candidate
- rejected row
- batch completion
- authorization
- audit

## DOCUMENTATION
Update:
- OpenAPI
- README
- developer documentation required by the repository

Document supported import aliases and mapping behavior.

## DEFINITION OF DONE
Do not claim completion until:
- backend builds
- frontend builds
- clean DB migration works
- CRUD works
- search works
- sorting works
- filtering works
- pagination works
- verification works
- Excel works
- CSV works
- flexible mapping works
- manual mapping override works
- preview works
- validation works
- normalization works
- duplicate detection works
- raw rows are preserved
- batch status/report works
- audit works
- authorization works
- tests pass
- OpenAPI is accurate
- README is updated

## FINAL RESPONSE FORMAT
Report:
1. Files created/changed
2. Database migrations
3. Backend implementation
4. Frontend implementation
5. Import mapping behavior
6. Tests and exact results
7. Acceptance criteria PASS/FAIL/BLOCKED
8. Specification deviations
9. Remaining work

Do not claim PASS without evidence.

## SCOPE CONTROL
Do not implement unrelated future functionality such as scraping, AI research, ICP qualification, CRM outreach, or company relationships unless strictly required by an existing dependency.

Start by inspecting the repository and the authoritative documents.
