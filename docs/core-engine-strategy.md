# Core Engine Strategy

This strategy focuses only on weaknesses that belong to the `jawce` engine itself.

It intentionally excludes:

- enterprise example apps,
- Redis, JDBC, JPA, or other infrastructure implementations,
- bot-specific business logic patterns,
- organization-specific deployment choices.

The goal here is to make `jengine` a stronger conversation engine regardless of which developer or company adopts it.

## Scope

The highest-value engine-owned weaknesses today are:

1. dynamic follow-up handling is still too manual
2. too much advanced behavior depends on internal session conventions
3. hook phase contracts are still broader than they should be
4. workflow recovery and restart semantics are not first-class enough
5. dynamic route and nested transition behavior is still harder to reason about than it should be

These five are strongly connected.

If they are handled well, `jawce` becomes a much better backbone for transactional and backend-driven chatbots across sectors.

## Strategy principles

## 1. Keep the engine as orchestrator

The engine should own:

- stage progression,
- route resolution,
- recovery semantics,
- hook lifecycle,
- dynamic choice registration and validation,
- state transition rules.

The engine should not own:

- biller rules,
- account eligibility,
- payment logic,
- booking logic,
- product truth.

That boundary should stay firm.

## 2. Replace implicit behavior with explicit engine concepts

Where the engine currently relies on:

- internal session keys,
- hidden routing branches,
- mixed hook return shapes,
- nested recursive preprocessing,

it should move toward:

- named engine state concepts,
- explicit phase results,
- explicit recovery behavior,
- traceable transition rules.

## 3. Preserve backward compatibility while improving the model

The safest path is:

- add the stronger model first,
- keep the current model working,
- migrate internal engine code and docs to the stronger model,
- only later de-emphasize the older paths.

That is the same strategy that worked well for named hooks.

## Workstream 1: First-class dynamic choice handling

Status:
Phase 1 is now underway in the engine with stage-local dynamic choice registration, inbound validation for dynamic-choice stages, and structured selected-choice capture for hooks.

## Problem

Today `jawce` can render dynamic buttons, lists, or text, but the next inbound response still depends on generic stage routing and session conventions.

That makes highly dynamic backend-driven flows harder than they should be.

Examples:

- account selection
- card selection
- biller selection
- payment instrument selection
- slot selection
- backend-driven field steps

## Engine goal

Introduce a first-class dynamic choice model.

The engine should be able to say:

- this stage rendered a bounded set of choices,
- those choices belong to this stage,
- these are the valid ids,
- this is how inbound responses are matched back,
- this is how expiration or replay is handled.

## Deliverables

### Phase 1

- add an engine-owned dynamic choice registry abstraction
- register generated option ids against the current stage
- validate inbound selections against the stage-local registry
- expose a clean hook-facing way to access the selected dynamic item

### Phase 2

- support buttons, lists, and plain text indexed selection through the same internal model
- support choice expiry and invalidation when the stage changes
- support backend correlation metadata per choice

### Phase 3

- add helper builders for dynamic option generation
- allow hooks to populate choices without constructing raw session state

## Expected outcome

Dynamic flows become much easier to build without exploding templates or overusing custom session data.

## Workstream 2: Replace internal session-key magic with explicit engine state

Status:
Phase 1 is now well established with `ConversationState` introduced into the engine hot path for stage progression, retry state, rollback, activity updates, dynamic template state, duplicate protection, and debounce timing.

## Problem

Advanced features currently rely on internal keys such as:

- dynamic current template,
- dynamic next template,
- retry flags,
- checkpoints,
- message id state,
- activity state.

This works, but it is difficult to reason about and easy to misuse mentally.

## Engine goal

Introduce a clearer internal state model for one conversation step and one conversation session.

## Deliverables

### Phase 1

- document current internal state keys formally
- group them conceptually into:
  - conversation progress
  - retry and recovery
  - dynamic render state
  - transport safety

### Phase 2

- introduce an engine-internal state wrapper like `ConversationState` or similar
- route engine logic through that wrapper instead of raw session key access spread across classes

### Phase 3

- centralize reads and writes for engine-owned state
- make state transitions testable as state transitions, not only as end-to-end webhook outcomes

