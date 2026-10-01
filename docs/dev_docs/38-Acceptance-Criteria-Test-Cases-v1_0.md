<!--
Document: 38-Acceptance-Criteria-Test-Cases-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Ready for Implementation
Scope: Acceptance criteria and test cases for all v1.2 changes
Audience: Engineering, QA, Claude Code
Depends on: 29–37
-->
# ProspectSoul — Acceptance Criteria & Test Cases

**Version:** 1.0
**Date:** 30 September 2026

---

## 1. Confidence Engine (Unit Tests)

### TC-1.1: Source-to-Confidence Mapping

| # | Input Source | Designation | Expected Confidence | Expected Mode |
|---|---|---|---|---|
| 1.1.1 | BUSINESS_CARD | null | HIGH | AUTO |
| 1.1.2 | FIELD_VISIT | null | HIGH | AUTO |
| 1.1.3 | REFERENCE | null | HIGH | AUTO |
| 1.1.4 | WEBSITE | null | MEDIUM | AUTO |
| 1.1.5 | GOOGLE_API | null | MEDIUM | AUTO |
| 1.1.6 | LINKEDIN | null | MEDIUM | AUTO |
| 1.1.7 | INDIAMART | null | LOW | AUTO |
| 1.1.8 | IMPORT_DEFAULT | null | MEDIUM | AUTO |
| 1.1.9 | MANUAL_ENTRY | null (user selects HIGH) | HIGH | MANUAL |
| 1.1.10 | MANUAL_ENTRY | null (user selects LOW) | LOW | MANUAL |

### TC-1.2: Designation Override — Standard Sources

| # | Input Source | Designation | Is DM? | Expected Confidence |
|---|---|---|---|---|
| 1.2.1 | WEBSITE | "MD" | Yes | HIGH |
| 1.2.2 | WEBSITE | "Managing Director" | Yes | HIGH |
| 1.2.3 | GOOGLE_API | "CEO" | Yes | HIGH |
| 1.2.4 | LINKEDIN | "Owner" | Yes | HIGH |
| 1.2.5 | IMPORT_DEFAULT | "General Manager" | Yes | HIGH |
| 1.2.6 | WEBSITE | "Accounts Manager" | No | MEDIUM |
| 1.2.7 | WEBSITE | "Store Keeper" | No | MEDIUM |
| 1.2.8 | IMPORT_DEFAULT | "Purchase Manager" | Yes | HIGH |
| 1.2.9 | BUSINESS_CARD | "CEO" | Yes | HIGH (no change) |
| 1.2.10 | REFERENCE | null | N/A | HIGH (no change) |

### TC-1.3: Designation Override — IndiaMART Exception

| # | Input Source | Designation | Is DM? | Expected Confidence |
|---|---|---|---|---|
| 1.3.1 | INDIAMART | "MD" | Yes | MEDIUM (not HIGH) |
| 1.3.2 | INDIAMART | "CEO" | Yes | MEDIUM |
| 1.3.3 | INDIAMART | "Owner" | Yes | MEDIUM |
| 1.3.4 | INDIAMART | "Accounts Manager" | No | LOW |
| 1.3.5 | INDIAMART | null | N/A | LOW |

### TC-1.4: Never-Downgrade Rule

| # | Existing Confidence | Existing Mode | New Source | New Designation | Expected |
|---|---|---|---|---|---|
| 1.4.1 | HIGH | AUTO | INDIAMART | null | HIGH (not downgraded) |
| 1.4.2 | HIGH | AUTO | WEBSITE | null | HIGH |
| 1.4.3 | MEDIUM | AUTO | INDIAMART | null | MEDIUM |
| 1.4.4 | HIGH | MANUAL | WEBSITE | "MD" | HIGH (manual not touched) |
| 1.4.5 | LOW | AUTO | WEBSITE | "MD" | HIGH (upgraded) |
| 1.4.6 | MEDIUM | AUTO | BUSINESS_CARD | null | HIGH (upgraded) |

### TC-1.5: Designation Alias Resolution

