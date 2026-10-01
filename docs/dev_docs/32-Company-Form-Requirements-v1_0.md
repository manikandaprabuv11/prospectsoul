<!--
Document: 32-Company-Form-Requirements-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Ready for Implementation
Scope: Create and Edit Company Form
Audience: Engineering, QA, Claude Code
Depends on: 29, 30, 31
-->
# ProspectSoul — Create / Edit Company Form Requirements

**Version:** 1.0
**Date:** 30 September 2026

---

## 1. Form Structure

One React component (`CompanyForm`) used for both Create (`/companies/new`) and Edit (`/companies/{id}/edit`) modes. Mode is determined by the presence of an existing company ID.

### 1.1 Section Layout

```
┌─────────────────────────────────────────────────────────────────┐
│ Create Company  /  Edit: ABC Pumps Pvt Ltd                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  IDENTITY                                                       │
│  ┌──────────────────────────┐  ┌──────────────────────────┐    │
│  │ Company Name *           │  │ Website                  │    │
│  └──────────────────────────┘  └──────────────────────────┘    │
│  ┌──────────────────────────┐  ┌──────────────────────────┐    │
│  │ Industry        ▾       │  │ Cluster          ▾       │    │
│  └──────────────────────────┘  └──────────────────────────┘    │
│  ┌──────────────────────────┐  ┌──────────────────────────┐    │
│  │ Size Band        ▾      │  │ GST Number               │    │
│  └──────────────────────────┘  └──────────────────────────┘    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Tags  [tag1] [tag2] [+ Add]                              │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  LOCATION                                                       │
│  ┌──────────────────────────┐  ┌──────────────────────────┐    │
│  │ City (autocomplete)      │  │ State (autocomplete)     │    │
│  └──────────────────────────┘  └──────────────────────────┘    │
│                                                                 │
│  COMMUNICATION                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Email                                                    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  PHONE NUMBERS                                                  │
│  ┌────────────────────────────────────────────────────────────┐│
│  │ (•) +91 98430XXXXX  Source: [Business Card ▾]  [H]        ││
│  │     Contact: R. Kumar (MD)                       [🗑]     ││
│  ├────────────────────────────────────────────────────────────┤│
│  │ ( ) +91 98440XXXXX  Source: [IndiaMART ▾]        [L]      ││
│  │     Designation: Purchase Manager                 [🗑]     ││
│  ├────────────────────────────────────────────────────────────┤│
│  │                    [+ Add Phone Number]                    ││
│  └────────────────────────────────────────────────────────────┘│
│                                                                 │
│  RECORD INFO (Edit only, read-only)                             │
│  Pipeline: [READY]  Verified: ✓ by Priya, 12 Jun 2026         │
│  Source: INDIAMART  Batch: #12                                 │
│  Created: 05 Jun 2026 by Import  Updated: 18 Jun 2026         │
│                                                                 │
│  ┌──────────┐  ┌──────────┐                                    │
│  │  Cancel   │  │  Save    │                                    │
│  └──────────┘  └──────────┘                                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Field Specifications

### 2.1 Identity Section

| Field | Type | Required | Validation | Notes |
|---|---|---|---|---|
| canonical_name | Text input | Yes | Min 2 chars, max 255 | Trimmed. Duplicate check triggered on blur (debounced 500ms) |
| website_domain | URL input | No | Valid domain after normalization | Normalized to registered domain on blur. Duplicate check on normalized value |
| industry | Dropdown | No | From reference list | Searchable dropdown |
| cluster | Dropdown | No | From reference list | Searchable dropdown |
| size_band | Dropdown | No | From enum (MICRO, SMALL, MEDIUM, LARGE) | |
| gst_number | Text input | No | GSTIN format regex + Luhn mod 36 check digit | Auto-uppercase. Validated on blur with specific error message |
| tags | Chip input | No | Free-form text, Admin-curated suggestions | Multi-select with typeahead from existing tags |

### 2.2 Location Section

| Field | Type | Required | Validation | Notes |
|---|---|---|---|---|
| city | Autocomplete | No | Matched against reference list; unmatched accepted with flag | Shows reference list suggestions. Unmatched values accepted but shown with ⚠ |
| state | Autocomplete | No | Matched against reference list; unmatched accepted with flag | Same as city |

### 2.3 Communication Section

| Field | Type | Required | Validation | Notes |
|---|---|---|---|---|
| email | Email input | No | RFC 5322 basic validation | |

### 2.4 Phone Number Repeater

Each phone row contains:

| Element | Type | Behavior |
|---|---|---|
| Primary radio | Radio button | One per company. Selecting auto-deselects others |
| Phone number | Text input, required per row | Accepts any format. Normalized on blur. Shows phone_type badge (Mobile/Landline/Invalid) after normalization. Duplicate check on normalized value |
| Number Source | Dropdown, required per row | Options: Business Card, Field Visit, Reference, Manual Entry, Website, Google API, LinkedIn, IndiaMART |
| Confidence | Badge or Dropdown | Read-only badge when source ≠ Manual Entry. Editable dropdown (High/Medium/Low) when source = Manual Entry |
| Contact link | Searchable dropdown | Shows existing contacts for this company. "Create new contact" option at bottom. Optional |
| Designation override | Text input | Shown only when no contact is linked. Free text. Optional |
| Remove button | Icon button | Removes the row. Confirm if it's the only number. Removing primary auto-selects next best |

### 2.5 Record Metadata Section (Edit Only)

All fields read-only:

| Field | Display |
|---|---|
| pipeline_state | StateBadge component |
| verification_status | VerifiedBadge with verified_by and verified_at |
| source | Import Source enum label |
| batch | Link to batch report (`/imports/{batch_id}`) |
| created_by / created_at | User name + relative time |
| updated_by / updated_at | User name + relative time |

---

## 3. Behavioral Rules

### 3.1 Duplicate Check on Create

Triggers:
- After company name blur (debounced 500ms): check normalized name + city
- After phone number blur (normalized): check against all company_phones
- After website blur (normalized): check against companies.website_domain

Display: inline warning below the triggering field. Example:
```
⚠ Possible duplicate: "ABC Pumps Pvt Ltd" (Coimbatore) — same normalized phone
  [View existing record →]  [Proceed anyway]
