# Router Hook Walkthrough

This guide explains exactly how the `router` hook works in `jawce`, how to redirect to another stage, and how to use it cleanly in a realistic chatbot.

## 1. What the router hook does

The `router` hook is a dynamic next-stage selector.

Normally `jawce` moves to the next stage by matching user input against the current template's `routes`.

A `router` hook lets you override that and say:

- ignore the static route table for this step
- inspect session, backend state, or user context
- return the real next stage at runtime

The hook does that by returning a `Hook` object with `redirectTo` set.

## 2. How to redirect to another stage

You define a `router` on the template:

```yaml
"MENU-DECIDER":
  type: text
  router: "com.example.booking.BookingRouter.decideMenu"
  message: "placeholder"
  routes:
    "re:.*": "HOME-MENU"
```

Then in Java:

```java
package com.example.booking;

import zw.co.dcl.jawce.engine.model.core.Hook;

public class BookingRouter {
    public Hook decideMenu(Hook hook) {
        Boolean loggedIn = hook.getSession().get(hook.getSessionId(), "loggedIn", Boolean.class);

        if (Boolean.TRUE.equals(loggedIn)) {
            hook.setRedirectTo("HOME-MENU");
        } else {
            hook.setRedirectTo("LOGIN-OR-REGISTER");
        }

        return hook;
    }
}
```

That is the core rule:

- `router` points to a hook
- the hook sets `redirectTo`
- `jawce` uses that stage as the next route

## 3. When the router runs

In normal request handling:

1. user input is received
2. `on-receive` and middleware run
3. `jawce` asks the current template for the next route
4. if `router` exists, it gets first chance to return `redirectTo`
5. if no redirect is returned, normal route matching continues

So the router is a dynamic override, not just a post-processing hint.

## 4. Clean usage patterns

Use `router` when the next stage depends on:

- login state
- backend eligibility
- product count
- workflow step returned by backend
- incomplete profile state
- whether the user has active bookings

Do not use it for every simple branch. Static `routes` are still better when the next step is obvious from user input.

## 5. Transient decision stages

One very clean pattern in `jawce` is a decision stage that does not really exist for the user as a visible message.

That stage can be:

- `transient: true`
- driven by `router`
- used only to decide the actual next visible template

Example:

```yaml
"POST-LOGIN-DECIDER":
  type: text
  transient: true
  router: "com.example.booking.BookingRouter.afterLogin"
  message: "placeholder"
  routes:
    "re:.*": "HOME-MENU"
```

This is useful when you want:

- login success to go to dashboard for some users
- onboarding for incomplete users
- payment-warning page for suspended users

without making the user see a fake intermediate message.

## 6. Booking chatbot walkthrough

Imagine a booking chatbot with:

- registration
- login
- dynamic templates
- a main menu
- a few backend-driven steps

Here is a clean project flow.

## 7. Suggested stages

### Authentication

- `START-MENU`
- `LOGIN-OR-REGISTER`
- `REGISTER-NAME`
- `REGISTER-PHONE`
- `LOGIN-ID`
- `LOGIN-PIN`
- `POST-LOGIN-DECIDER`

### Main menu

- `HOME-MENU`
- `BOOKING-MENU`
- `MY-BOOKINGS`
- `PROFILE-MENU`

### Booking flow

- `SERVICE-SELECT`
- `LOCATION-CAPTURE`
- `DATE-SELECT`
- `TIME-SLOT-SELECT`
- `BOOKING-REVIEW`
- `BOOKING-CONFIRM`
- `BOOKING-SUCCESS`

### Dynamic helper steps

- `AVAILABLE-SLOTS`
- `BOOKING-ROUTER`
- `PROFILE-ROUTER`

## 8. Example stage responsibilities

### `START-MENU`

Very simple first contact stage.

```yaml
"START-MENU":
  type: button
  message:
    body: "Welcome. Choose an option"
    buttons:
      - Login
      - Register
  routes:
    "login": "LOGIN-ID"
    "register": "REGISTER-NAME"
```

### `LOGIN-PIN`

This stage captures PIN and calls backend auth.

```yaml
"LOGIN-PIN":
  type: text
  on-receive: "com.example.booking.AuthHook.login"
  router: "com.example.booking.BookingRouter.afterAuth"
  message: "Enter your PIN"
  routes:
    "re:.*": "POST-LOGIN-DECIDER"
```

What happens here:

- user enters PIN
- `on-receive` validates against backend
- session gets `loggedIn`, `userId`, maybe `profileComplete`
- router decides where to go next

### `POST-LOGIN-DECIDER`

This can be transient:

```yaml
"POST-LOGIN-DECIDER":
  type: text
  transient: true
  router: "com.example.booking.BookingRouter.afterLogin"
  message: "placeholder"
  routes:
    "re:.*": "HOME-MENU"
```

