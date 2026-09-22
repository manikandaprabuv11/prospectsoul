<!--
Document: 22-Sales-Intelligence-UI-UX-Addendum-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Proposed — pending sign-off
Audience: Developers / Claude Code
Companion to: UI/UX Specification v1.0
-->
# ProspectSoul — Sales Intelligence Extension (UI/UX Addendum)

**Version:** 1.0
Screens are named exactly. Build these, not generic CRUD pages.

---

## 1. Navigation Map Update

```
Sidebar (role-filtered):
  Dashboard
  Companies                  (search — universal entry)
     └ Map                   ← NEW
  Imports
  Triage Queue
  Research
  Ready Pool                 (Sales Lead+)
  Exports                    (Sales Lead+)
  Reports
  Settings                   (Admin only)
     ├ NIC Codes             ← NEW
     └ Contact Roles         ← NEW
```

Two new second-level links, no new top-level nav area.

---

## 2. Screen — NIC Master `/settings/nic-codes` (Admin)

**Two views**, toggled: **Table** (default, for search + bulk edit) and **Tree** (for hierarchy inspection).

### 2.1 Table view

```
┌ NIC Code Master ──────────────────────────────────────────────────────────┐
│ [ + Add code ]  [ ↑ Import Excel ]     Last import: 2,034 codes · 12 Sep │
├───────────────────────────────────────────────────────────────────────────┤
│ [ Search: pumps                             ]  Level [ any ▾ ]  ☐ Primary only │
│                                                                           │
│ Code   Description                              Level  Type  Parent  Pri  │
│ 28     Manufacture of machinery and equipment    Div    Mfg    —     ★    │
│ 281    Manufacture of general-purpose machinery  Grp    Mfg    28    ★    │
│ 2813   Other pumps, compressors, taps and valves Cls    Mfg    281   —    │
│ 28131  Manufacture of hand pumps                 Sub    Mfg    2813  —    │
│ 28132  Other pumps, compressors, taps and valves Sub    Mfg    2813  ★    │
│ ...                                                                       │
│                                                              [Edit] [•••] │
└───────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Tree view

Collapsible, indented, shows counts of children and companies-tagged.

### 2.3 Add / Edit dialog

```
┌ Edit NIC code ─────────────────────────────────┐
│ Code            [ 28132              ]         │
│ Description     [ ...                        ] │
│ Industry type   ( ) Service  (•) Manufacturing │
│ Parent          [ 2813 — Other pumps ▾  ]      │
│ Primary         [✓] surface first in filters   │
│ Active          [✓]                            │
│                              [Cancel] [Save]   │
└────────────────────────────────────────────────┘
```

Validation: `parent` must be a prefix of `code`; changing `parent` to a non-prefix returns 422 with the reason.

### 2.4 Import flow

Three steps, mirroring the existing Import Wizard:
1. Upload Excel → 2. Preview (rows read, created vs updated, unresolved parents count) → 3. Confirm.
Re-import is idempotent by `code`.

---

## 3. Screen — Contact Roles `/settings/contact-roles` (Admin)

Simple list with add / rename / activate / deactivate. Same shape as Disqualification Reasons in existing spec §2.13.

```
┌ Contact Roles ────────────────────────────────┐
│ [ + Add role ]                                │
│ Sort  Label            Active   Used by       │
│ 10    MD / Owner        ●       412 contacts  │
│ 20    Director          ●        88           │
│ 30    CEO               ●        14           │
│ 40    COO               ●         6           │
│ ...                                           │
│ 999   Other             ●     1,209           │
└───────────────────────────────────────────────┘
```

Deactivated roles disappear from new-contact dropdowns but still render on existing contacts.

---

## 4. Screen — Companies List, extended `/companies`

Existing screen. Two additions: **richer filters**, and a **view toggle** (Flat / Grouped by NIC).

### 4.1 Filter panel (extended)

```
[ Search: name / phone / email / domain / contact …    ] [Filters ▾] [Download]

┌──────────────────────────────────────────────────────────────────────────┐
│ NIC       [ 22 · Rubber and plastics ▾ ]  ☑ include all sub-codes        │
│ Region    [ South — TN ▾ ]      District [ Kanchipuram ▾ ]  Pincode […]  │
│ Turnover  [ 1Cr ] – [ 10Cr ]    Employees [ 10 ] – [ 100 ]               │
│ GST       ( ) Any (•) Present ( ) Missing                                │
│ Contact   [ ] Has MD contact  [ ] Has Purchase Head contact              │
│ existing: state · tier · cluster · city · verification · stale · tag · source │
└──────────────────────────────────────────────────────────────────────────┘

