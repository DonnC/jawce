# Java WhatsApp ChatBot Engine
A dependency for creating complete WhatsApp chatbots with ease using a template-driven approach.

## Features
- **Template-Driven Design**: By default, you can use YAML | JSON templates for conversational flows. But you can implement your own TemplateStorageManager
- **Hooks for Business Logic**: Attach Java classes / RESTful endpoints to process messages or actions.
- Abstracts the API for WhatsApp Cloud.
- Supports all official WhatsApp message types including Flows
- Supports dynamic messages with placeholders.

## Setup
For a quick start - Fork the repository and attempt to run the chatbot in the `example` folder

> Developed with Java 17+ using maven

1. Clone repository
```bash
git clone git@github.com:DonnC/jawce.git
```
2. Install all project maven dependencies
- Install main engine dependencies in`jengine` folder
- Install dependencies for the `example/jchabot` folder

3. Navigate to the example chatbot >`example/jchatbot/src/main/resources/application.yml` properties file 
and replace configs with your WhatsApp account configs

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
