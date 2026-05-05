# jawce Implementation Plan

## Scope

`jawce` should not aim for a literal 1:1 port of `pywce`.

Its goal should be:

- preserve the core `pywce` mental model,
- keep templates and routing concepts familiar to `pywce` users,
- let Java and Spring Boot users solve the same class of problems with low migration friction,
- take advantage of the Java ecosystem for enterprise-grade deployment, governance, and integration.

The design target is not "Python in Java".
The design target is "the same engine idea, expressed the Spring Boot way".

## Product Positioning

`jawce` should become the stronger enterprise and integration-oriented sibling in the `wce` family.

That means:

- `pywce` stays excellent for fast iteration and Python-native chatbot work,
- `jawce` becomes the better fit for larger teams, regulated environments, and long-lived production systems,
- both should feel related enough that template authors and bot maintainers can move between them with minimal re-learning.

For example, if a bank builds a WhatsApp bot, `jawce` should feel natural because it can fit into:

- existing Spring Boot services,
- internal identity and access control patterns,
- audit logging and observability stacks,
- database-backed session and workflow persistence,
- event-driven integration with CRM, ticketing, fraud, and core banking systems.

## Compatibility Goal

The portability target should be "high-friction-free template migration", not "perfect source compatibility everywhere".

In practice this means:

- stage names should remain portable,
- route structures should remain very similar,
- core template kinds should remain familiar,
- common hooks like `on_receive`, `on_generate`, `router`, and template rendering should remain conceptually equivalent,
- a developer coming from `pywce` should mostly adjust hook implementation style and infrastructure wiring, not re-learn the engine.

Some differences are acceptable and even desirable:

- hooks may be Spring beans, Java classes, or REST endpoints,
- session and storage implementations should remain interface-driven and replaceable,
- runtime events and integrations should use Spring idioms,
- stronger validation and security policies may exist in Java by default.

## Framework Boundary

`jawce` should expose contracts, not infrastructure assumptions.

That is exactly why the library has interfaces such as:

- `ISessionManager`,
- `ITemplateStorageManager`,
- `IClientManager`.

The library should provide:

- stable engine contracts,
- basic default or reference implementations,
- examples that show how to wire custom infrastructure.

The library should not assume that every team wants:

- Redis for sessions,
- JDBC for template storage,
- JPA for persistence,
- any single database, cache, or message bus.

For example, a bank may want:

- a Redis-backed session manager,
- a JDBC or JPA-backed template registry,
- a tenant-aware storage layer,
- internal HTTP clients with custom auth and audit policies.

Those are application decisions.
`jawce` should make them easy through interfaces, but should not hardcode them as the framework default.

## Design Principles

## 1. Preserve template authorship familiarity

`jawce` should keep the authoring experience close enough to `pywce` that teams can reuse template knowledge.

That means preserving:

- stage-based flow authoring,
- YAML and JSON template support,
- route-based next-stage transitions,
- prop persistence,
- checkpoint behavior,
- dynamic rendering concepts,
- reusable trigger conventions.

## 2. Prefer Spring-native extension points over custom framework invention

Where `pywce` uses Python dynamism, `jawce` should prefer:

- Spring configuration properties,
- dependency injection,
- events,
- typed interfaces,
- REST clients,
- bean lifecycle hooks,
- standard validation and serialization libraries.

This keeps `jawce` aligned with how Java teams already build systems.

## 3. Optimize for production-grade deployments

For enterprise users, especially banks, the engine should be designed for:

- auditability,
- resiliency,
- traceability,
- explicit security boundaries,
- repeatable deployment,
- operational visibility.

That means engine behavior should be observable and controllable, not only flexible.

## 4. Treat compatibility as a contract that is tested

The closest thing to parity should be behavioral tests and documented support tables, not assumptions.

If `jawce` claims a `pywce` concept is supported, there should be:

- a documented authoring contract,
- an engine or adapter test proving it,
- a clear note where the Java behavior intentionally differs.

## Core Engine Ownership

The core engine should own:

- stage resolution,
- current and previous stage transitions,
- trigger evaluation,
- route matching,
- checkpoint semantics,
- session inactivity semantics,
- hook lifecycle policy,
- prop persistence,
- dynamic redirect handling,
- generation of a channel-agnostic outbound intent or stage result.

## Channel Adapter Ownership

The WhatsApp adapter should own:

- webhook parsing,
- signature validation,
- WhatsApp-specific message extraction,
- payload formatting,
- flow-specific payload rules,
- media upload and retrieval,
- actual message sending and response verification.

## Enterprise Platform Ownership

The surrounding Spring platform layer should own:

- security integration,
- persistence wiring,
- retry and resilience policy,
- observability,
- event publishing,
- external system integrations,
- operational configuration.

## Delivery Strategy

The safest sequence is:

1. define the intended Java contract clearly,
2. freeze current engine behavior with tests,
3. close the highest-impact parity gaps that affect template portability,
4. add Spring-native enterprise capabilities without destabilizing template authorship,
5. publish a support matrix so users know exactly what is portable and what is Java-specific.

## Phase-by-Phase Execution

## Phase 1. Define the jawce compatibility contract

Goal:
Make it explicit what `jawce` must preserve from `pywce`, and where it intentionally diverges.

Deliverables:

- a `pywce` to `jawce` template portability guide,
- a trigger behavior comparison table,
- a hook equivalence table,
- a message-type support matrix,
- documented Java-only extensions.

Important decisions to lock down:

- which template fields must remain compatible,
- which built-in triggers should behave the same,
- which hook concepts are preserved exactly,
- which features are best expressed differently in Java.

