My approach would be to split this into 3 layers and make the engine runtime resolve everything before traffic starts.

**1. Core idea**
I would treat the flow engine as an orchestrator, not as the place where business logic lives.

So the responsibilities become:

- engine: current stage, next stage, input capture, rendering, session updates
- hook/action layer: small executable units the engine can call
- domain services: payment, validation, customer lookup, billers, eligibility, OTP, etc.

That separation matters because it keeps the flow system simple even when the product gets complicated.

**2. Runtime model I would prefer**
At startup, the app should discover and register all callable handlers once.

That means:

- read all beans that qualify as flow handlers
- inspect supported hook types
- validate their signatures
- assign them stable names
- put them into a registry
- fail fast if a referenced handler is missing or invalid

Then at runtime, the engine only does:

- read stage config
- look up handler by name in registry
- call prebuilt invoker
- get result
- continue flow

So no repeated reflective lookup in the request path.

**3. The form of handlers I prefer**
I would prefer named handlers over raw `com.example.Foo.bar` strings.

Why:
- easier template authoring
- safer refactors
- less leakage of Java internals into templates
- easier validation and documentation

So instead of templates knowing classes and methods, they should know stable engine action names like:

- `captureAccount`
- `validateMeterNumber`
- `billingRouter`
- `loadBillerMenu`

The registry maps those names to actual implementations.

**4. What I would support**
I’d support 3 execution styles, but rank them:

1. direct bean handler
   Best for same-project logic.

2. service gateway bean
   Best when the handler needs to call another internal module or another app, but through a dedicated service abstraction.

3. external HTTP adapter
   Best only when the dependency is truly external or intentionally separated.

I would avoid “same app REST controller calling” for engine internals. If it’s the same Spring app, call the service directly.

**5. How I’d think about hook categories**
I would make hook types explicit because they do different jobs.

For example:

- `on-generate`
  Build prompt data or dynamic screen/message content.
- `on-receive`
  Validate/store/process the user input.
- `router`
  Decide next stage.
- maybe `pre-hook` / `post-hook`
  For cross-cutting flow concerns.

This helps because each one can have a narrow contract. Narrow contracts scale much better than one giant “universal hook” object.

**6. My ideal contract style**
I would prefer typed contracts over generic reflective methods.

Not because reflection is evil, but because typed contracts give you:

- compile-time help
- cleaner tests
- easier onboarding
- simpler tracing
- safer evolution

So the engine should know the difference between:
- a handler that returns render data
- a handler that returns a redirect
- a handler that returns validation outcome
- a handler that returns a final action result

That is cleaner than having one method return a loose map for everything.

**7. If you must keep method-string references**
If you want to preserve the current flexibility of referencing a bean method by string, I would still not use that raw form during requests.

I would do this:

- parse it once at startup
- resolve bean once
- resolve method once
- validate parameter and return types once
- create an invoker object once
- cache it in a registry forever

Then runtime execution is just registry lookup plus invoke.

So reflection becomes part of bootstrapping, not part of the hot path.

**8. What I think matters more than reflection for performance**
Reflection overhead is real, but in these systems it usually isn’t the first bottleneck.

The bigger factors are:

- database/session storage latency
- network calls
- template loading/parsing
- serialization
- thread blocking
- retries and timeout handling
- duplicate processing
- session contention

So my performance priority order would be:

1. avoid internal HTTP if same app
2. keep handlers pre-resolved
3. cache templates and route metadata
4. keep session access efficient
5. isolate slow downstream systems
6. make each flow step idempotent
7. add metrics per stage and per handler

**9. My scalability opinion**
For scale, I would optimize the system toward stateless engine nodes and externalized session state.

That means:

- multiple app instances can serve any user
- session state lives in Redis or a durable shared store
- handlers remain stateless
- business services are independent of engine transport
- engine instances only orchestrate and render

That architecture works well for both WhatsApp and USSD.

**10. What I would avoid**
I would avoid these patterns:

- templates pointing directly to arbitrary classes everywhere
- per-request reflection lookup
- internal REST calls to your own app for same-process logic
- business logic living inside router hooks
- one mega hook doing validation, rendering, routing, and payment
- generic maps everywhere with no typed contract

