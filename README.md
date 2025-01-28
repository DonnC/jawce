# Java WhatsApp ChatBot Engine
A dependency for creating WhatsApp chatbots using a template-driven approach.

Templates use YAML allowing you to define conversation flows and business logic in a clean and modular
way.

## Features

- **Template-Driven Design**: Use YAML templates for conversational flows.
- **Hooks for Business Logic**: Attach Java classes / RESTful endpoints to process messages or actions.
- Abstracts the API for WhatsApp Cloud.
- Supports dynamic messages with placeholders.

## Setup
For a quick start - Fork the repository and attempt to run the chatbot in the `example` folder

> Developed with Java 17

1. Clone repository
```bash
git clone git@github.com:DonnC/jawce.git
```
2. Install all project maven dependencies
- Start by installing dependencies in the `jsession` folder
- Next, install `jengine` folder dependencies
- Finally install dependencies for the `example/jchabot` folder
3. Navigate to the example chatbot >`example/jchatbot/src/main/resources/application.yml` properties file 
and replace configs with your WhatsApp account configs

```yaml
chatbot:
  configs:
    # ~ snippet ~
    hub-token: "your-webhook-hub-challenge-token"
    phone-number-id: "your-phone-number-id"
    access-token: "your-access-token"
```
4. Configure chatbot resources under the `resources` section
```yaml
resources:
  templates: "path-to-templates-dir"
  triggers: "path-to-triggers-dir"
  watcher: "path-to-watcher-dir"
```

Configure the full path to where the resources are, for example. 

If you clone the project in `C:\\Projects` folder, it will be like below
```yaml
resources:
    templates: C:\\Projects\\jawce\\example\\jchatbot\\src\\main\\resources\\templates
    triggers:  C:\\Projects\\jawce\\example\\jchatbot\\src\\main\\resources\\triggers
    watcher:   C:\\Projects\\jawce\\example\\jchatbot\\src\\main\\resources\\watch
```

> The watcher is used to listen to file changes in the watch dir which will trigger all templates to reload without restarting the service.

### Engine dependency
> Refer to the [Example ChatBot](https://github.com/DonnC/jawce/tree/main/example/jchatbot) for a quick getting started template

To include the jengine in your own project.

In your `pom.xml` dependencies add the following

```xml
<!-- your other dependencies -->

<dependency>
    <groupId>zw.co.dcl.jawce</groupId>
    <artifactId>jengine</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope>
</dependency>
```

Make sure you create a rest controller which handles 2 of the important logic
- webhook verification
- webhook payload

Checkout the `example` project for a starting point

## Documentation

Visit the [official documentation](https://docs.page/donnc/wce) for a detailed guide.

## Contributing

We welcome contributions! Please check out the [Contributing Guide](https://github.com/DonnC/jawce/blob/master/CONTRIBUTING.md) for details.

## License

This project is licensed under the MIT License. See the [LICENSE](https://github.com/DonnC/jawce/blob/master/LICENCE) file for details.
