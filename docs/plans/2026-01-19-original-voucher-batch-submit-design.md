# Original Voucher Batch Submit Design

## Context and goal
- The "Pre-Archive -> Document Pool" view shows original vouchers in DRAFT but only exposes a small row-level icon for submit.
- Users expect a clear batch action to submit selected vouchers for archive.
- Goal: add a batch "Submit for archive" action that operates on the current selection only, with a confirmation prompt.

## Scope
- Frontend only, in `OriginalVoucherListView`.
- Show batch submit only in pool mode (pre-archive document pool).
- Reuse existing API `POST /original-vouchers/{id}/submit`.
- No backend changes.

## UX and placement
- When rows are selected, show an action bar above the table.
- Action bar shows: "Selected X items" and a primary "Submit for archive" button.
- Button is disabled if no selection.
- Keep the existing row-level submit action for single-item flow.

## Behavior and data flow
- On click, show a confirmation dialog: "Submit selected vouchers to archive? This will enter approval flow."
- If confirmed, submit selected IDs with `Promise.allSettled` to avoid early abort.
- While submitting: disable the batch button and show "Submitting..." label.
- After completion:
  - Show a toast summary: success count / failure count.
  - If failures exist, show the first error message in the toast.
  - Refresh the list and clear selection.

## Error handling
- Handle network errors per item in the settled results.
- Do not fail the entire batch on a single error.
- If all failed, keep selection so user can retry.

## Testing
- Manual:
  - Select multiple rows and submit; confirm dialog appears.
  - Verify toast summary and list refresh.
  - Verify button disabled when no selection.
  - Verify row-level submit still works.
- Optional unit test:
  - Assert action bar renders only in pool mode and with selection.

## Out of scope
- Approval/confirm archive flow.
- Any backend workflow changes.
