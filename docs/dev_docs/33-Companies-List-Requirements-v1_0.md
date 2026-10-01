<!--
Document: 33-Companies-List-Requirements-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Ready for Implementation
Scope: Companies List page with confidence, designation, filters
Audience: Engineering, QA, Claude Code
Depends on: 29, 30, 31
-->
# ProspectSoul — Companies List Requirements

**Version:** 1.0
**Date:** 30 September 2026

---

## 1. Page Route

`/companies` — the universal entry point and the "10-second have we seen this company?" screen.

---

## 2. Column Layout

### 2.1 Default Visible Columns

| # | Column | Source | Width | Sortable | Notes |
|---|---|---|---|---|---|
| 1 | Company Name | canonical_name | flex | ✓ | Bold. Row click → detail page |
| 2 | Phone | primary_phone_normalized | 130px | ✓ | Formatted: 98430 XXXXX. "+N more" indicator if additional numbers exist |
| 3 | Confidence | primary phone's confidence | 80px | ✓ | Colored badge: H (green), M (amber), L (grey). Empty if no phone |
| 4 | Designation | primary phone's resolved designation | 120px | — | Text. "MD", "CEO", "Purchase Mgr", etc. Empty if none |
| 5 | Email | email | 180px | ✓ | Truncated with tooltip |
| 6 | Website | website_domain | 150px | ✓ | Link, opens in new tab |
| 7 | City | city | 120px | ✓ | |
| 8 | State | state | 100px | ✓ | |
| 9 | Industry | industry | 120px | ✓ | |
| 10 | Source | import source (batch) | 100px | ✓ | Badge: INDIAMART, EXCEL_CSV, etc. |
| 11 | Pipeline | pipeline_state | 100px | ✓ | StateBadge component |
| 12 | Verified | verification_status | 80px | ✓ | ✓ / ✗ with relative time tooltip |
| 13 | Created | created_at | 100px | ✓ (default desc) | Relative time: "2mo ago" |
| 14 | Actions | — | 60px | — | Edit pencil, kebab menu (Verify, Disqualify, etc.) |

### 2.2 Optional Columns (toggle via column settings)

Cluster, Size Band, Tags, Number Source (of primary), Completeness Score, Stale flag.

---

## 3. Phone Column Behavior

### 3.1 Primary Number Display

The Phone column shows the company's primary phone number formatted for readability: `98430 XXXXX` (5+5 split for Indian mobile numbers).

### 3.2 Additional Numbers Indicator

If the company has more than one phone number, show a muted "+N" badge after the primary number. Example: `98430 12345 +2`

### 3.3 Hover Tooltip for Additional Numbers

Hovering over the "+N" badge shows a tooltip listing all numbers:

```
98430 12345 (Primary, Business Card, High, MD)
98440 67890 (IndiaMART, Low, Purchase Mgr)
94440 11111 (Website, Medium)
```

Format per line: `number (source, confidence, designation if any)`

---

## 4. Confidence Column Behavior

### 4.1 Badge Display

| Level | Badge | Color |
|---|---|---|
| HIGH | **H** | Green background, white text |
| MEDIUM | **M** | Amber/yellow background, dark text |
| LOW | **L** | Grey background, white text |
| No phone | — | Empty cell |

### 4.2 Hover Detail

Hovering over the confidence badge shows: `"High — Business Card"` (confidence level + number source of the primary number).

---

## 5. Designation Column Behavior

### 5.1 Content

Shows the designation associated with the primary phone number:
- If the primary number is linked to a contact → contact's designation field
- If the primary number has a designation_override → that value
- If neither → empty cell

### 5.2 Decision-Maker Highlight

If the designation matches a decision-maker role (per the `decision_maker_designations` table), the text is shown with a ⭐ prefix: `⭐ MD`, `⭐ CEO`.

---

## 6. Search

### 6.1 Search Box

Single search input at the top. Searches across:
- company name (normalized + trigram fuzzy)
- phone (any format — normalized before lookup; searches all company_phones, not just primary)
- email
- website/domain
- contact name

### 6.2 Search Across All Phones

When searching by phone number, the search must match against all numbers on a company (via the `company_phones` table), not just the primary. If a non-primary number matches, the result row still shows the primary number in the Phone column but may highlight the matching number in a subtitle or search snippet.

---

## 7. Filters

### 7.1 Filter Panel

Expandable filter panel below the search box. Filters:

