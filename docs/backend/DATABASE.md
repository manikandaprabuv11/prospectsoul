# ProspectSoul Backend — DATABASE.md

## Purpose

This document defines the database engineering rules and regulations for the ProspectSoul Spring Boot backend.

It is a persistent reference for developers and Claude Code when designing, changing, testing, or reviewing the PostgreSQL database.

The backend architecture uses feature-first modules, Spring Data JPA repositories, DTO-first APIs, and migration-controlled database changes. fileciteturn2file10L1-L40

---

# 1. Core Database Principles

The database must be:

- consistent
- integrity-protected
- migration-driven
- query-efficient
- auditable where required
- safe for concurrent access
- compatible with JPA/Hibernate
- designed for long-term evolution

> **The database is the authoritative persistence layer. Business logic belongs in the service/domain layer, while database constraints enforce data integrity.**

Never rely only on frontend validation or Java validation for critical database integrity.

---

# 2. Database Technology

Use the project-approved relational database stack.

For ProspectSoul:

```text
PostgreSQL
Spring Data JPA
Hibernate
Flyway
```

Do not introduce another database technology without an explicit architectural decision.

---

# 3. Database Ownership

Every table must represent a clear business or technical concept.

Prefer feature-aligned tables such as:

```text
company
contact
activity
evidence
research
qualification
imports
```

Avoid arbitrary tables without a clear owner or purpose.

---

# 4. Naming Convention

Use:

```text
snake_case
```

Examples:

```text
company_id
company_name
created_at
updated_at
created_by
```

Do not mix:

```text
companyName
CompanyName
COMPANY_NAME
```

with the project's standard.

---

# 5. Primary Keys

Every persistent business entity must have a stable primary key.

Do not use mutable business fields such as:

```text
company_name
email
phone_number
```

as primary keys unless explicitly justified by the domain.

Prefer the project's standard generated identifier strategy.

For new entities, UUIDs are preferred when externally exposed identifiers and distributed-safe IDs are required.

Do not mix ID strategies arbitrarily.

---

# 6. Foreign Keys

Use database foreign keys for real relational dependencies.

Example:

```text
company_id
    ↓
company.id
```

Do not rely exclusively on application code to maintain referential integrity.

Foreign key names should normally follow:

```text
<referenced_table>_id
```

Examples:

```text
company_id
contact_id
user_id
```

---

# 7. Relationships

Before creating a relationship, determine:

- cardinality
- ownership
- lifecycle
- optionality
- deletion behavior
- query patterns

Do not create relationships merely because two concepts appear related.

---

# 8. Many-to-Many Relationships

Avoid anonymous many-to-many relationships when the relationship itself contains business data.

Prefer an explicit relationship table/entity.

Example:

```text
company_contact
├── id
├── company_id
├── contact_id
├── relationship_type
└── created_at
```

This provides room for business attributes and auditing.

---

# 9. Nullability

Use `NOT NULL` when the domain requires a value.

Example:

```sql
name VARCHAR(255) NOT NULL
```

Do not make every column nullable for convenience.

At the same time, do not make legitimately optional values mandatory.

Nullability must represent business meaning.

---

# 10. Unique Constraints

Use database-level unique constraints when a value must be unique.

Example:

```text
UNIQUE(source, external_id)
```

Do not depend only on:

```text
SELECT → check → INSERT
```

because concurrent requests can still create duplicates.

Before adding uniqueness, identify the actual business scope of the uniqueness rule.

---

# 11. Indexing

Indexes must support real query patterns.

Consider indexes for:

- foreign keys
- frequently filtered columns
- frequently sorted columns
- unique lookup fields
- external IDs
- high-selectivity search fields

Do not index every column.

Every index has:

- storage cost
- write cost
- maintenance cost

---

# 12. Composite Indexes

Use composite indexes when queries commonly filter on multiple columns together.

Example:

```text
(source, external_id)
```

Column order must reflect expected query patterns.

Do not create arbitrary composite indexes.

---

# 13. Search

Use appropriate PostgreSQL search mechanisms for the expected workload.

For large text searches, consider:

```text
PostgreSQL full-text search
pg_trgm
appropriate indexes
```

Do not blindly use:

```sql
LIKE '%term%'
```

against large tables without considering performance and indexing.

---

# 14. Pagination

Large datasets must use server-side pagination.

For ProspectSoul data-heavy features:

