# Example ChatBots
Example chatbots powered by `jawce`

## Setup
Before running, make sure bot config in `/src/main/resources/application.yml` are configured properly

The examples now rely on `jawce` default implementations for:

- file-backed sessions,
- YAML or JSON template loading,
- `RestTemplate`-based client wiring.

The `ehailing` example also uses the newer named-hook approach:

- templates reference stable hook names
- Spring beans expose the hook logic
- `jawce` resolves and validates the hook at startup

If using the default file session manager, you may delete old session data by deleting the `.session` folder.


## Quick Run
To quickly run the examples on local, consider using the local WhatsApp chatbot emulator available at [https://github.com/DonnC/wce-emulator.git](https://github.com/DonnC/wce-emulator.git)