Those patterns work early, but they become painful under load and during maintenance.

**11. The practical architecture I’d choose**
If I were building this seriously, I’d choose:

- template references stable handler names
- Spring scans and registers handlers at startup
- registry stores typed invokers
- engine orchestrates only
- domain services handle actual business work
- external HTTP only behind dedicated gateway services
- sessions in shared store
- metrics and tracing around every handler/stage

So in one sentence: my approach is “resolve once, validate once, cache forever, invoke cheaply, and keep business logic outside the flow engine.”

If you want, next I can show you the exact tradeoff between these 3 choices in your context:
- raw reflection method strings
- named registry handlers
- fully typed interface-per-hook model

---
Here’s the tradeoff the way I’d frame it for your case.

**1. Raw reflection method strings**
Example template idea:
- `on-receive: com.myapp.billing.CaptureHook.capture`
- `router: com.myapp.billing.WorkflowRouter.route`

**Pros**
- very flexible
- easy to start with
- no extra registry design needed
- lets developers wire almost anything quickly

**Cons**
- weak refactor safety
- easy to break silently at runtime
- templates become coupled to Java internals
- harder startup validation unless you add it yourself
- harder to document and standardize across teams
- per-request reflection is a bad steady-state model

**My opinion**
Good for early prototyping, not ideal as the long-term public developer experience.

---

**2. Named registry handlers**
Example template idea:
- `on-receive: captureAccount`
- `router: billingWorkflowRouter`

The app startup maps those names to real bean methods or handler classes.

**Pros**
- much cleaner authoring experience
- templates stay stable even if Java classes move
- strong startup validation
- reflection can be limited to boot time only
- good balance between flexibility and maintainability
- easier to add docs, metadata, and tooling later

**Cons**
- requires a registry/bootstrap design
- still may hide type mismatches if contracts are too loose
- naming discipline matters

**My opinion**
This is the sweet spot for your engine family.

---

**3. Fully typed interface-per-hook**
Example concept:
- `on-receive` handlers must implement one contract
- `router` handlers must implement another
- `on-generate` handlers must implement another

**Pros**
- strongest type safety
- best long-term maintainability
- best IDE support
- easiest testing model
- easiest to enforce conventions and quality across teams

**Cons**
- more structure upfront
- slightly less “free-form”
- developers may need to create more small classes
- can feel rigid if not designed well

**My opinion**
Best for mature production systems, especially if you expect many teams, many apps, or regulated environments.

---

**What I would choose**
I would combine 2 and 3:

- developer-facing templates use stable names
- runtime uses a registry
- registered handlers implement typed contracts

So the developer sees a simple named hook model, while the engine gets the safety of typed handlers.

That gives you:
- clean templates
- strong startup validation
- low runtime overhead
- safer scaling
- better DX

**The developer experience I’d aim for**

Instead of asking developers to expose random methods by class path, I’d want them to think like this:

1. Create a handler for a specific responsibility
   Examples:
- capture meter number
- load available billers
- route next step
- confirm payment review

2. Register it with a stable engine name
   Examples:
- `captureMeterNumber`
- `loadBillerCatalog`
- `paymentRouter`

3. Reference that name in templates
   No Java package names in templates.

4. Keep business logic in services
   Handlers orchestrate, services decide.

---

**How the new approach would look to the developer**

From the developer’s point of view, they’d work with three things:

**A. A template**
Something like:

```yaml
START:
  type: menu
  message: "Welcome\n1. Buy Power\n2. Pay Water"
  routes:
    "1": BILLER_FLOW_START
    "2": WATER_FLOW_START

BILLER_FLOW_START:
  type: input
  on-generate: loadBillerPrompt
  on-receive: captureBillerSelection
  router: billerSelectionRouter

FIELD_CAPTURE:
  type: input
  on-generate: fieldPromptGenerator
  on-receive: captureFieldValue
  router: workflowRouter

PAYMENT_CONFIRM:
  type: confirm
  on-generate: paymentReviewGenerator
  on-receive: confirmPaymentAction
  router: paymentRouter
```

