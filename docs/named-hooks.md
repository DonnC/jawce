# Named Hooks

`jawce` now supports a registry-first hook model that resolves hook handlers at startup instead of discovering them during live traffic.

This is the recommended direction for new projects.

## Why use named hooks

Named hooks improve three things at once:

- templates stay readable because they reference stable names instead of Java class paths
- the engine validates hook wiring at startup
- runtime execution avoids repeated reflective lookup

Instead of:

```yaml
on-receive: com.example.booking.BookingHooks.captureLogin
router: com.example.booking.BookingHooks.routeAfterLogin
```

you can now use:

```yaml
on-receive: captureLogin
router: routeAfterLogin
```

## Preferred styles

`jawce` supports three styles today.

### 1. Typed named hook beans

This is the preferred path for production code.

Create a Spring bean that implements the hook contract for the phase you want:

- `IReceiveHook`
- `IGenerateHook`
- `IRouterHook`
- `IMiddlewareHook`
- `ITemplateHook`
- `IGenericHook`

Example:

```java
@NamedFlowHook("captureLogin")
@Component
public class CaptureLoginHook implements IReceiveHook {
    @Override
    public Hook execute(Hook hook) {
        return hook;
    }
}
```

Then reference it in YAML:

```yaml
"LOGIN":
  type: text
  on-receive: captureLogin
  message: "Enter PIN"
  routes:
    "re:.*": "POST-LOGIN"
```

The hook type is inferred from the interface it implements.

### 2. Named method hooks

This is the bridge option when you want one Spring service to expose several related hook methods without creating one class per hook.

Example:

```java
@Service
public class BookingHookActions {
    @NamedFlowHook(value = "captureLogin", type = FlowHookType.RECEIVE)
    public Hook captureLogin(Hook hook) {
        return hook;
    }

    @NamedFlowHook(value = "routeAfterLogin", type = FlowHookType.ROUTER)
    public Hook routeAfterLogin(Hook hook) {
        hook.setRedirectTo("HOME-MENU");
        return hook;
    }
}
```

Then use the names in YAML:

```yaml
"LOGIN":
  type: text
  on-receive: captureLogin
  message: "Enter PIN"
  routes:
    "re:.*": "POST-LOGIN"

"POST-LOGIN":
  type: text
  transient: true
  router: routeAfterLogin
  message: "Working..."
  routes:
    "re:.*": "HOME-MENU"
```

Rules for named method hooks:

- method must be on a Spring bean
- method must be annotated with `@NamedFlowHook`
- method must declare either no arguments or one `Hook` argument
- method must return `Hook`
- `type` must match the phase where the hook is referenced

### 3. Legacy reflective hook strings

This still works for compatibility:

```yaml
on-receive: com.example.booking.BookingHooks.captureLogin
```

`jawce` now caches the reflective execution plan, so even legacy hooks avoid repeated class and method discovery.

This remains supported, but it is no longer the recommended authoring style.

## Runtime behavior

At startup, `jawce` now:

- discovers typed hook beans
- discovers annotated named hook methods
- validates duplicate names per hook phase
- validates template references when templates load
- caches the hook invokers in memory

At runtime, the engine only needs:

- the hook name from the template
- the expected hook phase
- a registry lookup
- invocation of the cached plan

## Recommendation

Use this decision guide:

- choose typed named hook beans when you want the clearest long-term contract
- choose named method hooks when you want to keep a few related flow actions in one service
- use raw class-method strings only for backward compatibility or incremental migration

## Migration approach

If you already have legacy reflective hooks:

1. keep them working as-is
2. move hot or important hooks to named references first
3. convert grouped logic into named method hooks
4. convert critical flows to typed hook beans over time

This lets a project evolve without a big-bang rewrite.

## Example migration

The `ehailing` sample now uses the named-hook model.

Before:

```yaml
on-receive: "zw.co.dcl.ehailing.hooks.CaptureHook.capture"
```

After:

```yaml
on-receive: "captureRideRequest"
```

And the Spring bean method is:

```java
@Service
public class CaptureHook {
    @NamedFlowHook(value = "captureRideRequest", type = FlowHookType.RECEIVE)
    public Hook capture(Hook hook) {
        return hook;
    }
}
```

This is the recommended incremental migration path for existing bots:

1. keep the existing hook class
2. register it as a Spring bean
3. annotate the method with a stable hook name
4. update YAML to use the stable name
