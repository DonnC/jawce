# jawce Template Compatibility

## Goal

`jawce` should keep template authoring familiar to `pywce` users, but not at the expense of forcing Java applications into Python-shaped infrastructure.

The compatibility target is:

- high portability for stage templates,
- low migration friction for common flows,
- explicit documentation where Java behavior differs,
- hook and infrastructure adaptation happening in Java code, not through major template rewrites.

## Compatibility Principles

## 1. Templates should stay familiar

A developer coming from `pywce` should still recognize:

- stage-based flow definitions,
- YAML and JSON template files,
- route-driven next-stage transitions,
- props and checkpoints,
- dynamic render concepts,
- trigger-oriented navigation.

## 2. Hooks will adapt more than templates

The main migration work should usually be in:

- hook implementation,
- Spring wiring,
- session and storage configuration,
- external system integration.

The main migration work should usually not be in:

- renaming most stage files,
- redesigning the route graph,
- rewriting core template structure from scratch.

## 3. Interfaces protect infrastructure freedom

`jawce` intentionally uses interfaces so the framework does not assume a single storage or session approach.

Key contracts:

- `ISessionManager`
- `ITemplateStorageManager`
- `IClientManager`

This means:

- the library can provide a basic default or reference implementation,
- developers can replace it with Redis, JDBC, JPA, file, cloud, or internal platform implementations,
- template portability does not depend on one infrastructure choice.

## What Should Migrate Cleanly

The following concepts should remain as close as possible between `pywce` and `jawce`:

| Concept | Portability Target | Notes |
|---|---|---|
| Stage names | High | Existing route names should usually carry over unchanged. |
| YAML or JSON templates | High | File format familiarity should remain. |
| Route structure | High | Static next-stage routing should feel the same. |
| Regex routes | High | Matching semantics should stay close and be documented. |
| `prop` behavior | High | Captured user input should map into session props similarly. |
| `checkpoint` behavior | High | Checkpoint authoring should remain familiar. |
| `on_receive` | High | Same concept, Java implementation style differs. |
| `on_generate` | High | Same concept, Java implementation style differs. |
| `router` | High | Same concept, contract should be documented clearly. |
| Dynamic rendering | Medium to High | Same idea, but Java implementation details are different. |
| Trigger-driven navigation | Medium to High | Similar concept, exact built-in behavior should be documented. |

## What Usually Changes During Migration

These are the areas where a `pywce` user should expect some adaptation:

| Area | Expected Change |
|---|---|
| Hook code | Python functions become Spring beans, Java classes, or REST hooks. |
| Dependency setup | Python package setup becomes Spring Boot bean wiring and config properties. |
| Session backend | Python default session handling may become a custom Java implementation. |
| Template storage | Java apps may load templates from classpath, files, or custom repositories. |
| External services | Java apps may route integrations through typed services, events, or internal clients. |

## Hook Mapping Guide

The conceptual mapping should be:

| pywce Concept | jawce Equivalent |
|---|---|
| Python hook function | Java method on a class, Spring bean method, or REST endpoint |
| `HookArg` | `Hook` |
| `template_body.render_template_payload` | `templateDynamicBody.renderPayload` |
| router redirect | `redirectTo` |
| flow payload data | `templateDynamicBody.flowPayload` |

The important rule is that `jawce` should preserve the hook lifecycle idea, even when the implementation style changes.

## Infrastructure Mapping Guide

`pywce` users should understand this early:

- `jawce` is not telling them to use Redis,
- `jawce` is not telling them to use JDBC,
- `jawce` is not telling them to store templates in a database,
- `jawce` is giving them interfaces so they can decide those things.

The framework can reasonably ship:

- a basic file-backed session example,
- a YAML or JSON template storage implementation,
- sample REST client wiring.

The application team can then replace those with:

- a Redis `ISessionManager`,
- a JDBC or JPA `ITemplateStorageManager`,
- a custom `IClientManager`,
- tenant-specific or compliance-specific adapters.

## Migration Guidance For Teams

If you are moving from `pywce` to `jawce`, the safest approach is:

1. keep your stage names and template layout as close as possible,
2. port the easiest hooks first,
3. verify route and trigger behavior with tests,
4. replace only the infrastructure pieces your Java application actually needs,
5. avoid over-customizing the engine until baseline compatibility is proven.

## Special Note For Enterprise Teams

For enterprise teams such as banks, the framework should support your architecture without assuming it.

That means `jawce` should help you implement:

- audit-friendly sessions,
- secure template access,
- internal service calls,
- data retention policies,
- observability and traceability,
- controlled deployment patterns.

But those choices should come from your application layer through the engine interfaces, not from a hardcoded framework dependency.
