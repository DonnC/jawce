# Engine Improvement Areas

These are the highest-value engine improvements visible after the current dynamic, routing, and validation work.

## 1. Make dynamic template lifecycle first-class

`jawce` now supports returning a fully built outbound template from `on-generate`, which is a strong step.

The next improvement is to make dynamic-template lifecycle clearer and more complete across:

- current-stage dynamic templates
- next-stage dynamic templates
- transient dynamic templates
- follow-up input handling

This needs a documented contract instead of scattered engine behavior.

## 2. Clarify dynamic route and dynamic template semantics

The current engine has smart behavior around:

- `router`
- transient stages
- nested preprocessing
- dynamic session keys

But it is still harder to reason about than it should be.

The engine would benefit from:

- a simpler execution model,
- better names,
- fewer hidden transitions,
- clearer tests around nested dynamic flow behavior.

## 3. Separate render-data hooks from template-replacement hooks

Right now the same hook result object can carry:

- render payload
- flow payload
- redirect target
- template replacement

That is flexible, but the contract is broad.

It would be cleaner to explicitly distinguish:

- render augmentation
- stage redirect
- full outbound template override

That would make advanced use cases easier to understand and document.

## 4. Improve dynamic follow-up input support

A dynamic outbound template is useful, but the next user response still depends on stage routing rules.

The engine would improve a lot with first-class support for:

- dynamic option registries,
- dynamic selection validation,
- stage-local dynamic choice models,
- easier mapping of button or list ids back into backend-owned state.

This matters for accounts, billers, offers, and payment instruments.

## 5. Strengthen hook contracts

Hooks are powerful, but they need more structure.

Key improvements:

- document expected return shapes more explicitly,
- make hook failure behavior more predictable,
- provide more typed examples,
- consider helper abstractions for common hook patterns.

This is especially important for Spring teams who want strong contracts.

## 6. Add more enterprise workflow examples

The engine would benefit from real examples for:

- backend-driven account selection
- dynamic biller flows
- OTP or step-up authentication
- preauth and payment review
- recoverable interrupted workflows

Examples matter here because they show how to use the engine without overloading templates.

## 7. Improve example and docs coverage for advanced dynamic handling

The advanced dynamic path was previously under-documented.

The engine should keep improving documentation for:

- full outbound template overrides,
- generic reusable stages,
- backend-driven workflow patterns,
- dynamic input capture strategies.

## 8. Consider a builder/helper API for dynamic templates

Building dynamic templates from raw maps works, but it is not the friendliest API.

The engine would feel more Java-native with helper builders such as:

- dynamic button template builder
- dynamic list template builder
- dynamic text template builder

This would reduce friction and make advanced hooks much cleaner.

## 9. Reduce reliance on internal or ambiguous session keys

The dynamic features currently lean on session-managed internal state.

That can work, but the engine should make more of that state:

- explicit,
- documented,
- testable,
- less surprising for adopters.

## 10. Improve compiler and build hygiene

There is still build noise such as the old compiler-plugin warning about unknown `parameters`.

That is not a functional bug, but it reduces confidence.

Cleaning these up will make the project feel more production-ready.