View: (•) Flat table   ( ) Grouped by NIC
```

### 4.2 Flat view (default)

```
Name              Primary NIC   Contact (Primary)         City         State  Tier  ⏱
ABC Pumps Pvt Ltd 28132 Pumps   R. Kumar (MD)             Coimbatore   TN     A     2mo
XYZ Plastics      2220 Plastics S. Raj (Purchase Head)    Kanchipuram  TN     B     new
...
```

Columns are user-toggleable. New default columns: `Primary NIC`, `Contact (Primary) with role`, `Region`, `Pincode`.

### 4.3 Grouped-by-NIC view

Only appears when a NIC filter is active. Companies grouped under their NIC node, lazy-loaded per group.

```
▾ 22   Manufacture of rubber and plastics products         47 companies
   ▾ 221  Manufacture of rubber products                    18
        ABC Rubber Industries          Coimbatore   TN   Tier A
        SRK Polymers                   Chennai      TN   Tier B
        ... [ show 16 more ]
   ▾ 222  Manufacture of plastics products                  29
        [ expand ]
   ▸ Directly tagged to 22 (no sub-code)                     0
```

Clicking `[ expand ]` on a group hits `GET /companies?nic_code_id=<uuid>` — lazy-loaded so the initial payload is just tree + counts.

### 4.4 Download button

Top-right of the list. Opens the download modal (§6).

---

## 5. Screen — Company Map `/companies/map`

Google Maps rendering. Owned + external data, clearly separated.

### 5.1 Layout

```
┌ Company Map ──────────────────────────────────────────────────────────┐
│ Pincode [ 641001 ]  →  Coimbatore, Tamil Nadu    Radius [ 5 km ▾ ]    │
├───────────────────────────────────────────────────────────────────────┤
│ TABS:  [ In ProspectSoul (12) ]  [ Found on Google (7) ]              │
│                                                                       │
│  ┌───────── GOOGLE MAP ─────────────────────────────────┐             │
│  │                                                      │             │
│  │        ● (owned, solid teal pin)                     │             │
│  │             ◌ (external, dashed amber pin)           │             │
│  │   ●                    ●         ◌                   │             │
│  │                                                      │             │
│  │        ◌         ●                                   │             │
│  │                              ● ●                     │             │
│  └──────────────────────────────────────────────────────┘             │
│                                                                       │
│  Below map — list matches active tab:                                 │
│                                                                       │
│  IN PROSPECTSOUL                                                      │
│  ┌ ABC Pumps Pvt Ltd — READY · Tier A                    [view →] ┐   │
│  ┌ XYZ Bearings — RESEARCH                                [view →] ┐   │
│  ...                                                                  │
│                                                                       │
│  FOUND ON GOOGLE — not yet in ProspectSoul                            │
│  ┌ Coimbatore Precision Castings                     [ + Add ]    ┐   │  ← dashed
│  ┌ SRK Foundry Works                                 [ + Add ]    ┐   │  ← dashed
│  Quota: 4,382 / 5,000 remaining today                                 │
└───────────────────────────────────────────────────────────────────────┘
```

### 5.2 Add flow

Clicking `+ Add` opens the existing manual-entry form (`/companies/new`) pre-filled from the Places payload:
- `canonical_name` ← `name`
- `address_line` ← `formatted_address`
- `primary_phone_normalized` ← `phone` (normalized)
- `pincode` ← from search context (Places doesn't always return one)
- `source` ← `GOOGLE_PLACES`

The analyst reviews, adjusts, and confirms. Dedup runs against existing companies in that pincode — a match surfaces the duplicate candidate flow rather than silently creating.

### 5.3 Visual rules

- Owned pins: solid teal, drop shadow, larger
- External pins: dashed amber outline, smaller, always with a "not in system" tooltip
- Never merged into one list styling
- Quota indicator persistent under the external tab

---

## 6. Screen — Download modal (invoked from Companies List)

```
┌ Download Companies ───────────────────────────────┐
│ Filter matches:  340 companies                    │
│                                                   │
│ Format          ( ) CSV        (•) Excel (.xlsx)  │
│                                                   │
│ Columns:                                          │
│   ☑ Name         ☑ Primary NIC   ☑ Region         │
│   ☑ Phone        ☑ Email         ☑ Website        │
│   ☑ City         ☑ State         ☑ Pincode        │
│   ☐ Turnover     ☐ Employees     ☐ GST            │
│   ☑ Primary contact (name + role)                 │
│   ☐ All contacts (one row per contact)            │
│                                                   │
│ ⓘ This download does NOT export the pipeline.     │
│   Companies stay in their current state.          │
│                                                   │
│                       [ Cancel ]  [ Download ]    │
└───────────────────────────────────────────────────┘
```

Explicit note that this is not a pipeline export — the term "Export" is reserved (see UX Rule 11).

---

## 7. Screen — Company Detail, extended `/companies/{id}`

Timeline remains the default tab. Two changes:

### 7.1 Contacts tab — multi-contact with roles

```
Contacts (3)                              [ + Add contact ]

