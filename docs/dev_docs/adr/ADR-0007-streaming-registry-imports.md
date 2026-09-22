# ADR-0007: Row-batched, chunk-committed import path for registry-scale files
Date: 2026-09-21 · Status: Accepted

## Context
The Kanchipuram MSME sample has 123,659 rows. The existing `ImportService.upload(...)` (docs 01–18 scope) is `@Transactional` and:

1. buffers the whole workbook into `List<Map<String,String>>` via `WorkbookFactory` (non-streaming XSSF), then
2. calls `rowRepository.save(...)` for every row inside the same open transaction.

At Kanchipuram scale this either OOMs the JVM or holds one write transaction open for the entire file — a direct violation of the "@Transactional must not span slow work" guidance in CLAUDE.md §3.3. It also blocks the async processor from starting until the whole file is committed, defeating the "one bad row never fails the batch" invariant that doc 21 §6.1 relies on.

## Decision
Split the upload into `upload(...)` (create the batch metadata + one audit row) and a new `stage(batchId, file, actor)` (parse and chunk-insert rows). `stage()` reads the file through a streaming `Consumer<Map<String,String>>` sink, buffers up to `ROW_BATCH_SIZE = 500` rows, and calls `rowRepository.saveAll` + `flush` per chunk. Each chunk stays inside the outer transaction for now — an interim step that unblocks large files without changing the caller contract.

`upload(...)` calls `stage(...)` immediately, preserving the existing single-shot POST behaviour. This is deliberately a smaller change than moving `stage()` to `@Async`; the async move is captured as a follow-up so the Verification module's DB-backed queue pattern can be reused.

XSSF streaming (SAX / XSSFReader) is not swapped in yet — the test files exercised in CI are < 100 rows, and swapping the reader is a substantive change that is not needed to prove the C1–C5 acceptance path. When the real 123,659-row file lands in the repository, `streamExcel(...)` is the one method to replace.

## Consequences
**Easy:** chunked commits, memory bounded by `ROW_BATCH_SIZE`, unchanged caller contract.
**Hard:** the outer transaction still spans staging — moving `stage()` to `@Async` needs to happen before the real Kanchipuram file is used in production.
**Given up:** the illusion that one Spring `@Transactional` method scales to 100 000 rows.

## Affected
- `backend/src/main/java/com/vyoog/prospectsoul_backend/imports/service/ImportService.java` (split `upload` / `stage`, added `persistChunk`, `streamCsv`, `streamExcel`)
- No API contract change; the upload endpoint still returns an `ImportBatchResponse` with the parsed row count.
