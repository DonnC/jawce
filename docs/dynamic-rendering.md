# Dynamic Rendering In `jawce`

`jawce` can support dynamic message generation, but the cleanest approach is to keep the conversation flow stable and let hooks decide what to render or where to route next.

This is the important mindset:

- templates should still define the main conversation skeleton,
- hooks should supply business data and dynamic decisions,
- backend systems should own complex product or biller logic,
- the bot should not explode into one YAML stage per backend variation.

## 1. What is easy today

The easiest dynamic patterns in `jawce` today are:

1. static template + dynamic placeholders
2. static stage + `on-generate` hook
3. dynamic next-stage selection through `router`
4. generic reusable stages backed by session data

These are already a good fit for most production bots.

## 2. The main hook points

`jawce` exposes the main extension points on a template:

- `on-receive`
- `on-generate`
- `dynamic`
- `router`
- `middleware`
- `template`

The practical meaning is:

- `on-receive` runs after user input is received
- `on-generate` runs before the next outbound message is generated
- `dynamic` decides the next message or template body to render, usually from backend-driven logic
- `router` can override the next stage dynamically
- `middleware` can apply cross-cutting logic
- `template` affects the rendered message body and render payloads for known template types

## 2.1 Dynamic stage contract

`jawce` now treats dynamic rendering with a clearer split:

- `type: dynamic` means the stage's final outbound shape is decided at runtime
- `on-generate` prepares context before that outbound message is built
- `dynamic` selects the actual runtime template or message to render
- `template` remains the body/render hook, including compatibility with older dynamic-template behavior

That means the most canonical dynamic pattern is:

```yaml
"ACCOUNT-SELECT":
  type: dynamic
  on-generate: "com.example.billing.AccountSelectorHook.prepare"
  dynamic: "com.example.billing.AccountSelectorHook.render"
  message: "placeholder"
  routes:
    "re:.*": "ACCOUNT-CONFIRM"
```

Use `on-generate` for preparation.
Use `dynamic` for full runtime message selection.
Use `template` when the message type is already known and you are shaping its body or render payload.

## 3. Recommended dynamic rendering pattern

For most bots, do not try to fully synthesize the whole conversation graph dynamically.

Instead:

1. keep a small number of stable stages in YAML
2. let hooks fetch backend data
3. store that data in session props or session keys
4. let the next template render from those values
5. use `router` only when the next stage must be decided dynamically

This keeps the bot readable and testable.

## 4. Dynamic text, button, or list from one business step

Imagine one stage called `SHOW-OFFER`, but sometimes you want:

- a plain text message,
- a button message,
- a list message.

The cleanest approach is not to force one stage to become every possible payload shape.

Instead use:

- `SHOW-OFFER-TEXT`
- `SHOW-OFFER-BUTTON`
- `SHOW-OFFER-LIST`

and let a `router` hook decide which one to use.

That is much easier to reason about than building a giant fully dynamic outbound message factory inside templates.

## 5. Example approach

### Template

```yaml
"SHOW-OFFER":
  type: dynamic
  router: "com.example.bot.hooks.OfferRouter.route"
  transient: true
  routes:
    "re:.*": "SHOW-OFFER-TEXT"

"SHOW-OFFER-TEXT":
  type: text
  on-generate: "com.example.bot.hooks.OfferRenderHook.prepare"
  message: "{{ s.offerMessage }}"
  routes:
    "re:.*": "NEXT-STAGE"

"SHOW-OFFER-BUTTON":
  type: button
  on-generate: "com.example.bot.hooks.OfferRenderHook.prepare"
  message:
    body: "{{ s.offerMessage }}"
    buttons:
      - "{{ s.primaryAction }}"
      - "{{ s.secondaryAction }}"
  routes:
    "accept": "NEXT-STAGE"
    "cancel": "START-MENU"

"SHOW-OFFER-LIST":
  type: list
  on-generate: "com.example.bot.hooks.OfferListHook.prepare"
  message:
    body: "{{ s.offerMessage }}"
    button: "Choose"
    sections:
      "Options":
        "default":
          title: "Default"
  routes:
    "re:.*": "NEXT-STAGE"
```

### Router hook

```java
package com.example.bot.hooks;

import zw.co.dcl.jawce.engine.model.core.Hook;

public class OfferRouter {
    public Hook route(Hook hook) {
        String displayType = String.valueOf(
                hook.getSession().get(hook.getSessionId(), "offerDisplayType")
        );

        if ("button".equalsIgnoreCase(displayType)) {
            hook.setRedirectTo("SHOW-OFFER-BUTTON");
        } else if ("list".equalsIgnoreCase(displayType)) {
            hook.setRedirectTo("SHOW-OFFER-LIST");
        } else {
            hook.setRedirectTo("SHOW-OFFER-TEXT");
        }

        return hook;
    }
}
```

### Render hook

```java
package com.example.bot.hooks;

import zw.co.dcl.jawce.engine.model.core.Hook;

import java.util.List;

public class OfferRenderHook {
    public Hook prepare(Hook hook) {
        hook.getSession().save(hook.getSessionId(), "offerMessage", "Your bundle is ready");
        hook.getSession().save(hook.getSessionId(), "primaryAction", "Accept");
        hook.getSession().save(hook.getSessionId(), "secondaryAction", "Cancel");
        return hook;
    }
}
```

This is simple and Spring-friendly:

- router decides the template kind
- render hook fills data
- YAML remains readable

## 6. Advanced dynamic body handling

There is also a more advanced dynamic-body path in the engine:

- `TemplateDynamicBody`
- `templateDynamicBody.template`
- `templateDynamicBody.renderPayload`
- dynamic session keys managed internally by the processor

This is the more "fully dynamic" path, and `type: dynamic` is now the clearest public entry point for it.

It can be useful when you want a hook to construct a template body dynamically at runtime instead of just filling placeholders.

But today it should be treated carefully because it is:

- less documented than the normal route-based pattern,
- harder to reason about,
- more engine-internal in feel than the stable YAML stage approach.

For most real bots, prefer:

- stable stages,
- reusable generic steps,
- router hooks,
- session-backed render data.

Use the deeper dynamic-body path only when the normal pattern becomes clearly insufficient.

## 7. When to use `templateDynamicBody.renderPayload`

The current engine already uses render payload data for cases like:

- WhatsApp template component generation
- flow initial payload data

That means `template` or render-oriented hooks are a good fit when:

- the stage type is known,
- but parts of the outbound payload must be computed dynamically.

That is different from `type: dynamic`, where the hook returns the actual runtime `BaseEngineTemplate`.

Examples:

- populate WhatsApp template components
- populate flow initial data
- inject computed variables for rendering

## 8. When not to go fully dynamic

Avoid fully dynamic outbound generation when the real problem is just business branching.

If the backend is deciding:

- eligibility,
- available options,
- next action,
- validation requirements,

then often the right answer is:

- generic reusable stages,
- session state,
- router hooks,
- a backend workflow service.

Not:

- hundreds of static stages,
- or one giant hook that tries to do everything.

## 9. Recommended production rule

Use this order of preference:

1. placeholders in stable templates
2. `on-generate` to prepare data for those templates
3. `router` to choose between a few stable template shapes
4. advanced dynamic-body generation only when necessary

That gives the best balance between flexibility and maintainability.