┌ R. Kumar                           MD / Owner            ★ Primary ┐
│ +91 98430 12345 · rkumar@abcpumps.in                              │
│                                       [ Edit ] [ Re-link ] [ ••• ]│
└───────────────────────────────────────────────────────────────────┘
┌ S. Raj                             Purchase Head                  ┐
│ +91 98450 22334 · purchase@abcpumps.in                            │
│                                       [ Edit ] [ Make primary  ]  │
└───────────────────────────────────────────────────────────────────┘
┌ P. Kumar                           HR Head                        ┐
│ +91 98450 44556                                                   │
│                                       [ Edit ] [ Make primary  ]  │
└───────────────────────────────────────────────────────────────────┘
```

### 7.2 Overview tab — NIC codes section

```
NIC codes                                             [ + Attach code ]
  ★ 28132  Manufacture of other pumps, compressors      Primary
    28131  Manufacture of hand pumps                    [Make primary]
    27102  Manufacture of transformers                  [Make primary] [Detach]
```

Primary badge, star icon. `Make primary` swaps atomically.

---

## 8. New Shared Components

| Component | Used in | Behavior |
|---|---|---|
| `NicPicker` | Filters, Company edit, Import mapping | Tree-typeahead; primary-flagged codes shown first; level chip; "include descendants" toggle |
| `NicBadge` | Companies list, Company detail | Code + short description; full path on hover |
| `ContactRoleBadge` | Contacts list, Company list column | Compact role tag |
| `ContactRolePicker` | Contact add/edit | Sorted by `sort_order`; deactivated roles hidden for new; visible for existing |
| `PincodeSearchBox` | Map screen | Debounced; validates 6-digit before firing |
| `ExternalResultCard` | Map screen | Dashed border, distinct accent, always labeled "not yet in ProspectSoul" |
| `MapPin` | Map screen | Two variants: solid teal (owned) / dashed amber (external) |
| `GroupedNicList` | Companies List grouped view | Collapsible tree of company groups, lazy-loaded per group |
| `DownloadModal` | Companies List | Column selection, format, filter-count preview |

---

## 9. UX Rules (additions)

Continuing the numbered list from UI/UX Spec v1.0 §4:

7. **External results are always visually distinct from owned records.** Dashed border, different pin style, explicit "not yet in ProspectSoul" label. Never merged into the same list styling.

8. **A pincode search never writes anything by itself.** Only the explicit "+ Add" action creates a record, and it always opens the manual-entry review form — no one-click silent create.

9. **NIC is never a required field.** Any form exposing it must accept empty without validation error.

10. **Contact role is required on new contacts.** Existing contacts without a role display as "Other" and can be corrected inline.

11. **"Export" and "Download" are distinct words.** Export = pipeline handoff, state changes to `EXPORTED`, logged. Download = a filter-scoped CSV/XLSX, no state change. The words are not interchangeable in labels, tooltips, or error messages.

12. **Quota-exceeded on external search surfaces the problem+json `detail` verbatim**, with the reset window — consistent with UX Rule 3.

13. **The primary NIC and primary contact star (★) mean "default display"** — not "the only one" and not "the correct one." Users can promote any code or contact.

14. **The grouped-by-NIC view is opt-in.** Flat list stays the default because phone-number and free-text searches don't benefit from grouping.
