<!--
Document: 29-PRD-Change-Request-v1_2.md
Project: ProspectSoul
Version: 1.2 (Change Request against PRD v1.1)
Status: Pending Approval
Scope: Mobile Number Confidence, Multi-Number Support, Company Form, GST, Filter Persistence
Audience: Product, Engineering, QA, Claude Code
-->
# ProspectSoul — PRD Change Request v1.2

**Version:** 1.2
**Date:** 30 September 2026
**Status:** Pending Approval
**Amends:** PRD v1.1, Technical Design Spec v1.0, UI/UX Spec v1.0, Company Requirements v1.0
**Owner:** Product

---

## Changelog — v1.1 → v1.2

Seven additions and three modifications to existing sections. The six-stage pipeline, roles, ICP engine, and evidence model are unchanged. The contact master concept is extended with per-number metadata. One previously deferred item (GST lookup) is partially promoted.

### Added to v1:

1. **Mobile Number Confidence** — every phone number on a company record carries a confidence level (High / Medium / Low) derived from its source and the contact's designation. This replaces the single `primary_phone_normalized` field with a multi-number model. See Section CR-1.
2. **Number Source** — a new concept distinct from Import Source. Number Source records how a specific phone number was obtained (Business Card, Field Visit, Reference, Manual Entry, Website, Google API, LinkedIn, IndiaMART). See Section CR-2.
3. **Multi-Number Company Support** — a company can have multiple phone numbers, each with its own source, confidence, designation/contact link, and primary flag. See Section CR-3.
4. **GST Number** — a new optional field on the Company record. Fetched from an external API when available. See Section CR-4.
5. **Company Form Redesign** — Create and Edit Company use a unified form with sections for identity, location, communication (phone repeater), and metadata. See Section CR-5.
6. **Companies List Enhancements** — Number Confidence badge and Designation column added to the companies table. See Section CR-6.
7. **Filter Persistence** — companies table filters are preserved in URL query parameters so they survive tab navigation and browser back. See Section CR-7.

### Modified existing sections:

- **Section 3.2 (Contact Master):** contacts.phones[] is no longer a plain array; phone numbers are first-class entities with metadata.
- **Section 10 (Data Model):** `company_phones` table added; `companies.primary_phone_normalized` becomes a derived/denormalized field computed from the best phone record.
- **Section 12 (Out of Scope):** "Field-level verification and numeric contact confidence scores" is partially promoted — confidence is per-number, not per-field, and is a categorical level (High/Medium/Low), not a numeric score.

### Open Decisions (flagged, not assumed):

| # | Decision Needed | Impact |
|---|---|---|
| OD-1 | **GST API source**: Which API provides GST numbers? Options: MCA/GST government portal (rate-limited, requires registration), third-party service (Signzy, KYC-Hub), or extraction during AI research from company websites. | Determines integration scope, cost, and whether this is v1 or v1.1 |
| OD-2 | **TradeIndia confidence level**: Not specified. Should it be Low (matching IndiaMART) or Medium (matching generic imports)? | Affects confidence engine rules |
| OD-3 | **"Import" as a Number Source**: When a phone arrives via file import and the import row doesn't specify a Number Source column, should the default be Medium regardless of Import Source, or should each Import Source have its own default (IndiaMART→Low, LinkedIn→Medium, EXCEL_CSV→Medium, TRADEINDIA→?)? | Affects auto-assignment logic |

---

## CR-1: Mobile Number Confidence

### 1.1 Definition

Mobile Number Confidence is a categorical assessment of how trustworthy a phone number is, based on its provenance and the associated contact's role. Three levels:

| Level | Meaning | Visual |
|---|---|---|
| **High** | Number obtained through a direct, verified channel (in-person, reference, decision-maker) | Green badge |
| **Medium** | Number obtained through a credible but unverified channel (website, API, file import) | Yellow badge |
| **Low** | Number obtained through a low-trust channel or bulk source | Red/grey badge |

### 1.2 Confidence replaces Priority

The original requirement used "Priority" and "Confidence" interchangeably. This CR standardizes on **Confidence** to avoid collision with the existing Tier concept (A/B/C), which the Domain Model glossary explicitly says "never means: Priority of work."

