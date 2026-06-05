# Dynamic Choice Model

This document describes the first-class dynamic choice handling now implemented in `jengine`.

The goal is to make backend-driven option selection a real engine concern instead of leaving it entirely to improvised session handling.

## What problem this solves

Before this change, `jawce` could render dynamic buttons, lists, and text menus, but the engine did not explicitly remember:

- which options were valid for the current stage
- how to validate a later selection against those options
- how to return the selected item to hooks as structured data

Now the engine can do that for dynamic-choice stages.

## Current model

The engine now supports a stage-local dynamic choice registry.

When a dynamic-choice stage is rendered successfully, the engine stores:

- the stage the choices belong to
- the set of valid choices for that stage

When the next inbound message arrives on that stage, the engine:

1. checks whether the stage has active dynamic choices
2. resolves the user input against those choices
3. rejects invalid selections before generic route fallback can accept them
4. passes the selected choice into hook `additionalData`

## Supported patterns

### Buttons

For dynamically generated `button` templates, the engine auto-discovers choices from the button labels.

### Lists

For dynamically generated `list` templates, the engine auto-discovers choices from list row ids and titles.

### Text-index menus

For dynamically generated `text` menus, the engine does not try to infer business choices from arbitrary text.

Instead, the hook should explicitly provide `dynamicChoices` metadata in `TemplateDynamicBody`.

That is the safest and most honest approach.

## Hook-facing shape

When a valid choice is selected, hooks receive:

```java
hook.getAdditionalData().get("dynamicChoice")
```

with values like:

- `stage`
- `input`
- `id`
- `label`
- `description`
- `ordinal`
- `metadata`

This makes downstream capture hooks much cleaner for accounts, billers, offers, slots, and payment instruments.

## What counts as a valid match

The engine currently resolves a dynamic choice by matching inbound input against:

- choice id
- choice label
- choice ordinal like `1`, `2`, `3`
- configured aliases

That gives enough flexibility for:

- button reply ids
- list row ids
- text-index selection
- simple alias-based matching

## Scope

This validation is intentionally scoped to dynamic-choice stages.

It does not replace the normal route model for ordinary static templates.

That matters because static YAML stages already have their own route semantics, and forcing all stages into dynamic-choice validation would create unnecessary regressions.

## Engine classes

Key implementation pieces:

- [DynamicChoice.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/model/dto/DynamicChoice.java)
- [DynamicChoiceSelection.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/model/dto/DynamicChoiceSelection.java)
- [DynamicChoiceRegistry.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/internal/dynamic/DynamicChoiceRegistry.java)
- [ConversationState.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/internal/state/ConversationState.java)
- [BaseTemplateProcessor.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/internal/abstracts/BaseTemplateProcessor.java)
- [WebhookProcessor.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/internal/service/WebhookProcessor.java)

## Tests

Current coverage includes:

- dynamic buttons rendering
- dynamic list rendering
- dynamic text rendering
- valid button selection capture
- valid list selection capture
- valid text-index selection capture
- invalid selection rejection before generic route transition

See:

- [WorkerEngineDynamicTemplateTest.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/test/java/zw/co/dcl/jawce/engine/api/WorkerEngineDynamicTemplateTest.java)

## What is still not done

This is the first proper engine version of dynamic choices, not the last.

Still worth improving later:

- first-class helper builders for constructing choices in hooks
- stronger support for nested dynamic workflows and multi-step dependent choices
- clearer distinction between presentation choice metadata and business correlation metadata
- optional choice expiry and invalidation rules beyond stage transition

## Bottom line

`jawce` now has a real dynamic choice model.

The engine can remember dynamic options, validate later selections, and return structured selected-choice data to hooks, while still keeping backend business logic outside the engine.