Possible outcomes:

- `HOME-MENU`
- `COMPLETE-PROFILE`
- `ACCOUNT-LOCKED`

That keeps auth logic clean and central.

## 9. Main menu handling

### `HOME-MENU`

This can still be a normal static menu:

```yaml
"HOME-MENU":
  type: button
  message:
    body: "What would you like to do?"
    buttons:
      - Book
      - My Bookings
      - Profile
  routes:
    "book": "SERVICE-SELECT"
    "my bookings": "MY-BOOKINGS"
    "profile": "PROFILE-ROUTER"
```

Static routes are perfect here because user intent is explicit.

## 10. A router-driven menu step

Suppose `Profile` should go to different places:

- first-time user goes to `COMPLETE-PROFILE`
- verified user goes to `PROFILE-MENU`
- suspended user goes to `PROFILE-BLOCKED`

Use a router stage:

```yaml
"PROFILE-ROUTER":
  type: text
  transient: true
  router: "com.example.booking.ProfileRouter.route"
  message: "placeholder"
  routes:
    "re:.*": "PROFILE-MENU"
```

Hook:

```java
package com.example.booking;

import zw.co.dcl.jawce.engine.model.core.Hook;

public class ProfileRouter {
    public Hook route(Hook hook) {
        Boolean profileComplete = hook.getSession().get(hook.getSessionId(), "profileComplete", Boolean.class);
        Boolean suspended = hook.getSession().get(hook.getSessionId(), "suspended", Boolean.class);

        if (Boolean.TRUE.equals(suspended)) {
            hook.setRedirectTo("PROFILE-BLOCKED");
        } else if (!Boolean.TRUE.equals(profileComplete)) {
            hook.setRedirectTo("COMPLETE-PROFILE");
        } else {
            hook.setRedirectTo("PROFILE-MENU");
        }

        return hook;
    }
}
```

That is a clean use of router:

- no fake user input matching
- no giant conditional YAML
- decision stays in Java where business logic belongs

## 11. Dynamic templates inside booking

Now imagine selecting available time slots.

Backend may return:

- 2 slots
- 8 slots
- 20 slots

That is exactly where the advanced dynamic template path helps.

`AVAILABLE-SLOTS` can be one logical stage whose `on-generate` hook decides whether to return:

- `ButtonTemplate`
- `ListTemplate`
- `TextTemplate`

based on slot count.

So the flow becomes:

- `DATE-SELECT`
- backend loads slots
- `AVAILABLE-SLOTS`
- hook builds final outbound template dynamically

That keeps booking UX clean without multiplying stage variants.

## 12. A booking step that uses both dynamic rendering and router

Here is a strong real-world combo:

### `BOOKING-REVIEW`

- `on-generate` builds review content dynamically
- `router` decides what happens after confirmation

Example outcomes:

- normal booking goes to `BOOKING-CONFIRM`
- prepaid hold required goes to `PAYMENT-HOLD`
- unavailable slot goes back to `AVAILABLE-SLOTS`

This is where `jawce` stays clean:

- rendering logic in `on-generate`
- next-step logic in `router`
- business state in backend/session

## 13. How `jawce` keeps this clean

The clean split is:

### Templates

Templates define:

- main flow structure
- user-facing message shells
- simple routes
- entry points for hooks

### Hooks

Hooks define:

- backend calls
- state updates
- render data
- dynamic template replacement
- redirect decisions

### Session

Session holds:

- logged-in state
- user id
- booking draft id
- slot list
- selected service
- selected location
- profile completion state

This keeps the bot maintainable as it grows.

## 14. Recommended developer rule

Use static routes when:

- the next step depends directly on what the user clicked or typed

Use `router` when:

- the next step depends on state, policy, or backend results

Use dynamic templates when:

- the message type itself should change at runtime

That three-part rule keeps `jawce` very manageable.

## 15. Practical booking example summary

A clean booking project in `jawce` would usually look like this:

1. `START-MENU`
2. `LOGIN-ID`
3. `LOGIN-PIN`
4. `POST-LOGIN-DECIDER`
5. `HOME-MENU`
6. `SERVICE-SELECT`
7. `LOCATION-CAPTURE`
8. `DATE-SELECT`
9. `AVAILABLE-SLOTS`
10. `BOOKING-REVIEW`
11. `BOOKING-CONFIRM`
12. `BOOKING-SUCCESS`

Router-heavy stages would usually be:

- `POST-LOGIN-DECIDER`
- `PROFILE-ROUTER`
- `BOOKING-ROUTER`

Dynamic-template-heavy stages would usually be:

- `AVAILABLE-SLOTS`
- `SERVICE-SELECT`
- `MY-BOOKINGS`

That is a very natural Spring-and-backend-friendly use of `jawce`.
