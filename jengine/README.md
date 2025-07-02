# JAWCE - WhatsApp ChatBot Engine
Official whatsapp cloud api template-based engine

Using reflective api and REST API based hooks

## Engine response exception
- The engine returns 2 buttons, Menu and Report (for bugs)
- Make sure your trigger has a menu and a report trigger for processing these 2

## Documentation
Visit the [docs here](https://docs.page/donnc/jawce)

## About
This project serves as a quick way to get WhatsApp chatbots up and running. It came to light due to the repetitive approach
of WhatsApp ChatBots i was creating.

### Integration as a dependency
Add the dependency to your `maven - pom` file
```xml
<!--  dependencies -->
<dependency>
    <groupId>zw.co.dcl.jawce</groupId>
    <artifactId>engine</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope>
</dependency>
```
