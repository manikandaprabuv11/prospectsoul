<!--
Document: 22-Sales-Intelligence-Claude-Kickoff-Prompt-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Audience: Claude Code
-->
# ProspectSoul — Sales Intelligence Extension: Claude Kickoff Prompt

You are the senior full-stack engineer implementing the ProspectSoul Sales Intelligence Extension.

---

## AUTHORITATIVE DOCUMENTS

Before coding, read in order:

1. ProspectSoul PRD v1.1
2. ProspectSoul Domain Model v1.0
3. ProspectSoul Technical Design Specification v1.0
4. ProspectSoul UI/UX Specification v1.0
5. ProspectSoul Implementation Roadmap v1.0
6. Technical-Team Developer Guide / CLAUDE.md
7. Docs 01–18 (Company Management vertical slice — already implemented)
8. **Doc 19 — Sales Intelligence Requirements**
9. **Doc 20 — Sales Intelligence Technical Specification**
10. **Doc 21 — Sales Intelligence Implementation Plan**

Document hierarchy must be respected. If documents conflict, **do not silently choose one — identify the conflict, stop, and ask.**

---

## OBJECTIVE

Implement a real end-to-end Sales Intelligence Extension across the existing:
- Spring Boot backend
- React frontend
- PostgreSQL database

Five sequential tracks:

- **C1** — NIC Master module
- **C2** — Company field extension + Contact roles + Multi-NIC join
- **C3** — Hierarchical NIC filter + Grouped view
- **C4** — Location Intelligence (Google Maps + Places)
- **C5** — Companies List Download

Full scope, criteria, and required tests are in doc 21.

**Never start track N+1 until track N's acceptance criteria all pass.**

---

## FIRST: INSPECT

Before changing any code, produce a **gap assessment**. Do not begin implementation until it is reviewed.

Inspect:
- Repository structure
- Current `companies` table, entity, all applied Flyway migrations — confirm exactly which of the 12 new fields already exist (do not add columns that already exist)
- Current `contacts` table, entity — confirm `is_md_owner`, `is_primary`, `designation`
- Current import framework, header normalizer, alias registry
- Current frontend routes and app shell
- Authentication / Keycloak role handling
- Existing tests
- `CLAUDE.md`

Report the gap as a table: for each planned change, is it new, partially present, or already present?

**Reuse existing code wherever appropriate.** Do not duplicate the app shell, the audit service, the import framework, or the alias registry.

---

## SAMPLE DATA

The system must import files that look like this through the existing Companies option:

```
LG_ST_Code  State        LG_DT_Code  District      Pincode  RegistrationDate  EnterpriseName
33          TAMIL NADU   574         KANCHIPURAM   600091   17/11/2025        GRACE BLUE METALS & SUPPLIERS
33          TAMIL NADU   574         KANCHIPURAM   600097   17/11/2025        Vijaya enterprises
33          TAMIL NADU   574         KANCHIPURAM   602105   17/11/2025        SHIV SHAKTI ELECTRICALS HARDWARE
```

7 columns, no phone, no email, no website, no NIC codes, no address. Import must succeed cleanly with `address_line`, `company_nic_codes`, `contacts`, and other unmapped fields empty. `RegistrationDate` parses as `dd/MM/yyyy`.

The system must also import the fuller Kanchipuram Excel (adds `CommunicationAddress` and `Activities` JSON array) — see doc 19 §3 and doc 20 §9 for full field handling.

Both must work.

---

## TWELVE CRITICAL CONSTRAINTS

Break any of these and the work is not accepted:

1. **Do not recreate the `companies` or `contacts` table.** Additive `ALTER TABLE` migrations only. Never modify an applied migration.
2. **NIC is optional everywhere.** Absent → no rows in `company_nic_codes`, no validation error, row never rejected.
3. **A company can carry multiple NIC codes and multiple contacts.** Scalar-column shortcuts are forbidden — use the join table `company_nic_codes` and the existing `contacts` table extended with `role_id`. The Kanchipuram file has one row with 91 NIC codes; anything that discards them is broken.
4. **Contact role is required on new contacts** and comes from the configured `contact_roles` list. `is_md_owner` is preserved for backward compat and kept in sync when role is `MD_OWNER`.
5. **Resolve NIC parents at import time by longest existing prefix** — not fixed-length truncation. Real data skips levels.
6. **NIC master import is reference-data import.** Must NOT create `import_batches` / `import_rows` rows.
7. **Pincode search must not fetch external results from the `companies` table.** External results come live from the Places API and are **never persisted automatically**.
8. **"+ Add" from the map routes through the existing `POST /api/v1/companies` manual-entry endpoint** — batch-of-one, normalization, dedup, audit. Do not build a second creation path.
9. **`GOOGLE_PLACES_API_KEY` stays server-side only.** Never in a frontend bundle. Maps JS API key is separate and origin-restricted at Google Cloud Console.
10. **Do not rename or repurpose "ICP".** NIC filtering is a search facet on `companies`. ICP Qualification (PRD §3.3) stays reserved for the versioned Tier decision engine.
11. **"Download" ≠ "Export".** Download = filter-scoped CSV/XLSX, no state change. Export = pipeline handoff, changes state to `EXPORTED`. Two endpoints, two verbs, one clear meaning each. The word "Export" never appears in the Download modal.
12. **Every value that was nullable stays nullable.** A 7-column import row must succeed. Do not add `NOT NULL` to any of the new columns.

