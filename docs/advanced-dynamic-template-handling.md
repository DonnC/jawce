# Advanced Dynamic Template Handling

This guide covers the more advanced pattern where the backend or hook builds the actual outbound template at runtime.

This is different from:

- placeholder substitution
- static template variants plus router branching

This pattern is useful when one business step may need to become:

- `text`
- `button`
- `list`
- or another supported message type

based on live business logic.

## 1. When to use this pattern

Use it when:

- the backend determines the best display type,
- the option count changes dynamically,
- the exact outbound message shape is unknown until runtime,
- you want to avoid maintaining many near-duplicate YAML stages.

Your account-selection example is a perfect fit:

- if accounts <= 3, show buttons
- if accounts <= 10, show a list
- otherwise show text

## 2. What now works in `jawce`

`jawce` now supports a first-class dynamic-stage path where:

- `type: dynamic` marks the stage as runtime-rendered
- `on-generate` prepares context before rendering
- `dynamic` returns the fully built `BaseEngineTemplate` for the current outbound response

That means a hook can dynamically supply:

- a `TextTemplate`
- a `ButtonTemplate`
- a `ListTemplate`

for the same logical stage.

This is done through `Hook.templateDynamicBody.template`.

## 3. Recommended stage design

Keep the stage itself generic.

Example:

```yaml
"ACCOUNT-SELECT":
  type: dynamic
  on-generate: "com.example.billing.AccountSelectorHook.prepare"
  dynamic: "com.example.billing.AccountSelectorHook.render"
  message: "placeholder"
  routes:
    "re:.*": "ACCOUNT-CONFIRM"
```

Important point:

- the static YAML stage is only a shell,
- `on-generate` prepares anything the dynamic hook needs,
- the `dynamic` hook decides the final outbound template,
- the route can stay generic if the backend validates the chosen input afterward.

## 4. How the hook should think

The hook should:

1. fetch or read the business data
2. decide the display mode
3. build the final template object
4. return it in `templateDynamicBody.template`

Do not make the YAML explode just to express presentation variants.

## 5. Example hook

```java
package com.example.billing;

import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AccountSelectorHook {
    public Hook prepare(Hook hook) {
        hook.getSession().save(hook.getSessionId(), "prompt", "Select a payment account");
        return hook;
    }

    @SuppressWarnings("unchecked")
    public Hook render(Hook hook) {
        List<String> accounts = hook.getSession().getGlobal("accounts", List.class);
        String prompt = String.valueOf(hook.getSession().get(hook.getSessionId(), "prompt"));

        Map<String, Object> templateMap;

        if (accounts.size() <= 3) {
            templateMap = Map.of(
                    "type", "button",
                    "message", Map.of(
                            "body", prompt,
                            "buttons", accounts
                    )
            );
        } else if (accounts.size() <= 10) {
            Map<String, Object> rows = new LinkedHashMap<>();
            for (int i = 0; i < accounts.size(); i++) {
                rows.put(
                        "acc-" + (i + 1),
                        Map.of(
                                "title", accounts.get(i),
                                "description", "Select " + accounts.get(i)
                        )
                );
            }

            templateMap = Map.of(
                    "type", "list",
                    "message", Map.of(
                            "body", prompt,
                            "button", "Accounts",
                            "sections", Map.of("Accounts", rows)
                    )
            );
        } else {
            StringBuilder body = new StringBuilder(prompt);
            for (int i = 0; i < accounts.size(); i++) {
                body.append("\\n").append(i + 1).append(". ").append(accounts.get(i));
            }

            templateMap = Map.of(
                    "type", "text",
                    "message", body.toString()
            );
        }

        hook.setTemplateDynamicBody(
                TemplateDynamicBody.builder()
                        .template(SerializeUtils.toTemplate(templateMap))
                        .build()
        );

        return hook;
    }
}
```

## 6. Why this works well

This pattern keeps one logical stage while allowing many presentation shapes.

That is a strong fit when:

- the backend is the real workflow owner,
- the bot is just the channel adapter,
- presentation is chosen at runtime.

## 7. What to be careful about

This pattern solves dynamic rendering, but not everything automatically.

`jawce` still keeps compatibility with the older pattern where:

- a `type: dynamic` stage uses `template` to return the runtime template body
- or a normal static stage uses `on-generate` to replace the outbound template directly

That remains supported, but for new work the clearer contract is:

- full runtime message shaping: `type: dynamic` + `dynamic`
- light preparation: `on-generate`
- render/body shaping for known message types: `template`

### Input handling

If the hook returns buttons or a list, the next input will still be processed by the stage’s routing logic.

So you should usually pair this with one of these:

- a generic `re:.*` route and backend validation in `on-receive`
- a router step after selection
- session-backed option lookup

That is often enough for account selection, biller selection, and payment choice flows.

### Keep business truth in the backend

Do not let the hook become a mini database of products, accounts, billers, or validation rules.

The hook should translate backend state into channel presentation.

### Reuse generic capture stages

This works best when the next stage is also generic.

For example:

- `ACCOUNT-SELECT`
- `FIELD-CAPTURE`
- `PREAUTH`
- `PAYMENT-REVIEW`

## 8. Best fit scenarios

This advanced dynamic-template path is especially useful for:

- account selection
- dynamic biller selection
- backend-driven menus
- payment option selection
- offer presentation
- eligibility-driven user journeys

## 9. When router is still better

Do not force this pattern everywhere.

Router variants are still simpler when:

- there are only a few stable presentation outcomes,
- each variant has meaningfully different follow-up behavior,
- you want the YAML flow to stay highly visible.

Use the advanced template path when the runtime flexibility clearly outweighs the simpler static flow.
