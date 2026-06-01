# Java WhatsApp ChatBot Engine
A dependency for creating complete WhatsApp chatbots with ease using a template-driven approach.

## Features
- **Template-Driven Design**: By default, you can use YAML | JSON templates for conversational flows. But you can implement your own TemplateStorageManager
- **Hooks for Business Logic**: Attach Java classes / spring beans / RESTful endpoints to process messages or actions.
- Ships with basic default implementations for file sessions, YAML/JSON template loading, and `RestTemplate`-based HTTP client wiring. Override them by providing your own interface beans.
- Abstracts the API for WhatsApp Cloud: focus on your core chatbot functions.
- Supports all official WhatsApp message types including Flows
- Supports dynamic messages with placeholders.
- Auto configs via properties file
- Event driven architecture with Spring Events

## Setup
For a quick start - Fork the repository and attempt to run the chatbot in the `example` folder

> Developed with Java 17+ using maven

1. Clone repository
```bash
git clone git@github.com:DonnC/jawce.git
```
2. Install all project maven dependencies
- Install main engine dependencies in`jengine` folder
- Install dependencies for the examples in the `example/` folder

3. Navigate to the example chatbot >`example/ehailing/src/main/resources/application.yml` properties file 
and replace configs with your WhatsApp account configs

> Checkout the complete local [WhatsApp Chatbot Emulator](https://github.com/DonnC/wce-emulator.git) tool!

### Engine dependency
> Refer to the [Example ChatBot](https://github.com/DonnC/jawce/tree/main/example/ehailing) for a quick getting started template

To include the jengine in your own project.

In your `pom.xml` dependencies add the following

```xml
<!-- your other dependencies -->

<dependency>
    <groupId>zw.co.dcl.jawce</groupId>
    <artifactId>jengine</artifactId>
    <version>LATEST-VERSION</version>
    <scope>compile</scope>
</dependency>
```

Make sure you create a rest controller which handles 2 of the important logic
- webhook verification
- webhook payload

Checkout the `example` project for a starting point

## Documentation

Visit the [official documentation](https://docs.page/donnc/wce) for a detailed guide.

For a source-based comparison of how this Spring Boot port maps to the original Python engine, see [docs/project-findings.md](./docs/project-findings.md).
For the Java-first evolution plan, including template portability and enterprise considerations, see [docs/implementation-plan.md](./docs/implementation-plan.md).
For migration-oriented authoring guidance, see [docs/template-compatibility.md](./docs/template-compatibility.md).
For the current message support view, see [docs/message-support-matrix.md](./docs/message-support-matrix.md).
For practical dynamic rendering patterns, see [docs/dynamic-rendering.md](./docs/dynamic-rendering.md).
For router-hook usage and backend-driven payment/biller flow design, see [docs/router-hook-and-backend-driven-flows.md](./docs/router-hook-and-backend-driven-flows.md).
For advanced backend-built outbound templates, see [docs/advanced-dynamic-template-handling.md](./docs/advanced-dynamic-template-handling.md).
For the most important current engine improvement areas, see [docs/engine-improvement-areas.md](./docs/engine-improvement-areas.md).
For a step-by-step explanation of router redirects and a booking-bot walkthrough, see [docs/router-hook-walkthrough.md](./docs/router-hook-walkthrough.md).
For load-testing guidance and high-load performance notes, see [docs/stress-testing-and-performance.md](./docs/stress-testing-and-performance.md).
For the recommended `jawce` history-interface design, see [docs/history-interface-approach.md](./docs/history-interface-approach.md).
For the implemented history feature, configuration, and file-rotation behavior, see [docs/history-feature.md](./docs/history-feature.md).
For the preferred named-hook model, typed hook contracts, and method-backed named hooks, see [docs/named-hooks.md](./docs/named-hooks.md).
For an honest multi-industry assessment of where `jawce` is strong, weak, and how it can improve as a chatbot backbone, see [docs/industry-chatbot-assessment.md](./docs/industry-chatbot-assessment.md).
For an engine-only roadmap focused on fixing core orchestration weaknesses inside `jengine`, see [docs/core-engine-strategy.md](./docs/core-engine-strategy.md).

## Contributing

We welcome contributions! Please check out the [Contributing Guide](https://github.com/DonnC/jawce/blob/master/CONTRIBUTING.md) for details.

## License

This project is licensed under the MIT License. See the [LICENSE](https://github.com/DonnC/jawce/blob/master/LICENCE) file for details.
