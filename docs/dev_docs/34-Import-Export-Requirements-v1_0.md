<!--
Document: 34-Import-Export-Requirements-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Ready for Implementation
Scope: Import changes for multi-phone, confidence, GST; Export unchanged
Audience: Engineering, QA, Claude Code
Depends on: 29, 30, 31
-->
# ProspectSoul — Import and Export Requirements

**Version:** 1.0
**Date:** 30 September 2026

---

## 1. Import — New Mapping Targets

### 1.1 Extended Target Fields

The import mapping framework now supports these additional target fields:

| Target Field | Database Destination | Description |
|---|---|---|
| primary_phone_normalized | company_phones (is_primary=true) | Existing. Now creates a company_phone record |
| alternate_phone_1 | company_phones (is_primary=false) | Second phone number |
| alternate_phone_2 | company_phones (is_primary=false) | Third phone number |
| owner_phone | company_phones (is_primary=false, designation_override=derived) | Owner/MD phone — creates record with designation |
| number_source | company_phones.number_source | Per-row override of default number source |
| designation | Contact designation or phone designation_override | Role/title of the contact |
| gst_number | companies.gst_number | Company GST number |

### 1.2 New Column Aliases

| Target | Aliases |
|---|---|
| alternate_phone_1 | Alternate Phone, Alt Phone, Phone 2, Mobile 2, Second Phone, Second Mobile, Other Phone, Other Mobile, Alt Mobile |
| alternate_phone_2 | Phone 3, Mobile 3, Third Phone, Third Mobile |
| owner_phone | Owner Phone, Owner Mobile, MD Phone, MD Mobile, Director Phone, Director Mobile, Proprietor Phone, Managing Director Phone |
| number_source | Number Source, Phone Source, Mobile Source, Source of Number, Source of Phone, Source of Mobile |
| designation | Designation, Title, Position, Role, Contact Title, Person Title, Contact Designation, Person Designation |
| gst_number | GST, GSTIN, GST Number, GST No, GST No., Tax ID, Tax Number, GST Identification Number, GSTIN No |

---

## 2. Import — Multi-Phone Processing

### 2.1 Processing Order

When an import row maps to multiple phone columns, processing order is:
1. `primary_phone_normalized` → creates company_phone with is_primary=true
2. `owner_phone` → creates company_phone with designation_override from the designation column (or "Owner" if no designation mapped)
3. `alternate_phone_1` → creates company_phone
4. `alternate_phone_2` → creates company_phone

### 2.2 Normalization per Phone

Each phone value undergoes the same normalization pipeline:
- Strip country prefix (+91, 0091, leading 0)
- Remove spaces, hyphens, parentheses, dots
- Validate: exactly 10 digits
- Classify: MOBILE (starts 9/8/7/6), LANDLINE, INVALID
- Invalid values: retained in number_raw, number_normalized left NULL, phone_type set to INVALID

### 2.3 Dedup per Phone

Each normalized phone triggers a duplicate check against all existing company_phones. Dedup candidates identify which specific phone matched:

```json
{
  "match_rule": "SAME_PHONE",
  "incoming_phone": "9843012345",
  "matched_company_id": "...",
  "matched_phone_id": "...",
  "matched_phone_is_primary": true
}
```

### 2.4 Multiple Phones — Same Row, Same Company

All phones from a single import row belong to the same company record. If the primary phone dedup matches an existing company but an alternate phone matches a different company, this is flagged as a **conflict** in the import report (not silently resolved):

```
Row 42: Primary phone matches "ABC Pumps" but Alt Phone matches "XYZ Industries"
Resolution required: manual triage
```

---

## 3. Import — Confidence Assignment

### 3.1 Automatic Assignment from Import Source

When the import row does NOT have a number_source column mapped:

| Import Source (batch level) | Default Number Source | Default Confidence |
|---|---|---|
| EXCEL_CSV | IMPORT_DEFAULT | Medium |
| INDIAMART | INDIAMART | Low |
| TRADEINDIA | IMPORT_DEFAULT | Medium (pending OD-2) |
| LINKEDIN | LINKEDIN | Medium |
| MANUAL_ENTRY | MANUAL_ENTRY | Per user selection |

### 3.2 Per-Row Number Source Override

When the import row HAS a number_source column mapped, the row value takes precedence over the batch-level default. Valid values (case-insensitive, normalized):

