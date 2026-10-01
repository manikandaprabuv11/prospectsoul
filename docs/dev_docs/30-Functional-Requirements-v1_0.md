<!--
Document: 30-Functional-Requirements-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Ready for Implementation
Scope: Mobile Number Confidence, Company Form, Companies List, Filter Persistence
Audience: Product, Engineering, QA, Claude Code
Depends on: 29-PRD-Change-Request-v1_2.md
-->
# ProspectSoul — Functional Requirements: Mobile Number Confidence & Company Enhancements

**Version:** 1.0
**Date:** 30 September 2026

---

## FR-1: Mobile Number Confidence

### FR-1.1 Confidence Levels

The system shall support three confidence levels for phone numbers: High, Medium, Low.

### FR-1.2 Auto-Assignment from Number Source

When a phone number is added to the system, the confidence level shall be automatically assigned based on the Number Source:

| Number Source | Default Confidence |
|---|---|
| BUSINESS_CARD | High |
| FIELD_VISIT | High |
| REFERENCE | High |
| MANUAL_ENTRY | User-selected (High, Medium, or Low) |
| WEBSITE | Medium |
| GOOGLE_API | Medium |
| LINKEDIN | Medium |
| INDIAMART | Low |
| IMPORT_DEFAULT | Medium |

### FR-1.3 Designation Override

If the phone number is associated with a contact whose designation matches a decision-maker role, the confidence shall be upgraded to High regardless of the Number Source. Decision-maker designations:

- MD / Managing Director
- CEO / Chief Executive Officer
- COO / Chief Operating Officer
- Owner / Proprietor
- Director
- Partner
- Founder
- Chairman
- General Manager
- Plant Manager
- Purchase Manager
- Production Manager

The decision-maker designation list shall be Admin-configurable (stored in a reference table, not hardcoded).

### FR-1.4 Designation Override Exception

IndiaMART numbers associated with a decision-maker designation shall be upgraded from Low to Medium, not to High. Rationale: IndiaMART data quality is inherently lower; designation information in IndiaMART exports is often inaccurate.

### FR-1.5 Never-Downgrade Rule

When the same normalized phone number arrives from a new source with a lower confidence level, the existing confidence shall not be downgraded. The new source record is retained for lineage, but the number's effective confidence remains at its highest historical level.

### FR-1.6 Manual Override

An Analyst or Admin may manually override the confidence level of any phone number. Manual overrides shall:
- Require a reason (free text, minimum 10 characters)
- Be recorded in the audit log
- Set `confidence_mode` to MANUAL
- Not be overwritten by subsequent auto-assignment

### FR-1.7 Confidence Display

- In the companies list: a colored badge (High=green, Medium=amber, Low=grey) on the primary number.
- In company detail: each phone number shows its confidence badge inline.
- In the company form (edit): each phone row shows the confidence badge. Manual Entry sources show a confidence dropdown.

---

## FR-2: Number Source

### FR-2.1 Number Source Enum

The system shall support the following Number Source values:

BUSINESS_CARD, FIELD_VISIT, REFERENCE, MANUAL_ENTRY, WEBSITE, GOOGLE_API, LINKEDIN, INDIAMART, IMPORT_DEFAULT.

### FR-2.2 Number Source is Required

Every phone number record must have a Number Source. On import, if no Number Source column is mapped, the system assigns IMPORT_DEFAULT (or the Import Source-specific default per CR-2 §2.2).

### FR-2.3 Number Source is Immutable

Once set, a phone number's source cannot be changed. If the same number arrives from a different source, a new source record is created (the number's confidence uses the highest).

### FR-2.4 Number Source Selection in Form

The Company Create and Edit forms shall display a Number Source dropdown for each phone number row. Options: Business Card, Field Visit, Reference, Manual Entry, Website, Google API, LinkedIn, IndiaMART.

---

## FR-3: Multi-Number Company Support

### FR-3.1 Multiple Phone Numbers

A company may have zero or more phone numbers. There is no hard limit, but the UI should be practical for up to 20 numbers.

### FR-3.2 Primary Number

Exactly one number may be marked as primary. If no number is explicitly marked, the system selects the best number using the ranking: highest confidence → linked to decision-maker contact → most recently added. The primary number is displayed in the companies list Phone column.

### FR-3.3 Phone Number Fields

Each phone number record shall store:

| Field | Required | Description |
|---|---|---|
| number_raw | Yes | The number as entered/imported |
| number_normalized | Yes (computed) | 10-digit normalized form |
| phone_type | Yes (computed) | MOBILE, LANDLINE, or INVALID |
| number_source | Yes | Number Source enum value |
| confidence | Yes (computed or selected) | HIGH, MEDIUM, LOW |
| confidence_mode | Yes (computed) | AUTO or MANUAL |
| is_primary | Yes (default false) | Whether this is the primary number |
| contact_id | No | Link to a contact record |
| designation_override | No | Free-text designation when no contact is linked |
| batch_id | No | Import batch that introduced this number |
| import_row_id | No | Specific import row |

### FR-3.4 Phone Repeater in Form

The Company Create and Edit forms shall include a phone number repeater section:
- "Add Phone Number" button adds a new row.
- Each row: phone number input, Number Source dropdown, Confidence badge/dropdown, Contact link (searchable dropdown of company contacts) or Designation text, Primary radio button, Remove button.
- At least one phone number row is shown by default (but not required to save).
- Removing the primary number auto-selects the next best number.

### FR-3.5 Dedup Against All Numbers

