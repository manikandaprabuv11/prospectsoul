<!--
Document: 20-Sales-Intelligence-Domain-Model-Addendum-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Proposed — pending sign-off
Audience: Everyone — product, sales, engineering, Claude Code
Companion to: Domain Model v1.0, Sales Intelligence PRD Addendum v1.0
-->
# ProspectSoul — Sales Intelligence Extension (Domain Model Addendum)

**Version:** 1.0
Business concepts only. No SQL, no APIs.

---

## 1. Extended Entity Map

```
                        ┌─────────────────────┐
                        │      COMPANY        │  ← unchanged anchor
                        │  (pipeline state)   │
                        └─────────┬───────────┘
     ┌──────────┬──────────┬─────┴──────┬──────────┬──────────┐
     ▼          ▼          ▼            ▼          ▼          ▼
  CONTACTS   ACTIVITIES  QUALIFI-      NIC CODES  EXPORTS   ALIASES
  (multi,    (research,  CATIONS       (many,     (what
   role-      calls,     (versioned)   primary    left,
   tagged)    notes)                    flag)     when)
    │            │           │
    ▼            ▼           │
  CONTACT   ATTACHMENTS      │
  ROLES        │             │
  (MD/HR/      ▼             │
   Purchase..) EVIDENCE ─────┘


NIC MASTER (reference data, hierarchical, admin-editable)
  Section → Division → Group → Class → Sub-class
  Each node carries a "primary" flag surfacing it in filter pickers.
```

**Two new relationships**, and one existing one extended:

- `COMPANY 1 : N CONTACT` — **extended** to explicit multi with role tags
- `COMPANY N : N NIC_CODE` — **new**, via `company_nic_codes`
- `NIC_CODE 1 : N NIC_CODE` — **new**, self-referencing tree via `parent_id`

---

## 2. New Entity — NIC Code (Hierarchical Reference)

```
1  Crop and animal production .......................  Section    (1 digit)
 └─ 14  Animal production ............................  Division   (2 digits)
     └─ 141  Raising of cattle and buffaloes .........  Group      (3 digits)
         └─ 1411  Raising and breeding of cattle .....  Class      (4 digits)
             └─ 14111  (example sub-class) ...........  Sub-class  (5 digits)
```

- **Level** is derived from code length (1–5 digits)
- **Parent** is resolved and stored once at import time — not derived at query time
- `industry_type` (Service / Manufacturing) is a flat attribute on every row
- Any node can be marked `is_primary` (independently at any level) — surfaces first in filter pickers
- A company can be classified at **any level**, and at **any number** of nodes

---

## 3. Extended Entity — Contact

### 3.1 What changes

| Before | After |
|---|---|
| One primary contact per company, `is_primary` boolean, `is_md_owner` boolean, `designation` as free text | Zero-to-many contacts per company. `is_primary` remains as the default-display marker. A first-class `role` tag replaces free-text designation as the filter/report dimension. `is_md_owner` retained for backward compatibility. |

### 3.2 New concept — Contact Role

A configured, extensible list of tags that describe **what a contact is at that company**:

`MD / Owner · Director · CEO · COO · CFO · HR Head · Purchase Head · Sales Head · Plant Head · Accounts · Admin · IT · Other`

Rules:

- A company can have **many contacts sharing a role** (two purchase managers is legal)
- A company can have **many roles on one contact** — no, actually one role per contact (a person is one thing to that company; if they also handle IT, add a second contact linked by name)
- Role is **required** on new contacts; existing rows migrate as `MD / Owner` (if `is_md_owner`) or `Other`
- The role list is **Admin-editable** — add / rename / deactivate, never hard-delete

### 3.3 What stays

- `association_start` / `association_end` — the seed of the future person-entity model, unchanged
- `verification_status` — unchanged
- Merging a company preserves all contacts on the survivor with roles intact

---

## 4. New Entity — Company ↔ NIC (join)

A single company can carry many NIC codes. The join table:

- **Preserves the raw code and description as supplied** — so an unmatched code is never lost
- Links to the master `nic_codes` when a match exists, `NULL` otherwise
- Marks exactly one row per company as `is_primary` — denormalized to `companies.primary_nic_code_id` for fast list rendering
- Retains `sequence_no` from the source array — so re-imports produce stable ordering

---

## 5. Extended Company Fields

Twelve new nullable columns. None changes existing meaning. See PRD Addendum §2.2 for the full list.

Two of them warrant explanation as concepts, not just fields:

- **`primary_nic_code_id`** — a denormalization of one row in `company_nic_codes`. Two sources of truth for the same fact, kept consistent by a service-level rule: setting/changing primary always updates both. The reason we accept this: the Companies List renders 25 rows with the primary NIC visible in <200ms; joining on a 5-row-per-company join table for every list query would fail that budget.
- **`source_reference`** — an external identity from the originating source (Udyam number, IndiaMART ID, TradeIndia listing, etc.). New dedup rule ⓪ matches on `source + source_reference` before any other rule. This is what stops registry re-imports from generating hundreds of thousands of false candidates.

---

## 6. New Business Invariants

These extend the numbered invariants in Domain Model v1.0 §5:

10. **NIC classification never blocks company creation.** Present → linked or raw-only. Absent → no rows in `company_nic_codes`. No validation error either way.
11. **NIC codes are reference data, not per-record free text.** Analysts pick from the master; they don't type into a classification field.
12. **Every company has zero or one primary NIC.** Not more. Setting a new primary demotes the previous one atomically.
13. **Every company has zero or one primary contact.** Same atomic rule.
14. **Contact role is a controlled vocabulary.** New roles are added via Admin; nothing else creates a role value.
15. **External location results are transient.** No `id`, no persistence, until a human explicitly adds one via the manual-entry flow.
16. **Region ≠ State ≠ Cluster ≠ District.** Four distinct facets:
    - `state` is geography (top-level administrative)
    - `district` is geography (sub-state administrative)
    - `cluster` is an industry vertical grouping
    - `region` is a sales-territory construct
17. **Companies List download does not change pipeline state.** Only pipeline `Export` (Stage 5) moves companies to `EXPORTED`.

---

## 7. Vocabulary Additions

| Term | Means | Never means |
|---|---|---|
| NIC Code | A node in the government industrial classification tree | An ICP criterion, a tier, or a cluster |
| NIC Filter | A taxonomy filter on the Companies List | ICP Qualification |
| Primary NIC | The default NIC code for a company, shown on lists and used for grouped display | The only NIC — a company can have many |
| Contact Role | A first-class tag describing what a contact is at the company | `designation` — that stays as free text |
| Primary Contact | The default-display contact for a company | The only contact — a company can have many |
| External Result | A live business record from an outside source, not yet in ProspectSoul | A Company record |
| Region | A sales-territory grouping | A state, district, or industry cluster |
| Download | A filter-scoped CSV/Excel pulled from the Companies List — no state change | Pipeline Export |
| Export | The pipeline Stage 5 handoff — logged, state → `EXPORTED` | A casual download |

---

## 8. Flow Additions

### 8.1 How a multi-NIC company is created from a registry import

```
Import row (Kanchipuram Excel)
   ↓
Parse Activities JSON → [{code: 47522, desc: …}, {code: 47594, desc: …}]
   ↓
Create/match Company via extended dedup (source_ref → name+pincode → …)
   ↓
For each activity in the array:
   - Look up nic_codes by code (match → nic_code_id, no match → NULL)
   - Insert company_nic_codes row with raw + resolved + sequence_no
   ↓
First activity marked is_primary → primary_nic_code_id denormalized
   ↓
Row lands on the Company Timeline as one IMPORTED event with all NIC codes visible
```

### 8.2 How a hierarchical filter resolves

```
User clicks "22 - Rubber and plastics" in NicPicker with "include descendants" on
   ↓
Backend recursive CTE walks nic_codes.parent_id downward from 22
   ↓
Set of NIC ids: {22, 221, 2211, 22111, …, 222, 2220, 22201, …}
   ↓
GET /companies filtered by company_nic_codes.nic_code_id IN (set)
   ↓
Grouped view: server also returns per-node counts for the tree
```
