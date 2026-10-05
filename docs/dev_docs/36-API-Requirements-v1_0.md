<!--
Document: 36-API-Requirements-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Ready for Implementation
Scope: New and modified API endpoints for phone management, GST, confidence
Audience: Engineering, Claude Code
Depends on: 29, 30, 31
-->
# ProspectSoul — API Requirements

**Version:** 1.0
**Date:** 30 September 2026

---

## 1. New Endpoints

### 1.1 Company Phones

```
GET    /api/v1/companies/{companyId}/phones
POST   /api/v1/companies/{companyId}/phones
PATCH  /api/v1/company-phones/{phoneId}
DELETE /api/v1/company-phones/{phoneId}
POST   /api/v1/company-phones/{phoneId}/override-confidence
```

#### GET /api/v1/companies/{companyId}/phones

Returns all phone numbers for a company, ordered: primary first, then by confidence (HIGH→MEDIUM→LOW), then by created_at descending.

**Response:**
```json
[
  {
    "id": "uuid",
    "number_raw": "+91 98430 12345",
    "number_normalized": "9843012345",
    "phone_type": "MOBILE",
    "number_source": "BUSINESS_CARD",
    "confidence": "HIGH",
    "confidence_mode": "AUTO",
    "designation": "MD",
    "is_decision_maker": true,
    "is_primary": true,
    "contact_id": "uuid",
    "contact_name": "R. Kumar",
    "designation_override": null,
    "override_reason": null,
    "created_at": "2026-06-05T10:30:00Z",
    "created_by": "uuid"
  }
]
```

#### POST /api/v1/companies/{companyId}/phones

Adds a phone number to a company.

**Request:**
```json
{
  "number_raw": "98430 12345",
  "number_source": "BUSINESS_CARD",
  "confidence": null,
  "contact_id": "uuid",
  "designation_override": null,
  "is_primary": true
}
```

- `confidence`: only honored when `number_source` is `MANUAL_ENTRY`; otherwise auto-computed
- If `is_primary` is true, all other phones on this company have is_primary set to false
- Normalization and phone_type classification run automatically
- Dedup check runs against all company_phones
- If dedup match found → 409 response with match details

**Response:** 201 Created with the full CompanyPhoneResponse

**Errors:**
- 400: Invalid phone number format (after normalization attempt)
- 409: Duplicate phone number found — `{ "match": { "company_id": "...", "company_name": "...", "phone_id": "..." } }`
- 422: Business rule violation (e.g., Manual Entry without confidence selection)

#### PATCH /api/v1/company-phones/{phoneId}

Updates a phone record. Supports partial update.

**Request (any subset):**
```json
{
  "number_raw": "98430 12346",
  "number_source": "FIELD_VISIT",
  "contact_id": "uuid",
  "designation_override": "Purchase Manager",
  "is_primary": true
}
```

- Changing number_raw triggers re-normalization and re-dedup
- Changing number_source triggers confidence recomputation (never-downgrade applies)
- Changing contact_id triggers confidence recomputation (designation may change)
- Changing is_primary to true clears other primaries
- Cannot change confidence directly via PATCH (use override endpoint)

**Response:** 200 OK with updated CompanyPhoneResponse

#### DELETE /api/v1/company-phones/{phoneId}

Removes a phone record. If it was primary, the system auto-selects the next best phone and updates companies.primary_phone_normalized.

**Response:** 204 No Content
**Side effects:** Audit log entry, primary recomputation, companies.primary_phone_normalized update

#### POST /api/v1/company-phones/{phoneId}/override-confidence

Manually overrides the confidence level.

**Request:**
```json
{
  "confidence": "HIGH",
  "reason": "Confirmed via direct call to this number on 15 Sep 2026"
}
```

**Validation:**
- `reason` required, min 10 characters
- `confidence` must be HIGH, MEDIUM, or LOW
- Caller must have ANALYST, SALES_LEAD, or ADMIN role

**Response:** 200 OK with updated CompanyPhoneResponse (confidence_mode = MANUAL)

---

### 1.2 Decision-Maker Designations (Admin)

```
GET    /api/v1/admin/decision-maker-designations
POST   /api/v1/admin/decision-maker-designations
PATCH  /api/v1/admin/decision-maker-designations/{id}
DELETE /api/v1/admin/decision-maker-designations/{id}
```

Admin-only. CRUD for the designation reference list. Deactivating (not deleting) a designation does NOT retroactively change existing phone confidences (never-downgrade).

---

## 2. Modified Endpoints

### 2.1 POST /api/v1/companies (Create)

**Request — new fields:**
```json
{
  "canonical_name": "ABC Pumps Pvt Ltd",
  "website_domain": "abc-pumps.com",
  "city": "Coimbatore",
  "state": "Tamil Nadu",
  "industry": "Pumps",
  "cluster": "Pumps",
  "size_band": "SMALL",
  "email": "info@abc-pumps.com",
  "gst_number": "33AABCA1234A1ZA",
  "tags": ["LGB-group"],
  "phones": [
    {
      "number_raw": "98430 12345",
      "number_source": "BUSINESS_CARD",
      "contact_id": null,
      "designation_override": "MD",
      "is_primary": true
    },
    {
      "number_raw": "98440 67890",
      "number_source": "MANUAL_ENTRY",
      "confidence": "MEDIUM",
      "is_primary": false
    }
  ]
}
```