| Row Value | Mapped To |
|---|---|
| Business Card, BusinessCard, BC | BUSINESS_CARD → High |
| Field Visit, FieldVisit, FV | FIELD_VISIT → High |
| Reference, Ref, Referral | REFERENCE → High |
| Website, Web, Site | WEBSITE → Medium |
| Google, Google API, GoogleAPI | GOOGLE_API → Medium |
| LinkedIn, LI | LINKEDIN → Medium |
| IndiaMART, Indiamart, IM | INDIAMART → Low |

Unrecognized values → logged as warning in row report, default to IMPORT_DEFAULT → Medium.

### 3.3 Designation-Based Upgrade

After all phones are created for a row, the ConfidenceEngine checks:
- If the row has a designation column → resolve against `decision_maker_designations`
- If decision-maker → upgrade confidence per FR-1.3 / FR-1.4 rules
- This applies to all phones created from that row (primary, alternates, owner)

### 3.4 Owner Phone Special Case

When a column maps to `owner_phone`:
- designation_override defaults to "Owner" (or the mapped designation column value)
- ConfidenceEngine immediately evaluates: "Owner" is a decision-maker → High confidence
- Exception: if batch Import Source is INDIAMART → Medium confidence

---

## 4. Import — GST Number Processing

### 4.1 Mapping

When a GST column is mapped, the value is:
1. Trimmed and uppercased
2. Format-validated (regex)
3. Check-digit validated (Luhn mod 36)
4. If valid → stored in companies.gst_number
5. If invalid → retained in the import row as raw data, flagged as validation error: "Invalid GST number format" or "GST check digit failed"

### 4.2 Dedup on GST

GST number is NOT a dedup field in v1.2. Two companies may have different GST numbers (legitimate — branch registrations). GST uniqueness is a warning, not a block:
- If an import row's GST matches an existing company's GST → log an informational note in the row report: "GST matches existing company [name]"
- This does not create a duplicate candidate

---

## 5. Import — Triage Merge with Multi-Phone

### 5.1 Merge Side-by-Side

The triage merge screen (UI/UX Spec §2.6) now shows phone numbers as a list:

```
EXISTING: ABC Pumps                    INCOMING: ABC Pump Industries
phones:                                phones:
  98430 12345 (Primary, High, MD)        98430 12345 (Primary, Medium)
  98440 67890 (Medium, Purchase)         98450 99999 (Medium)
```

### 5.2 Merge Rules for Phones

On MERGE:
- Phones that match by normalized number → keep the existing record; do NOT downgrade confidence
- Phones that do not match → add as new company_phone records on the survivor
- Primary designation: the existing primary remains primary unless the analyst explicitly changes it
- Never-downgrade rule applies: if the incoming phone has lower confidence, the existing confidence is retained

### 5.3 Merge Audit

The merge record's `field_decisions` JSON now includes a `phones` section:

```json
{
  "phones": {
    "kept": ["phone_id_1"],
    "added": ["phone_id_3"],
    "matched_not_changed": ["phone_id_2"]
  }
}
```

---

## 6. Export — No Changes

### 6.1 Existing Export Columns Preserved

The export CSV column layout is unchanged. The existing export template defines which columns are included. The current columns remain:

Company Name, Phone (primary), Email, Website, City, State, Industry, Cluster, Tags, Pipeline State, Verification Status.

### 6.2 Future Export Enhancements (Not v1.2)

When the export template system is enhanced in a future version, the following columns should be addable:
- All phone numbers (multi-value cell or separate columns)
- Primary phone confidence
- Primary phone designation
- GST Number
- Number Source

These are noted here for design reference but are NOT implemented in this scope. The existing export templates and CSV output remain unchanged.

---

## 7. Import Wizard — UI Changes

### 7.1 Step 2: Mapping

The mapping screen must:
- Show new target fields (alternate_phone_1, alternate_phone_2, owner_phone, number_source, designation, gst_number) in the target field dropdown
- Group phone-related targets under a "Phone Numbers" heading in the dropdown
- Show a tooltip explaining that multiple phone columns are supported

### 7.2 Step 4: Preview

The preview screen must:
- Show all mapped phone columns with their normalized values and auto-assigned confidence
- Show GST number with validation status (✓ or ⚠)
- Show number source assignment (either from row data or batch default)

### 7.3 Step 8: Report

The batch report must include:
- Per-row: phone count (primary + alternates), confidence levels assigned, any validation warnings
- Summary: total phones created, confidence distribution (High/Medium/Low counts), GST validation failures
