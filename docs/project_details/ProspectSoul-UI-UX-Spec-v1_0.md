# ProspectSoul — UI/UX Specification
**Version:** 1.0 · **Date:** July 2026 · **Companion to:** PRD v1.1, Technical Design Spec v1.0
**Audience:** Developers / Claude Code. Screens are named exactly; build these, not generic CRUD pages.

**Design principles:** data-dense internal tool, keyboard-friendly queues, evidence always one click away, timeline is the heart. Desktop-first (analysts work on laptops); responsive is nice-to-have, not v1.

---

## 1. Navigation Map

```
Sidebar (role-filtered):
  Dashboard
  Companies            (search — the universal entry point)
  Imports              → Import Wizard → Batch Report
  Triage Queue         (badge: pending count)
  Research
     ├ Research Queue  (badge: queued/processing)
     └ Review Queue    (badge: awaiting review)
  Ready Pool           (Sales Lead+)
  Exports              (Sales Lead+)
  Reports
  Settings             (Admin only)

Global header: search box (jumps to Companies with q=), user menu (Keycloak logout)
Deep link everywhere: /companies/{id} is shareable and is THE company URL.
```

---

## 2. Screens

### 2.1 Dashboard  `/`
Role-aware landing page.
- **Analyst:** "My work" — Triage pending, Review Queue depth, companies in RESEARCH, recent batches with status.
- **Sales Lead:** READY pool by tier (A/B/C cards), recent exports, override rate.
- **COO/Viewer:** pipeline funnel (state counts), source-funnel mini chart, quality tiles (verified %, stale count).
Each tile clicks through to the underlying filtered screen.

### 2.2 Companies (Search)  `/companies`
The 10-second "have we seen this company?" screen.
```
[ Search: name / phone / email / domain / contact …          ] [Filters ▾]
Filters: state · tier · cluster · city · verification · stale · tag · source
┌──────────────────────────────────────────────────────────────────────┐
│ Name            State        Tier  Cluster   City       Verified  ⏱ │
│ ABC Pumps       READY        A     Pumps     Coimbatore  ✓   2mo ago │
│ ABC Pump Ind.   DISQUALIFIED —     Pumps     Coimbatore  ✗   1y ago │ ← greyed, still visible
└──────────────────────────────────────────────────────────────────────┘
```
- Search hits activity/note text too (result shows matching snippet).
- Row click → Company Detail. DISQUALIFIED/ARCHIVED rows visually muted, never hidden by default.

### 2.3 Company Detail  `/companies/{id}` — **Timeline is the default tab**
```
┌ ABC Pumps Pvt Ltd  [READY] [Tier A · ICP v3] [Verified ✓ by Priya, 12 Jun] [Stale?]
│ abc-pumps.com · 98430xxxxx · Coimbatore · Pumps · tags: [LGB-group]
│ Actions: Verify · Add Activity · Send to Research · Qualify · Disqualify ▾
├─ Tabs: ● Timeline | Overview | Contacts | Evidence | Qualifications
│
│ TIMELINE (newest first)
│  ⬤ 18 Jun 2026  EXPORTED — batch "June Tier-A push" → CRM (by Kumar)
│  ⬤ 12 Jun 2026  QUALIFIED Tier A — ICP v3 (by Priya)          [view breakdown]
│  ⬤ 10 Jun 2026  CALL — "Spoke to MD's office; no ERP in use…" (by Priya)
│  ⬤ 09 Jun 2026  AI RESEARCH verified — 6 claims, 6 evidenced   [view]
│  ⬤ 08 Jun 2026  Attachment: catalogue.pdf (by Priya)
│  ⬤ 05 Jun 2026  MERGED from "ABC Pump Industries" (IndiaMART row 214)
│  ⬤ 05 Jun 2026  IMPORTED — batch #12, IndiaMART
```
- **Overview:** editable fields (core-field edit shows "will drop verification" warning), completeness meter with missing-field hints.
- **Contacts:** table, MD/Owner star, primary marker, add/edit, re-link to another company (closes association with end date).
- **Evidence:** all evidence cards — claim, excerpt (quoted), source link, capture date; filter by verified/unverified.
- **Qualifications:** append-only history; each entry expandable to per-criterion answers with linked evidence chips.
- Returning-company banner: if state is DISQUALIFIED and a new import row just matched it, a banner shows the disqualification reason + date at the very top.

### 2.4 Import Wizard  `/imports/new` (3 steps)
1. **Upload & source** — file drop, source enum select, template select (or "generic CSV").
2. **Mapping** — detected columns → target fields; unmapped columns explicitly marked "ignored"; [Save as template] for Admin.
3. **Confirm** — row count, template summary → Start Import → redirects to Batch Report.

### 2.5 Batch Report  `/imports/{id}`
Live-updating (poll while PROCESSING).
```
Batch #12 · IndiaMART · 820 rows · ▓▓▓▓▓▓░░ 78%
Created 512 · Duplicate-flagged 214 · Rejected 41 · Pending 53
Tabs: Rejected (row + reason, raw data expandable) | Duplicates (→ Triage) | Created
```
Nothing silently dropped: rejected rows always inspectable with raw data.

