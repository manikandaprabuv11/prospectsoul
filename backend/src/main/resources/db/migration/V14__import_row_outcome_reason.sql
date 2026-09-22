-- Adds a stable outcome-reason code to each import row so failure modes
-- can be counted and reported without free-text mining of error_message.
-- Consumed by the Activities JSON parser (`activities_json_invalid`) and
-- the new source-aware dedup ("source_reference_match", "phone_match" …).
ALTER TABLE import_rows
    ADD COLUMN outcome_reason VARCHAR(100);

CREATE INDEX idx_import_rows_outcome_reason ON import_rows(outcome_reason);