Notice what’s nice here:
- no class names
- no reflection syntax
- names describe behavior
- template stays business-readable

---

**B. A handler**
The developer implements a clearly defined handler type.

Conceptually:

```java
@NamedFlowHandler("captureFieldValue")
class CaptureFieldValueHandler implements OnReceiveHook {
    execute(context) -> validate/store the input and return outcome
}
```

Or:

```java
@NamedFlowHandler("workflowRouter")
class WorkflowRouterHandler implements RouterHook {
    execute(context) -> decide next stage from session + backend state
}
```

The key idea is:
- one handler = one job
- explicit contract
- predictable return type

---

**C. A service**
The actual business logic sits below the handler.

Conceptually:

```java
class BillingWorkflowService {
    nextField(...)
    validateField(...)
    preauth(...)
    submitPayment(...)
}
```

Then the handler becomes thin:

- read session/input
- call service
- update flow context
- return result

That is much cleaner than putting real business rules in the engine layer.

---

**What startup would do behind the scenes**
Without exposing complexity to the developer, the engine would:

- scan for flow handler beans
- group them by contract type
- register them by stable name
- validate duplicates
- validate template references
- fail fast if `workflowRouter` is missing
- optionally expose a startup report of registered handlers

That gives the developer confidence early instead of runtime surprises.

---

**What I’d want the developer to feel**
The desired DX is:

- “I create small named handlers.”
- “I wire them in YAML by simple names.”
- “I put real domain logic in services.”
- “If I misconfigure something, the app fails at startup.”
- “I don’t need to know reflection internals.”

That’s a much better product experience than:
- “put a fully qualified class and method string into YAML and hope it resolves later.”

---

**How I’d position the options for your engine users**

If you want flexibility tiers, I’d expose them like this:

1. `preferred`
   Named typed handlers
   Best for most developers.

2. `advanced`
   Named method-backed handlers discovered at startup
   Useful when someone wants method-style wiring without full handler classes.

3. `legacy/compatibility`
   Raw class-method strings
   Still supported if needed, but not the recommended authoring style.

This lets you evolve the engine without breaking people immediately.

---

**My final recommendation**
For `jawce`-style engines I would make the official developer model:

- templates reference stable hook names
- hooks are registered Spring beans
- hooks implement typed contracts
- services carry business logic
- startup validates everything
- runtime dispatch avoids repeated reflection

That gives you the best balance of:
- performance
- scalability
- team maintainability
- developer friendliness

If you want, I can next show you a more concrete side-by-side of:
- current developer experience in your reflection style
- improved named-registry style
- improved typed-handler style

using one real example like bill payment or account opening.

---

They’re two possible levels of abstraction.

`FlowAction` is the simpler, more unified model.
`RouterHook`, `OnReceiveHook`, `OnGenerateHook` are the more explicit, typed model.

My recommendation: use the typed model internally and present simple names to developers in templates.

So this is how they relate.

**Option 1: One generic contract**
Everything implements one interface:

```java
public interface FlowAction {
    ActionResult execute(FlowContext context);
}
```

Then:
- `captureAccount` is a `FlowAction`
- `billingRouter` is also a `FlowAction`
- `loadPrompt` is also a `FlowAction`

The difference is not the interface, but where the engine uses it.

For example:
- `on-receive: captureAccount` means call that action during receive phase
- `router: billingRouter` means call that action during routing phase

This is flexible, but weaker semantically because all handlers look the same.

---

**Option 2: Typed hook contracts**
This is the cleaner design for a serious engine.

You define separate interfaces for separate responsibilities.

For example:

```java
public interface OnReceiveHook {
    ReceiveResult execute(FlowContext context);
}
```

```java
public interface OnGenerateHook {
    GenerateResult execute(FlowContext context);
}
```

```java
public interface RouterHook {
    RouteResult execute(FlowContext context);
}
```

Then:
- `captureAccount` implements `OnReceiveHook`
- `billingRouter` implements `RouterHook`
- `loadPrompt` implements `OnGenerateHook`

