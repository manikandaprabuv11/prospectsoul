<!--
Document: 18-Verification-Implementation-Notes-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: As-built
Scope: Company Verification Module (Twilio Lookup)
Audience: Engineering, QA
Position in doc set: docs/dev_docs/18 — the as-built record for the module defined in
13 (Requirements), 14 (Technical Specification), 15 (Implementation Plan),
16 (Kickoff Prompt) and 17 (UI/UX Design). Records the Phase 1 gap assessment and
every decision taken where the specifications were silent or disagreed with the
repository as it actually exists.
-->
# ProspectSoul — Verification Module Implementation Notes
**Version:** 1.0 · **Status:** As-built · **Module:** Verify
**Doc index:** 18 of the `docs/dev_docs` set

## 1. Phase 1 gap assessment

What the repository already had, and what the module therefore reused rather than rebuilt.

| Area | Found | Consequence for this module |
|---|---|---|
| **JSON casing** | `JacksonConfig` sets `PropertyNamingStrategies.SNAKE_CASE` globally; `application.yaml` repeats it. Existing tests assert `canonical_name`, `total_elements`. | Every Verify DTO is declared in camelCase and serialised as `snake_case`. Doc 14 §8's camelCase examples are presentation only, as that section itself notes. The Twilio payload is mapped with explicit `@JsonProperty`, so a change to the app-wide strategy cannot break provider parsing. |
| **Company verification state** | `companies.verification_status` (`UNVERIFIED` / `VERIFIED` / `INVALIDATED`), `verified_by VARCHAR(255)`, `verified_at`. `CompanyService.verify()` already exists behind `POST /api/v1/companies/{id}/verify`. Editing a core field drops verification to `INVALIDATED`. | Canonical state is reused untouched. No parallel verification field was added. `INVALIDATED` is treated as eligible for re-verification. |
| **Actor representation** | Every actor column (`companies.created_by`, `verified_by`, `audit_log.actor`) is `VARCHAR(255)` holding the Keycloak subject via `CurrentUser.id(auth)`. There is **no** actor-type column anywhere. | See §3.1 — the user/system distinction is recorded on the activity and in the audit action rather than by inventing a column. |
| **Audit** | `AuditService.record(entityType, entityId, actor, action, previousState, newState)`, `@Transactional(propagation = MANDATORY)`. | Reused as-is. `MANDATORY` is what forces every Verify mutation to write its audit row inside the same transaction. |
| **Activities** | **Missing.** PRD v1.1 §10 specifies an `activities` table and §3.6 defines the `VERIFICATION` type, but no table, entity or service existed. | See §3.2 — a minimal `activity` module was built, because doc 14 §12 makes it a hard dependency. |
| **Phone normalization** | `PhoneNormalizer` reduces to 10 national digits, strips `+91`/`0091`/leading `0`/punctuation, and validates 10 digits. Exhaustively unit-tested. | Reused verbatim. `E164PhoneFormatter` only prefixes the configured dialling code, since Lookup requires E.164. |
| **Async / background work** | `AsyncConfig` with `@EnableAsync` and an import executor. `ImportProcessingService` runs `@Async` with per-row transactions and a `@Lazy self` reference. **No** `@EnableScheduling` and **no** existing `SKIP LOCKED` queue. | The worker is the first Spring-scheduled component; `@EnableScheduling` lives in `VerificationConfig`. The `@Lazy self` pattern was reused — see §3.5. |
| **Pagination and errors** | `PageResponse.from(Page)` gives `{content, page, size, total_elements, total_pages}`. `GlobalExceptionHandler` maps `ResourceNotFoundException`→404, `ConflictException`→409, `BusinessRuleException`→422, validation→400 with `errors[]`, `IllegalArgumentException`→400, all `application/problem+json`. | Reused. No new exception type and no new error shape. |
| **RBAC** | `RoleConstants.HAS_READ` / `HAS_MUTATE` and `@PreAuthorize`. `@EnableMethodSecurity` is on `SecurityConfig`, which is `@Profile("!dev")`. | Reused. Note the pre-existing gap: the `dev` profile's `DevSecurityConfig` does **not** enable method security, so `@PreAuthorize` is inert in `dev`. Out of scope here, flagged in §5. |
| **Flyway** | V1–V6 applied; `ddl-auto: validate`. | Added V7 and V8; nothing existing was edited. |
| **Frontend** | Central `apiClient` with token provider and `ApiError`/`ProblemDetail` normalisation; shared `queryClient`; feature-first `src/features/<name>/{api,components,hooks,pages,types}`; `usePermissions()`; sidebar driven by a `navItems` array; shadcn/ui with `badge`, `button`, `card`, `input`, `label`, `select`, `separator`, `skeleton`, `textarea`. | Reused. No second HTTP client, no second query client, no Redux. `checkbox`, `dialog` and `progress` were added to `components/ui/` — see §3.7. |

## 2. What was built