| Filter | Type | Values |
|---|---|---|
| Pipeline State | Multi-select chips | IMPORTED, TRIAGE, RESEARCH, QUALIFICATION, READY, EXPORTED, DISQUALIFIED, ARCHIVED |
| Tier | Multi-select chips | A, B, C |
| Confidence | Multi-select chips | High, Medium, Low |
| Number Source | Multi-select dropdown | All NumberSourceType values |
| Has Decision-Maker | Toggle | Yes / No / Any |
| City | Searchable dropdown | From reference list |
| State | Searchable dropdown | From reference list |
| Industry | Searchable dropdown | From reference list |
| Cluster | Searchable dropdown | From reference list |
| Verification | Select | Verified, Unverified, Any |
| Stale | Toggle | Yes / No / Any |
| Import Source | Multi-select dropdown | All ImportSource enum values |
| Tags | Multi-select chip input | From existing tags |

### 7.2 Active Filter Display

Active filters shown as dismissible chips above the table:

```
[Pipeline: READY ×] [Confidence: High ×] [City: Coimbatore ×]  [Clear all]
```

### 7.3 Filter Count

Show total matching count: "Showing 1–25 of 1,420 companies"

---

## 8. Filter Persistence (URL-Based)

### 8.1 URL Encoding

Every filter, sort, and pagination state is encoded in the URL query string:

```
/companies?q=pump&state=READY,QUALIFICATION&confidence=HIGH&city=Coimbatore&sort=created_at,desc&page=1&size=25
```

### 8.2 State Management

- **On mount:** read filter state from URL params. If none, use defaults (no filters, sort=created_at,desc, page=1, size=25).
- **On filter change:** update URL params via `useSearchParams` (React Router) or equivalent. Do NOT push a new history entry for every keystroke; debounce search input (300ms) and batch filter changes.
- **On pagination:** update `page` param.
- **On sort:** update `sort` param.

### 8.3 Back Button Behavior

When a user navigates from the companies list to a company detail page and clicks Back:
- The browser restores the previous URL (with all filter params).
- The companies list component reads the params and restores the exact view.
- No flash of unfiltered data — use TanStack Query's `keepPreviousData` or `placeholderData` option.

### 8.4 New Tab Behavior

Ctrl+click or right-click "Open in New Tab" on a row:
- The company detail opens in a new tab at `/companies/{id}`.
- The original tab's URL (with filters) is unchanged.
- The user returns to the original tab and sees their filtered view intact.

### 8.5 Shareable URLs

Filter URLs are shareable. User A can copy `/companies?confidence=HIGH&state=READY` and send it to User B (if User B has the appropriate role).

---

## 9. Table Behavior

### 9.1 Pagination

Server-side pagination. Default page size 25. Options: 10, 25, 50, 100.

### 9.2 Sorting

Server-side sorting. Default: created_at descending. Multi-sort: Shift+click to add secondary sort.

### 9.3 Row Selection

Checkbox column (first column, not shown in §2.1 layout). Used for bulk actions: Send to Research, Create Export.

### 9.4 Row Click

Click on a row (not on the checkbox) navigates to `/companies/{id}`. The URL should be an `<a>` tag so Ctrl+click opens in a new tab.

### 9.5 Visual States

- DISQUALIFIED / ARCHIVED rows: muted opacity (0.6), never hidden by default
- Stale rows: ⏱ icon in the Verified column
- Unverified rows: ✗ in Verified column

### 9.6 Empty State

When no results match filters: "No companies match your filters. [Clear filters] or [Import a file] to get started."

### 9.7 Loading State

Skeleton rows (5 rows) on initial load. On filter change: keep previous data visible, show subtle loading indicator (top progress bar).

---

## 10. API Contract

### 10.1 Request

```
GET /api/v1/companies?q=pump&state=READY&confidence=HIGH&city=Coimbatore
    &number_source=BUSINESS_CARD&has_decision_maker=true
    &sort=created_at,desc&page=0&size=25
```

New query parameters:
- `confidence` — filter by primary phone confidence: HIGH, MEDIUM, LOW (comma-separated)
- `number_source` — filter by primary phone number source (comma-separated)
- `has_decision_maker` — boolean, true = primary phone linked to a decision-maker contact

### 10.2 Response

Existing envelope with additional fields per item:

```json
{
  "content": [
    {
      "id": "...",
      "canonical_name": "ABC Pumps Pvt Ltd",
      "primary_phone_normalized": "9843012345",
      "primary_phone_confidence": "HIGH",
      "primary_phone_designation": "MD",
      "additional_phone_count": 2,
      "email": "...",
      "website_domain": "abc-pumps.com",
      "city": "Coimbatore",
      "state": "Tamil Nadu",
      "industry": "Pumps",
      "source": "INDIAMART",
      "pipeline_state": "READY",
      "verification_status": "VERIFIED",
      "gst_number": "33AABCA1234A1ZA",
      "created_at": "2026-06-05T10:30:00Z"
    }
  ],
  "page": 0,
  "size": 25,
  "total_elements": 1420,
  "total_pages": 57
}
```