```

"Proceed anyway" allows saving — the import framework creates a duplicate candidate as usual.

### 3.2 Verification Drop Warning on Edit

When the user modifies a core field (canonical_name, website_domain, city, state, industry, cluster, or changes/adds/removes the primary phone number):

```
⚠ This company is currently Verified. Saving these changes will reset 
  verification to Unverified. Continue?
  [Cancel]  [Save and Reset Verification]
```

### 3.3 Confidence Auto-Computation

When the user selects a Number Source for a phone row:
1. If source = Manual Entry → enable Confidence dropdown (default Medium)
2. If source = anything else → compute confidence per FR-1.2 rules, display as read-only badge
3. If the user then links a decision-maker contact → upgrade confidence to High (or Medium for IndiaMART)

### 3.4 Phone Type Auto-Detection

After phone number normalization (blur):
- 10 digits starting with 9/8/7/6 → show "Mobile" badge
- 10 digits starting with other → show "Landline" badge
- Failed normalization → show "Invalid" badge with the raw value retained

### 3.5 GST Validation Feedback

On blur of GST Number field:
- Valid format + valid check digit → green checkmark
- Valid format, invalid check digit → "⚠ Check digit invalid — verify the number"
- Invalid format → "GST number must be 15 characters: 2 digits + 5 letters + 4 digits + 1 letter + 1 alphanumeric + Z + 1 alphanumeric"

### 3.6 Save Behavior

**Create:**
1. Client-side validation
2. POST /api/v1/companies with full payload including phones[] and gst_number
3. On success → redirect to `/companies/{newId}` (company detail)
4. On 400 → map field errors to form fields (including per-phone-row errors)
5. On 409 (duplicate) → show duplicate candidate inline

**Edit:**
1. Client-side validation
2. PATCH /api/v1/companies/{id} with changed fields
3. On success → redirect to `/companies/{id}` with success toast
4. On 400 → map field errors
5. On 409 (concurrent edit) → "This record was modified by another user. Refresh and retry."

---

## 4. Role Behavior

| Role | Create | Edit | Phone Numbers |
|---|---|---|---|
| ANALYST | ✓ | ✓ | Add, edit, remove, set primary, select source/confidence |
| SALES_LEAD | ✓ | ✓ | Same as Analyst |
| ADMIN | ✓ | ✓ | Same + can override confidence on any source |
| VIEWER | — (no access to form) | Read-only view of existing data | — |
| COO | — | Read-only | — |

---

## 5. Accessibility & UX

- Tab order: top-to-bottom, left-to-right within sections, then through phone rows
- All required fields marked with asterisk and `aria-required`
- Validation errors announced via `aria-live` region
- Phone repeater: keyboard shortcut to add row (Alt+P), remove row (Delete when focused on empty row)
- Autosave: No. Explicit save only. Unsaved changes trigger browser beforeunload warning.
