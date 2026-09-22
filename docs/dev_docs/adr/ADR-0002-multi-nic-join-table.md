# ADR-0002: Multi-NIC via join table, not scalar column
Date: 2026-07 · Status: Accepted

## Context
Analysis of Kanchipuram_data.xlsx (123,659 rows): 25,514 rows (21%) carry more than one NIC code. Maximum observed: 91 activities on one company. A scalar `nic_code` column on `companies` silently discards everything after the first, violating the "nothing silently dropped" invariant. A JSON blob would work but breaks filter/index/join.

## Decision
- New join table `company_nic_codes` — one row per (company, NIC) pair
- Store both raw (`nic_code_raw`, `description_raw`) and resolved (`nic_code_id`) — unmatched codes preserved
- Exactly one row per company flagged `is_primary` (partial unique index)
- Denormalize primary to `companies.primary_nic_code_id` for fast list rendering

## Consequences
**Easy:** multi-NIC, hierarchical grouping, unmatched-code preservation, atomic primary via DB constraint.
**Hard:** denormalized `primary_nic_code_id` must stay consistent with the join row — enforced in service layer.
**Given up:** trivial "show one industry per company" — replaced by primary badge + full list on detail.

## Affected
- Migration V<next+3>__company_nic_codes.sql
- Migration V<next+1>__company_sales_fields.sql (adds `primary_nic_code_id`)
- Companies API — new endpoints for attach/detach/make-primary
- Companies filter — `nic_code_id`, `nic_parent_id`
- Company detail UI (NIC codes section on Overview tab)
- Import — Activities JSON parser writes join rows
