# Pagination In `jawce`

This guide explains how to handle paginated option selection in `jawce` without turning every page into a different stage.

The design goal is simple:

- keep the conversation stage stable
- let the backend own the items
- let the hook decide how each page is rendered
- let the engine remember page state safely

That fits the `jawce` style much better than trying to hardcode `PAGE-1`, `PAGE-2`, `PAGE-3` templates.

## 1. What the engine gives you now

`jengine` now has a first-class pagination helper API:

- [PaginationSupport.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/api/pagination/PaginationSupport.java)
- [PaginationRequest.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/api/pagination/PaginationRequest.java)
- [PaginationSelection.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/api/pagination/PaginationSelection.java)
- [PaginationState.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/api/pagination/PaginationState.java)
- [PaginationMode.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/main/java/zw/co/dcl/jawce/engine/api/pagination/PaginationMode.java)

It supports:

- `LIST` pagination
- `TEXT` pagination
- `BUTTON` pagination

It also handles:

- page state persistence in session
- `Back` and `Next` navigation choices
- page-local ordinals for text menus
- structured selection parsing for both items and navigation actions

## 2. The recommended mental model

Treat pagination as a single business step with many pages, not many different stages.

The clean pattern is:

1. one stable stage such as `ACCOUNT-PICK`
2. a `dynamic` hook renders the current page
3. an `on-receive` hook captures either item selection or nav selection
4. a `router` hook rerenders the same stage when the user chose `Back` or `Next`
5. normal routing continues when the user chose a real item

That means your YAML stays small even when the backend returns 200 items.

## 3. Why this matters for WhatsApp

WhatsApp interactive list messages are constrained.

The helper already respects that reality:

- list rows are capped at 10 total
- if pagination is needed, navigation rows consume part of those 10
- button mode is also limited and should be used only for very small sets

So for list pagination:

- first page may show business items plus `Next`
- middle pages may show business items plus `Back` and `Next`
- last page may show business items plus `Back`

For text pagination, the engine keeps things more flexible because text can list more content, but you will usually still want a practical page size like `10`.

## 4. Core API flow

The helper API has four main steps.

### 4.1 Build choices from backend data

Use `PaginationSupport.mapChoices(...)` to convert your backend rows into `DynamicChoice` objects.

```java
List<DynamicChoice> choices = PaginationSupport.mapChoices(
        accounts,
        account -> account.getId(),
        account -> account.getDisplayName(),
        account -> account.getMaskedNumber(),
        (account, ordinal) -> Map.of(
                "accountId", account.getId(),
                "currency", account.getCurrency()
        )
);
```

This keeps business metadata attached to the choice so later hooks can use it.

### 4.2 Render the current page

Use `PaginationSupport.render(...)` inside your `dynamic` hook.

```java
public Hook renderAccounts(Hook hook) {
    var request = PaginationRequest.builder()
            .stateKey("accounts")
            .mode(PaginationMode.LIST)
            .pageSize(10)
            .title("Accounts")
            .prompt("Select an account")
            .buttonLabel("Choose")
            .sectionTitle("Available accounts")
            .choices(loadAccountChoices(hook))
            .build();

    hook.setTemplateDynamicBody(PaginationSupport.render(hook, request));
    return hook;
}
```

`stateKey` is important.
That is how the helper remembers which pagination state belongs to this step.

### 4.3 Handle the inbound selection

Use `PaginationSupport.handleSelection(...)` inside your `on-receive` hook.

```java
public Hook captureAccountSelection(Hook hook) {
    var selection = PaginationSupport.handleSelection(hook);

    if(selection.isPresent() && selection.get().isItem()) {
        hook.getSession().save(
                hook.getSessionId(),
                "selectedAccount",
                hook.getAdditionalData().get("dynamicChoice")
        );
    }

    return hook;
}
```

This does two things:

- if the user chose `Back` or `Next`, the helper updates the page state in session
- it returns a typed `PaginationSelection` so your hook can tell item selection from navigation

### 4.4 Rerender the same stage on navigation

Use `PaginationSupport.selection(...)` inside your `router` hook.

```java
public Hook routeAccountSelection(Hook hook) {
    PaginationSupport.selection(hook).ifPresent(selection -> {
        if(selection.isNavigation()) {
            hook.setRedirectTo("ACCOUNT-PICK");
        }
    });

    return hook;
}
```

That is the key trick.
Navigation is not a business completion.
It is just a request to rerender the same step at a different page.

## 5. Canonical YAML pattern

This is the clean recommended stage shape.

```yaml
"ACCOUNT-PICK":
  type: dynamic
  on-receive: "com.example.bank.AccountHooks.captureAccountSelection"
  dynamic: "com.example.bank.AccountHooks.renderAccounts"
  router: "com.example.bank.AccountHooks.routeAccountSelection"
  params:
    rerenderStage: ACCOUNT-PICK
  message: "placeholder"
  routes:
    "re:.*": "ACCOUNT-CONFIRM"
```

What this means:

