# ADR-0008: The header alias `Region` maps to `region`, not `state`
Date: 2026-09-21 · Status: Accepted

## Context
The pre-existing `ColumnAliasRegistry` (docs 01–18) maps the source header `Region` to the target field `state`. Doc 23 §Alias Table introduces a first-class `region` target field with `Region` as one of its aliases, and Domain Model Addendum Invariant 16 explicitly separates the two: "Region ≠ State ≠ Cluster ≠ District — four distinct facets." Keeping `Region` mapped to `state` would make it impossible to import the Sales-Intelligence Region facet correctly, and would silently overwrite the geographic State with a sales-territory string on every registry import that ships a `Region` column.

## Decision
Remove `"Region"` from the `state` alias list and add the new `region` target field with the alias set specified in doc 23:
```
region ← Region, Zone, Territory, Sales Region
```
`state` retains `State, State Name, Province, Company State`. The mapping-suggestion service now returns `region` for the header `Region`.

## Consequences
**Easy:** the two facets stay distinct in the model; registry imports populate `region` as intended.
**Hard:** any user-saved import template previously created against the doc-01 registry may still map `Region → state` — those templates are visible in the Admin UI and can be corrected there. No mapping is silently changed on their behalf.
**Given up:** backward compatibility of the exact alias mapping for the single header `Region`. Every other alias is unchanged.

## Affected
- `imports/mapping/ColumnAliasRegistry.java` (Region removed from `state`, added to `region`)
- `imports/mapping/ColumnAliasRegistryTest.java` (assertion updated)
- No migration required; the change affects live suggestion, not stored data.
