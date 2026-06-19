# Router Hooks And Backend-Driven Flows

This guide explains two things:

1. how the `router` hook should be used in `jawce`
2. how to design scalable backend-driven chatbots such as prepaid, bill-payment, or banking flows

## 1. What the `router` hook is for

The `router` hook exists so the engine can decide the next stage dynamically at runtime.

This is useful when the next stage cannot be known from static user-input matching alone.

Typical reasons:

- backend eligibility decides the next step
- user account state changes the flow
- a catalog or biller set is dynamic
- the same input should branch differently depending on context

In `jawce`, this is much cleaner than encoding too much branching directly into the template file.

## 2. Good real-world uses for `router`

### Eligibility routing

Example:

- prepaid customer can proceed
- postpaid customer must be redirected
- suspended customer must be blocked

The router can redirect to:

- `PREPAID-MENU`
- `POSTPAID-INFO`
- `ACCOUNT-BLOCKED`

### Channel-aware presentation routing

Example:

- 3 options: use buttons
- 20 options: use list
- 1 option: send text and continue
- 23 options: stay on one stage and paginate the presentation

The router can redirect to:

- `SHOW-OPTIONS-BUTTON`
- `SHOW-OPTIONS-LIST`
- `SHOW-OPTIONS-TEXT`

If the requirement is page navigation within the same business step, prefer the pagination helper pattern instead of creating one stage per page:

- [Pagination In `jawce`](./pagination.md)

### Existing workflow recovery

Example:

- resume abandoned checkout
- continue OTP verification
- restart fresh

The router can redirect to:

- `RESUME-CHECKOUT`
- `WAIT-OTP`
- `START-MENU`

### Policy-driven routing

Example:

- bill amount too high requires supervisor or second factor
- trusted amount can continue normally

The router can redirect to:

- `STEP-UP-AUTH`
- `PAYMENT-CONFIRM`

### Backend-owned step routing

Example:

- backend says next required field is meter number
- or account number
- or customer name confirmation

The router can redirect to:

- `CAPTURE-ACCOUNT-FIELD`
- `CAPTURE-FREE-TEXT-FIELD`
- `CONFIRM-DETAILS`

## 3. What `router` should not do

Do not make the router become the whole bot.

It should not:

- perform all rendering decisions plus all validation plus all API orchestration plus all payment logic in one method,
- replace templates entirely,
- hold the only copy of workflow state.

The router should decide direction.

The backend service should decide business logic.

The templates should handle presentation.

## 4. Scalable pattern for backend-driven prepaid or bill-payment bots

Your scenario is a very common one:

- the backend owns billers and bill configs
- the backend returns dynamic field definitions
- the backend owns regex/validation rules
- the backend performs preauth
- the backend performs payment
- each biller can differ a lot

If the bot tries to hardcode every biller in YAML, the flow becomes unmaintainable very quickly.

The right approach is not "one template per biller".

The right approach is "a generic conversation shell over a backend workflow engine".

## 5. Recommended architecture

Split responsibility like this:

### Backend workflow service

This should own:

- biller catalog
- biller ids and names
- required fields
- field order
- field labels
- regex or validation rules
- preauth logic
- payment logic
- allowed actions
- next-step decision

### `jawce` bot layer

This should own:

- WhatsApp transport
- session continuity
- message presentation
- high-level conversational stages
- channel UX decisions like text vs button vs list

### Session state

Session should store only workflow context such as:

- workflow instance id
- current biller id
- current step id
- collected field values
- presentation mode
- latest backend step descriptor

## 6. The generic-stage strategy

Instead of creating a stage per biller, create a small reusable stage set.

Example:

- `START-MENU`
- `BILLER-SEARCH`
- `BILLER-PICK`
- `FIELD-CAPTURE`
- `FIELD-CONFIRM`
- `PREAUTH`
- `PAYMENT-REVIEW`
- `PAYMENT-SUBMIT`
- `SUCCESS`
- `FAILURE`

These stages stay stable even if you add 500 new billers.

## 7. How `FIELD-CAPTURE` should work

This is the key scaling trick.

Do not create:

- `CAPTURE-ZESA-METER`
- `CAPTURE-DSTV-SMARTCARD`
- `CAPTURE-WATER-ACCOUNT`
- `CAPTURE-INSURANCE-POLICY`