This is usually what I’d recommend.

---

**So which is which?**
In your example:

```yaml
on-receive: captureAccount
router: billingRouter
```

That should mean:

- `captureAccount` = a registered handler for the receive phase
- `billingRouter` = a registered handler for the routing phase

So conceptually:

```java
@Component("captureAccount")
class CaptureAccountHook implements OnReceiveHook {
    ...
}
```

```java
@Component("billingRouter")
class BillingRouterHook implements RouterHook {
    ...
}
```

That is the cleanest mapping.

---

**How the engine would think about it**
When the engine reads:

```yaml
on-receive: captureAccount
```

it should look up `captureAccount` in the `OnReceiveHook` registry.

When it reads:

```yaml
router: billingRouter
```

it should look up `billingRouter` in the `RouterHook` registry.

So the same name cannot just mean “anything”. It belongs to a hook category.

That gives you safety:
- a router can’t accidentally be registered where a receive hook is expected
- a generate hook can’t accidentally be used as a router
- startup validation becomes clearer

---

**Why I prefer typed hooks over one `FlowAction`**
Because the return types are naturally different.

For example:

`OnReceiveHook` usually cares about:
- validating input
- storing values
- maybe setting error state
- maybe indicating whether input is accepted

`RouterHook` usually cares about:
- deciding next stage
- maybe deciding stay/retry/end

`OnGenerateHook` usually cares about:
- building screen/message data
- maybe dynamic options
- maybe final rendered content parts

If you force all of those into one `ActionResult`, it works, but it tends to become a big generic result object with too many nullable fields.

That gets messy over time.

---

**What I would likely define**
I’d define something like this conceptually:

- `FlowContext`
  Common input to all hooks

Contains things like:
- session
- current stage
- current input
- channel
- user info
- engine services
- maybe request metadata

Then:

- `OnReceiveHook -> ReceiveResult`
- `OnGenerateHook -> GenerateResult`
- `RouterHook -> RouteResult`

Each result should be narrow.

Example idea:

`ReceiveResult`
- accepted/rejected
- validation message
- updated props or state markers

`GenerateResult`
- render data
- maybe dynamic choices
- maybe full override for advanced cases

`RouteResult`
- next stage
- maybe terminal/end flag
- maybe redirect reason

---

**What the developer would do**
The developer should not usually think in terms of reflection or registry mechanics.

They should think:

- “I need to process input here” -> implement `OnReceiveHook`
- “I need to compute the next stage” -> implement `RouterHook`
- “I need to build dynamic prompt content” -> implement `OnGenerateHook`

Then register by name and use the name in YAML.

So to answer your question directly:

- If it is used by `on-receive`, define it as `OnReceiveHook`
- If it is used by `router`, define it as `RouterHook`
- If it is used by `on-generate`, define it as `OnGenerateHook`

`FlowAction` is just a simplified alternative if you want one generic interface for everything.

---

**My practical recommendation for you**
If you want a robust engine, do this:

- keep `FlowContext` shared
- make hook interfaces separate
- keep template references simple names
- maintain a registry per hook type

So:
- `ReceiveHookRegistry`
- `GenerateHookRegistry`
- `RouterHookRegistry`

That is cleaner than one global registry of all callables.

---

**When would I still use `FlowAction`?**
I’d use a single `FlowAction` only if:
- you want a very lightweight engine
- you want minimal framework code
- you are still exploring the right contract shapes
- you expect lots of custom action categories soon

But for your use case, especially with banking/billers/USSD/WhatsApp-style flows, I think typed hooks are the better long-term choice.

If you want, I can next show a concrete mini example of the three together:
- `OnGenerateHook`
- `OnReceiveHook`
- `RouterHook`

for one flow like “capture meter number then route to confirm or retry”.

---
Right, that part sits under the developer experience. The developer should barely notice it, but the engine should rely on it heavily.

When I say “hook caching,” I mean this:

the template says:

```yaml
on-receive: captureAccount
router: billingRouter
```