| # | Input | Expected Match | Expected DM? |
|---|---|---|---|
| 1.5.1 | "M.D." | MD | Yes |
| 1.5.2 | "managing director" | MD | Yes |
| 1.5.3 | "Proprietor" | Owner | Yes |
| 1.5.4 | "GM" | General Manager | Yes |
| 1.5.5 | "Procurement Manager" | Purchase Manager | Yes |
| 1.5.6 | "Sales Executive" | (no match) | No |
| 1.5.7 | "Office Boy" | (no match) | No |
| 1.5.8 | "" (empty) | (no match) | No |
| 1.5.9 | null | (no match) | No |
| 1.5.10 | "  CEO  " (whitespace) | CEO | Yes |

---

## 2. Phone CRUD (Integration Tests)

### TC-2.1: Add Phone to Company

| # | Scenario | Expected |
|---|---|---|
| 2.1.1 | Add first phone as primary | Phone created, is_primary=true, company.primary_phone_normalized updated |
| 2.1.2 | Add second phone, not primary | Phone created, is_primary=false, primary unchanged |
| 2.1.3 | Add second phone as primary | New phone is_primary=true, old phone is_primary=false, primary_phone_normalized updated |
| 2.1.4 | Add phone with invalid number | Phone created, phone_type=INVALID, number_normalized=NULL |
| 2.1.5 | Add phone with landline number (044-XXXXXXX) | Normalized to 10 digits, phone_type=LANDLINE |
| 2.1.6 | Add phone with +91 prefix | Prefix stripped, normalized to 10 digits |
| 2.1.7 | Add phone linked to MD contact | Confidence auto-set to HIGH |
| 2.1.8 | Add phone, IndiaMART source, linked to MD | Confidence set to MEDIUM (exception) |

### TC-2.2: Update Phone

| # | Scenario | Expected |
|---|---|---|
| 2.2.1 | Change number_source from WEBSITE to BUSINESS_CARD | Confidence upgraded AUTO to HIGH |
| 2.2.2 | Change number_source from BUSINESS_CARD to WEBSITE | Confidence stays HIGH (never-downgrade) |
| 2.2.3 | Link a contact with designation "CEO" | Confidence upgraded to HIGH |
| 2.2.4 | Unlink decision-maker contact | Confidence stays at current level |
| 2.2.5 | Change is_primary on a VERIFIED company | Verification dropped |

### TC-2.3: Delete Phone

| # | Scenario | Expected |
|---|---|---|
| 2.3.1 | Delete non-primary phone | Phone removed, primary unchanged |
| 2.3.2 | Delete primary phone (others exist) | Primary auto-reassigned to best remaining |
| 2.3.3 | Delete last phone | Company has no phones, primary_phone_normalized set to NULL |
| 2.3.4 | Delete phone → audit log | Audit entry with PHONE_REMOVED action |

### TC-2.4: Confidence Override

| # | Scenario | Expected |
|---|---|---|
| 2.4.1 | Override MEDIUM to HIGH with reason | confidence=HIGH, mode=MANUAL, override_reason stored |
| 2.4.2 | Override HIGH to LOW with reason | confidence=LOW, mode=MANUAL |
| 2.4.3 | Override without reason | 422 error |
| 2.4.4 | Override with reason < 10 chars | 422 error |
| 2.4.5 | Override by VIEWER role | 403 error |
| 2.4.6 | Auto-recompute after manual override | Manual confidence NOT changed |

---

## 3. Company Form (Frontend / E2E Tests)

### TC-3.1: Create Company

| # | Scenario | Expected |
|---|---|---|
| 3.1.1 | Create with name only | Company created, no phones, pipeline_state=IMPORTED |
| 3.1.2 | Create with name + one phone | Company + phone created, phone is primary |
| 3.1.3 | Create with name + two phones, one primary | Both phones created, correct primary |
| 3.1.4 | Create with GST number (valid) | Company created with gst_number |
| 3.1.5 | Create with GST number (invalid format) | Validation error on GST field |
| 3.1.6 | Create with duplicate phone | Inline warning shown; user can proceed |
| 3.1.7 | Create with duplicate name+city | Inline warning shown |
| 3.1.8 | Add phone row, select Manual Entry, choose Low confidence | Phone created with LOW confidence, MANUAL mode |

### TC-3.2: Edit Company

| # | Scenario | Expected |
|---|---|---|
| 3.2.1 | Edit: add a phone number | New phone record created |
| 3.2.2 | Edit: remove a phone number | Phone deleted, primary recomputed if needed |
| 3.2.3 | Edit: change primary phone on Verified company | Verification drop warning shown, verification reset on save |
| 3.2.4 | Edit: change company name on Verified company | Verification drop warning shown |
| 3.2.5 | Edit: add GST number | GST saved, audit logged |
| 3.2.6 | Edit: change non-core field (tags) on Verified company | No verification warning |

