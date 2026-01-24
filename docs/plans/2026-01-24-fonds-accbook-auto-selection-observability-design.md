# Fonds-Based Accbook Auto-Selection + Observability Design

## Summary
Add backend route selection to derive a single accbook from the current fonds
using `accbook_mapping`. If no mapping exists, fail fast. Record selection
details in sync history/task and emit structured logs for auditing.

## Goals
- Select accbook strictly by current fonds (X-Fonds-No or user default).
- Fail when fonds has no mapping.
- Persist route selection context for audit and debugging.
- Improve log visibility for route selection and guard failures.

## Non-goals
- Multi-accbook sync per fonds.
- Frontend-led accbook selection.
- Cross-fonds sync in a single run.

## Current Behavior
- `config_json.accbookCode` drives sync selection.
- `accbook_mapping` only guards (mismatch triggers failure).

## Proposed Behavior
1) Build `dtoConfig` as today.
2) Resolve accbook by current fonds:
   - Parse `accbook_mapping`.
   - Find the single accbook whose mapped fonds equals current fonds.
   - If none or more than one, fail with a clear message.
   - Override `dtoConfig.accbookCode` and `dtoConfig.accbookCodes`.
3) Run `validateFondsAccbookMapping`.
4) Continue sync using the selected accbook.

## Data Model Changes
Add columns to `sys_sync_history` and `sys_sync_task`:
- `resolved_accbook_code` (varchar)
- `resolved_fonds_code` (varchar)
- `selection_mode` (varchar: BY_MAPPING, FALLBACK, ERROR)
- `mapping_hit` (boolean)

Optionally enrich `sync_params` with the same fields for compatibility.

## Observability
- Log `route_select` with scenarioId, configId, currentFonds,
  selectedAccbook, mappingHit, selectionMode.
- Log `route_guard` with expected/actual fonds on mismatch.
- Expose new fields in sync history/task status responses.

## Error Handling
- Missing `currentFonds` -> "missing fonds context".
- No mapping for fonds -> "fonds not mapped".
- Multiple accbooks mapped to same fonds -> "mapping not unique".

## Implementation Notes
- Add `getAccbookForFonds` to `ErpConfig` (inverse lookup of mapping).
- Apply override in `ErpSyncService` before guard validation.
- Extend `SyncHistory`, `SyncTask`, and `SyncTaskStatus` DTOs.
- Add a migration to backfill new columns as null.

## Testing
- Unit: fonds DEMO selects CS002 and persists fields in history/task.
- Failure: no mapping returns clear error.
- Regression: `config_json.accbookCode` no longer drives selection when
  mapping exists.
