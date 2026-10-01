<!--
Document: 39-Claude-Kickoff-Prompt-v1_2.md
Project: ProspectSoul
Version: 1.2
Status: Ready for Implementation
Scope: Mobile Number Confidence, Multi-Phone, Company Form, Companies List, GST, Filters
Audience: Claude Code
Depends on: 29–38
-->
# ProspectSoul — Claude Implementation Kickoff Prompt v1.2

You are the senior full-stack engineer implementing the v1.2 Mobile Number Confidence and Company Form Enhancement feature for ProspectSoul.

## AUTHORITATIVE DOCUMENTS

Read in this order before coding:

**Existing (unchanged):**
1. ProspectSoul PRD v1.1
2. ProspectSoul Domain Model v1.0
3. ProspectSoul Technical Design Specification v1.0
4. ProspectSoul UI/UX Specification v1.0
5. ProspectSoul Implementation Roadmap v1.0
6. CLAUDE.md / Technical-Team Developer Guide

**v1.2 additions (this scope):**
7. 29-PRD-Change-Request-v1_2.md — Amendments to PRD v1.1
8. 30-Functional-Requirements-v1_0.md — Complete functional spec
9. 31-Technical-Data-Model-Changes-v1_0.md — Schema, entities, DTOs, services
10. 32-Company-Form-Requirements-v1_0.md — Create/Edit form spec
11. 33-Companies-List-Requirements-v1_0.md — List page with confidence/designation
12. 34-Import-Export-Requirements-v1_0.md — Import multi-phone, confidence, GST
13. 35-Confidence-Rules-Engine-v1_0.md — Confidence computation rules
14. 36-API-Requirements-v1_0.md — New and modified endpoints
15. 37-Database-Migration-Requirements-v1_0.md — Migration scripts
16. 38-Acceptance-Criteria-Test-Cases-v1_0.md — Test matrix

Document hierarchy must be respected. Existing PRD v1.1 is authoritative; v1.2 documents amend it. If documents conflict, do not silently choose one — identify the conflict.

## OBJECTIVE

Implement the following on top of the existing ProspectSoul codebase:

1. **company_phones table** — multi-phone support with source, confidence, designation, lineage
2. **Confidence Engine** — deterministic auto-computation of confidence level per phone
3. **Decision-maker designations** — configurable reference table with Admin CRUD
4. **GST number** — new field on companies with format validation
5. **Company Create/Edit form redesign** — unified form with phone repeater
6. **Companies List enhancements** — confidence badge, designation column, new filters
7. **Filter persistence** — URL-based filter state on companies list
8. **Import enhancements** — multi-phone columns, number source, designation, GST mapping
9. **Dedup extension** — check all company_phones, not just primary
10. **Backfill migration** — move existing phone data into company_phones

This is a real end-to-end implementation across backend, frontend, and database.

## FIRST: INSPECT

Before changing code:
- Read all v1.2 documents (items 7–16 above)
- Inspect current company entity/DTO/controller/service
- Inspect current import processor and mapping framework
- Inspect current companies list component and its filters
- Inspect current company form components
- Inspect current dedup detection logic
- Inspect current database migrations
- Inspect current audit service

Return a short gap assessment: what exists, what needs to change, what's new.

## IMPLEMENTATION PHASES

### Phase A — Database Foundation
1. Create Flyway migrations (Doc 37) in correct order:
   - Enums (number_source_type, confidence_level, confidence_mode, phone_type)
   - company_phones table with indexes and constraints
   - decision_maker_designations table with seed data
   - companies.gst_number column with constraint
   - Import mapping aliases
2. Verify migrations apply cleanly
3. Do NOT modify existing migrations

### Phase B — Confidence Engine
1. Implement `ConfidenceEngine` service (Doc 35)
2. Implement `DecisionMakerDesignationService` for reference data lookup
3. Unit test all confidence rules: source mapping, designation override, IndiaMART exception, never-downgrade, manual override
4. Every test case in TC-1.x (Doc 38) must pass

### Phase C — Phone CRUD Backend
1. Create `CompanyPhone` entity, repository, DTO, mapper
2. Create `CompanyPhoneService` with add/update/delete/recomputePrimary
3. Create phone API endpoints (Doc 36 §1.1)
4. Integrate with existing `CompanyService`: create/update now handle phones[]
5. Extend `NormalizationService` to return phone_type classification
6. Integration test all TC-2.x cases

