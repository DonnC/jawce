# Internal State Model

This document describes the current engine-owned conversation state model inside `jengine`.

It is an internal engine concern.

Bot developers still interact with normal `jawce` hooks, templates, and `ISessionManager`.

The point of this model is to reduce the spread of raw session-key reads and writes across the engine core.

## Why this was introduced

Historically, `jawce` relied on direct access to keys like:

- `CURRENT_STAGE`
- `PREV_STAGE`
- `SESSION_CHECKPOINT_KEY`
- `SESSION_DYNAMIC_RETRY_KEY`
- `CURRENT_MSG_ID_KEY`
- `LAST_ACTIVITY_KEY`
- dynamic template body keys

That worked, but it made engine flow harder to reason about.

Now the hot-path engine code is moving through `ConversationState`.

See:

- [ConversationState.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/internal/state/ConversationState.java)

## What `ConversationState` owns today

### Conversation progress

- current stage
- previous stage
- start-menu initialization
- stage advancement
- rollback on failed outbound send

### Recovery state

- checkpoint stage
- retry pending marker
- retry marker clearing

### Transport safety state

- current processed message id
- duplicate-protection message history queue
- debounce timestamp

### Session activity state

- last activity timestamp
- authenticated marker lookup

### Dynamic render state

- current dynamic template body
- next dynamic template body
- clearing transient dynamic next-body state

## Where it is used now

The wrapper is now part of the engine hot path in:

- [BaseTemplateProcessor.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/internal/abstracts/BaseTemplateProcessor.java)
- [WebhookProcessor.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/internal/service/WebhookProcessor.java)
- [WhatsAppHelperService.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/internal/service/WhatsAppHelperService.java)
- [Worker.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/api/Worker.java)

This means stage progression, retry state, rollback, activity updates, and startup defaults are less dependent on scattered ad hoc session handling.
It also means duplicate protection and debounce timing now live in the same internal state model instead of being handled separately in `Worker`.

## What this improves

### Better reasoning

It is now easier to explain engine behavior as state transitions instead of “some class writes some keys and another class interprets them later.”

### Safer future refactors

Dynamic choice handling, interruption recovery, and transition simplification will be easier now because the engine already has a central place to express core state.

### Better tests

The state model now has direct characterization tests in:

- [ConversationStateTest.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/test/java/zw/co/dcl/jawce/engine/internal/state/ConversationStateTest.java)

## What is not done yet

This is not the final state cleanup phase.

Remaining likely improvements:

- move debounce and message-history queue handling into engine-owned state helpers too
- reduce direct raw session access in a few remaining engine paths
- distinguish conversation state from transport safety state even more clearly
- decide whether dynamic-choice registry state should live beside `ConversationState` or in a dedicated model

## Bottom line

`ConversationState` is the first real internal state boundary for `jawce`.

It does not change the developer-facing model, but it gives the engine a much cleaner foundation for the next core workstreams.
