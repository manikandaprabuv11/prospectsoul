# ADR-0005: Download and Export are distinct operations
Date: 2026-07 · Status: Accepted

## Context
`POST /api/v1/exports` (PRD Stage 5) exists to hand qualified leads to a CRM. It moves companies to `state=EXPORTED` and logs the event. Sales now also wants "give me a CSV of these 340 companies I'm currently viewing" — which must NOT change pipeline state, because they might filter and download the same set multiple times a day for analysis.

Overloading the existing endpoint would silently move companies to `EXPORTED` on every casual download.

## Decision
Two endpoints, two verbs:
- `POST /api/v1/exports` — pipeline Export. State → EXPORTED. Logged. Sales Lead+ only. Unchanged behavior.
- `POST /api/v1/companies/download` — filter-scoped CSV/XLSX. No state change. No `exports` row. Audit only. Any read role.

UI copy: "Export" appears only on Ready Pool. "Download" appears on Companies List. The two words are not interchangeable.

## Consequences
**Easy:** clear semantics; casual analysis does not corrupt the pipeline; Viewer role can download (read-based) without new permission model.
**Hard:** enforcement of vocabulary in UI copy — reviewers must catch label drift.
**Given up:** the appearance of a single "export data" feature.

## Affected
- New endpoint `POST /api/v1/companies/download`
- New Download modal on Companies List
- UX Rule 11 in doc 22
- README + user documentation