### Phase D — Dedup Extension
1. Modify duplicate detection to check `company_phones.number_normalized` instead of only `companies.primary_phone_normalized`
2. Dedup result includes matched phone details
3. Test TC-5.3.x cases

### Phase E — Import Extension
1. Add new mapping targets (alternate_phone_1/2, owner_phone, number_source, designation, gst_number)
2. Add column aliases (Doc 37 §7)
3. Modify import row processor to create company_phone records
4. Apply confidence auto-assignment per import source (Doc 34 §3)
5. Handle multi-phone conflicts (Doc 34 §2.4)
6. Test TC-5.x cases

### Phase F — GST
1. Add gst_number to Company entity/DTO
2. Implement format + check digit validation
3. Add to company create/update endpoints
4. Add import mapping for GST
5. Test TC-5.4.x cases

### Phase G — Company Form Frontend
1. Implement unified CompanyForm component (Doc 32)
2. Implement phone repeater with source dropdown, confidence badge, contact link
3. Implement inline duplicate check (debounced)
4. Implement verification drop warning
5. Implement GST validation on blur
6. Test TC-3.x cases

### Phase H — Companies List Frontend
1. Add Confidence, Designation columns (Doc 33 §2.1)
2. Implement phone display with "+N more" tooltip
3. Add new filters: Confidence, Number Source, Has Decision-Maker
4. Implement URL-based filter persistence (Doc 33 §8)
5. Test TC-4.x cases

### Phase I — Backfill and Migration
1. Run backfill migration on development data
2. Run post-migration normalization job
3. Verify backfill results match existing data
4. Verify primary_phone_normalized is unchanged

### Phase J — Audit, Authorization, Tests
1. Verify all new mutations produce audit_log entries
2. Verify VIEWER gets 403 on all write endpoints
3. Run full TC-7.x and TC-8.x test suites
4. Run existing test suite — no regressions

### Phase K — Documentation
1. Update OpenAPI spec with new endpoints and modified request/response shapes
2. Update README with new schema, new endpoints, new import aliases
3. Document confidence engine rules in developer documentation

## KEY RULES

### Confidence Engine
- Source → confidence mapping is deterministic (Doc 35 §2)
- Decision-maker designation upgrades to HIGH except IndiaMART → MEDIUM (Doc 35 §3)
- Never downgrade existing confidence automatically (Doc 35 §4)
- Manual override is never auto-changed (Doc 35 §5)

### Phone Numbers
- company_phones is the source of truth; companies.primary_phone_normalized is denormalized
- At most one primary per company (enforced by partial unique index)
- Dedup checks ALL phones, not just primary
- Each phone has lineage: source, batch, row, created_by

### Company Form
- Unified Create/Edit form (Doc 32)
- Phone repeater: add/remove rows, source dropdown, confidence badge, contact link
- Core-field edit on Verified company → verification drop warning
- Inline duplicate check on name and phone

### Companies List
- Confidence and Designation columns (Doc 33)
- URL-based filter persistence — filters survive Back button and tab return
- Search matches ALL phones, not just primary

### Import
- Multi-phone columns supported (primary, alternate_1, alternate_2, owner_phone)
- Confidence auto-assigned from Import Source and Number Source
- GST validated on import
- Export columns unchanged

### Existing behavior preserved
- companies.primary_phone_normalized continues to work for backward compatibility
- Existing dedup rules still function (now extended to all phones)
- Existing import templates continue to work
- Existing export format unchanged

## DEFINITION OF DONE

Do not claim completion until every item in Doc 38 §9 "Definition of Done" passes.

## FINAL RESPONSE FORMAT

Report:
1. Files created/changed
2. Database migrations (in order)
3. Backend implementation summary
4. Frontend implementation summary
5. Confidence engine behavior
6. Import changes
7. Tests and exact results per TC category
8. Acceptance criteria PASS/FAIL/BLOCKED per section
9. Specification deviations (with rationale)
10. Remaining work or open decisions

Do not claim PASS without evidence.

## SCOPE CONTROL

Do not implement:
- GST API lookup integration (pending OD-1)
- Phone number verification via SMS/OTP
- Automatic confidence upgrade from call outcomes
- Export template changes
- AI research changes
- ICP qualification changes
- Any other v1 feature not listed in this scope

If a dependency requires a minimal change to an out-of-scope area, document it and implement the smallest compatible addition.