---

## BACKEND RULES

- Feature-first modular architecture: `controller / service / repository / entity / dto / mapper`
- Controllers must never expose JPA entities
- UUID ids, `/api/v1`, snake_case JSON, RFC-7807 errors
- Server-side authorization; test 403 paths for Viewer on mutations, non-Admin on `/admin/*`, non-Sales-Lead on pipeline Export
- `AuditService` / `audit_log` for every mutation — do not build a second audit system
- Flyway migrations, immutable once applied
- Primary-flag atomicity enforced at both service and DB level (partial unique index)

---

## ADR ON DEMAND — Do Not Create Now

**Do not create an `/adr` folder at project setup.**

The folder is created **only when you actually change existing code, existing flow, or deviate from docs 01–22 during implementation.** At that moment:

1. Create `/adr` at the repository root if it does not exist
2. Create `/adr/README.md` with the template below on first creation
3. Add one ADR file per behavior-changing decision, named `ADR-NNNN-<short-slug>.md`
4. Number sequentially starting at `0001`

**When to write an ADR:**
- You change existing code paths, existing flows, or existing behavior
- You deviate from any spec in docs 01–22
- You introduce a new invariant or business rule not already in the specs
- You reject or override a specified approach for a documented reason

**Not required for:**
- New code that follows the specs as written
- Cosmetic refactors within a single new module
- Bug fixes in your own new code

**ADR template:**

```markdown
# ADR-NNNN: <Title>
Date: YYYY-MM-DD
Status: Proposed | Accepted | Superseded by ADR-XXXX

## Context
What existed before, and what forced a decision.

## Decision
What you're doing.

## Consequences
What this makes easy. What it makes hard. What was given up.

## Affected
Files / migrations / APIs / specs touched.
```

**`/adr/README.md` template (create on first ADR):**

```markdown
# Architecture Decision Records

Short records of decisions that changed existing code, existing behavior, or deviated from docs 01–22.

## Numbering
Sequential 4-digit. Never reuse. Superseded ADRs stay in place with `Status: Superseded by ADR-XXXX`.

## Index
- ADR-0001 — <title>
- ADR-0002 — <title>
```

---

## TESTS REQUIRED

**Unit**
- NIC level detection, longest-prefix parent resolution including skipped-level branches
- Idempotent NIC re-import by `code`
- Recursive descendant expansion
- Activities JSON parser: valid, single element, `"NA"`, empty array, malformed
- `dd/MM/yyyy` date parser (string variant, not Excel-date-serial variant)
- Contact role backfill from `is_md_owner`
- GST format (regex), pincode format (6-digit)
- Atomic primary-flag transitions (NIC and contact)
- Header alias resolution for all new aliases
- Dedup rule ⓪ (`source + source_reference`) before rules ①-③

**Integration (Testcontainers)**
- 7-column sample from doc 19 §3 — all rows created cleanly
- Real Kanchipuram Excel import — exact expected counts (see doc 21 C2 criterion 5)
- Company import with / without / unknown NIC codes
- Recursive NIC filter — exact counts against seed
- Places search — **assert zero DB writes**
- "+ Add" — batch-of-one with `source=GOOGLE_PLACES`, dedup applied
- Download — pipeline state unchanged, no `exports` row created
- 403 authorization paths for every mutation
- Audit rows written for every mutation

**AI**
- No changes; existing MockAiProvider baseline unchanged

---

## DOCUMENTATION

Update:
- OpenAPI (all new endpoints, all new filters)
- README (how to run, what works now, migration order)
- Alias registry documentation (all new aliases from doc 20 §9.1)
- `/adr` if any existing-flow changes were made

---

## DEFINITION OF DONE

Do not claim completion until every criterion in doc 21's Release Gate passes, with evidence per criterion.

---

## FINAL RESPONSE FORMAT

1. Files created / changed
2. Database migrations added
3. Backend implementation summary per track
4. Frontend implementation summary per track
5. 7-column sample import results
6. Kanchipuram import results (row counts, rejections, flags, join-table rows)
7. Tests and exact results
8. Acceptance criteria PASS / FAIL / BLOCKED per criterion, per track
9. ADRs written (if any) — with a one-line summary of each
10. Specification deviations (none expected — flag with reason if any)
11. Remaining work

**Do not claim PASS without evidence.**

---

## SCOPE CONTROL

Do not implement automated re-classification, GST registry lookups, scheduled pincode sweeps, external-response caching, Udyam-number as identity field, AI research changes, ICP qualification changes, CRM outreach, cross-company contact merging, or company relationships — unless strictly required by an existing dependency.

Start by inspecting the repository and the authoritative documents. Then produce the gap assessment. Then start Track C1.