### 2.6 Triage Queue  `/triage` — keyboard-first
```
Pair 3 of 214                          [M]erge  [N]ot duplicate  [R]eject  [→ skip]
Matched on: same normalized phone (9843012345)
┌ EXISTING: ABC Pumps ────────────┬ INCOMING: ABC Pump Industries ──────┐
│ name    ABC Pumps        (•)    │ ABC Pump Industries        ( )      │
│ domain  abc-pumps.com    (•)    │ —                                   │
│ phone   9843012345       (•)    │ 9843012345                          │
│ city    Coimbatore       (•)    │ Coimbatore                          │
│ contact R. Kumar (MD)           │ + S. Raj (Purchase)  [will be added]│
└─────────────────────────────────┴─────────────────────────────────────┘
⚠ Existing company is DISQUALIFIED (Trader, Mar 2026) — history shown below
```
- Merge: radio per field picks survivor value; incoming extras (contacts, notes) appended automatically.
- Clean (non-flagged) records appear as a fast-confirm list: one keystroke each.

### 2.7 Research Queue  `/research/queue`
Table: company, requested by, status (QUEUED/PROCESSING/COMPLETE/FAILED + error), timestamps. Header: today's AI usage vs daily cap (progress bar). Retry action on FAILED. Entry point is usually bulk: Companies screen → select rows → "Send to Research" (cap-exceeded → clear 422 message).

### 2.8 Review Queue  `/research/review` — the evidence-checking screen
```
ABC Pumps — AI Research draft (unverified)          4 of 37 in queue
Summary: "Manufacturer of centrifugal pumps…"                [edit ✎]
CLAIMS
✅ Manufacturer (not trader)   "…in-house CNC machining…"  abc-pumps.com/products ↗   [Accept][Edit][Reject]
✅ ISO 9001 certified          "…certified since 2015…"    abc-pumps.com/quality ↗    [Accept][Edit][Reject]
⚠ Turnover — NOT FOUND (no evidence on site)
[Accept all remaining]  [Mark record Verified]  [Next →]
```
- Every claim: excerpt quoted verbatim + source link opening in new tab. Reject asks a one-line reason.
- "Mark record Verified" enabled only when all claims are resolved.

### 2.9 Qualification  `/companies/{id}/qualify`
```
ICP: Manufacturing SME — Owner-Driven · v3
HARD FILTERS   ✓ Manufacturing  ✓ Not trader  ✓ Location in scope
CRITERIA
MD/Owner access      AI suggests YES  — evidence: [call note 10 Jun] [about page ↗]   ( YES / NO / UNKNOWN )
Owner-driven ops     AI suggests YES  — evidence: [about page ↗]                      ( YES / NO / UNKNOWN )
Org size < 300       AI suggests YES  — evidence: [size signals]                      ( … )
ERP maturity: low    AI suggests YES  — evidence: [call note]                         ( … )
──────────────────────────────────────────────
Computed tier: A        Final tier: [ A ▾ ]     (change ⇒ override reason required)
[Confirm Qualification]        [Disqualify… ⇒ reason dropdown + note, required]
```
Evidence chips open a side panel — the analyst never leaves the screen to verify.

### 2.10 Ready Pool  `/ready`  (Sales Lead+)
Companies table pre-filtered to READY; filters tier/cluster/city/completeness; stale rows flagged ⏱ with export-time warning. Select rows → **Create Export** → modal: destination label + export template → confirm → CSV downloads, rows move to EXPORTED.

### 2.11 Exports  `/exports`
Log table: date, label, count, by whom, [download again]. Row expands to company list.

### 2.12 Reports  `/reports`
Tabs (each = one API endpoint, table + one chart, CSV download button):
- **Source Funnel** — grouped bars per source: imported → survived dedup → qualified (stacked by tier) → exported; date range picker.
- **Pipeline** — state counts, weekly movement, aging per stage, queue depths.
- **Quality** — completeness histogram, verified %, stale count, by cluster/city.
- **Qualification Outcomes** — tier distribution, disqualification reasons ranked, override rate.

### 2.13 Settings  `/settings`  (Admin)
- **ICP Profiles:** criteria list editor (key, label, weight, evidence-required flag) + tier thresholds; **Save = new version** with confirmation ("existing qualifications keep v3; new ones use v4").
- **Import Templates / Export Templates:** mapping and column-layout editors.
- **Disqualification Reasons:** label list, activate/deactivate (never delete — referenced by history).
- **System:** research batch size, daily AI cap, staleness days, name stop-suffixes.
- **Users:** read-only Keycloak mirror with roles.

---

## 3. Shared Components

| Component | Used in | Behavior |
|---|---|---|
| `StateBadge` | everywhere | color-coded pipeline state |
| `TierBadge` | everywhere | A/B/C/Disqualified + ICP version on hover |
| `EvidenceChip` / `EvidenceCard` | review, qualification, detail | claim + quoted excerpt + source link + capture date; chip opens side panel |
| `TimelineItem` | company detail | icon per type, actor, relative time, expandable payload |
| `DataTable` | all lists | server pagination/sort, column filters, row selection, CSV export where allowed |
| `StaleFlag` | search, ready pool, export | ⏱ + days since verification, threshold from settings |
| `ReasonModal` | disqualify, reject, override | dropdown (configured list) + free-text; confirm disabled until reason chosen |

## 4. UX Rules

1. Destructive/terminal actions (disqualify, reject, archive, merge) always confirm and always capture a reason where the domain requires one.
2. Empty states teach: empty Triage Queue says "No duplicates pending — import a file to get started" with a button.
3. Errors surface the problem+json `detail` verbatim — no generic "something went wrong."
4. All queue screens show position ("4 of 37") and support keyboard advance.
5. AI content is always visually marked (robot icon + "unverified" tint) until a human verifies it.
6. Loading = skeletons on first load, background refresh silent (TanStack Query defaults).
