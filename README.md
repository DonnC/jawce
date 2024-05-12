# A Java WhatsApp ChatBot Engine

Check out the [documentation available here](https://docs.page/donnc/jawce)

A Template based Java ChatBot engine for WhatsApp Cloud API

## Demo
https://github.com/DonnC/jawce/assets/47761288/f1c9754e-5f29-455e-ba57-54cf7338286b


## Hooks
The engine uses both endpoint based hook mechanism and JAVA reflective api

Each hook controller method should accept a `HookArgs` record type post request body.

For security reasons, the hook method can verify header key: `X-WA-ENGINE-KEY` which will contain the key
passed on the engine config class

Each reflective hook args should have a constructor that takes a `HookArgs args` single param

A reflective hook can define custom params to send to the calling method as defined in `.methodArgs()`

All hook params will be a Map object.

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