**Behavior:**
- Creates the company record
- Creates each phone record via CompanyPhoneService
- Sets companies.primary_phone_normalized from the best phone
- Creates a batch-of-one (Import Source = MANUAL_ENTRY)
- Runs dedup on each phone
- Returns the full CompanyDetailResponse including phones[]

### 2.2 PATCH /api/v1/companies/{id} (Update)

**Request — new fields:**
```json
{
  "gst_number": "33AABCA1234A1ZA",
  "phones": [
    {
      "id": "existing-phone-uuid",
      "number_raw": "98430 12345",
      "number_source": "BUSINESS_CARD",
      "is_primary": true
    },
    {
      "number_raw": "98450 99999",
      "number_source": "WEBSITE",
      "is_primary": false
    }
  ]
}
```

**Phone update semantics:**
- Phones with an `id` → update existing record
- Phones without an `id` → create new record
- Existing phones NOT in the list → removed (with audit)
- This is a full-replacement strategy for phones. Partial phone updates use the phone-specific endpoints.

**Verification drop:** If any phone with is_primary=true changes its number_raw or is_primary flag, and the company is VERIFIED → verification is dropped (same as other core-field edits).

### 2.3 GET /api/v1/companies/{id} (Detail)

**Response — new fields:**
```json
{
  "id": "...",
  "canonical_name": "ABC Pumps Pvt Ltd",
  "gst_number": "33AABCA1234A1ZA",
  "primary_phone_normalized": "9843012345",
  "primary_phone_confidence": "HIGH",
  "primary_phone_designation": "MD",
  "phones": [
    { "...full CompanyPhoneResponse..." }
  ],
  "...existing fields..."
}
```

### 2.4 GET /api/v1/companies (List)

**New query parameters:**

| Parameter | Type | Description |
|---|---|---|
| confidence | string (CSV) | Filter by primary phone confidence: HIGH,MEDIUM,LOW |
| number_source | string (CSV) | Filter by primary phone number_source |
| has_decision_maker | boolean | true = primary phone linked to decision-maker |
| gst | string | Search by GST number (prefix match) |

**Response — new fields per item:**

```json
{
  "primary_phone_confidence": "HIGH",
  "primary_phone_designation": "MD",
  "additional_phone_count": 2,
  "gst_number": "33AABCA1234A1ZA"
}
```

### 2.5 POST /api/v1/imports (Upload)

No change to the endpoint signature. The import processor internally handles:
- New mapping targets (alternate phones, designation, number_source, gst_number)
- Phone record creation via CompanyPhoneService
- Confidence auto-assignment via ConfidenceEngine

---

## 3. Validation Rules (all phone endpoints)

| Rule | Error Code | Message |
|---|---|---|
| Phone normalization fails (not 10 digits after stripping) | 400 | "Phone number must be 10 digits after normalization" |
| number_source missing | 400 | "Number source is required" |
| Manual Entry without confidence | 422 | "Confidence level required for Manual Entry source" |
| Override without reason | 422 | "Override reason is required (minimum 10 characters)" |
| GST format invalid | 400 | "GST number format invalid: expected 15 characters matching pattern XX XXXXX XXXX X XZ X" |
| GST check digit invalid | 400 | "GST check digit validation failed" |
| Duplicate phone (same company) | 409 | "This phone number already exists on this company" |
| Duplicate phone (different company) | 409 | "This phone number exists on company [name] — duplicate candidate created" |

---

## 4. Audit Log Entries

New action types for audit_log:

| action | entity_type | Trigger |
|---|---|---|
| PHONE_ADDED | COMPANY_PHONE | Phone created |
| PHONE_UPDATED | COMPANY_PHONE | Phone record changed |
| PHONE_REMOVED | COMPANY_PHONE | Phone deleted |
| PHONE_PRIMARY_CHANGED | COMPANY_PHONE | Primary flag reassigned |
| CONFIDENCE_OVERRIDE | COMPANY_PHONE | Manual confidence override |
| CONFIDENCE_RECOMPUTED | COMPANY_PHONE | Auto-recomputation changed the level |
| GST_UPDATED | COMPANY | GST number added or changed |

---

## 5. Authorization Matrix

| Endpoint | ANALYST | SALES_LEAD | ADMIN | VIEWER | COO |
|---|---|---|---|---|---|
| GET phones | ✓ | ✓ | ✓ | ✓ | ✓ |
| POST phone | ✓ | ✓ | ✓ | — | — |
| PATCH phone | ✓ | ✓ | ✓ | — | — |
| DELETE phone | ✓ | ✓ | ✓ | — | — |
| Override confidence | ✓ | ✓ | ✓ | — | — |
| Admin designations | — | — | ✓ | — | — |
| Create/edit company (with phones) | ✓ | ✓ | ✓ | — | — |