- Migrations `V7__create_activities.sql`, `V8__create_verification_tables.sql`.
- `activity/` — minimal: entity, repository, service, mapper, response DTO.
- `verification/` — `config`, `controller`, `dto/{request,response}`, `entity`, `mapper`,
  `provider`, `repository`, `service`, `specification`, `worker`.
- Frontend `features/verification/` — `api`, `components`, `hooks`, `pages`, `types`; plus the
  `/verify` and `/verify/:id` routes and the sidebar entry.

## 3. Decisions taken where the specifications were silent or disagreed

### 3.1 Actor type: the requester is recorded, the automation is described

Doc 14 §11 requires the distinction between the user who requested the batch and the
system that performed the check, and says to document the mapping rather than invent a
convention. The schema has no actor-type column.

**Decision.** `companies.verified_by` records the **requesting user's** Keycloak subject —
they are accountable for the decision to verify. The automated nature is recorded twice
alongside it:

- the `VERIFICATION` activity content carries `automated: true`, `method: PHONE_LOOKUP`,
  the provider, the provider reference, the line type, `proves:
  PHONE_VALIDITY_AND_LINE_TYPE` and `ownership_verified: false`;
- the audit action is `VERIFY_AUTOMATED`, deliberately distinct from the `VERIFY` action
  the manual endpoint writes.

**Why not a system actor id.** Writing a synthetic `system` value into `verified_by` would
lose accountability and break the existing `verified_by`-is-a-subject contract that the
verified-companies table and the user directory both rely on.

### 3.2 The `activities` table was created

Doc 14 §12 and doc 16 both require `VERIFICATION` activities and forbid a second timeline
store. No `activities` table existed. Doc 16's scope control permits a strictly necessary
minimal implementation but asks for it to be proposed rather than built silently — this
section is that proposal, recorded rather than blocking the run.

**Decision.** `V7` creates `activities` exactly as PRD v1.1 §10 specifies it (id,
company_id, type, content jsonb, ai_provider, ai_model, prompt_version, verified,
created_by, created_at), with the five PRD §3.6 types. Only what Verify needs is wired: a
write path (`ActivityService.record`, `MANDATORY` like `AuditService`) and read queries for
timeline composition. `attachments` and `evidence` — separate tables in PRD §10 — were
**not** created; they belong to the research module.

`activity.verified` is left `false` for verification activities: the column means
"a human has verified these claims", and doc 13 §7 is explicit that a Lookup proves line
validity, not ownership or a human conversation. Marking it `true` would overstate it.

### 3.3 Column types: `VARCHAR(255)`, not `UUID`

Doc 14 §3 specifies `verification_batches.requested_by UUID` and `filter_added_by UUID`.

**Decision.** Both are `VARCHAR(255)`. Every actor column already in this schema is
`VARCHAR(255)`, and `filter_added_by` is compared directly against
`companies.created_by`. Typing it `UUID` would force a cast on the hot filter path and
break consistency with `audit_log.actor`.

### 3.4 One endpoint was added: `GET /api/v1/verifications/added-by-options`

Doc 17 §3 requires an "Added By: All / Mani / Kumar" dropdown. No endpoint reachable by the
Analyst, Viewer or COO roles can populate it — the user directory
(`/api/v1/admin/users`) is Admin-only.

**Decision.** Added a read-only endpoint inside the verification module, behind
`HAS_READ`, returning the distinct actors present in `companies.created_by` with their
display names and company counts. It is additive, derives its options from the values the
filter can actually match, and changes no existing contract. The seven endpoints named in
doc 14 §8 are all implemented exactly as specified.

### 3.5 Self-invocation had to be routed through the Spring proxy

`VerificationWorker.runOnce()` calls `claim()`, and `VerificationItemProcessor.process()`
calls `prepare()`/`complete()`. A plain self-call bypasses the transaction proxy, which
would have been silently wrong in two ways: `claim()`'s `FOR UPDATE SKIP LOCKED` row locks
would have been released before the status update, letting two workers claim one row; and
`AuditService.record()`'s `MANDATORY` propagation would have thrown.

**Decision.** Both classes take a `@Lazy` self-reference and call through it — the same
pattern `ImportProcessingService` already uses.

### 3.6 Batch counters are recomputed, never incremented

Doc 14 §9's loop ends with "update batch counters".

**Decision.** Counters are derived from an aggregate over the batch's items on every
update, and the worker reconciles every non-terminal batch on every pass. An incremented
counter can be skewed by a crash mid-batch, a re-queued retry or a concurrent worker; a
recount is always right, and it is what makes restart recovery and
`GET /verifications/active` trustworthy without any extra bookkeeping.

### 3.7 Filters use Specifications, not `:param IS NULL OR …`

The first implementation used the JPQL `(:param IS NULL OR column = :param)` pattern
already present in `UserRepository`. PostgreSQL rejects it once a parameter's only context
is `IS NULL`: `ERROR: could not determine data type of parameter $5`.

