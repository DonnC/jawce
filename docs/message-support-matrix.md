# jawce Message Support Matrix

## Purpose

This document separates:

- what `jawce` models,
- what `jawce` intends to support,
- what should be treated as portability goals from `pywce`,
- what needs verification in tests before being advertised as complete.

It is better to publish a precise matrix than to claim blanket parity.

## Support Levels

- `Implemented`: the runtime generation path is present in the current engine.
- `Modeled`: a template or message model exists, but end-to-end support is incomplete or needs verification.
- `Planned`: should be supported for portability or product reasons, but should not be claimed as complete yet.

## Current Engine-Oriented Matrix

| Template Kind | Current Status | Portability Goal | Notes |
|---|---|---|---|
| `text` | Implemented | High | Core path should remain stable. |
| `button` | Implemented | High | Important for most guided chatbot flows. |
| `list` | Implemented | High | Important for menu-driven bots. |
| `flow` | Implemented | High | Needs contract tests because it is enterprise-relevant. |
| `media` | Implemented | High | Includes media payload generation path. |
| `image` | Implemented | High | Routed through media generation path. |
| `document` | Implemented | High | Routed through media generation path. |
| `cta` | Implemented | High | Supported in payload generation. |
| `request-location` | Implemented | High | Present in payload generation. |
| `location` | Modeled | High | Generation method exists but needs reachable end-to-end support verification. |
| WhatsApp `template` | Modeled | High | Model exists; runtime generation should be completed or clearly limited. |
| `dynamic` | Modeled | High | Dynamic concepts exist but need explicit, tested runtime completion. |
| catalog or product types | Modeled or Planned | Medium | Should only be advertised once the full path is in place. |

## pywce Portability Priorities

The most important message types for low-friction migration are:

1. `text`
2. `button`
3. `list`
4. `flow`
5. `media`
6. `request-location`
7. `location`
8. WhatsApp `template`
9. `dynamic`

These should be the first message types locked down with tests and docs.

## What The Framework Should Ship By Default

The framework should provide:

- the engine contracts,
- payload generation for the supported message types,
- basic default or reference implementations for sessions and template loading,
- clear documentation for extension points.

The framework should not assume:

- a Redis-only session strategy,
- a JDBC-only storage strategy,
- a single enterprise integration stack.

That is why message support and infrastructure support should be documented separately.

## Required Follow-Up For Each Message Type

Before a message type is called fully supported, there should be:

1. a template example,
2. a payload generation test,
3. a migration note if it behaves differently from `pywce`,
4. a clear statement if special infrastructure is required.

## Recommended Next Actions

The highest-value follow-up is:

1. add adapter tests for every `Implemented` row,
2. either complete or narrow the `Modeled` rows,
3. update the main README feature claims to match this matrix,
4. add side-by-side examples for the highest-priority `pywce` migration types.