### 1.3 Relationship to existing concepts

- **Record-level verification** (PRD §6) is unchanged. A company record is Verified/Unverified.
- **Contact confidence** (PRD §3.2: "verification status + source + last-verified date") is unchanged at the contact level.
- **Number Confidence** is a new, separate dimension. A Verified company can have Low-confidence numbers. A High-confidence number on an Unverified company does not make it Verified.

### 1.4 Previously deferred scope

PRD §12 deferred "field-level verification and numeric contact confidence scores." This CR partially promotes that deferral:
- **What is promoted:** per-number confidence as a categorical level (High/Medium/Low), auto-computed from source and designation.
- **What remains deferred:** field-level verification (each field individually verified), numeric confidence scores, and confidence on non-phone fields (email, website, address).

---

## CR-2: Number Source (Distinct from Import Source)

### 2.1 Two separate concepts

| Concept | Scope | Values | Where stored |
|---|---|---|---|
| **Import Source** | Per batch — how the data entered the system | EXCEL_CSV, INDIAMART, TRADEINDIA, LINKEDIN, MANUAL_ENTRY, API | `import_batches.source` |
| **Number Source** | Per phone number — how that specific number was obtained | BUSINESS_CARD, FIELD_VISIT, REFERENCE, MANUAL_ENTRY, WEBSITE, GOOGLE_API, LINKEDIN, INDIAMART, IMPORT_DEFAULT | `company_phones.number_source` |

### 2.2 Mapping from Import Source to default Number Source

When a phone number arrives through file import and the import row does not have a Number Source column mapped, the system assigns a default:

| Import Source | Default Number Source | Default Confidence |
|---|---|---|
| EXCEL_CSV | IMPORT_DEFAULT | Medium |
| INDIAMART | INDIAMART | Low |
| TRADEINDIA | IMPORT_DEFAULT | Medium *(pending OD-2)* |
| LINKEDIN | LINKEDIN | Medium |
| MANUAL_ENTRY | MANUAL_ENTRY | User-selected |
| API | Per API configuration | Per API configuration |

### 2.3 LinkedIn and IndiaMART

PRD §4 Stage 0 already lists INDIAMART and LINKEDIN as v1 Import Sources for file-based imports. The Number Source values LINKEDIN and INDIAMART apply to numbers arriving through those file imports. Direct API integration with either platform remains out of scope (PRD §12).

---

## CR-3: Multi-Number Company Support

### 3.1 Current state

`companies.primary_phone_normalized` holds one number. `contacts.phones[]` is a plain array. Neither carries metadata.

### 3.2 New model

Phone numbers become first-class entities in a `company_phones` table. Each record carries: the raw and normalized number, its source, confidence, type (mobile/landline/invalid), a link to the associated contact (nullable), a primary flag, and full lineage (batch, row, created_by/at).

`companies.primary_phone_normalized` is retained as a denormalized field, computed from the best phone record (highest confidence → verified contact → most recent). This preserves the existing dedup rule ("same normalized phone") without schema changes to dedup logic.

### 3.3 Existing dedup impact

Dedup rule ① "same normalized phone" must now check against **all** numbers on a company, not just the primary. The dedup match output should identify which specific number matched.

### 3.4 Contact integration

A phone number can optionally link to a contact (`contact_id`). When a number is linked:
- The contact's designation is displayed alongside the number.
- If the contact is an MD/Owner/CEO/COO or applicable Manager, the confidence is upgraded to High (designation override rule, CR-1).

When a number is not linked to a contact, a free-text `designation_override` field allows recording the designation without creating a full contact record (e.g., during quick manual entry).

---

## CR-4: GST Number

### 4.1 New field

`companies.gst_number` — a 15-character alphanumeric GST Identification Number (GSTIN), nullable. Format: 2 digits (state code) + 10 characters (PAN) + 1 digit (entity number) + 1 character (Z default) + 1 check digit.

### 4.2 Validation

Format validation on entry (regex: `^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$`). The check digit algorithm (Luhn mod 36) should be implemented for full validation.

