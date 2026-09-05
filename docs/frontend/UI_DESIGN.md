Frontend UI Design Rules
Purpose
This document defines the core rules for creating, implementing, and modifying the ProspectSoul frontend UI.

Before creating a new screen, implementing a design, redesigning an existing screen, or making visual changes, Claude Code must follow these rules.

1. Existing Design First
Before changing an existing UI:

Inspect the existing screen.

Identify existing design patterns.

Reuse existing components and styles.

Preserve the established visual language.

Change only what the requirement asks for.

Do not redesign unrelated parts of the application.

2. Design Consistency
All new UI must look like it belongs to the existing application.

Maintain consistency in:

Colors

Typography

Spacing

Borders

Radius

Shadows

Icons

Buttons

Inputs

Tables

Cards

Navigation

Layout

Do not introduce arbitrary visual styles.

3. Design System
Use the project's existing design system and component library when available.

Prefer:

text
Existing component
        ↓
Reuse / extend
        ↓
New component only when necessary
Do not create duplicate versions of existing components.

Avoid hard-coding design values when project design tokens or variables already exist.

4. Layout
Use a consistent layout system.

Maintain:

Clear visual hierarchy

Consistent spacing

Proper alignment

Predictable content widths

Appropriate whitespace

Responsive layouts

Avoid arbitrary positioning unless the design specifically requires it.

Prefer layout systems such as:

text
Flexbox
CSS Grid
Existing layout components
5. Spacing
Use the project's established spacing scale.

Do not randomly use different spacing values for similar UI elements.

Related elements should have consistent spacing.

Example:

text
Page
 ├── Header
 ├── Filters
 ├── Content
 └── Pagination
should follow a predictable spacing hierarchy.

6. Typography
Use the project's existing typography system.

Maintain consistency in:

text
Font family
Font size
Font weight
Line height
Letter spacing
Use typography hierarchy for:

text
Page title
Section title
Body text
Labels
Helper text
Captions
Do not introduce new fonts without an explicit requirement.

7. Colors
Use existing design-system colors whenever available.

Maintain semantic meaning for colors:

text
Primary
Success
Warning
Error
Info
Neutral
Do not use arbitrary colors for individual components.

Do not use color as the only indicator of status or meaning.

8. Components
Prefer reusable components.

Examples:

text
Button
Input
Select
Modal
Table
Card
Badge
Tabs
Dropdown
Pagination
Alert
Loading
EmptyState
ErrorState
If an existing component already satisfies the requirement, reuse it.

If a new reusable pattern is required, add it to the appropriate component location instead of implementing one-off copies.

9. Responsive Design
Every new screen must consider:

text
Desktop
Tablet
Mobile
Use responsive layouts rather than fixed dimensions where possible.

Check:

Navigation

Tables

Forms

Cards

Modals

Filters

Buttons

Text wrapping

Content overflow

Do not allow horizontal overflow unless intentionally required.

10. Accessibility
Accessibility is mandatory.

Use:

Semantic HTML

Proper labels

Keyboard navigation

Visible focus states

Accessible buttons

Appropriate contrast

Meaningful alt text

ARIA only when necessary

Do not use clickable <div> elements when a semantic <button> or link is appropriate.

11. Forms
Forms must have:

Clear labels

Consistent input styling

Validation feedback

Required-field indication

Loading/submitting state

Error state

Success behavior

Validation messages should appear close to the relevant field.

Follow the backend API contract for submitted fields.

12. Tables and Data Views
Tables must provide clear:

Column hierarchy

Alignment

Row spacing

Sorting indicators

Pagination

Loading state

Empty state

Error state

Large datasets should use server-side pagination rather than rendering everything at once.

13. Loading States
Do not leave the interface visually empty while data is loading.

Use the project's standard:

text
Skeleton
Spinner
Loading indicator
Loading states should preserve the layout where possible to avoid unnecessary layout shifts.

14. Empty States
Empty states should clearly explain what happened.

Example:

text
No companies found.

Try changing your search or filters.
Differentiate an empty result from an API error.

15. Error States
Errors should be visually clear and actionable.

Where appropriate provide:

text
Error message
Retry action
Relevant guidance
Do not expose technical backend details to users.

16. UX
UI should be predictable and easy to understand.

Important actions should have clear visual hierarchy.

Avoid:

unnecessary animations

excessive decoration

confusing navigation

inconsistent button behavior

unexpected layout changes

excessive modals

Design should prioritize usability over visual complexity.

17. Design Implementation
When implementing a provided design:

text
Understand design
      ↓
Inspect existing components
      ↓
Map design to existing system
      ↓
Implement
      ↓
Check responsive behavior
      ↓
Compare with intended design
      ↓
Fix inconsistencies
Reproduce the intended:

Layout

Hierarchy

Spacing

Typography

Colors

Components

Interactions

Do not approximate the design unnecessarily.

18. Figma / Design References
When a Figma or other design reference is provided:

Inspect the complete relevant design.

Follow the intended layout.

Reuse existing project components where appropriate.

Match spacing and hierarchy accurately.

Preserve responsive intent.

Do not invent unrelated UI.

If the design conflicts with the existing design system, follow the explicit project requirement and avoid changing unrelated screens.

19. Existing UI Changes
When modifying an existing screen:

text
Requirement
    ↓
Identify affected component
    ↓
Make minimum required change
    ↓
