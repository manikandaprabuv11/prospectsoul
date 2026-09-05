# Frontend Components Rules

## Purpose

This document defines the core rules for creating, organizing, and maintaining React components.

Before creating or modifying components, Claude Code must follow these rules.

---

## 1. Component Responsibilities

A component should have one clear responsibility.

Components should focus on:

- UI rendering
- User interaction
- Composition of child components

Do not put large amounts of business logic, API calls, or complex data processing directly inside UI components.

---

## 2. Component Organization

Use a clear separation between shared components and feature components.

Recommended structure:

```text
src/
├── components/
│   ├── common/
│   │   ├── Button/
│   │   ├── Input/
│   │   ├── Modal/
│   │   ├── Table/
│   │   └── Pagination/
│   │
│   └── layout/
│       ├── Header/
│       ├── Sidebar/
│       └── PageLayout/
│
└── features/
    ├── companies/
    │   └── components/
    ├── contacts/
    │   └── components/
    └── activities/
        └── components/
```

Use feature-specific components inside the relevant feature.

Use shared components only when they are genuinely reusable.

---

## 3. Component Naming

Use PascalCase for React component names.

Examples:

```text
CompanyCard
CompanyTable
CompanyForm
SearchInput
Pagination
PageHeader
```

Files should normally match the component name.

Example:

```text
CompanyCard.jsx
CompanyForm.jsx
Pagination.jsx
```

Avoid vague names such as:

```text
Thing.jsx
Common.jsx
Helper.jsx
Component1.jsx
```

---

## 4. Reusable Components

Create a reusable component when:

- the same UI is used in multiple places;
- the behavior is consistent;
- the component has a clear API;
- reuse improves maintainability.

Do not create abstractions only to avoid a few repeated lines.

Prefer simple, understandable components over overly generic components.

---

## 5. Shared Components

Shared components should remain domain-independent where possible.

Good:

```text
Button
Modal
Input
Table
Pagination
LoadingSpinner
EmptyState
ErrorState
```

Avoid putting business-specific behavior into common components.

For example, avoid making:

```text
CommonTable
```

know directly about:

```text
Company
Contact
Activity
```

Pass data and behavior through props.

---

## 6. Feature Components

Business-specific UI belongs to the relevant feature.

Example:

```text
features/
└── companies/
    └── components/
        ├── CompanyForm.jsx
        ├── CompanyTable.jsx
        ├── CompanyFilters.jsx
        └── CompanyDetails.jsx
```

Do not place feature-specific components into the global common component directory.

---

## 7. Props

Components should receive data and behavior through explicit props.

Example:

```jsx
<CompanyCard
  company={company}
  onSelect={handleSelect}
/>
```

Avoid hidden dependencies and unnecessary global state.

Props should have clear names and predictable types.

---

## 8. Prop Validation / Types

Use the project's standard type-safety approach.

If using TypeScript:

```typescript
type CompanyCardProps = {
  company: Company;
  onSelect: () => void;
};
```

If using JavaScript, use appropriate prop validation or JSDoc according to project conventions.

Do not leave complex component contracts undocumented.

---

## 9. Component State

Keep state as close as possible to where it is used.

Use local component state for:

```text
modal visibility
input state
selected tab
temporary UI state
```

Do not move every piece of state into global state.

Use server-state tools for API/server data.

---

## 10. API Calls

Components must not directly call:

```javascript
fetch()
axios.get()
axios.post()
```

Use:

```text
Component
    ↓
Hook
    ↓
API Service
    ↓
HTTP Client
```

Refer to `API_INTEGRATION.md` for API integration rules.

---

## 11. Custom Hooks

Move reusable stateful logic into custom hooks.

Example:

```text
useCompanies()
useCompany()
useCreateCompany()
useDebounce()
```

A component should not become a large collection of repeated effects and API logic.

---

## 12. Business Logic

Do not put significant business logic inside JSX.

Avoid:

```jsx
{items
  .filter(...)
  .map(...)
  .sort(...)
  .reduce(...)}
```

when the logic becomes complex.

Move reusable or complex logic into:

```text
hooks
utils
services
feature logic
```

Components should remain easy to read.

---

## 13. JSX

Keep JSX readable.

Avoid deeply nested conditional expressions and large inline functions.

Prefer extracting complex UI into smaller components.

Bad:

```jsx
return (
  <div>
    {condition ? (
      ...
    ) : anotherCondition ? (
      ...
    ) : anotherCondition ? (
      ...
    ) : (
      ...
    )}
  </div>
);
```