Duplicate detection rule ① ("same normalized phone") shall check incoming numbers against **all** phone numbers on all companies, not just primary numbers. The dedup match output shall include which specific number matched and on which company.

---

## FR-4: GST Number

### FR-4.1 GST Field

The company record shall include an optional `gst_number` field (15 characters, uppercase alphanumeric).

### FR-4.2 GST Validation

- Format validation: regex `^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$`
- Check digit validation: Luhn mod 36 algorithm on the first 14 characters
- Invalid GST numbers are rejected with a specific validation error, not silently accepted

### FR-4.3 GST in Form

Shown in the Identity section of the Create/Edit form. Editable field with format validation on blur.

### FR-4.4 GST in Import

Support import mapping for GST number. Column aliases: GST, GSTIN, GST Number, GST No, GST No., Tax ID, Tax Number.

### FR-4.5 GST API Lookup

**Deferred pending OD-1.** The field, validation, and manual entry are v1.2 scope. Automatic API lookup is deferred until the API source is decided.

---

## FR-5: Company Create and Edit Form

### FR-5.1 Unified Form

The Create Company and Edit Company pages shall use the same form component with the same fields in the same layout. The form renders differently based on mode:
- **Create:** all editable fields shown; metadata section hidden; pipeline_state defaults to IMPORTED.
- **Edit:** all editable fields shown with current values; metadata section shown as read-only; core-field edit warning displayed.

### FR-5.2 Form Sections

1. **Identity:** canonical_name* (text, required), website_domain (URL input, normalized on blur), industry (dropdown from reference list), cluster (dropdown), size_band (dropdown), gst_number (text, validated), tags (multi-select/free-form chip input)
2. **Location:** city (autocomplete from reference list), state (autocomplete from reference list)
3. **Communication:** email (email input, validated), Phone Number Repeater (per FR-3.4)
4. **Source Information (Create only, optional):** Number Source for each phone (per FR-2.4)
5. **Record Metadata (Edit only, read-only):** pipeline_state badge, verification_status with verified_by/at, import source and batch link, created_by/at, updated_by/at

### FR-5.3 Verification Warning on Edit

When an edit changes a core field on a Verified company, the form shall display a warning: "Editing [field name] will reset this company's verification status to Unverified." The user must confirm before saving.

Core fields for verification drop: canonical_name, website_domain, primary phone number, city, state, industry, cluster.

### FR-5.4 Duplicate Warning on Create

After the user enters a company name or phone number in the Create form, the system shall run an inline duplicate check (debounced, 500ms after last keystroke) and display any matches: "Possible duplicate: [Company Name] ([City]) — [match reason]". The user may proceed (creating a duplicate candidate) or navigate to the existing record.

### FR-5.5 Lineage on Save

Creating a company through the form creates a batch of one (Import Source = MANUAL_ENTRY). Every phone number added through the form records the user as created_by and the Number Source as selected.

---

## FR-6: Companies List Enhancements

### FR-6.1 Canonical Column Set

The companies list table shall display these columns by default:

Company Name, Phone, Confidence, Designation, Email, Website, City, State, Industry, Source, Pipeline State, Verification, Created At, Actions.

### FR-6.2 Phone Column

Displays the primary phone number (or best number per FR-3.2). If the company has additional numbers, show "+N more" as a hover tooltip listing the other numbers with their confidence badges.

### FR-6.3 Confidence Column

Displays the primary number's confidence level as a colored badge:
- **H** (green) — High
- **M** (amber) — Medium
- **L** (grey) — Low
- Empty if the company has no phone numbers.

### FR-6.4 Designation Column

Displays the designation of the contact linked to the primary phone number:
- If the primary number is linked to a contact: show the contact's designation (e.g., "MD", "CEO", "Purchase Mgr")
- If the primary number has a designation_override: show that
- Empty if neither is available

### FR-6.5 Filter Additions

Add to the existing filter set:
- **Confidence:** multi-select (High, Medium, Low)
- **Number Source:** multi-select (all Number Source values)
- **Has Decision-Maker Number:** boolean toggle

### FR-6.6 Sort Support

The Confidence column shall be sortable (High > Medium > Low).

---

## FR-7: Filter Persistence

### FR-7.1 URL-Based Filters

All filter state on the Companies list shall be encoded in URL query parameters:

| Parameter | Example |
|---|---|
| q | `q=coimbatore` |
| state | `state=READY,QUALIFICATION` |
| tier | `tier=A,B` |
| confidence | `confidence=HIGH,MEDIUM` |
| city | `city=Coimbatore` |
| verification | `verification=VERIFIED` |
| source | `source=INDIAMART` |
| number_source | `number_source=BUSINESS_CARD` |
| sort | `sort=created_at,desc` |
| page | `page=2` |
| size | `size=25` |

### FR-7.2 Browser Back Behavior

When a user clicks a company row (opening the detail page), then clicks the browser Back button, the companies list shall restore with all previously active filters, sort, and page position intact.

### FR-7.3 Open in New Tab

When a user Ctrl+clicks or right-click "Open in New Tab" on a company row, the companies list tab retains its filter state because the URL has not changed. The company detail opens in the new tab.

### FR-7.4 Shareable Filter URLs

A user may copy the companies list URL (including filter parameters) and share it. Another user opening that URL sees the same filtered view (subject to their role permissions).

### FR-7.5 Default State

When no query parameters are present, the companies list loads with no filters, default sort (created_at descending), page 1, size 25.
