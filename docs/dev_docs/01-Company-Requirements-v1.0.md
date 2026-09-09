<!--
Document: 01-Company-Requirements-v1.0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Scope: Company Management + Excel/CSV Import
Audience: Product, Engineering, QA, Claude Code
-->
# ProspectSoul — Company Management Requirements

**Version:** 1.0  
**Status:** Ready for implementation  
**Scope:** Company CRUD + Excel/CSV Import + Common App Shell

## 1. Purpose
Implement a complete Company Master vertical slice for ProspectSoul across the existing Spring Boot backend and React frontend.

The module must support company creation, viewing, editing, searching, sorting, filtering, pagination, verification, auditability, and Excel/CSV import through the existing import framework.

## 2. Authoritative project context
The existing ProspectSoul documentation hierarchy is:
1. PRD v1.1
2. Domain Model v1.0
3. Technical Design Specification v1.0
4. UI/UX Specification v1.0
5. Implementation Roadmap v1.0
6. Technical-Team Developer Guide / CLAUDE.md

Do not silently resolve conflicts. The implementation must preserve the documented architecture and scope.

## 3. Company Master fields
The Company record must support:
- id: UUID
- canonical_name: required company name
- normalized_name: system-generated normalized matching name
- website_domain: nullable
- primary_phone_normalized: nullable
- email: nullable
- city: nullable
- state: nullable
- cluster: nullable
- industry: nullable
- size_band: nullable
- tags: nullable/list
- source: nullable at company level if the implementation keeps source lineage there
- pipeline_state
- completeness_score
- verification_status
- verified_by: nullable initially
- verified_at: nullable initially
- created_by: nullable initially where no authenticated actor value is available
- created_at
- updated_by: nullable initially where no authenticated actor value is available
- updated_at

Import source is required on every import batch and uses the documented values:
EXCEL_CSV, INDIAMART, TRADEINDIA, LINKEDIN, MANUAL_ENTRY, API (reserved/not active in v1).

## 4. CRUD
### Create
Create a company through the service layer. Normalize supported values, validate required data, perform deterministic duplicate checks, and record the mutation in audit_log.

Manual entry follows the same lineage concept as the import framework and is a batch-of-one.

### Read
Company list supports:
- search
- pagination
- sorting
- filters
- detail view

### Update
Editable company fields can be changed only by authorized roles. Core-field changes must follow the documented verification invalidation rule.

### Delete
Do not introduce destructive hard deletion as a normal Company action. Use the documented lifecycle states and permanent history model.

## 5. Search and filtering
The Company list must support documented search behavior and useful filters including:
- company name
- phone
- email
- website/domain
- city
- state
- industry
- cluster
- source
- pipeline state
- verification status

Use the existing documented search conventions. Do not replace them with unrelated search architecture.

## 6. Verification
New records start unverified unless an existing project rule says otherwise.
- verification_status
- verified_by
- verified_at

Verification is record-level in v1.
Verification mutation must be audited.
When a core company field is changed, apply the existing rule that verification is dropped.

## 7. Common application shell
Use/reuse one common:
- Header
- Sidebar
- Main content area
- Footer

The current scope must expose functional navigation for:
- Dashboard
- Companies
- Imports

Do not duplicate the shell in each page.

## 8. Excel/CSV import
Support file-based import through the documented import framework:
- Excel
- CSV

Every input row must first be retained as an import row before processing. Nothing is silently discarded.

The import flow must expose:
1. Upload
2. Header detection
3. Mapping
4. Preview
5. Validation
6. Import
7. Processing status
8. Batch report

## 9. Flexible column-name recognition
Excel/CSV users are not required to use exact database/API field names.

The importer must normalize headers and use a configurable alias/mapping mechanism.

Examples:

### Company name → canonical_name
Company, Company Name, Company_Name, CompanyName, Organization, Organization Name, Organisation, Firm, Firm Name, Business Name, Legal Name

### Phone → primary_phone_normalized
Phone, Phone Number, Phone No, Phone No., Mobile, Mobile Number, Mobile No, Contact Number, Contact No, Telephone, Telephone Number, Business Phone, Office Phone, Contact Phone

### Email → email
Email, Email ID, Email Address, E-mail, E-mail ID, Mail, Company Email, Business Email

### Website → website_domain
Website, Website URL, Web, URL, Company Website, Web Address, Domain

### City → city
City, Town, Location, Company City, Business Location

### State → state
State, State Name, Province, Region, Company State

### Industry → industry
Industry, Business Type, Sector, Business Sector, Industry Type

Aliases must be extensible/configurable rather than buried in controller code.

## 10. Header normalization
Normalize headers before matching:
- trim
- case-fold
- collapse whitespace
- treat spaces/underscores/hyphens/punctuation consistently
- support common joined forms such as CompanyName

The normalized header is then matched against configured aliases.

## 11. Mapping confidence
Use:
1. exact configured alias
2. normalized alias match
3. suggestion/fuzzy matching only where safe

If confidence is low or ambiguous, do not silently choose a target field. Show a suggested mapping and allow manual selection or Ignore Column.

## 12. Value normalization
Apply the existing ProspectSoul normalization rules:
- Phone: normalize Indian formats to the documented comparable form; invalid values are retained and flagged, not silently discarded.
- Website: normalize to registered domain.
- Company name: case-fold, strip punctuation/whitespace and configured legal suffixes.
- Location: normalize city/state against the configured reference data.

## 13. Duplicate handling
Use the documented deterministic duplicate checks:
1. normalized phone
2. website domain
3. normalized company name + city

A duplicate match creates a candidate. Do not automatically merge.
Human resolution remains authoritative.

## 14. Import report
Show at minimum:
- total rows
- created
- duplicate-flagged
- rejected
- processing state
- row-level error/reason
- raw row visibility
- created company reference where applicable

## 15. Auditability
Record mutations using the existing AuditService/audit_log architecture. Do not create a second audit mechanism.

## 16. Authorization
Enforce role permissions server-side. UI hiding is not sufficient.
Viewer must not be able to mutate Company records or import data.

## 17. Acceptance criteria
The feature is complete when:
- Company CRUD works end-to-end.
- Search/sort/filter/pagination work.
- Verification works and is audited.
- Excel and CSV upload work.
- Headers with different names map correctly.
- User can correct mappings.
- Preview and validation work.
- Values are normalized.
- Duplicate candidates are detected without auto-merge.
- Raw import rows are preserved.
- Async batch status/report works.
- Common header/sidebar/footer are reused.
- Authorization and 403 tests exist.
- OpenAPI and README match the implementation.
- Unit and integration tests pass.
