# jawce Project Findings

## Overview

`jawce` is not just a Java rewrite of the WhatsApp client calls from `pywce`.
It is a Spring Boot port of the larger `pywce` idea:

- define conversation flow in templates,
- keep user progress in session state,
- let hooks provide business logic,
- generate WhatsApp Cloud payloads from engine templates,
- keep transport concerns separate from conversation orchestration.

After tracing both codebases, the Java port does preserve the core shape of `pywce`, but it does not yet have complete one-to-one behavior parity across every feature and message type.

The best way to describe it is:

- `jawce` successfully carries over the main engine model from `pywce`,
- it adds a strong Spring-oriented, event-driven, interface-first Java flavor,
- it still has a few gaps where the abstraction exists but the full runtime path is not yet complete.

## What The Original pywce Does Best

`pywce` is strongest when viewed as a stateful WhatsApp chatbot engine rather than just a WhatsApp API wrapper.

Its best ideas are:

1. template-driven conversation design,
2. session-backed stage transitions,
3. hook-based extension points,
4. built-in recovery and trigger handling,
5. a clean separation between engine templates and WhatsApp payload formatting.

The practical execution in Python is centered around:

- `Engine` for webhook entry and session bootstrapping,
- `Worker` for duplicate checks, debounce, inactivity, and error recovery,
- `MessageProcessor` for route selection, triggers, props, checkpoints, and hooks,
- `TemplateMessageProcessor` for rendering engine templates into WhatsApp requests.

That is the behavioral baseline for judging the port.

## What jawce Achieves Well

## 1. It keeps the same core engine idea

The Java port clearly preserves the same high-level architecture as `pywce`.

The closest Java equivalents are:

- `Worker` as the webhook orchestration and resilience layer,
- `WebhookProcessor` as the stage-resolution and pre/post-hook engine,
- `PayloadGenerator` as the outbound WhatsApp payload builder,
- `WhatsAppHelperService` as the transport helper and session update boundary.

This means the port is not superficial. It genuinely carries over the central model of:

- load current stage,
- inspect user input,
- resolve triggers or routes,
- run hooks,
- build the next outbound payload,
- update session state.

That is the most important part of `pywce`, and `jawce` does achieve it.

## 2. It gives the port its own Java and Spring identity

`jawce` is not trying to behave like Python written in Java. It leans into Spring Boot in useful ways.

Notable Java-native touches include:

- auto-configuration via `JawceAutoConfig`,
- interface-based extension through `ISessionManager`, `ITemplateStorageManager`, and `IClientManager`,
- Spring bean hook resolution before reflective fallback,
- REST hook support as a first-class option,
- event-driven processing with `WebhookEvent`, `OnceOffMessageEvent`, and `OnceOffHookEvent`,
- property-based configuration via `JawceConfig`.

This is a good adaptation. It keeps the template engine concept from `pywce`, but expresses extensibility in a way that fits a Spring application much better than a direct transliteration would.

## 3. It preserves the operational chatbot features that matter most

The Java engine includes many of the behaviors that make `pywce` more than a webhook router:

- duplicate message suppression,
- debounce handling,
- stale webhook rejection,
- checkpoint support,
- inactivity handling,
- prop persistence,
- dynamic router hooks,
- pre-send and post-receive hook phases,
- reply tagging, read receipts, typing indicators, and reactions.

Those behaviors are spread mainly across:

- `jengine/.../Worker.java`,
- `jengine/.../WebhookProcessor.java`,
- `jengine/.../BaseTemplateProcessor.java`,
- `jengine/.../WhatsAppHelperService.java`.

On that core behavioral layer, the port is directionally faithful to `pywce`.

## Where jawce Differs From pywce

These differences are not necessarily bad. Some are deliberate Java adaptations. Some are current limitations.

## 1. Trigger handling is less feature-complete than pywce

In `pywce`, built-in triggers such as `menu`, `back`, `retry`, and `report` are treated explicitly inside the engine flow.

In `jawce`, the trigger story is more centered on configured trigger routes plus retry/checkpoint logic inside `WebhookProcessor`.

What this means in practice:

- `jawce` still supports trigger-driven redirection,
- but the Python built-in trigger behavior is not mirrored as explicitly or as comprehensively in the Java code,
- some of the user-friendly fallback behavior from `pywce` looks less centralized in `jawce`.

So the port captures the idea of triggers, but not the exact Python trigger contract.

## 2. Some message-type support is modeled more broadly than it is executed

`jawce` advertises support for all official WhatsApp message types including flows.

The model layer does include a good spread of template classes, but the runtime payload generation path is narrower than the README suggests.

From the current `PayloadGenerator.generate()` switch, the implemented outbound paths are clearly present for:

- `text`,
- `button`,
- `list`,
- `cta`,
- `flow`,
- `media`,
- `document`,
- `image`,
- `request-location`.

There are gaps between the model surface and the executed payload surface:

- `dynamic` is modeled but not emitted directly by the generator,
- WhatsApp `template` messages are modeled but not handled in the generator switch,
- `location` has a generator method but is not reachable from the current switch,
- catalog and product-style message coverage is not carried through end-to-end the way `pywce` does.

So the Java port has a strong foundation, but it should not yet be described as full parity with the Python engine's message coverage.

## 3. Dynamic template support is partially there but still rough

`jawce` has real support for dynamic behavior:

- `TemplateDynamicBody`,
- dynamic session keys,
- nested preprocessing logic,
- hook-driven render payload injection,
- dynamic next-template handling.

That is promising and clearly inspired by the Python engine.

At the same time, the implementation still shows signs of incompleteness:

- the logic is more complex and less documented,
- `WebhookProcessor` includes a comment acknowledging unclear dynamic-stage behavior,
- some dynamic and template paths exist in the model but are not fully completed in payload generation.

This is one of the main areas where the Java port has the right ambition but still needs hardening.

## 4. The Spring event model is a real improvement, not just a difference

This is one area where `jawce` arguably has a stronger platform story than `pywce`.

The event-driven pieces make it easier to:

- dispatch background-like work inside a Spring application,
- send once-off messages,
- trigger side effects decoupled from the immediate controller flow,
- integrate with broader enterprise application structure.

That is a genuine Java-side advantage and a good example of the port adding its own touch without abandoning the original idea.

## Where jawce Is Currently Weaker

## 1. The test coverage does not yet prove engine parity

The Python project has focused tests around engine flow, hooks, resilience, and transport contracts.

In the Java repo, the visible tests are currently much lighter:

- Spring context load tests,
- a session manager test,
- example application smoke tests.

What is missing is the same kind of engine behavior characterization that exists on the Python side, for example:

- start-stage behavior,
- next-route transitions,
- invalid-input recovery,
- trigger behavior,
- checkpoint behavior,
- hook ordering,
- dynamic route behavior,
- payload contract checks by message type.

So even where the Java code looks right by inspection, the parity claim is weaker because it is less protected by tests.

## 2. Some README claims are broader than the current source supports

The `jawce` README says the engine:

- supports all official WhatsApp message types including flows,
- abstracts the WhatsApp Cloud API so you can focus on chatbot logic.

The second statement is fair.

The first statement is only partially true in the current source state. The model set is broad, but the executable payload generation path is not yet complete enough to claim feature-complete parity with `pywce`.

## 3. Some implementation areas still need documentation or cleanup

There are a few signs that the architecture is ahead of the implementation polish:

- dynamic-template processing is clever but under-documented,
- some modeled features do not yet fully connect to runtime generation,
- error and retry behavior is practical but not yet backed by a strong engine test matrix,
- message support is easier to infer from source than from a precise support table.

This does not make the port weak. It means the port is promising and substantial, but still maturing.

## Bottom Line

`jawce` does achieve the main thing that matters from `pywce`:
it ports the template-driven, session-backed WhatsApp chatbot engine concept into Java and Spring Boot in a credible way.

It is not merely a Java WhatsApp client. It preserves the original framework idea:

- templates define the flow,
- session stores the state,
- hooks provide business logic,
- the engine handles routing and orchestration,
- transport stays separate.

That said, the current Java port is best described as:

- architecturally faithful to `pywce`,
- meaningfully adapted for Spring Boot,
- partially but not completely behavior-parallel with the Python version.

If the question is "does `jawce` capture what `pywce` is trying to do?", the answer is yes.

If the question is "is `jawce` already a complete, one-for-one parity port of `pywce`?", the answer is not yet.

## Recommended Next Steps

To move `jawce` from a strong port to a more confidently equivalent engine, the highest-value next steps are:

1. add Java engine characterization tests that mirror the Python engine tests,
2. complete the missing payload generation paths for `dynamic`, `template`, `location`, and catalog/product-style messages where intended,
3. document the exact trigger contract in Java, especially where it intentionally differs from `pywce`,
4. simplify or better document dynamic-template routing and nested preprocessing,
5. replace broad README feature claims with a precise support matrix until parity is complete.

## Validation Notes

These findings are based on source inspection across both repositories, especially:

- `pywce` engine and service flow,
- `jawce` worker, processor, hook, and payload generation layers,
- available docs and examples in both projects.

I could not fully validate runtime behavior from tests in this environment because:

- the local Python test run could not start without `ruamel.yaml`,
- the Maven wrapper commands did not execute cleanly from this shell session.

So this document is a code-driven assessment, not a fully executed parity certification.