**Decision.** The Verify filters are Criteria `Specification`s
(`VerificationCompanySpecification`, `VerificationBatchSpecification`), matching the
existing `CompanySpecification` pattern. Predicates are built only for filters that are
present, so no untyped bind is ever produced, and all filtering, sorting and paging stays
in SQL. The item search joins `companies` through an EXISTS subquery rather than filtering
names in Java, which would have broken pagination.

### 3.8 `GET /verifications/active` answers `204`, not `404` or a wrapper

"Nothing is running" is a normal state of the Verify page, not a missing resource. The
endpoint returns `204 No Content`, and the frontend API service normalises that to `null`
so the query has a real cached value and the card has one shape to render.

### 3.9 Skip reasons beyond the two the requirements name

Doc 13 §6 names `NO_PHONE` and `ALREADY_VERIFIED`.

**Decision.** Two more were added so that a batch's own record explains every selected
company: `NOT_ELIGIBLE` (archived) and `FILTER_MISMATCH` (the company does not match the
Added By / date filters the batch was created under). Filters are re-applied server-side,
so passing an id that does not match them cannot smuggle a company into the batch — it is
recorded as a visible skip instead of silently disappearing. A present-but-unusable phone
reports `NO_PHONE` with the detail in `failure_message`, keeping to the code the
requirements name.

### 3.10 Inclusive date ranges resolve against UTC days

`date_from`/`date_to` are inclusive per doc 13 §5. They are converted to a half-open
instant range `[date_from 00:00 UTC, date_to+1 00:00 UTC)`, matching the project's
UTC-everywhere convention (`hibernate.jdbc.time_zone: UTC`, ISO-8601 UTC in JSON).

### 3.11 No Twilio SDK

Doc 14 §4 requires that the domain not depend on Twilio SDK details, and §5 requires
`TWILIO_LOOKUP_BASE_URL` to be configurable.

**Decision.** `TwilioPhoneVerificationProvider` calls the documented REST contract
(`GET {base}/v2/PhoneNumbers/{e164}?Fields=line_type_intelligence`) with Spring's
`RestClient` and Basic auth. One read-only endpoint does not justify an SDK dependency,
and a configurable base URL has to stay redirectable at a stub. The bean is
`@ConditionalOnProperty` on `prospectsoul.verification.provider`, so `VERIFICATION_PROVIDER`
selects the implementation as doc 14 §16 intends.

### 3.12 Three shadcn/ui primitives were added

`checkbox`, `dialog` and `progress` did not exist in `components/ui/`. Multi-select, the
confirmation dialog and the progress bar are all explicit requirements. They are built on
Radix (`radix-ui`, already installed) in the same shadcn/ui + CVA + Tailwind style as the
existing primitives, so focus management, the space key, the indeterminate state and the
`progressbar` ARIA role are correct rather than re-implemented per screen.

## 4. Testing notes

- No test makes a live provider call. `src/test/resources/application.yaml` leaves the
  Twilio credentials empty, so the real provider short-circuits to
  `PROVIDER_NOT_CONFIGURED` before opening a connection; verification tests additionally
  override the bean with `StubPhoneVerificationProvider` via `@Primary`.
- The scheduled poll is disabled in tests (`worker-enabled: false`) and the worker is
  driven with explicit `runOnce()` calls, so every assertion happens at a known point
  instead of racing a background thread.
- `activities.content` is `jsonb`, so PostgreSQL rewrites key order and spacing on read.
  Tests assert on the parsed content, not the raw text.
- `Company.createdAt` is mapped `updatable = false`, so back-dating a fixture for the
  date-range tests requires a direct SQL update.

## 5. Known gaps outside this module's scope

1. **Method security is not enabled in the `dev` profile.** `@EnableMethodSecurity` is on
   `SecurityConfig` (`@Profile("!dev")`); `DevSecurityConfig` authenticates but does not
   enable method security, so `@PreAuthorize` is inert when running locally with
   `SPRING_PROFILES_ACTIVE=dev`. Every Verify endpoint is annotated and the `403`/`401`
   paths are asserted under the `test` profile, where `SecurityConfig` is active. Fixing
   the dev profile changes application-wide security configuration and belongs in its own
   change.
2. **No Company Timeline UI.** Verification writes `activities` and `audit_log` rows and
   `ActivityRepository` exposes the timeline read, but no screen composes the timeline yet —
   that screen is the company module's, not Verify's.
3. **No generated OpenAPI document.** Springdoc still has no release compatible with
   Spring Boot 4.1.1. The contract is documented in the README's API endpoints table and
   pinned by `VerificationControllerIntegrationTest`.
4. **Batch cancellation is not exposed.** `CANCELLED` exists in the status enum because
   doc 13 §8 lists it, and the worker and counter logic respect it, but no endpoint sets
   it — no requirement asks for cancellation.
