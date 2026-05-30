# `jawce` History Feature

`jawce` now has a real history subsystem for audit, replay support, and downstream analysis.

It follows the normal `jawce` approach:

- the engine defines the contract
- the library ships a safe default
- applications can replace it with their own implementation

## 1. Contract

The engine contract is:

```java
public interface IHistoryManager {
    void record(ChatHistoryEvent event);
}
```

So if a team wants JDBC, Kafka, JPA, Elasticsearch, S3, or a bank-grade event pipeline, they provide their own `IHistoryManager` bean.

## 2. What the engine records

The engine now emits structured history events for:

- inbound accepted messages
- duplicate skipped messages
- stale skipped messages
- debounce skipped messages
- stage resolution
- outbound payload generation
- outbound send success
- outbound send failure
- engine errors

These are represented by `HistoryEventType` and stored as `ChatHistoryEvent`.

## 3. Spring event approach

History recording is intentionally off the hot path.

The engine publishes `ChatHistoryEvent` through Spring events, then `HistoryEventListener` submits persistence work to a background executor.

That means:

- webhook processing does not wait on file writes
- event order is preserved by default with a single history executor thread
- applications can still replace the manager implementation cleanly

## 4. Default implementation

The bundled default is `FileHistoryManager`.

It is not enabled automatically.

If no custom `IHistoryManager` bean exists:

- default behavior is `NoOpHistoryManager`
- file history is enabled only when configured

## 5. File format

The bundled file manager stores events as `NDJSON` / JSON Lines.

That format is a strong fit here because it is:

- append-friendly
- stream-friendly
- replay-friendly
- easy to parse line by line
- easy to ship into analytics pipelines later

Each line is one `ChatHistoryEvent`.

## 6. File size handling

Yes, the bundled file manager now handles file growth.

It supports:

- max file size rotation
- max retained file count pruning

So the local file store does not grow as one unbounded transcript file.

This makes it acceptable for:

- local development
- demos
- short-lived pilots
- lower-volume single-node deployments

But for high-traffic production chatbots, especially banking, the file manager should still be treated as a reference implementation, not the final persistence strategy.

## 7. Properties

```yaml
jawce:
  history:
    file-enabled: true
    dir: "./.history"
    file-prefix: "jawce-history"
    max-file-size-bytes: 5242880
    max-files: 20
    executor-threads: 1
```

Notes:

- `executor-threads=1` is the safest default because it preserves write order
- if an application provides its own `IHistoryManager`, that bean takes over

## 8. Banking / scale guidance

For a serious banking bot, I would recommend:

- keep the engine event model
- replace the file manager with a persistent store or event pipeline
- use correlation ids and retention policies outside the engine
- redact or tokenize sensitive fields before long-term storage where required

Typical production choices would be:

- database-backed history
- queue or event-bus publishing
- search / analytics sink
- object-store archival

## 9. Replay thinking

The current design is replay-friendly because it stores normalized event records, not only raw session data.

That is the right base for:

- journey reconstruction
- incident review
- analytics
- later replay tooling

without mixing long-term history into runtime session state.
