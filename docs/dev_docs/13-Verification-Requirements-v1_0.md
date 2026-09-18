<!--
Document: 13-Verification-Requirements-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Scope: Company Verification Module (Twilio Lookup)
Audience: Product, Engineering, QA, Claude Code
Position in doc set: docs/dev_docs/13 — companion to 01–12 (existing) and to the PRD v1.1,
Domain Model v1.0, Technical Design Spec v1.0, UI/UX Spec v1.0, Implementation Roadmap v1.0,
and Technical-Team Developer Guide. Does not replace any of them.
-->
# ProspectSoul — Company Verification Requirements
**Version:** 1.0 · **Status:** Approved for implementation · **Module:** Verify
**Doc index:** 13 of the `docs/dev_docs` set

## 1. Purpose
The Verify module is a dedicated operational workspace for checking prospect-company phone numbers through Twilio Lookup and maintaining ProspectSoul's record-level verification state.

It must support:
- Verify navigation/sidebar entry
- filtering unverified companies by Added By and date range
- selecting companies
- background verification
- live progress while staying on the page
- persistent progress after navigation/reload
- completed-job history and statistics
- verified-company listing
- audit and company-timeline integration
- existing Keycloak RBAC

## 2. Product Alignment
ProspectSoul already defines record-level verification as `UNVERIFIED` / `VERIFIED (by, at)` (PRD §6, Domain Model Invariant 5). Verification is an existing activity type (`VERIFICATION`) and already appears in the Company Timeline (PRD §3.6). This module does not introduce a new verification concept — it **operationalizes the existing one at batch scale**, the same way the Research Queue operationalizes AI research (PRD Stage 3).

Nothing in this module changes:
- the meaning of `UNVERIFIED` / `VERIFIED`
- the rule that editing a core field drops verification
- the fact that verification is record-level, not field-level (PRD Decision 5)

## 3. User Flow
```text
Verify sidebar
  → Verify page
  → Verify New Companies
  → filter companies
  → select unverified companies
  → review selection
  → Start Verification
  → background job
  → Twilio Lookup
  → persist result
  → update company verification state
  → audit + timeline
  → completed history
```

## 4. Verify Page
Sections:
1. Page header and `Verify New Companies` CTA
2. Current Verification
3. Previous Verification Jobs
4. Verified Companies

The final company table shows **verified companies only**.

## 5. Filters
Required:
- Added By: All or a specific user
- Date From
- Date To
- Verification Status: default `UNVERIFIED`

Date range is inclusive.

## 6. Selection Rules
A company is eligible when:
- it exists and is not archived/deleted;
- it is currently unverified;
- it has a usable primary phone;
- it is not already being processed by another verification batch.

No eligible phone:
`SKIPPED` with `NO_PHONE`.

Already verified when the batch starts:
`SKIPPED` with `ALREADY_VERIFIED`.

## 7. Twilio Verification
The backend uses Twilio Lookup v2 Line Type Intelligence.

Recommended v1 success rule:
`valid = true` AND line type is `mobile`.

This proves phone validity/line classification, not ownership or human answer. This distinction must be visible in the UI (Section 9 of `14-Verification-Technical-Specification-v1_0.md`) so analysts do not mistake it for a confirmed human conversation.

The frontend must never call Twilio directly.

## 8. Background Processing
Starting a verification must return immediately with `202 Accepted`.

Statuses:
- `QUEUED`
- `PROCESSING`
- `COMPLETED`
- `COMPLETED_WITH_ERRORS`
- `FAILED`
- `CANCELLED`

One company/provider failure must not stop the batch — same per-record failure isolation principle as the existing Research Worker (Technical Design Spec §6).

## 9. Progress
Progress must survive:
- route changes
- browser refresh
- backend restart

The backend database is the source of truth.

The UI shows:
- percentage
- total
- queued
- processing
- verified/completed
- failed
- skipped
- current company
- start time
- elapsed time

## 10. History
Completed jobs retain:
- requester
- filter snapshot
- total
- verified
- failed
- skipped
- started/completed timestamps
- detailed item results

## 11. Verified Companies
The Verify page's bottom table returns verified companies only, with:
- company
- verified by
- verified at
- added by
- added at
- verified phone
- line type
- carrier

## 12. Permissions
Existing roles (Technical Design Spec §3):
- Analyst: view/start verification
- Sales Lead: view/start verification
- Admin: view/start verification
- Viewer: read-only
- COO: read-only

Server-side authorization is mandatory. UI hiding is not sufficient (Domain Model Invariant: authorization must be enforced server-side).

## 13. Non-Goals
- SMS/OTP ownership verification
- calling a phone number
- sales outreach
- scraping
- CRM sync
- replacing existing company verification
- storing Twilio credentials in the frontend

## 14. Relationship to Scope Controls
This module is additive within existing v1 scope (PRD §12/§43). It does not:
- introduce a new pipeline state
- change qualification or export rules
- add a new AI capability
- add a new timeline/event store

If any implementation detail here appears to require one of the above, stop and raise the conflict rather than resolving it silently (per the Developer Guide's conflict-handling rule).
