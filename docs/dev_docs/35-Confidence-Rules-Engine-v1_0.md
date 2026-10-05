<!--
Document: 35-Confidence-Rules-Engine-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Ready for Implementation
Scope: Confidence computation rules and engine
Audience: Engineering, QA, Claude Code
Depends on: 29, 30, 31
-->
# ProspectSoul — Mobile Number Confidence Rules Engine

**Version:** 1.0
**Date:** 30 September 2026

---

## 1. Overview

The Confidence Engine is a deterministic, unit-testable service that computes the confidence level (HIGH, MEDIUM, LOW) for every phone number in the system. It runs:
- On phone creation (import or manual entry)
- On phone update (source change, contact link change)
- On contact designation change (linked phone re-evaluated)
- On manual override (by Analyst/Admin)

---

## 2. Source-to-Confidence Matrix

### 2.1 Base Confidence by Number Source

| Number Source | Base Confidence | Rationale |
|---|---|---|
| BUSINESS_CARD | HIGH | Direct personal exchange — the number was handed to the team |
| FIELD_VISIT | HIGH | In-person verification — the team visited the site |
| REFERENCE | HIGH | Referred by a trusted source — personal network |
| MANUAL_ENTRY | USER_SELECTED | Analyst judgment — they choose H/M/L based on their knowledge |
| WEBSITE | MEDIUM | Publicly available, likely current, but not personally verified |
| GOOGLE_API | MEDIUM | Aggregated data, reasonably current, not personally verified |
| LINKEDIN | MEDIUM | Professional network data, moderate reliability |
| INDIAMART | LOW | Bulk directory data, frequently outdated, designation data unreliable |
| IMPORT_DEFAULT | MEDIUM | Generic file import with no specific source attribution |

### 2.2 User-Selected (Manual Entry) Defaults

When Number Source is MANUAL_ENTRY:
- The UI presents a dropdown with HIGH, MEDIUM, LOW
- Default selection: MEDIUM
- The user may change it to HIGH or LOW before saving
- Confidence mode is set to MANUAL if the user changes from default, AUTO if they accept Medium

---

## 3. Designation Override Rules

### 3.1 Decision-Maker Check

When a phone number is associated with a designation (either via linked contact or designation_override), the engine checks the designation against the `decision_maker_designations` table.

Matching algorithm:
1. Normalize the input designation: trim, lowercase, collapse whitespace
2. Check against `decision_maker_designations.designation` (case-insensitive exact match)
3. Check against `decision_maker_designations.aliases[]` (case-insensitive exact match)
4. If matched → is_decision_maker = true

### 3.2 Designation Override — Standard Sources

For all Number Sources EXCEPT INDIAMART:
- If is_decision_maker = true AND base confidence < HIGH → upgrade to HIGH
- If is_decision_maker = false → use base confidence

### 3.3 Designation Override — IndiaMART Exception

For Number Source = INDIAMART:
- If is_decision_maker = true AND base confidence (LOW) < MEDIUM → upgrade to MEDIUM (not HIGH)
- Rationale: IndiaMART designation data is unreliable. The upgrade is partial.

### 3.4 Designation Override Summary

| Source | No DM | DM |
|---|---|---|
| BUSINESS_CARD | HIGH | HIGH (no change) |
| FIELD_VISIT | HIGH | HIGH (no change) |
| REFERENCE | HIGH | HIGH (no change) |
| MANUAL_ENTRY | User-selected | HIGH (unless user already selected HIGH) |
| WEBSITE | MEDIUM | HIGH |
| GOOGLE_API | MEDIUM | HIGH |
| LINKEDIN | MEDIUM | HIGH |
| INDIAMART | LOW | MEDIUM |
| IMPORT_DEFAULT | MEDIUM | HIGH |

---

## 4. Never-Downgrade Rule

### 4.1 Rule Definition

Once a phone number achieves a confidence level, it cannot be automatically downgraded by a subsequent event. Specifically:

- If the same normalized phone arrives from a new source with lower confidence → the phone record's confidence stays at its highest level
- If a linked contact's designation changes from decision-maker to non-decision-maker → the phone's confidence stays at its current level (it was legitimately upgraded)
- If the phone's number_source is changed to a lower-confidence source → the confidence stays at its current level

### 4.2 Exceptions

The never-downgrade rule does NOT apply to:
- **Manual override by Analyst/Admin:** a human can explicitly set any confidence level (including lower), with a mandatory reason
- **Phone marked as INVALID after re-validation:** if a number is later determined to be invalid (e.g., normalization corrected), the confidence discussion is moot (INVALID numbers have no operational confidence)