```text
search
filter
sort
pagination
```

should be handled efficiently at the database/repository layer.

The backend architecture already recommends specifications for complex dynamic filtering and pagination. fileciteturn2file15L1-L20

Never load an entire large table merely to paginate in Java.

---

# 15. Query Design

Every query should have a clear purpose.

Avoid loading complete entity graphs when only a few fields are required.

Use projections or summary queries where appropriate.

Avoid unnecessary:

```sql
SELECT *
```

for large or performance-sensitive queries.

---

# 16. N+1 Queries

Be alert for JPA N+1 query problems.

Do not solve N+1 problems by making every relationship:

```java
FetchType.EAGER
```

The existing backend architecture explicitly requires deliberate decisions about fetch strategy and relationships. fileciteturn2file0L1-L35

Prefer targeted solutions such as:

```text
fetch join
EntityGraph
projection
optimized repository query
```

when justified.

---

# 17. Transactions

Transaction boundaries belong primarily at the service/application layer.

Typical flow:

```text
Controller
    ↓
Service @Transactional
    ↓
Repository
    ↓
Database
```

Use read-only transactions for suitable read operations when beneficial.

Do not put transaction management randomly in controllers.

---

# 18. Transaction Scope

A transaction should represent a meaningful business operation.

Avoid keeping a database transaction open while waiting for:

- external APIs
- AI providers
- long-running file processing
- network operations

Separate database operations from slow external calls when possible.

---

# 19. Concurrency

Consider concurrent updates to important business records.

Use optimistic locking where appropriate:

```text
version
```

Do not assume two users cannot update the same record simultaneously.

Use pessimistic locking only when there is a demonstrated requirement.

---

# 20. Data Integrity

Important rules should be enforced at the appropriate layers:

```text
DTO validation
      +
Service/domain validation
      +
Database constraints
```

Example:

```text
required field
    → DTO validation

business rule
    → Service

foreign-key integrity
    → Database

uniqueness
    → Database
```

Do not place every rule in only one layer.

---

# 21. Audit Fields

For important business tables, consider:

```text
created_at
updated_at
created_by
updated_by
```

Use the project's established auditing strategy.

Do not add unnecessary audit fields to purely technical tables.

---

# 22. Timestamp Rules

Use a consistent timestamp strategy.

Prefer timezone-aware timestamps for distributed systems and persist timestamps consistently, normally using UTC semantics.

Do not arbitrarily mix:

```text
local time
UTC
timestamp without timezone
timestamp with timezone
```

---

# 23. Soft Delete

Do not introduce soft delete automatically.

Use it only when the business requires retaining records while marking them inactive.

If required, use one consistent mechanism, for example:

```text
deleted_at
```

or:

```text
deleted
```

All relevant queries must consistently respect the deletion state.

---

# 24. Status Fields

Status values must be controlled.

Example:

```text
status
```

The valid values should be defined consistently across:

```text
database
backend domain
DTOs
frontend
```

Do not allow arbitrary status strings when the domain has a finite state model.

---

# 25. Database Enums

Do not automatically use PostgreSQL enum types.

Choose deliberately between:

```text
VARCHAR + validation
CHECK constraint
lookup/reference table
database enum
```

Consider:

- change frequency
- migration complexity
- query requirements
- maintainability
- project conventions

---

# 26. JPA Entity Mapping

Database schema and JPA mappings must remain aligned.

Use deliberate mappings for:

```text
@Id
@GeneratedValue
@Column
@JoinColumn
@OneToMany
@ManyToOne
@OneToOne
@ManyToMany
```

Be deliberate about:

- ownership
- fetch type
- cascade
- orphan removal
- foreign keys

The backend architecture explicitly warns against blindly using `CascadeType.ALL` and `FetchType.EAGER`. fileciteturn2file0L20-L35

---

# 27. Cascade Rules

Use cascade operations only when the child lifecycle is genuinely owned by the parent.

Do not blindly use:

```java
CascadeType.ALL
```

Ask:

> If the parent is deleted, should this child really be deleted automatically?

If the answer is not clearly yes, do not use cascading deletion.

---

# 28. Orphan Removal

Use:

```java
orphanRemoval = true
```

only when the child cannot meaningfully exist independently from its parent.

Do not use orphan removal merely for convenience.

---

# 29. Lazy Loading

Prefer lazy loading where appropriate.