When UI logic becomes complex, split it into components or clear helper logic.

---

## 14. Loading States

API-driven components must handle loading states.

Example:

```text
Loading
    ↓
Success
```

Do not render incomplete data while the initial request is still loading.

Use shared loading components where appropriate.

---

## 15. Empty States

Differentiate:

```text
Loading
Empty
Error
Success
```

Example:

```text
Loading → spinner/skeleton

Empty → "No companies found"

Error → error message/retry

Success → data
```

Do not show an empty state while data is still loading.

---

## 16. Error States

Components that depend on API data must provide an appropriate error state.

Where possible, provide:

```text
clear message
retry action
```

Do not expose technical backend errors or stack traces to users.

---

## 17. Forms

Forms should have:

- clear field labels
- validation
- loading/submitting state
- error handling
- success handling
- accessible controls

Do not duplicate form validation logic unnecessarily.

Follow the backend API contract for submitted fields.

---

## 18. Accessibility

Components must be accessible by default.

Use:

- semantic HTML
- labels for inputs
- accessible buttons
- keyboard navigation
- meaningful focus states
- appropriate ARIA attributes when necessary

Do not use:

```html
<div onClick={...}>
```

as a replacement for a button when a real `<button>` is appropriate.

---

## 19. Performance

Avoid unnecessary rendering and expensive calculations.

Prefer:

- pagination
- virtualization for very large lists
- memoization only when useful
- stable props where appropriate
- efficient list rendering

Do not use `useMemo`, `useCallback`, or `React.memo` everywhere without a performance reason.

Optimization should solve an actual problem rather than add unnecessary complexity.

---

## 20. Lists

Always provide stable keys when rendering lists.

Prefer:

```jsx
items.map(item => (
  <CompanyRow key={item.id} />
))
```

Avoid using array indexes as keys when stable identifiers are available.

---

## 21. Component Composition

Prefer composition over overly complex components.

Instead of one large component:

```text
CompanyPage.jsx
```

prefer:

```text
CompanyPage
├── PageHeader
├── CompanyFilters
├── CompanyTable
│   └── CompanyRow
└── Pagination
```

This improves readability and maintainability.

---

## 22. Component Size

There is no mandatory line-count limit, but a component should be reviewed when it becomes difficult to understand.

Consider splitting when a component contains multiple independent responsibilities such as:

```text
data fetching
filter management
form management
table rendering
modal rendering
business logic
```

---

## 23. Reuse Before Duplication

Before creating a new component:

1. Search existing components.
2. Check shared components.
3. Check the relevant feature.
4. Determine whether an existing component can be reused.
5. Create a new component only when necessary.

Do not create duplicate components with slightly different names.

---

## 24. Styling

Follow the project's established styling system.

Do not introduce a new styling approach for one component without a clear reason.

Keep component styles close to the component when the project convention supports it.

Avoid excessive inline styles.

---

## 25. Component Dependencies

Keep dependencies explicit.

Avoid components that secretly depend on:

```text
global state
specific routes
specific API responses
browser globals
```

unless that dependency is part of the component's intended responsibility.

---

## 26. Testing

Important reusable and business-critical components should have tests.

Test:

- rendering
- user interaction
- important states
- validation
- error handling
- accessibility behavior where appropriate

Prefer testing user-visible behavior rather than implementation details.

---

## 27. Component Review Checklist

Before completing a component:

```text
[ ] Responsibility is clear
[ ] Correct feature/common location
[ ] Naming follows convention
[ ] Props are explicit
[ ] State is kept at the correct level
[ ] API calls are not inside components
[ ] Complex logic is extracted
[ ] Loading state handled
[ ] Empty state handled
[ ] Error state handled
[ ] Accessibility considered
[ ] List keys are stable
[ ] No unnecessary re-render optimization
[ ] Existing components were checked for reuse
[ ] Tests added where appropriate
```

---

## 28. Core Rules

> **One component should have one clear responsibility.**

> **Keep feature components inside their feature.**

> **Keep shared components domain-independent.**

> **Do not call APIs directly from components.**

> **Use hooks for reusable stateful logic.**

> **Keep complex business logic outside JSX.**

> **Reuse existing components before creating new ones.**

> **Do not optimize prematurely.**

> **Accessibility is part of component quality.**

> **Components should remain simple, readable, reusable, and maintainable.**