### 4.3 Implementation

```pseudocode
function computeConfidence(phone, newSource, designation, existingConfidence, existingMode):
    // Rule 0: Manual overrides are never auto-changed
    if existingMode == MANUAL:
        return existingConfidence

    // Rule 1: Compute base confidence from source
    baseConfidence = SOURCE_CONFIDENCE_MAP[newSource]

    // Rule 2: Apply designation override
    if isDecisionMaker(designation):
        if newSource == INDIAMART:
            designationConfidence = MEDIUM
        else:
            designationConfidence = HIGH
        baseConfidence = max(baseConfidence, designationConfidence)

    // Rule 3: Never downgrade
    if existingConfidence is not null:
        return max(existingConfidence, baseConfidence)

    return baseConfidence
```

Where `max()` uses the ordering: HIGH > MEDIUM > LOW.

---

## 5. Manual Override

### 5.1 Who Can Override

| Role | Can Override |
|---|---|
| ANALYST | Yes — on phones they manage (any company they can edit) |
| SALES_LEAD | Yes |
| ADMIN | Yes — any phone |
| VIEWER | No |
| COO | No |

### 5.2 Override Process

1. User clicks "Override Confidence" on a phone row (in company detail or edit form)
2. Modal: select new confidence level (HIGH/MEDIUM/LOW), enter reason (min 10 characters)
3. On confirm:
   - `confidence` updated to selected value
   - `confidence_mode` set to MANUAL
   - `override_reason` stored
   - Audit log entry: entity_type=COMPANY_PHONE, entity_id=phone_id, action=CONFIDENCE_OVERRIDE, previous_state={level, mode}, new_state={level, mode, reason}

### 5.3 Manual Override Visibility

Phones with MANUAL confidence mode show a special indicator:
- In the form: badge shows "H✎" (confidence + manual icon)
- In the detail view: tooltip "Manually set to High by Priya — 'Confirmed via direct call'"
- In the companies list: the confidence badge is unchanged (H/M/L) — the manual indicator is only visible in detail/edit views

---

## 6. Confidence Recomputation Triggers

| Event | Action |
|---|---|
| Phone created (import or form) | Compute confidence from source + designation |
| Phone number_source changed | Recompute (never-downgrade applies) |
| Contact linked to phone | Recompute with contact's designation |
| Contact unlinked from phone | Do not downgrade (never-downgrade rule) |
| Contact designation changed | Recompute all phones linked to that contact (never-downgrade applies) |
| Same normalized phone imported from new source | Do not downgrade existing phone. New source logged for lineage |
| Manual override applied | Set directly, skip auto-computation |
| Designation_override field changed | Recompute (never-downgrade applies) |

---

## 7. Completeness Score Impact

The existing completeness score (PRD §6) weights "mobile phone" as a core field. With multi-phone support:

- **Has phone:** true if the company has at least one phone record with phone_type = MOBILE or LANDLINE and number_normalized IS NOT NULL
- **Phone quality bonus (optional, Admin-configurable):** if the primary phone has HIGH confidence, the phone component of completeness is weighted at 100%. MEDIUM = 75%. LOW = 50%. This is a future enhancement — v1.2 uses a simple has/doesn't-have check.

---

## 8. Edge Cases

### 8.1 Same Number, Different Companies

Two different companies can legitimately have the same phone number (e.g., a shared reception number). This is handled by dedup: the second occurrence creates a duplicate candidate. If resolved as NOT_DUPLICATE, both companies retain the number independently. Confidence is computed independently for each.

### 8.2 Same Number, Same Company, Different Sources

If the same normalized phone number is added to the same company from a different source:
- Do NOT create a duplicate phone record
- Update the existing record's confidence using never-downgrade logic
- Log the additional source in the audit trail for lineage

### 8.3 Phone Number Correction

If a phone number is corrected (e.g., one digit wrong):
- The old phone record is soft-updated (number_raw and number_normalized changed)
- Confidence is NOT reset — the source and designation haven't changed
- If the corrected number matches a dedup candidate, the system flags it

### 8.4 Bulk Import — Thousands of Phones

Performance consideration: the confidence engine must handle batch computation efficiently. For import processing:
- Resolve decision-maker designations once per batch (cache the designation list)
- Compute confidence per phone in the import row processor
- Dedup checks are already batched in the import pipeline