### TC-3.3: Form Validation

| # | Scenario | Expected |
|---|---|---|
| 3.3.1 | Submit without company name | "Company name is required" error |
| 3.3.2 | Phone row with no number_source selected | "Number source is required" error |
| 3.3.3 | GST with valid format, invalid check digit | Warning: "Check digit invalid" |
| 3.3.4 | Two phone rows marked primary | Only last one stays primary |
| 3.3.5 | Email with invalid format | "Invalid email address" error |

---

## 4. Companies List (Frontend / E2E Tests)

### TC-4.1: Column Display

| # | Scenario | Expected |
|---|---|---|
| 4.1.1 | Company with HIGH confidence primary phone | Green "H" badge in Confidence column |
| 4.1.2 | Company with no phone | Empty Confidence column, empty Phone column |
| 4.1.3 | Company with MD phone | "⭐ MD" in Designation column |
| 4.1.4 | Company with 3 phones | Phone column: primary number + "+2" badge |
| 4.1.5 | Hover "+2" badge | Tooltip with all phone details |
| 4.1.6 | Company with no designation | Empty Designation column |

### TC-4.2: Filters

| # | Scenario | Expected |
|---|---|---|
| 4.2.1 | Filter by Confidence=HIGH | Only companies with HIGH primary phone confidence shown |
| 4.2.2 | Filter by Number Source=BUSINESS_CARD | Only companies with primary from Business Card |
| 4.2.3 | Filter by Has Decision-Maker=Yes | Only companies with DM designation on primary phone |
| 4.2.4 | Combine: Confidence=HIGH + State=READY | Intersection of both filters |
| 4.2.5 | Clear all filters | Full unfiltered list restored |

### TC-4.3: Filter Persistence

| # | Scenario | Expected |
|---|---|---|
| 4.3.1 | Apply filters → URL contains query params | URL matches filter state |
| 4.3.2 | Click company row → Back button | Previous filter state restored from URL |
| 4.3.3 | Ctrl+click company row | New tab opens, original tab filters intact |
| 4.3.4 | Copy URL with filters → open in new browser | Same filtered view loads |
| 4.3.5 | Navigate to /companies (no params) | Default state: no filters, page 1, sort created_at desc |
| 4.3.6 | Change page to 3 → Back from detail | Returns to page 3 |
| 4.3.7 | Sort by Confidence → Back from detail | Sort by Confidence preserved |

### TC-4.4: Search

| # | Scenario | Expected |
|---|---|---|
| 4.4.1 | Search by primary phone number | Company found |
| 4.4.2 | Search by alternate (non-primary) phone number | Company found (phone search includes all phones) |
| 4.4.3 | Search by phone with spaces/dashes (any format) | Normalized before search, company found |

---

## 5. Import (Integration Tests)

### TC-5.1: Single Phone Import

| # | Scenario | Expected |
|---|---|---|
| 5.1.1 | Import row with phone, no number_source column | Phone created with IMPORT_DEFAULT source, MEDIUM confidence |
| 5.1.2 | Import row with phone, IndiaMART batch | Phone created with INDIAMART source, LOW confidence |
| 5.1.3 | Import row with phone + number_source="Business Card" | Phone created with BUSINESS_CARD, HIGH confidence |

### TC-5.2: Multi-Phone Import

| # | Scenario | Expected |
|---|---|---|
| 5.2.1 | Row with Phone + Alt Phone columns | Two phone records created (one primary, one not) |
| 5.2.2 | Row with Phone + Owner Phone columns | Two records; owner phone has designation_override |
| 5.2.3 | Row with Phone + Alt Phone + Alt Phone 2 | Three phone records created |
| 5.2.4 | Row with Owner Phone + designation="MD" from IndiaMART | Owner phone confidence = MEDIUM (IndiaMART DM exception) |

### TC-5.3: Import Dedup with Multi-Phone

| # | Scenario | Expected |
|---|---|---|
| 5.3.1 | Primary phone matches existing company | Duplicate candidate created, match details include phone |
| 5.3.2 | Alt phone matches existing company | Duplicate candidate created for alt phone match |
| 5.3.3 | Primary matches Company A, Alt matches Company B | Conflict flagged in report, requires manual resolution |
| 5.3.4 | Same phone already on same company (re-import) | Never-downgrade; existing confidence retained |