Instead create one generic field-capture stage.

The backend returns a field descriptor like:

```json
{
  "stepId": "capture-account-number",
  "displayName": "Meter Number",
  "fieldKey": "meterNumber",
  "inputType": "text",
  "regex": "^\\d{11}$",
  "errorMessage": "Enter a valid 11 digit meter number",
  "nextAction": "CAPTURE_FIELD"
}
```

Then:

- `on-generate` renders the current field label or instructions
- `on-receive` validates input or calls backend validation
- `router` decides whether to stay on another `FIELD-CAPTURE`, move to `FIELD-CONFIRM`, or fail

## 8. How to keep templates simple

Keep templates presentation-focused.

Example:

```yaml
"FIELD-CAPTURE":
  type: text
  on-generate: "com.example.billing.FieldPromptHook.prepare"
  on-receive: "com.example.billing.FieldCaptureHook.capture"
  router: "com.example.billing.WorkflowRouter.route"
  message: "{{ s.currentPrompt }}"
  routes:
    "re:.*": "FIELD-CAPTURE"
```

This stage does not know whether the user is paying electricity, TV, water, or insurance.

It only knows:

- show the current prompt
- capture the current response
- let backend-driven routing decide the next stage

That is exactly the kind of simplicity you want.

## 9. Suggested session model

A simple session shape is enough:

- `workflowId`
- `billerId`
- `billerName`
- `currentStepId`
- `currentPrompt`
- `currentFieldKey`
- `currentFieldRegex`
- `currentDisplayType`
- `collectedFields`
- `preauthResult`

This keeps the YAML generic and lets the backend remain the true source of workflow truth.

## 10. Presentation strategy for billers and options

Do not let the bot explode because the backend returned many options.

Use a simple rule:

- if options are very few, use buttons
- if options are many but bounded, use list
- if options are too many, use search/filter text capture

That means the router can choose:

- `BILLER-PICK-BUTTON`
- `BILLER-PICK-LIST`
- `BILLER-SEARCH`

The backend still owns the actual biller records.

## 11. Recommended developer approach for your prepaid use case

Here is the safest way to build it.

### Step 1. Treat the backend as the workflow engine

Do not encode biller field logic in templates.

The backend should answer questions like:

- what billers exist?
- what next field is required?
- how should the field be validated?
- is preauth allowed?
- what does review data look like?
- is payment allowed?

### Step 2. Keep `jawce` as the conversation adapter

Let `jawce` do:

- session management
- routing shell
- message formatting
- WhatsApp-specific UX

### Step 3. Use generic stages

Do not create one template tree per biller.

Use generic stages that can serve any biller.

### Step 4. Use hooks to translate backend step descriptors into chat prompts

`on-generate` should take backend state and convert it into:

- prompt text
- buttons
- list rows
- review summary

### Step 5. Use `router` to interpret backend next-step decisions

The backend can return a logical next step like:

- `PICK_BILLER`
- `CAPTURE_FIELD`
- `CONFIRM`
- `PREAUTH`
- `PAY`
- `DONE`

The router can map those to `jawce` stages.

### Step 6. Keep payments and preauth out of template logic

Templates should not own:

- payment orchestration
- eligibility rules
- regex registries
- biller field definitions

Those belong in backend services.

## 12. What this buys you

If you follow this model:

- adding new billers does not require template rewrites
- changing a field regex does not require a redeploy of all bot flows
- payment rules stay in the backend where they belong
- your bot stays readable
- testing becomes much easier
- enterprise change control becomes cleaner

This matters a lot in banking, prepaid, and regulated payment systems.

## 13. A practical decision rule

Use this rule when designing a step:

- if it is channel presentation, keep it in `jawce`
- if it is business truth, keep it in the backend
- if it is next-stage choice, let the router translate backend intent into a bot stage

That is the clean boundary.

## 14. Final recommendation

For dynamic billers and payment flows, the best long-term model is:

- backend-managed workflow definitions
- generic `jawce` conversation stages
- `on-generate` for prompt shaping
- `on-receive` for capture and backend calls
- `router` for next-step translation

That gives you the Java/Spring way of doing it:

- strong backend services
- clear contracts
- reusable engine stages
- less template sprawl
- easier scaling as the product grows
