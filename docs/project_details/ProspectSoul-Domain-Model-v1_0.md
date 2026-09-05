# ProspectSoul — Domain Model (Conceptual)
**Version:** 1.0 · **Date:** July 2026 · **Companion to:** PRD v1.1
**Audience:** Everyone — product, sales, engineering, Claude Code. This is the shared mental model. No SQL, no APIs — business concepts only.

---

## 1. The One-Sentence Model

> A **Company** accumulates **Activities** that produce **Evidence**, which supports a versioned **Qualification**, which gates an **Export** — and every record of all of it is permanent.

---

## 2. Entity Map

```
                        ┌─────────────────────┐
                        │      COMPANY        │  ← the anchor. One per real-world company, ever.
                        │  (pipeline state)   │
                        └─────────┬───────────┘
          ┌───────────┬───────────┼────────────┬──────────────┐
          ▼           ▼           ▼            ▼              ▼
     CONTACTS     ACTIVITIES  QUALIFICATIONS  EXPORTS     ALIASES
     (people,     (research,  (tiered,        (what left, (merged
      MD flag)     calls,      versioned,      when, to    identities —
                   visits,     append-only)    where)      old IDs still
                   notes)         ▲                        resolve)
                      │           │
                      ▼           │
                 ATTACHMENTS      │
                 (brochures,      │
                  catalogues)     │
                      │           │
                      ▼           │
                  EVIDENCE ───────┘
                  (claim + source + excerpt + date;
                   cited by qualification criteria)
```

Supporting entities that exist around the anchor:

```
IMPORT BATCH → IMPORT ROWS → (normalization) → DUPLICATE CANDIDATES → COMPANY
ICP PROFILE → ICP VERSIONS → CRITERIA → QUALIFICATION
COMPANY RELATIONSHIPS (parent/subsidiary/plant) — designed, not built in v1
```

---

## 3. The Three Core Flows

### Flow A — How data becomes a Company
```
Import Batch
   ↓  (every row captured raw — nothing silently dropped)
Import Rows
   ↓  (automatic)
Normalization        phone → 10 digits · website → domain · name → matching form
   ↓
Duplicate Check      deterministic: phone / domain / name+city
   ↓
 ┌─────────────┴──────────────┐
 no match                match found
   ↓                          ↓
Company created         Duplicate Candidate
(state: TRIAGE)              ↓ (human decides)
                        Merge / Not-a-duplicate / Reject
```

### Flow B — How a Company becomes knowledge
```
Company → Research Queue → AI Research (batch, async)
                                ↓
                     Draft with EVIDENCE per claim
                     (no evidence ⇒ "not found", never asserted)
                                ↓
                        Review Queue (human)
                                ↓
              Verified Activities + Attachments on the TIMELINE
```

### Flow C — How knowledge becomes a decision
```
ICP Profile (versioned criteria)
        ↓ evaluates
Company + its Evidence
        ↓ AI suggests per-criterion answers, citing evidence
Human confirms / corrects / overrides (override ⇒ mandatory reason)
        ↓
QUALIFICATION  (Tier A / B / C / Disqualified)
   · stamped with ICP version
   · append-only — re-qualification adds, never overwrites
        ↓ (Tier A/B/C)
READY pool → EXPORT (recorded: what, when, where, who)
```

---

## 4. Company Lifecycle (State Machine)

Every company has **exactly one** state at all times.

```
IMPORTED → TRIAGE → RESEARCH → QUALIFICATION → READY → EXPORTED
                \________________________________/
                 DISQUALIFIED / ARCHIVED (terminal,
                 reachable from any state, always with a reason,
                 always still searchable)
```

Allowed shortcuts: TRIAGE → QUALIFICATION (analyst judges record already complete).
Re-entry: a DISQUALIFIED company hit by a new import re-surfaces with full timeline shown before any effort is spent.

---

## 5. Business Invariants (The Rules Everything Obeys)

1. **One company = one real-world entity.** Duplicates are merged, never coexist. Merged IDs become aliases that still resolve.
2. **AI never changes state.** No auto-merge, no auto-qualify, no auto-disqualify. AI drafts; humans decide.
3. **No evidence, no claim.** An AI assertion without a citation is stored as "not found."
4. **Qualifications and merges are append-only.** History is never rewritten.
5. **Every terminal decision has a reason.** Disqualification, rejection, override — all carry a recorded reason.
6. **Every record has lineage.** Any field traces to the batch and row that introduced it.
7. **The timeline is the truth.** It is composed from the real tables at query time — there is no second event store to drift out of sync.
8. **Research activities only.** Sales outreach (demos, follow-ups) is CRM territory; logging it here is the signal the lead should already have been exported.
9. **Configuration over code.** ICP criteria, import mappings, export layouts, reason lists, queue caps — all Admin-editable, all versioned where they affect decisions.

---

## 6. Vocabulary (Say the Same Words Everywhere)

| Term | Means | Never means |
|---|---|---|
| Company | A prospect organization record | A customer, an ERP tenant |
| Contact | A person at a company | A CRM lead |
| Activity | A research/verification touch | A sales outreach event |
| Evidence | Claim + source + excerpt + date | An unsourced AI opinion |
| Qualification | A tier decision under an ICP version | A sales stage |
| Tier | A / B / C per the sales playbook | Priority of work |
| Export | The handoff moment to CRM | A sync or integration |
| Batch | One import event with lineage | A background job |
