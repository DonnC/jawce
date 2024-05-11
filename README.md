# WhatsApp ChatBot Engine

Using official Cloud API

The engine uses both endpoint based hook mechanism and JAVA reflective api

### Differences

For rest calls, make sure the base endpoint is defined in the config.

All rest calls are POST request that should receive `HookArg` request body

By default, the engine assume reflective api if `rest:..` is not found on the hook name

```yaml
# rest call
6003:
  type: text
  template: "rest:/session/data"
  message: "Provide reference for txn: {{ type }}, {{ currency }} {{ amount }}"

# reflective api call
6005:
  type: text
  template: "com.example.test.className:methodName"
  message: "Provide reference for txn: {{ type }}, {{ currency }} {{ amount }}"
```

## Test Details
- hub token: `jawce-hub-token-123`

## Dynamic routing

A template should have a `router` field to declare a router method that computes the next route to go to.

The router method should only return a String which equals a key in the templates context map

A rest hook response should have a `route` key in the `additionalData` Map response

## Dynamic Body creation

Every method to process dynamic template should return an instance of `TemplateDynamicBody`

### Dynamic payload type response

OP - optional field

- button

```json
 {
  "header": "OP - header",
  "title": "OP - message title",
  "body": "message body",
  "footer": "OP - footer",
  "buttons": [
    "btn1",
    "btn2"
  ],
  "routes": {
    "confirm": 3101,
    "cancel": 5000,
    "re:.*": 0
  }
}
```

- text

```json
 {
  "text": "text body"
}
```

- list

```json
 {
  "header": "OP - header",
  "title": "OP - message title",
  "body": "message body",
  "footer": "OP - footer",
  "button": "btn text",
  "sections": {
    "title1": {
      "0": {
        "title": "item1",
        "description": "desc"
      },
      "1": {
        "title": "item2",
        "description": "desc"
      }
    }
  },
  "routes": {
    "confirm": 3101,
    "cancel": 5000,
    "re:.*": 0
  }
}
```

## Hooks

Each hook controller method should accept a `HookArgs` record type post request body.

For security reasons, the hook method can verify header key: `X-WA-ENGINE-KEY` which will contain the key
passed on the engine config class

Each reflective hook args should have a constructor that takes a `HookArgs args` single param

A reflective hook can define custom params to send to the calling method as defined in `.methodArgs()`

All hook params will be a Map object.

The `HookArgs` is defined as

```java
public record HookArgs(
        SessionManager session,  // current user session manager
        WaCurrentUser channelUser, // contains waId, current msgId
        String userInput, // the processed user input for the current stage
        Map<String, Object> additionalData, // Flow data or any other data
        Map<String, Object> methodArgs
) {
}
```

### Reflective Example
```java
class ExampleHook {
  private final HookArgs args;

  public ExampleHook(HookArgs args) {
    this.args = args;
  }
  
  public Object hookMethod() {
    // get custom optional hook params 
    log.info("Received hook param: {}", params.methodArgs());
    
    // .. your logic here ..
    return args;
  }
}
```

Template
```yaml
4000:
  type: text
  template: "path.ExampleHook:hookMethod"
  params:
    name: jack
    age: 10
  message: message body
# ...
```

Tap into custom logic for the given template

A - called after tpl is send to user and received response

B - called before tpl is send to user

- checkpoint > mark a stage as a fallback policy to go to when a dynamic message with Retry button is sent. If users
  clicks it, it will return to the latest checkpoint in line
- template > B, call before msg is built, dynamically build msg or inject msg data
- authenticate > B, allow message if user is authenticated or has valid session
- on-generate > B, pre generation method before msg is send back to user
- prop > A, save response to session with mentioned key
- on-receive > A, call method when response is received
- validator > A, call after response is received, handle all response data validations
- middleware > A, call after response comes in and all validations are run
- router > A, call after response is received to dynamically compute next route to go to
- ack > A, bluetick user message, mark message as read

## Running methods

Hook and its return type

### Pre-template process methods: Before-Generation

- authentication -> `Map<String, dynamic>`
- template -> type == flow || dynamic ? `TemplateDynamicBody` : `Map<String, dynamic>`
- on-generate -> void

### Post-template process methods: After-Response

- validator -> void
- on-receive -> void
- middleware -> void
- prop -> void
- router -> `String`
- ack -> void

## Rules

1. Usually methods defined in `template`, `middleware`, `validator` only have 1 param passed down to them ie `HookArgs`
2. It will have the info necessary for the method to handle further processing

#### Template
For templated body, make sure the `template` function returns a map which has fields defined in the body

#### HOOKS [Reflective & REST]

In template:

```yaml
6003:
  type: text
  template: "rest:/session/data"
  message: "Provide reference for txn: {{ type }}, {{ currency }} {{ amount }}"
```

REST Hook Code Sample

Every hook method should accept a `HookArgsRest` type of request body

```java
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/session")
class TxnService {

    @PostMapping("/data")
    public HookArgsRest getTxnSessionData(@RequestBody HookArgsRest args) {
      // get data from session or somewhere
      // and return template mapped fields
      args.setTemplateDynamicBody(
              new TemplateDynamicBody(
                      null, 
                      null,
                      Map.of(
                              "type", "ZIPIT",
                              "currency", "USD",
                              "amount", 250.50
                      )
              )
      );
      
      return args;
    }
}
```

> JAVA Reflective API Hook Code Sample

Every hook method should accept a `HookArgs` type of request body

```yaml
"PAYMENT_STAGE":
  type: text
  router: "com.example.app.ExampleRefHook:postPaymentDynamicRouter"
  message: "Kindly confirm your payment details below"
```

```java
public class ExampleRefHook {
  private final HookArgs args;

  public ExampleRefHook(HookArgs args) {
    this.args = args;
  }

  public HookArgs postPaymentDynamicRouter() {
//    dynamically route to checkout stage
//    else show a payment success message
    if(args.getMethodArgs().containsKey("PAYMENT_PAGE")) {
        args.setAdditionalData(
                Map.of(
                        EngineConstants.REST_HOOK_DYNAMIC_ROUTE_KEY, "NEXT_CHECKOUT_STAGE"
                )
        );
    }
    
    else {
      args.setAdditionalData(
              Map.of(
                      EngineConstants.REST_HOOK_DYNAMIC_ROUTE_KEY, "DEFAULT_PAYMENT_SUCCESS_STAGE"
              )
      );
    }
    
    return args;
  }
  
//  .. other hook methods ..
}
```

## Flow

Each flow response should return the screen name
this will be used to match the next route to process the request
the input will be the screen flow name

Template

```yaml
3500:
  type: flow
  template: "rest:https://example.com/book-appointment"
  message:
    id: 199
    name: BOOK_APPOINTMENT_FLOW
    draft: true
    title: "Book Appointment"
    body: "Click the button below to start your appointment booking with Mr Doc Online."
    footer: Booking
    button: Start Booking
  routes:
    "re:.*": CONFIRM_BOOKING_STAGE
```