### 4.3 Source

**Pending OD-1.** The API source for GST lookup is not yet decided. The field, validation, and UI are specified here; the integration is designed but implementation depends on OD-1 resolution.

### 4.4 Display

Shown in the Company Detail Overview tab and the Edit form. Not shown in the companies list table (too wide; available via column customization if needed later).

---

## CR-5: Company Form Redesign

### 5.1 Unified form

One form structure for both Create and Edit. Fields are grouped into sections:

1. **Identity:** canonical_name (required), website_domain, industry, cluster, size_band, gst_number, tags
2. **Location:** city, state (validated against reference list)
3. **Communication:** email, plus a phone number repeater (see CR-3). Each phone row: number, number_source (dropdown), confidence (auto-computed or user-selected for Manual Entry), contact link or designation, primary radio button.
4. **Metadata (Edit only, read-only):** pipeline_state, verification_status, verified_by/at, import source, batch reference, created/updated by/at.

### 5.2 Number Source dropdown options

Business Card, Field Visit, Reference, Manual Entry, Website, Google API, LinkedIn, IndiaMART.

### 5.3 Manual Entry confidence

When Number Source is Manual Entry, the Confidence dropdown becomes editable (High / Medium / Low). For all other sources, Confidence is auto-computed and displayed as a read-only badge.

---

## CR-6: Companies List Enhancements

### 6.1 New columns

| Column | Content |
|---|---|
| **Confidence** | Badge (H/M/L) showing the primary number's confidence level |
| **Designation** | Designation of the contact linked to the primary number (e.g., "MD", "CEO", "Purchase Manager"), if available |

### 6.2 Best-number display

The Phone column shows the primary number. If no primary is explicitly set, the system selects the best number: highest confidence → linked to a decision-maker → most recently added.

### 6.3 Column reconciliation

The Company Tech Spec §14 and UI/UX Spec §2.2 list different column sets. This CR establishes the canonical column set:

Company Name, Phone, Confidence, Designation, Email, Website, City, State, Industry, Source, Pipeline State, Verification, Created At, Actions.

Cluster is available as a filter but not a default visible column (analysts can toggle it).

---

## CR-7: Filter Persistence

### 7.1 Behavior

All filter state on the Companies list (search query, filter selections, sort column/direction, current page) is stored in URL query parameters. When a user opens a company in a new tab and returns to the companies list tab, their filters are preserved.

### 7.2 Implementation

URL-based (not sessionStorage). Filters are bookmarkable and shareable. The URL is the source of truth; the UI reads filter state from the URL on mount.

Example: `/companies?q=coimbatore&state=READY&tier=A&sort=created_at,desc&page=2`

---

## Amendments to Existing Sections

### PRD §3.2 Contact Master — Amendment

Add after "A contact has: name, designation, phone(s), email...":

> Phone numbers on contacts are linked through the `company_phones` table. A contact's phones[] array is replaced by the phone records that reference that contact_id. Adding a phone to a contact creates a company_phone record with the contact linkage.

### PRD §10 Data Model — Amendment

Add after `contacts`:

> **company_phones** — id, company_id, contact_id (nullable), number_raw, number_normalized, phone_type (MOBILE/LANDLINE/INVALID), number_source (enum), confidence (HIGH/MEDIUM/LOW), confidence_mode (AUTO/MANUAL), designation_override (nullable), is_primary (bool), batch_id (nullable), import_row_id (nullable), created_by, created_at, updated_by, updated_at

Add to `companies`:

> **gst_number** — nullable, 15-character GSTIN, format-validated on entry

### PRD §12 Out of Scope — Amendment

Move from deferred to partially promoted:

> ~~Field-level verification and numeric contact confidence scores~~ → **Partially promoted in v1.2:** per-number confidence as a categorical level (High/Medium/Low). Numeric confidence scores and confidence on non-phone fields remain deferred.

Add to deferred:

> - GST API integration source (OD-1 pending; field and validation in v1.2, API lookup deferred until source decided)
> - Phone number verification via SMS/OTP or third-party service
> - Automatic confidence upgrade based on successful call outcome