## Phase 2. Characterize current jawce engine behavior with tests

Goal:
Protect today’s Java engine behavior before deep refactor or feature completion.

Tests to add first:

- new user starts at configured start stage,
- existing user resumes current stage,
- button route transitions work,
- regex route transitions work,
- checkpoint stages are remembered,
- props are saved into session props,
- duplicate message IDs are skipped,
- debounce suppresses rapid repeat processing,
- stale webhook requests are skipped,
- inactivity triggers recovery behavior,
- `on_receive` and `on_generate` run in the expected lifecycle order,
- `router` hook can redirect the next stage,
- invalid input returns a recovery response without corrupting stage,
- session updates happen only after successful outbound send.

Suggested grouping:

- `tests/engine/...`
- `tests/adapter/whatsapp/...`
- `tests/integration/...`

## Phase 3. Close the template portability gaps

Goal:
Make `pywce` template migration low-friction for the most important flows.

Priority areas:

- complete outbound support for `dynamic`,
- complete outbound support for WhatsApp `template` messages,
- complete reachable support for `location`,
- complete catalog and product message support if they remain part of the supported contract,
- document any intentionally unsupported `pywce` template constructs.

Definition of success:

- a `pywce` user can move common templates over with only small adjustments,
- differences are mostly in hook implementation and app wiring,
- unsupported fields fail clearly rather than silently degrading.

## Phase 4. Standardize trigger and hook behavior

Goal:
Reduce migration surprises for `pywce` users.

Focus items:

- decide whether `menu`, `back`, `retry`, and `report` should become explicit first-class engine behaviors,
- align trigger naming and route semantics where practical,
- define a clear Java contract for reflective hooks, Spring bean hooks, and REST hooks,
- document how hook return data controls render payload, redirect behavior, and dynamic template bodies.

This phase matters because template familiarity alone is not enough. Bot logic authors need predictable engine semantics too.

## Phase 5. Add bank-grade enterprise hardening

Goal:
Make `jawce` strong for regulated and high-accountability production environments.

Important capabilities:

- strong contracts for persistent session and storage backends,
- structured audit logs for inbound webhook, route decision, hook execution, and outbound response,
- correlation IDs and distributed tracing support,
- configurable retention and redaction policies for sensitive user data,
- explicit timeout and retry policies for hook REST calls,
- stronger webhook signature validation and failure reporting,
- role-aware admin and support integration patterns,
- configurable dead-letter or failure event handling for downstream integrations.

Important boundary:

- the library should ship default or reference implementations only,
- production Redis, JDBC, JPA, or other infrastructure integrations should be developer-supplied implementations of the engine interfaces,
- the framework should document these extension points clearly rather than picking a single enterprise stack by default.

For a bank scenario, the engine should make it easy to answer questions like:

- what stage was this user in,
- what input caused the route transition,
- which hook made the decision,
- which payload was sent,
- whether any sensitive payload was masked in logs,
- whether the session expired, retried, or escalated.

## Phase 6. Publish a migration and authoring guide

Goal:
Make adoption easier for both existing `pywce` users and Java-first teams.

The guide should include:

- "coming from `pywce`" examples,
- side-by-side template examples,
- hook conversion examples from Python to Spring bean or REST hook,
- session manager replacement examples,
- template storage examples using classpath and external directories,
- enterprise deployment notes.

## Suggested Initial Test Matrix

| Area | Behavior | Priority |
|---|---|---|
| Engine | start stage routing | P0 |
| Engine | current stage resume | P0 |
| Engine | button transition | P0 |
| Engine | regex route transition | P0 |
| Engine | checkpoint behavior | P0 |
| Engine | prop persistence | P0 |
| Engine | hook lifecycle ordering | P0 |
| Engine | router redirect | P0 |
| Engine | invalid input recovery | P0 |
| Engine | duplicate detection | P1 |
| Engine | debounce behavior | P1 |
| Engine | stale webhook rejection | P1 |
| Engine | inactivity reset | P1 |
| Adapter | text payload rendering | P0 |
| Adapter | button payload rendering | P0 |
| Adapter | list payload rendering | P0 |
| Adapter | flow payload rendering | P0 |
| Adapter | template payload rendering | P0 |
| Adapter | location payload rendering | P1 |
| Adapter | dynamic payload rendering | P1 |
| Adapter | catalog or product payload rendering | P1 |
| Security | signature verification | P0 |
| Security | REST hook auth forwarding | P1 |
| Observability | route decision logging | P1 |
| Enterprise | persistent session backend contract | P1 |

## Recommended First Docs To Add After This

The next documentation pieces that would add the most value are:

1. `docs/template-compatibility.md`
2. `docs/trigger-contract.md`
3. `docs/message-support-matrix.md`
4. `docs/enterprise-deployment.md`
5. `docs/migrating-from-pywce.md`

## Definition Of Done

This phase should be considered successful when:

- `jawce` is clearly positioned as Java-first rather than a literal Python clone,
- core template authoring feels familiar to `pywce` users,
- message and trigger support are explicitly documented and tested,
- engine behavior is protected by a meaningful Java test suite,
- enterprise concerns like auditability, persistence, and observability are part of the design rather than afterthoughts.

## Bottom Line

The right direction for `jawce` is:

- keep the engine concepts recognizable to `pywce` users,
- preserve a high degree of template portability,
- embrace Spring Boot idioms for hooks, events, config, and integration,
- optimize for production-grade enterprise use cases such as banking,
- prove compatibility and support through tests and docs rather than by claiming a 1:1 port.
