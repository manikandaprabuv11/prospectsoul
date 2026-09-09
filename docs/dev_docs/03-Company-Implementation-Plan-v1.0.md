<!--
Document: 03-Company-Implementation-Plan-v1.0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Scope: Company Management + Excel/CSV Import
Audience: Product, Engineering, QA, Claude Code
-->
# ProspectSoul — Company Management Implementation Plan

**Version:** 1.0  
**Execution model:** vertical slice  
**Owner:** Engineering / Claude Code

## Phase 0 — Repository and specification audit
Tasks:
- read CLAUDE.md
- read PRD, Domain Model, Technical Design, Roadmap, UI/UX docs
- inspect current backend/frontend/database
- identify reusable Company/Import code
- identify existing app shell
- identify existing APIs and migrations
- produce a short gap assessment

Exit criteria:
- no uncertainty about existing architecture
- no duplicate implementation created unnecessarily

## Phase 1 — Database and domain foundation
Tasks:
- inspect existing company/import migrations
- create new Flyway migration(s) only where required
- add email/source/audit actor fields requested for Company
- create/update Company entity
- create/update import mapping entities only if needed
- add indexes
- add constraints
- add repository methods

Exit criteria:
- clean database migration succeeds
- existing migrations remain untouched
- entity/repository tests pass

## Phase 2 — Company backend
Implement:
- create
- get by id
- list
- search
- sort
- filter
- pagination
- update
- verify
- timeline integration if already supported

Use DTO-first architecture.

Exit criteria:
- API works end-to-end
- OpenAPI generated
- validation and authorization tested

## Phase 3 — Company frontend
Implement/reuse:
- AppShell
- Header
- Sidebar
- Footer
- Companies list
- Company create
- Company edit
- Company detail
- verification action
- search/sort/filter/pagination

Exit criteria:
- no duplicated shell
- loading/empty/error states
- role restrictions reflected in UI while backend remains authoritative

## Phase 4 — Import persistence
Implement/reuse:
- import batch creation
- raw row persistence
- row outcome model
- batch status
- batch report

Exit criteria:
- raw rows survive failed processing
- row numbers and raw data visible
- batch can be polled

## Phase 5 — Header mapping
Implement:
- HeaderNormalizer
- configurable alias registry
- exact matching
- normalized matching
- safe suggestions
- confidence
- manual override
- Ignore Column

Initial aliases must include Company Name, Phone/Telephone/Contact Number, Email, Website, City/Town/Location, State/Region, Industry/Sector/Business Type.

Exit criteria:
- differently named columns map correctly
- ambiguous mappings are not silently accepted

## Phase 6 — Import wizard
Implement frontend:
- upload
- detect
- map
- preview
- validate
- confirm
- progress
- report

Exit criteria:
- a real sample Excel file can complete the workflow

## Phase 7 — Value normalization and duplicate checks
Implement documented normalization rules.

Duplicate order:
1. phone
2. website domain
3. normalized name + city

Exit criteria:
- normalized values are comparable
- invalid values are retained/flagged
- duplicates become candidates
- no automatic merge

## Phase 8 — Audit and authorization
Verify:
- company create audit
- company update audit
- verification audit
- import mutation audit
- mapping mutation audit where applicable
- Viewer 403
- unauthorized import 403

Exit criteria:
- all introduced mutations are auditable
- server-side permissions tested

## Phase 9 — Test and harden
Run:
- unit tests
- backend integration tests/Testcontainers
- frontend tests if existing project setup supports them
- build
- lint/type checks if configured

Test failure paths:
- missing company name
- invalid phone
- ambiguous mapping
- unsupported file
- empty file
- duplicate
- malformed row
- unauthorized request

## Phase 10 — Documentation and handoff
Update:
- OpenAPI
- README
- import mapping documentation
- supported aliases
- run instructions
- test instructions

Final report:
- changed files
- migrations
- APIs
- frontend routes
- tests
- acceptance criteria
- deviations
- remaining work

## Release gate
Do not mark complete until:
- backend build passes
- frontend build passes
- clean migration passes
- CRUD passes
- import passes
- mapping passes
- normalization passes
- duplicate detection passes
- audit passes
- authorization passes
- required tests pass
- docs updated

## Scope control
Do not implement:
- automated scraping
- programmatic source API
- AI research
- ICP qualification
- CRM outreach
- company relationships
- full contact lifecycle
unless an existing dependency makes a minimal implementation necessary.
