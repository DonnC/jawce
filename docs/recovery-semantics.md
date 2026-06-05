# Recovery Semantics

This document describes the first-stage recovery model now implemented in `jengine`.

The goal is to make fallback behavior predictable instead of leaving it spread across exception handlers and session conventions.

## Why this exists

Serious chatbots need a reliable answer to questions like:

- what happens when a hook fails?
- what happens when a session expires?
- what happens when a user presses `Retry`?
- when do we restart the full flow versus just re-render the last stage?

`jawce` now treats these as explicit recovery intents.

## Recovery intents

The engine currently resolves failures into one of these intents:

- `RESTART`
- `RETRY`
- `RETURN_TO_MENU`

These are resolved centrally through `RecoveryPolicyResolver`.

## Current policy

### `RESTART`

Used for:

- `SessionExpiredException`
- `SessionInactivityException`

Engine behavior:

- clear the user session
- send a quick-button fallback with `Menu`
- allow global restart triggers like `hi`, `start`, and `menu` to enter cleanly again

This is the right behavior when the conversation should no longer trust the old workflow state.

### `RETRY`

Used for:

- `HookException`
- `TemplateRenderException`

Engine behavior:

- store `SESSION_DYNAMIC_RETRY_KEY`
- store the current stage as `SESSION_CHECKPOINT_KEY`
- send a quick-button fallback with `Retry`
- when the user presses `Retry`, skip the failing receive hook and re-render the checkpoint stage
- clear the retry marker after the retry response is generated successfully

This is intentionally a stage retry, not a full workflow restart.

That distinction matters for backend-driven bots where a temporary downstream issue should not force the user back to the start menu.

### `RETURN_TO_MENU`

Used for:

- `ResponseException`
- `UserSessionValidationException`
- unexpected uncaught exceptions

Engine behavior:

- keep the current session unless a handler explicitly clears it
- send a quick-button fallback with `Menu` or `Menu + Report`

This is the safe default for failures where the current stage is no longer useful but the overall conversation can still continue.

## Retry lifecycle

The retry path is now more explicit:

1. a recoverable processing error occurs
2. the engine marks retry-pending state
3. the engine stores the checkpoint stage
4. the user presses `Retry`
5. the engine resolves the next stage to the checkpoint
6. the engine skips the failing receive-hook re-execution for that retry request
7. the engine re-renders the checkpoint stage
8. the retry marker is cleared after success

This prevents the old failure loop where `Retry` could immediately re-run the same broken receive path.

## What bot authors should expect

Bot authors should think about recovery like this:

- use `SessionExpiredException` or inactivity to force a clean restart
- use `HookException` for temporary backend or orchestration failures that deserve a retry
- use `ResponseException` when user-facing recovery should return to a safe menu path

The engine owns the recovery transport behavior.

The bot still owns the business meaning of the failure.

## Current limits

This is the first recovery-hardening step, not the final model.

Still to come from the broader core-engine strategy:

- a clearer distinction between workflow reset and session reset
- more explicit interruption recovery
- stronger engine-owned recovery state instead of raw session-key access
- richer escalation/report semantics

## Tests

The current behavior is covered by engine tests, including:

- timeout restart through `Menu`
- timeout restart through global triggers
- retry checkpoint recovery without re-invoking the failing receive hook

See:

- [WorkerEngineResilienceTest.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/test/java/zw/co/dcl/jawce/engine/api/WorkerEngineResilienceTest.java)
- [Worker.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/api/Worker.java)
- [WebhookProcessor.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/internal/service/WebhookProcessor.java)