Do not access lazy relationships outside the correct persistence boundary.

Do not turn relationships eager just to fix a serialization or query problem.

Solve the actual query/design problem.

---

# 30. Schema Generation

Production schema must be controlled by migrations.

Do not use automatic Hibernate schema generation as the production source of truth.

Avoid relying on:

```text
hibernate.ddl-auto=create
hibernate.ddl-auto=create-drop
```

for production.

Flyway migrations are the authoritative schema history.

---

# 31. Migration Location

All database migrations belong under:

```text
src/main/resources/db/migration/
```

The existing backend file structure explicitly defines this location. fileciteturn2file10L1-L40

---

# 32. Migration Naming

Use:

```text
V<version>__<description>.sql
```

Examples:

```text
V1__initial_schema.sql
V2__create_company_table.sql
V3__create_contact_table.sql
V4__add_company_indexes.sql
```

Migration names must clearly describe their purpose.

---

# 33. Never Modify Applied Migrations

Once a migration has been applied to a shared environment:

> **Never edit the migration file.**

Create a new migration.

Example:

```text
V5__add_company_source.sql
```

Do not modify:

```text
V2__create_company_table.sql
```

after it has been applied.

The existing backend rules explicitly require a new migration for schema changes. fileciteturn2file2L1-L20

---

# 34. Migration Determinism

A fresh database must be buildable from the complete migration history.

Migrations must not depend on:

- manual SQL
- undocumented database changes
- developer-specific state
- external scripts not part of the deployment process

---

# 35. Schema vs Data Migrations

Separate structural changes from large data transformations when practical.

Example:

```text
V6__add_source_column.sql
V7__backfill_company_source.sql
V8__make_source_not_null.sql
```

This makes changes easier to validate and roll out safely.

---

# 36. Destructive Changes

Treat these as high-risk:

```text
DROP TABLE
DROP COLUMN
large DELETE
ALTER COLUMN TYPE
```

Before destructive changes:

- inspect dependencies
- inspect application usage
- inspect existing data
- consider backups
- consider rollback/recovery
- consider phased migration
- verify production impact

Never perform destructive changes casually.

---

# 37. Safe Column Renames

For important production tables, consider a phased approach:

```text
add new column
    ↓
backfill
    ↓
update application
    ↓
verify
    ↓
remove old column later
```

This is safer than immediately renaming a heavily used column in a live system.

---

# 38. Large Backfills

Do not blindly update millions of rows in one transaction.

For large data migrations:

- batch operations
- monitor execution time
- control transaction size
- consider locks
- verify indexes
- monitor production impact

---

# 39. Repository Rules

Repositories are the persistence boundary.

Feature repositories belong inside the relevant feature:

```text
company/repository/
contact/repository/
imports/repository/
```

Use Spring Data JPA repositories such as:

```java
JpaRepository
```

The existing backend architecture defines repositories as the persistence layer. fileciteturn2file0L1-L20

Business logic must remain in services/domain logic.

---

# 40. Dynamic Filtering

For complex dynamic search/filter/sort requirements, use feature-specific specifications where appropriate.

Example:

```text
company/specification/CompanySpecification.java
```

Preferred flow:

```text
Controller
    ↓
Search Request DTO
    ↓
Service
    ↓
Specification / Repository
    ↓
Database
```

Do not put complex query construction in controllers.

---

# 41. Native SQL

Prefer Spring Data JPA/JPQL when sufficient.

Use native SQL only when there is a clear reason, such as:

- PostgreSQL-specific functionality
- complex performance-sensitive queries
- full-text search
- database-specific features

Document the reason when native SQL is non-obvious.

---

# 42. Projections

Use projections for large list/search responses when appropriate.

Example:

```text
CompanySummaryProjection
CompanySummaryResponse
```

Avoid loading large entity graphs when a small set of columns is sufficient.

---

# 43. API and Database Separation

Never expose database entities directly through REST.

Use:

```text
Database
    ↓
JPA Entity
    ↓
Mapper
    ↓
Response DTO
    ↓
Controller
```

For incoming data:

```text
Request DTO
    ↓
Service
    ↓
Entity
    ↓
Repository
```

This follows the project's DTO-first backend rules. fileciteturn2file1L1-L20

---

# 44. Sensitive Data

Do not store sensitive data unless there is a real business requirement.

Before adding a sensitive column, determine:

- why it is needed
- who can access it
- retention requirements
- whether encryption is required
- whether it can appear in logs
- whether it should be returned through DTOs

---

# 45. Secrets

Never commit:

```text
database passwords
production credentials
API keys
private connection strings
```

Use environment variables or the project's secret-management system.

Example:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
```

---

# 46. Backups and Recovery

Production databases require an appropriate:

```text
backup
restore
recovery
```

strategy.

Database migrations are not a substitute for backups.

High-risk schema changes must consider recovery before execution.

---

# 47. Local Development Database

Use the project's standard local database environment.

If PostgreSQL is managed by Docker Compose, use that project configuration rather than creating unrelated local database configurations.

Keep local credentials outside committed secrets.

---

# 48. Integration Testing

Database-sensitive behavior must be tested against a real or representative PostgreSQL environment where practical.

Test:

- migrations
- JPA mappings
- constraints
- foreign keys
- unique constraints
- repository queries
- pagination
- filtering
- transactions
- concurrency-sensitive behavior where applicable

Do not rely exclusively on mocked repositories.

---

# 49. Database Review Checklist

Before merging a database change:

```text
[ ] Current schema inspected
[ ] Existing migrations inspected
[ ] Requirement understood
[ ] Naming convention followed
[ ] Primary key reviewed
[ ] Foreign keys reviewed
[ ] Nullability reviewed
[ ] Unique constraints reviewed
[ ] Indexes reviewed
[ ] Relationships reviewed
[ ] Audit fields reviewed
[ ] Timestamp strategy reviewed
[ ] JPA mappings updated
[ ] Repository queries reviewed
[ ] Transaction boundaries reviewed
[ ] Existing data impact reviewed
[ ] Migration created
[ ] Migration tested
[ ] Tests added/updated
[ ] Clean database verified
[ ] Upgrade of existing database verified
[ ] Destructive impact reviewed
[ ] Documentation updated if required
```

---

# 50. Before Every Database Change

Claude must first:

1. Read `DATABASE.md`.
2. Read relevant PRD/domain/architecture documentation.
3. Inspect existing entities.
4. Inspect existing migrations.
5. Inspect repositories.
6. Inspect related DTOs.
7. Inspect existing constraints.
8. Inspect existing indexes.
9. Check whether the table/column already exists.
10. Determine whether the change is additive, corrective, or destructive.

Do not create duplicate schema objects because the existing schema was not inspected.

---

# 51. Database Change Workflow

Use:

```text
Understand requirement
        ↓
Inspect existing schema
        ↓
Inspect domain model
        ↓
Design schema change
        ↓
Review constraints/indexes
        ↓
Create migration
        ↓
Update JPA entity
        ↓
Update repository
        ↓
Update service/DTOs if required
        ↓
Add tests
        ↓
Run migration
        ↓
Run integration tests
        ↓
Review final diff
```

---

# 52. Definition of Done

A database-related change is complete only when:

```text
[ ] Requirement implemented
[ ] Existing schema inspected
[ ] Migration created
[ ] Migration naming correct
[ ] Migration deterministic
[ ] No applied migration modified
[ ] Primary keys correct
[ ] Foreign keys correct
[ ] Nullability correct
[ ] Unique constraints correct
[ ] Indexes considered
[ ] JPA mapping updated
[ ] Repository updated
[ ] Transactions reviewed
[ ] Data migration considered
[ ] Destructive impact reviewed
[ ] Tests pass
[ ] Clean database migration verified
[ ] Existing database upgrade verified
[ ] Documentation updated where necessary
```

---

# 53. Most Important Rule

> **Never change the database by guessing.**

Before creating or modifying a table, column, relationship, index, constraint, or migration:

```text
Inspect
    ↓
Understand
    ↓
Design
    ↓
Migrate
    ↓
Map
    ↓
Test
    ↓
Verify
```

The migration history must remain the authoritative, reviewable record of database evolution.

---

## Final Database Architecture

```text
Spring Boot
    │
    ├── Controller
    │       ↓
    │   Request DTO
    │       ↓
    ├── Service
    │       ↓
    ├── Repository
    │       ↓
    ├── JPA Entity
    │       ↓
    └── PostgreSQL
            ↑
       Flyway Migration
```

The database layer must remain:

**migration-driven + constraint-aware + query-efficient + JPA-compatible + testable + secure + maintainable.**