- `dynamic` builds the current page
- `on-receive` updates page state or stores the chosen item
- `router` redirects back to `ACCOUNT-PICK` only for nav actions
- the normal route to `ACCOUNT-CONFIRM` happens only when a real item was selected

## 6. Example: account picker that changes presentation mode

Imagine a banking bot that fetches user accounts from the backend and wants:

- buttons when accounts are very few
- list when the count fits well
- paginated list when the count is larger
- text pagination when the count is very large or descriptions are too long

The backend can stay the source of truth for account data.
The hook decides the best WhatsApp presentation.

```java
public Hook renderAccounts(Hook hook) {
    List<AccountDto> accounts = accountService.fetchAccounts(hook.getSessionId());

    if(accounts.size() <= 3) {
        hook.setTemplateDynamicBody(PaginationSupport.render(hook, PaginationRequest.builder()
                .stateKey("accounts")
                .mode(PaginationMode.BUTTON)
                .pageSize(3)
                .prompt("Select an account")
                .choices(toChoices(accounts))
                .build()));
        return hook;
    }

    if(accounts.size() <= 24) {
        hook.setTemplateDynamicBody(PaginationSupport.render(hook, PaginationRequest.builder()
                .stateKey("accounts")
                .mode(PaginationMode.LIST)
                .pageSize(10)
                .prompt("Select an account")
                .buttonLabel("Accounts")
                .sectionTitle("Available accounts")
                .choices(toChoices(accounts))
                .build()));
        return hook;
    }

    hook.setTemplateDynamicBody(PaginationSupport.render(hook, PaginationRequest.builder()
            .stateKey("accounts")
            .mode(PaginationMode.TEXT)
            .pageSize(10)
            .prompt("Select an account")
            .choices(toChoices(accounts))
            .build()));
    return hook;
}
```

This avoids having:

- `ACCOUNT-PICK-BUTTON`
- `ACCOUNT-PICK-LIST`
- `ACCOUNT-PICK-LIST-PAGE-2`
- `ACCOUNT-PICK-TEXT-PAGE-3`

The stage stays stable.
The hook adapts the view.

## 7. How text pagination behaves

Text pagination has one very important behavior:

- visible items on each page are renumbered from `1`

So if page 2 is showing account 11 to account 20, the user can still reply:

- `1`
- `2`
- `3`

for the visible page items.

That is much friendlier than forcing the user to reply with the global item number like `11`.

The helper keeps the actual business `id` in the dynamic choice metadata, so your bot still knows the real selected account.

## 8. How to approach pagination in complex backend-driven bots

For bots like:

- banking
- bill payment
- insurance
- healthcare bookings
- retail catalog selection

the scalable rule is:

- backend owns the items
- backend owns business eligibility
- `jawce` owns the conversation continuity and channel UX

So pagination should live at the presentation boundary, not inside your core business service.

The backend should return:

- item id
- display label
- optional description
- optional metadata

Then the bot layer turns that into `DynamicChoice` values and chooses:

- `BUTTON`
- `LIST`
- `TEXT`

depending on channel constraints.

## 9. When to use router and when not to

Use `router` when:

- the user selected `Back` or `Next`
- the backend says the next stage has changed
- the same inbound input must branch differently depending on state

Do not use `router` just to paginate if your hook can rerender the same stage after a nav selection.

The clean pattern is still:

- `on-receive` updates selection state
- `router` decides if this was navigation
- `dynamic` rerenders

## 10. Session and cleanup considerations

Pagination state is stored in the session under an internal helper key.

That means:

- it survives the stateless webhook roundtrip model
- it is naturally scoped per chat session
- it works with the existing JAWCE session contract

When the paginated step is finished, you should clear the pagination state if you know the step is complete.

```java
PaginationSupport.clear(hook, "accounts");
```

That is especially useful when:

- the same stage may be revisited later with a fresh dataset
- the dataset may shrink or reorder

## 11. Practical recommendations

- Prefer `LIST` for medium option sets where titles and short descriptions matter.
- Prefer `TEXT` for very large sets or when you need more freedom in formatting.
- Use `BUTTON` only for very small sets.
- Keep page sizes conservative even when text could hold more.
- Always preserve business ids in `DynamicChoice.metadata`.
- Let your backend stay unaware of WhatsApp page navigation details.

## 12. Current tests

The current coverage includes:

- helper rendering behavior
- list pagination with `Back` and `Next`
- text pagination with page-local ordinals
- rerendering the same stage on navigation

See:

- [PaginationSupportTest.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/test/java/zw/co/dcl/jawce/engine/api/pagination/PaginationSupportTest.java)
- [WorkerEngineDynamicTemplateTest.java](/C:/Users/DEVELOPER/Documents/Personal/Personal/Projects/wce/jawce/jengine/src/test/java/zw/co/dcl/jawce/engine/api/WorkerEngineDynamicTemplateTest.java)

## Bottom line

`jawce` pagination now works best as a stable-stage dynamic pattern:

- one stage
- one render hook
- one receive hook
- one router hook
- session-backed page continuity

That keeps the bot readable, Spring-friendly, and realistic for production WhatsApp flows.