The engine should not, on every request:
- rescan Spring beans
- search for matching handlers
- resolve class/method names again
- inspect signatures again
- rebuild invocation logic again

Instead, at startup it should build permanent lookup tables in memory.

**What gets cached**
I would cache these things:

- hook name
- hook type
- resolved bean instance or bean supplier
- resolved invocation target
- validated signature metadata
- prebuilt invoker object

So in memory the engine effectively has:

- receive hook registry
- generate hook registry
- router hook registry

Each registry is just a map.

Conceptually:

- `captureAccount` -> cached receive-hook invoker
- `billingRouter` -> cached router-hook invoker
- `fieldPromptGenerator` -> cached generate-hook invoker

That is the caching part.

---

**What the cached object really is**
Not just “the bean”.

It should ideally be a prebuilt callable wrapper.

Meaning the engine stores something like:

- handler name
- already resolved target
- already validated contract
- already prepared invocation path

So runtime becomes:

1. read hook name from stage config
2. map lookup
3. invoke cached callable

That’s it.

---

**Why this matters**
Because without caching, request-time execution might do work like:

- find the bean by name or type
- find the method
- inspect parameters
- verify return type
- maybe coerce arguments
- then call it

That repeated resolution is unnecessary overhead and adds complexity.

With caching, all that work happens once during startup.

---

**Two kinds of caching depending on design**

**1. Typed hook caching**
If the handler implements `RouterHook`, `OnReceiveHook`, or `OnGenerateHook`, caching is simple.

You cache the handler reference directly.

For example, conceptually:
- `captureAccount` -> `OnReceiveHook` instance
- `billingRouter` -> `RouterHook` instance

Then runtime invocation is just interface dispatch.

This is the cleanest and cheapest model.

---

**2. Reflection-backed caching**
If you still allow method-string style like:
- `com.example.BillingHooks.capture`
- or a named method-backed handler

then the cache stores:
- resolved bean
- resolved `Method` or `MethodHandle`
- prepared invoker wrapper

So yes, even in a reflection-friendly model, the reflection should be done once and then cached.

---

**Where this fits in the lifecycle**

**Startup phase**
The engine should:
- discover handlers
- validate them
- build registries
- cache invokers
- validate template references if possible

**Runtime phase**
The engine should:
- lookup by hook name
- invoke cached handler
- continue stage flow

So caching is not an extra feature. It is part of how the runtime should fundamentally work.

---

**How I’d think about it structurally**
There are really two separate concerns:

**Developer-facing API**
How the developer defines a hook:
- by implementing `RouterHook`
- by implementing `OnReceiveHook`
- by annotation/name

**Engine runtime API**
How the engine executes it:
- via cached registry lookup
- via prebuilt invoker
- with zero repeated discovery work

That second part is the caching mechanism.

---

**The simplest mental model**
Think of it like Spring request mapping.

Spring does not rediscover controllers and methods on every HTTP request from scratch.

It resolves handler mappings ahead of time and keeps them ready.

I would do the same for flow hooks.

Your engine should have its own “handler mapping” layer for:
- on-generate
- on-receive
- router

That’s the hook caching I meant.

---

**What I’d cache in each approach**

If using typed interfaces:
- cache the hook bean itself in the correct typed registry

If using named method-backed handlers:
- cache bean + method + invocation wrapper

If using raw reflection strings:
- convert raw string references into cached invokers at startup, then never parse them again during requests

---

**What the developer sees**
The developer will mostly just see:
- define hook
- name hook
- reference hook in template

The caching is internal.

So from the developer’s view:

```yaml
on-receive: captureAccount
router: billingRouter
```

looks simple.

But internally the engine has already cached:
- `captureAccount` in the receive registry
- `billingRouter` in the router registry

That is why runtime stays fast and predictable.

---

**My preferred caching approach**
If I were designing it, I’d prefer this order:

1. best
   Cache typed hook instances directly

2. next best
   Cache method-handle-backed invokers

3. least preferred
   Do reflection repeatedly at runtime

So the typed hook model is not only cleaner semantically, it also gives you the easiest and strongest caching story.

---