### TC-5.4: GST Import

| # | Scenario | Expected |
|---|---|---|
| 5.4.1 | GST column mapped, valid GST | Stored on company record |
| 5.4.2 | GST column mapped, invalid format | Row validation error, GST not stored |
| 5.4.3 | GST column mapped, valid format, bad check digit | Row validation warning, GST stored with flag |
| 5.4.4 | GST matches existing company | Informational note in report (not a dedup block) |

### TC-5.5: Import Merge with Phones

| # | Scenario | Expected |
|---|---|---|
| 5.5.1 | Merge: incoming phone matches existing | Existing phone kept, confidence not downgraded |
| 5.5.2 | Merge: incoming has new phone | New phone added to survivor company |
| 5.5.3 | Merge: incoming primary vs existing primary | Existing primary retained unless analyst changes |

---

## 6. Database Migration (Verification)

### TC-6.1: Migration Execution

| # | Check | Expected |
|---|---|---|
| 6.1.1 | All migrations apply cleanly on empty database | No errors |
| 6.1.2 | All migrations apply cleanly on existing production-like data | No errors |
| 6.1.3 | Backfill: count of company_phones matches count of non-null primary_phone_normalized | Equal |
| 6.1.4 | Backfill: primary_phone_normalized unchanged on companies table | Existing values preserved |
| 6.1.5 | decision_maker_designations seeded with 12 rows | 12 active rows |
| 6.1.6 | GST constraint works: valid GST accepted | INSERT succeeds |
| 6.1.7 | GST constraint works: invalid GST rejected | CHECK violation |
| 6.1.8 | Unique primary index: two phones on same company both primary | INSERT fails |

---

## 7. Authorization (Security Tests)

| # | Role | Action | Expected |
|---|---|---|---|
| 7.1 | VIEWER | POST /api/v1/companies/{id}/phones | 403 |
| 7.2 | VIEWER | PATCH /api/v1/company-phones/{id} | 403 |
| 7.3 | VIEWER | DELETE /api/v1/company-phones/{id} | 403 |
| 7.4 | VIEWER | POST confidence override | 403 |
| 7.5 | ANALYST | POST /api/v1/companies/{id}/phones | 201 |
| 7.6 | ANALYST | POST confidence override | 200 |
| 7.7 | VIEWER | POST /api/v1/admin/decision-maker-designations | 403 |
| 7.8 | ADMIN | POST /api/v1/admin/decision-maker-designations | 201 |

---

## 8. Audit (Verification)

| # | Action | Expected Audit Entry |
|---|---|---|
| 8.1 | Add phone via form | PHONE_ADDED with phone details |
| 8.2 | Update phone source | PHONE_UPDATED with previous/new source |
| 8.3 | Delete phone | PHONE_REMOVED with phone details |
| 8.4 | Override confidence | CONFIDENCE_OVERRIDE with reason |
| 8.5 | Add GST number | GST_UPDATED with new value |
| 8.6 | Import creates phone | PHONE_ADDED with batch_id reference |
| 8.7 | Merge adds phone from incoming | PHONE_ADDED + merge reference |

---

## 9. Definition of Done

The v1.2 feature is complete when ALL of the following pass:

- [ ] Database migrations apply cleanly
- [ ] Backfill produces correct phone records from existing data
- [ ] Confidence Engine unit tests pass (all TC-1.x)
- [ ] Phone CRUD integration tests pass (all TC-2.x)
- [ ] Company form works end-to-end for create and edit (all TC-3.x)
- [ ] Companies list shows confidence, designation, phone count (all TC-4.1.x)
- [ ] Filters work including new confidence/source/DM filters (all TC-4.2.x)
- [ ] Filter persistence via URL works (all TC-4.3.x)
- [ ] Phone search works against all company phones (all TC-4.4.x)
- [ ] Import handles multi-phone, confidence, GST (all TC-5.x)
- [ ] Authorization enforced server-side (all TC-7.x)
- [ ] Audit entries recorded for all mutations (all TC-8.x)
- [ ] OpenAPI documentation updated
- [ ] README updated with new schema and endpoints
- [ ] No regressions in existing company CRUD, import, search, dedup
