# Frontend State Management Rules

## Purpose

This document defines the core rules for managing state in the React frontend.

Before creating or modifying state management code, Claude Code must follow these rules.

---

## 1. State Categories

Classify state before deciding where it belongs.

```text
Local UI State
Server State
Global Application State
URL State
Form State
```

Do not place all state into one global store.

---

## 2. Local UI State

Use local React state for state that belongs to one component or a small component tree.

Examples:

```text
modal open/closed
selected tab
dropdown state
temporary UI values
expanded/collapsed sections
```

Use:

```javascript
useState()
```

when appropriate.

Keep state as close as possible to the components that use it.

---

## 3. Server State

Server state includes data received from the backend.

Examples:

```text
companies
contacts
activities
users
evidence
API responses
```

Use the project's server-state solution, preferably TanStack Query when it is part of the application.

Do not duplicate server data unnecessarily in local or global state.

Refer to `API_INTEGRATION.md` for API rules.

---

## 4. Server State Rules

Server state should support:

```text
fetching
caching
refetching
pagination
mutations
error handling
stale data management
query invalidation
```

Do not manually rebuild these behaviors with multiple `useEffect()` and `useState()` calls when the project's server-state library already provides them.

---

## 5. Global State

Use global state only when data is genuinely required across unrelated parts of the application.

Examples may include:

```text
authenticated user information
application preferences
global UI state
shared application configuration
```

Do not put feature-specific or temporary component state into global state.

---

## 6. Context API

Use React Context for appropriate cross-tree dependencies.

Good examples:

```text
theme
authentication context
application configuration
```

Do not use Context as a replacement for every type of state management.

Avoid putting frequently changing large datasets into Context when a dedicated server-state or state-management solution is more appropriate.

---

## 7. Form State

Form state should remain inside the form or its dedicated form-management solution.

Examples:

```text
input values
validation state
submission state
dirty state
touched state
```

Do not store temporary form fields in global application state unless there is a genuine cross-page requirement.

---

## 8. URL State

State that should be shareable, bookmarkable, or preserved through navigation should use the URL where appropriate.

Examples:

```text
search
page
page size
filters
sorting
selected resource
```

Example:

```text
/companies?page=0&size=25&search=abc
```

Do not duplicate URL state unnecessarily in multiple stores.

---

## 9. State Ownership

The component or feature that owns a piece of state should be the lowest appropriate level in the component tree.

Prefer:

```text
Parent
 ├── Child A
 └── Child B
```

with state in `Parent` when both children require it.

Do not move state to global storage unless there is a real need.

---

## 10. Prop Drilling

A small amount of prop passing is acceptable.

Do not introduce global state merely to avoid passing one or two props.

If deeply nested components repeatedly require the same state, consider:

```text
component composition
custom hooks
Context
feature state
```

based on the actual requirement.

---

## 11. State Updates

State updates should be predictable and immutable.

Do not directly mutate React state.

Bad:

```javascript
state.items.push(item);
```

Prefer:

```javascript
setItems(prev => [...prev, item]);
```

Use functional updates when the new state depends on the previous state.

---

## 12. Derived State

Do not store values that can be calculated from existing state.

Avoid:

```text
items
filteredItems
```

when `filteredItems` can be derived from `items` and the current filter.

Prefer deriving values through:

```text
calculation
selector
memoization when justified
```

This prevents inconsistent duplicated state.

---

## 13. Single Source of Truth

Each piece of information should have one authoritative source.

Avoid storing the same data independently in:

```text
local state
global state
Context
server cache
```

without a clear reason.

Duplicated state can become inconsistent.

---

## 14. Loading and Error State

For server state, use the server-state solution's standard status handling.

Represent:

```text
loading
success
empty
error
```

Do not create multiple independent loading flags for the same API request unless required.

---

## 15. Mutations

Create, update, and delete operations should use the application's standard mutation mechanism.

After successful mutations:

```text
update cache
or
invalidate affected queries
```

Do not manually maintain multiple copies of the same server data.

---

## 16. State Persistence

Persist state only when there is a clear requirement.

Examples:

```text
user preferences
selected settings
non-sensitive UI preferences
```

Do not persist sensitive information unnecessarily.

Never persist:

```text
passwords
private keys
server secrets
API secrets
```

---

## 17. Performance

Avoid unnecessary state updates.

Do not:

- store derived values unnecessarily;
- create global state for local values;
- trigger broad re-renders without need;
- duplicate server data;
- refetch data unnecessarily.

Use memoization only when it provides a real benefit.

Do not use:

```javascript
useMemo()
useCallback()
React.memo()
```

everywhere without a performance reason.

---

## 18. State and Components

Components should consume state through clear interfaces.

Prefer:

```text
Component
    ↓
Hook
    ↓
State / Server State
```

Avoid components directly manipulating complex global stores unless that is the established project pattern.

Refer to `COMPONENTS.md` for component responsibilities.

---

## 19. State and API Integration

Keep API communication separate from state presentation.

Preferred:

```text
Component
    ↓
Custom Hook
    ↓
Query / Mutation
    ↓
API Service
    ↓
HTTP Client
    ↓
Backend
```

Do not combine API implementation and UI state logic into one large component.

---

## 20. State Review Checklist

Before completing state-related code:

```text
[ ] State category identified
[ ] Correct state owner identified
[ ] Local state used where appropriate
[ ] Server state uses the standard server-state solution
[ ] Global state used only when necessary
[ ] URL state used where appropriate
[ ] No unnecessary duplicated state
[ ] Derived state is not unnecessarily stored
[ ] State updates are immutable
[ ] Loading/error/empty states handled
[ ] Mutations update or invalidate affected state
[ ] No unnecessary persistence
[ ] No unnecessary performance optimization
```

---

## 21. Core Rules

> **Keep state as close as possible to where it is used.**

> **Do not put everything into global state.**

> **Treat backend data as server state.**

> **Use the project's standard server-state solution for API data.**

> **Use URL state for shareable and navigational state where appropriate.**

> **Do not duplicate the same source of truth.**

> **Do not store derived values unnecessarily.**

> **Never mutate React state directly.**

> **Persist state only when there is a clear requirement.**

> **Choose the simplest state solution that satisfies the requirement.**