Preserve existing behavior
    ↓
Check related states
Do not perform unrelated refactoring during a UI change unless required.

20. Performance
UI should remain performant.

Avoid:

unnecessary re-renders

huge DOM trees

unoptimized large images

unnecessary animations

expensive calculations during render

Use appropriate lazy loading, virtualization, and memoization when there is a real need.

Do not optimize prematurely.

21. UI Review Checklist
Before completing a UI task:

text
[ ] Existing design inspected
[ ] Existing components reused where appropriate
[ ] Layout is consistent
[ ] Spacing is consistent
[ ] Typography follows the design system
[ ] Colors follow the design system
[ ] Responsive behavior checked
[ ] Accessibility considered
[ ] Loading state handled
[ ] Empty state handled
[ ] Error state handled
[ ] No unnecessary duplicate components
[ ] No unrelated UI changes
[ ] Design compared against the intended result
22. Core Rules
Inspect the existing UI before making design changes.

Reuse the existing design system and components.

New UI must visually belong to the existing application.

Do not introduce arbitrary colors, fonts, spacing, or component styles.

Do not redesign unrelated parts of the application.

Implement provided designs accurately rather than approximating them.

Every new UI must consider responsive behavior.

Accessibility is mandatory.

Prefer reusable components over one-off implementations.

Keep UI changes focused on the requested requirement.

23. Recommended UI Component Library
The ProspectSoul frontend should use shadcn/ui as the primary component library.

Why shadcn/ui
shadcn/ui is the recommended choice because it aligns perfectly with our design philosophy:

Total Control: Components are copy-pasted into your codebase, not installed as a dependency. You own and control every component.

No Version Lock-in: Since you own the code, you're never forced to upgrade or fight against library updates.

Full Customizability: Every component can be modified to match our exact design system without fighting pre-built styles.

Accessibility First: All components are built with Radix UI primitives and are fully accessible out of the box.

Tailwind CSS Integration: Seamless integration with Tailwind CSS, which is our chosen styling solution.

Modern Development Experience: Excellent TypeScript support and developer experience.

Alternative Options
If a different approach is needed, these are acceptable alternatives:

Library	Best For
Ant Design	Enterprise dashboards, complex admin panels, data-heavy applications
Chakra UI	MVPs, accessible consumer apps, rapid prototyping
Mantine	SaaS products, modern applications with extensive component needs
Material UI (MUI)	Projects requiring Material Design compliance
Decision Guidelines
Default: Use shadcn/ui for all new development.

Enterprise Dashboard: Consider Ant Design if building a complex admin panel with data tables and forms.

MVP / Prototype: Consider Chakra UI for fast development with accessibility built-in.

Material Design Requirement: Use Material UI (MUI) if Material Design is explicitly required.

Component Implementation Rule
When implementing any UI component:

text
Check if shadcn/ui has the component
        ↓
    YES → Use shadcn/ui component
        ↓
    NO → Check if existing custom component exists
        ↓
    YES → Reuse/extend existing component
        ↓
    NO → Build new component following design rules
24. Design Tokens
The following design tokens must be used consistently throughout the application:

Colors
text
Primary: #3B82F6 (Blue)
Success: #10B981 (Green)
Warning: #F59E0B (Amber)
Error: #EF4444 (Red)
Info: #6366F1 (Indigo)
Neutral: #6B7280 (Gray)
Typography
text
Font Family: Inter (default), system-ui fallback
Font Sizes: 12px, 14px, 16px, 18px, 20px, 24px, 30px, 36px, 48px
Font Weights: 400 (regular), 500 (medium), 600 (semibold), 700 (bold)
Spacing Scale
text
0: 0px
1: 4px
2: 8px
3: 12px
4: 16px
5: 20px
6: 24px
7: 32px
8: 40px
9: 48px
10: 56px
11: 64px
12: 80px
13: 96px
14: 112px
15: 128px
Border Radius
text
none: 0px
sm: 2px
md: 4px
lg: 8px
xl: 12px
2xl: 16px
full: 9999px
Shadows
text
sm: 0 1px 2px rgba(0,0,0,0.05)
md: 0 4px 6px rgba(0,0,0,0.07)
lg: 0 10px 15px rgba(0,0,0,0.10)
xl: 0 20px 25px rgba(0,0,0,0.15)
2xl: 0 25px 50px rgba(0,0,0,0.25)
25. Implementation Checklist for New Screens
When creating a new screen:

text
[ ] Checked shadcn/ui for available components
[ ] Reused existing custom components where appropriate
[ ] Followed design token standards (colors, spacing, typography)
[ ] Responsive for desktop, tablet, and mobile
[ ] Accessibility requirements met (semantic HTML, ARIA, keyboard nav)
[ ] Loading state implemented
[ ] Empty state implemented
[ ] Error state implemented
[ ] Performance considerations addressed
[ ] Code reviewed against UI Review Checklist
[ ] Design verified against intended reference
26. Final Design Principles
Consistency > Novelty - Familiar patterns are better than creative ones.

Simplicity > Complexity - Simple designs are easier to maintain and use.

Function > Decoration - Every visual element should serve a purpose.

Reusability > Duplication - Build once, use everywhere.

Accessibility > Convenience - Design for all users from the start.

Performance > Features - Fast loading is a feature itself.

User Testing > Assumptions - Validate design decisions with actual users.