**Short answer**
You didn’t see the caching part because it is not something the template author writes explicitly.

It is an engine internals concern:
- resolve hooks at startup
- store them in typed registries
- invoke cached handlers at runtime

That is the hook caching model I meant.

If you want, I can show you the exact internal flow next:
- app startup
- registry build
- template validation
- runtime execution of `on-generate`, `on-receive`, `router`
- where caching is hit at each step

---
You do not necessarily need a brand new `FlowContext` if your current `Hook` object already plays that role well.

My view: keep one first-class runtime object, but make sure its responsibility is clear.

So you have two good options:

**Option 1: Keep `Hook` and evolve it**
This is probably the best path if `Hook` is already central in your engine.

In that case, `Hook` is effectively your flow context object already.

Meaning:
- session
- sessionId
- user
- current stage
- previous stage
- input payload
- render props
- transient engine state
- maybe channel metadata

all live there.

Then your typed contracts become:

- `OnReceiveHook.execute(Hook hook)`
- `OnGenerateHook.execute(Hook hook)`
- `RouterHook.execute(Hook hook)`

That is perfectly fine.

You do not need a separate `FlowContext` just because the name sounds cleaner.

---

**Option 2: Introduce `FlowContext` later as a refinement**
This only becomes useful if your current `Hook` object is overloaded or semantically confusing.

For example, if `Hook` currently mixes too many concerns:
- engine state
- hook definition metadata
- transport data
- mutable results
- execution config
- domain scratchpad

then a dedicated `FlowContext` can help separate:
- “what is being executed”
  from
- “the runtime state available to execution”

But that is a design cleanup decision, not a mandatory one.

---

**My actual recommendation for you**
Keep `Hook` as the first-class object for now, but tighten its meaning.

I would treat it as:
- the unified runtime context passed into handlers

not as:
- a loose bag of unrelated engine things

So conceptually, your `Hook` becomes the execution context for the flow.

That means when a developer writes a hook, they receive one stable object:
- session access
- user details
- current input
- resolved template/stage info
- helper utilities
- engine-managed state

That is a good model.

---

**What matters more than the class name**
The important question is not whether it is called `Hook` or `FlowContext`.

The important questions are:

- is it stable?
- is it small enough to understand?
- does it expose the right runtime data?
- does it avoid mixing too many unrelated responsibilities?
- can typed handlers use it consistently?

If yes, then `Hook` is enough.

---

**How I’d shape it mentally**
I would think of `Hook` as:

- input context to handlers
- mutable session-aware runtime state
- shared execution envelope for one flow step

Then different hook types return different result objects.

So:
- `OnGenerateHook` reads from `Hook`, returns `GenerateResult`
- `OnReceiveHook` reads/writes via `Hook`, returns `ReceiveResult`
- `RouterHook` reads from `Hook`, returns `RouteResult`

That is cleaner than putting both context and all outcomes back into one mega object.

---

**Why this is a strong compromise**
This lets you preserve your current engine philosophy:
- one unified object across the lifecycle

while still improving architecture:
- typed handler interfaces
- typed result contracts
- cached hook registries
- better startup validation

So you get improvement without forcing a disruptive rewrite of your core model.

---

**When I would introduce `FlowContext` anyway**
I would only consider renaming or splitting later if one of these becomes true:

- `Hook` name starts confusing developers
- `Hook` contains both hook config and runtime state in awkward ways
- too many engine internals leak into business handlers
- testing handlers becomes messy because `Hook` is too broad
- different channels need different context views and the class becomes bloated

If that happens, then `FlowContext` can be introduced as either:
- a rename of the current `Hook`
- or a smaller runtime-facing subset built from `Hook`

But I would not force that now.

---

**So the short answer**
No, you do not need an extra `FlowContext` if your existing `Hook` already serves as the unified runtime object.

My recommended direction is:
- keep `Hook` as the first-class execution context
- define typed hook interfaces around it
- cache handlers in registries
- return typed results instead of overloading the context object with every possible outcome

That would fit your current design well.

If you want, I can next show what a well-shaped `Hook` should contain versus what should not live in it.