## Expected outcome

The engine becomes easier to debug, easier to extend, and less surprising.

## Workstream 3: Narrow hook phase contracts further

## Problem

The engine has improved with named hooks and internal result mapping, but hooks still broadly communicate through `Hook`.

That keeps compatibility high, but the phase semantics are still looser than ideal.

## Engine goal

Keep `Hook` as the stable execution context, but move toward narrower phase-owned result contracts internally.

## Deliverables

### Phase 1

- keep `Hook` input stable
- expand internal result wrappers:
  - receive result
  - generate result
  - router result
  - template render result

### Phase 2

- route more engine decisions through those typed results
- reduce direct engine branching on loosely populated `Hook` fields

### Phase 3

- add optional developer helper abstractions so common hook patterns are easier to write
- keep the raw `Hook` path available for compatibility

## Expected outcome

Hook behavior becomes easier to reason about, document, and validate, without forcing an abrupt API rewrite.

## Workstream 4: Make recovery and restart semantics first-class

Status:
Phase 1 is now underway in the engine with explicit recovery intent resolution, retry checkpoint handling, and regression coverage for timeout restart and retry behavior.

## Problem

Timeout, retry, menu return, trigger restart, interrupted progression, and fallback behavior still feel partly emergent rather than explicitly modeled.

The recent timeout fix improved this, but the broader model still needs hardening.

## Engine goal

Promote recovery to a first-class engine concern.

The engine should explicitly know:

- when a conversation has expired,
- how restart should behave,
- how retry should behave,
- how checkpoint resume should behave,
- how safe restart triggers should behave.

## Deliverables

### Phase 1

- define recovery semantics clearly:
  - restart
  - retry
  - checkpoint resume
  - report or escalation

### Phase 2

- introduce explicit recovery policy handling in engine flow
- stop encoding too much recovery meaning through special-case checks

### Phase 3

- support recoverable interrupted workflow continuation more cleanly
- support stronger separation between:
  - session reset
  - workflow reset
  - stage retry

## Expected outcome

Timeout and fallback behavior become predictable, testable, and easier for bot authors to trust.

## Workstream 5: Simplify dynamic route and nested transition semantics

## Problem

The engine can currently do useful things with:

- transient router stages,
- nested preprocessing,
- dynamic route redirection,
- dynamic next-template processing,

but the execution path is still more complex than it should be.

## Engine goal

Make the stage transition model easier to understand and easier to extend.

## Deliverables

### Phase 1

- document the current transition model explicitly
- characterize the current nested behavior with more tests

### Phase 2

- refactor preprocessing into explicit transition steps instead of recursive-feeling control flow
- distinguish:
  - current stage processing
  - next stage resolution
  - transient reroute resolution
  - outbound rendering

### Phase 3

- reduce or eliminate hidden nested transition behavior where possible
- expose clearer stage-resolution tracing

## Expected outcome

The engine becomes more maintainable and advanced routing becomes easier to explain.

## Recommended execution order

The best order is:

1. recovery semantics
2. internal state cleanup
3. dynamic choice handling
4. transition model simplification
5. deeper hook result narrowing

Why this order:

- recovery fixes immediately improve trust
- internal state cleanup makes later work safer
- dynamic choice handling unlocks many real-world backend-driven bots
- transition simplification reduces hidden complexity before further expansion
- deeper hook result narrowing is easier once the flow model is cleaner

## What success looks like

The engine should reach a point where:

- dynamic account or biller flows feel native, not improvised
- restart, retry, and timeout paths are predictable
- fewer engine features depend on implicit session keys
- advanced routing can be explained as a simple lifecycle
- hook behavior is clearer without losing flexibility

## Bottom line

The right engine strategy is not to make `jawce` bigger in every direction.

It is to make its core orchestration model:

- more explicit,
- more bounded,
- more dynamic where it matters,
- more predictable under interruption,
- easier to understand internally.

If these core engine workstreams are completed well, `jawce` becomes a much stronger backbone for serious WhatsApp workflow bots without needing to assume anything about how individual developers handle persistence, databases, or enterprise integrations.
