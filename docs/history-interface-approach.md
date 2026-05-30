# History Interface Approach For `jawce`

This guide explains how I would add a proper history capability to `jawce`, similar in spirit to `pywce`, but in a Java/Spring way.

## 1. What exists today

`jawce` already has a small internal session key for message-id history:

- `SessionConstant.SESSION_MESSAGE_HISTORY_KEY`

and `Worker` uses it mainly for duplicate detection and short message queue handling.

That is useful, but it is not a real history or audit feature.

It is not enough for:

- analytics
- conversation replay
- regulator-friendly audit trails
- debugging production incidents
- reconstructing business journeys

So I would treat the current piece as:

- duplicate-message protection

not:

- history management

## 2. What a real history feature should do

A true history extension should support:

- inbound message capture
- outbound message capture
- stage transition capture
- hook or decision metadata capture
- replay-friendly ordered events
- searchable conversation activity

Optionally:

- error events
- backend call summaries
- latency measurements
- correlation ids

## 3. Design principle

Just like sessions and template storage, history should be interface-first.

That means:

- the engine defines the contract
- the library may provide a basic default example
- production apps choose their own persistence strategy

So I would not hardcode:

- JPA
- Redis
- Kafka
- JDBC

into the engine.

## 4. Proposed interface

I would add something like:

```java
package zw.co.dcl.jawce.engine.api.iface;

import zw.co.dcl.jawce.engine.model.history.ChatHistoryEvent;

public interface IHistoryManager {
    void record(ChatHistoryEvent event);
}
```

And the event model would be explicit.

## 5. Proposed event model

I would not store raw strings only.

I would use a typed event such as:

```java
public class ChatHistoryEvent {
    private String sessionId;
    private String waId;
    private String messageId;
    private HistoryEventType type;
    private String currentStage;
    private String nextStage;
    private String templateType;
    private String direction;
    private Object payload;
    private Instant timestamp;
    private Map<String, Object> metadata;
}
```

Possible event types:

- `INBOUND_RECEIVED`
- `INBOUND_SKIPPED_DUPLICATE`
- `INBOUND_SKIPPED_OLD`
- `STAGE_RESOLVED`
- `HOOK_EXECUTED`
- `OUTBOUND_GENERATED`
- `OUTBOUND_SENT`
- `ENGINE_ERROR`

This event-style model is much more replayable and analyzable than a simple transcript string.

## 6. Why event-based history is better

For a banking bot, event history is better because:

- you can reconstruct the conversation timeline
- you can see stage transitions
- you can inspect routing decisions
- you can audit user behavior separately from backend behavior
- you can replay flows for debugging

This is much more useful than only storing message bodies.

## 7. Where the engine should record history

I would record at these points:

### Inbound webhook accepted

Record:

- session id
- message id
- raw normalized input
- inbound type

### Duplicate or stale message skipped

Record:

- reason
- message id
- timestamp

### Stage decision completed

Record:

- current stage
- next stage
- whether router redirected
- whether trigger fired

### Outbound message generated

Record:

- template type
- stage
- normalized outbound payload

### Outbound send result

Record:

- success or failure
- message id if available
- rollback or clear action if applicable

### Engine errors

Record:

- exception type
- stage
- session id
- compact error message

## 8. How replay should work

Replay does not need to mean the engine blindly reruns old production side effects.

I would treat replay as:

- reconstructing a timeline
- rebuilding stage progression
- optionally simulating engine decisions in a safe environment

For that reason, history should store:

- enough event detail for sequence reconstruction
- but not assume production-side effect re-execution

This is important for banking systems.

## 9. Recommended storage approaches

### Lightweight default

The library can ship a very basic example like:

- file history manager

This is only for demo or local usage.

### Real production options

Apps can provide:

- JDBC history manager
- JPA history manager
- Kafka publishing history manager
- Elasticsearch/OpenSearch sink
- cloud event bus sink

That matches the existing `jawce` philosophy much better.

## 10. Suggested Spring-friendly pattern

A strong pattern would be:

- `IHistoryManager` interface in engine
- `NoOpHistoryManager` as default
- optional example implementation outside the hot core
- history publishing in engine at key checkpoints

This keeps adoption easy:

- no mandatory infra
- no heavy dependency assumptions
- production teams override with their own bean

## 11. Why not just put full history into session

Because session is the wrong place for long-term conversation audit.

Session data is for:

- current working state
- flow continuity
- short-lived runtime context

History is for:

- long-term analysis
- replay
- operations
- compliance

Mixing those concerns becomes messy quickly.

## 12. My recommendation for `jawce`

I would add history in phases.

### Phase 1

Add:

- `IHistoryManager`
- `NoOpHistoryManager`
- `ChatHistoryEvent`
- engine recording for inbound, stage resolution, outbound, and errors

### Phase 2

Add:

- tests proving events are emitted correctly
- a simple file example implementation
- documentation on production implementations

### Phase 3

Add:

- replay utility support
- correlation helpers
- structured event metadata for analytics

## 13. Why this is the right Java approach

This fits the `jawce` direction because:

- it is interface-first
- it avoids forcing one storage technology
- it gives safe defaults
- it lets enterprise teams plug in their own compliance-grade persistence

That is exactly how sessions and template storage should also feel.

## 14. Final view

So my direct view is:

- `jawce` does not yet have a real `pywce`-style history capability
- the current message queue is not enough for audit or replay
- the right answer is a dedicated `IHistoryManager` plus event-style history records

That would make the engine much stronger for banking, analytics, incident review, and replay tooling.
