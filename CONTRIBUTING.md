# Contributing to Jawce

Thank you for considering contributing to Jawce!

By contributing, you're helping improve a robust tool designed for building WhatsApp chatbots and using WhatsApp Cloud APIs effortlessly.

---

## Getting Started

### Prerequisites
Ensure you have the following installed before contributing:
- Java 17
- An active [TEST] WhatsApp Cloud API setup with the necessary tokens and configurations.
- Or use WhatsApp ChatBot emulator tool https://github.com/DonnC/wce-emulator.git

### Setting Up the Project
1. Fork the repository on GitHub.
2. Clone your forked repository locally:
   ```bash
   git clone https://github.com/DonnC/jawce.git
   cd jawce
   ```
3. Install maven dependencies:

---

## Contribution Workflow

### 1. Reporting Issues
If you encounter any bugs, feature requests, or documentation issues, please [open an issue](https://github.com/DonnC/jawce/issues).

### 2. Suggesting Features
Feel free to suggest new features or improvements

### 3. Making Changes
- Before you start coding, create a new branch for your changes:
  ```bash
  git checkout -b feature/my-new-feature
  ```
- Keep your changes modular and adhere to the existing coding style.
- Ensure you add comments where necessary.

### 4. Running Tests
Write tests for your contributions in the `tests` folder.

### 5. Submitting Your Changes
- Commit your changes with meaningful messages:
  ```bash
  git commit -m "Add feature: Support for multiple template triggers"
  ```
- Push your branch to your forked repository:
  ```bash
  git push origin feature/my-new-feature
  ```
- Open a pull request from your branch to the `main` branch of the original repository.

---

### Writing Tests
All new features and bug fixes should include test coverage.
Place your tests in the corresponding module `tests/` directory.
Use meaningful names for test files and classes.

---

## Special Contributions for PyWCE

### Implementing session manager

### Adding New Engine Features
The engine processes chatbot logic via YAML templates. When adding new engine capabilities:
- Add corresponding engine logic in corresponding folder under `jengine`.
- Document new template options or hooks clearly in `README`.

---

## Getting Help
If you're stuck or have any questions, don't hesitate to open a discussion on the [GitHub Discussions page](https://github.com/DonnC/jawce/discussions).

Thank you for contributing to jawce!
```
