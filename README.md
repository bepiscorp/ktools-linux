# 🚀 ktools-linux

## 📘 Overview

Modular Kotlin CLI tools for Linux with git-style subcommands.

## 🔧 Setup

- ⬇️ Clone the repo
- 🏗️ Run `./gradlew build`

## 🧪 Testing

- ✅ `./gradlew test`

## 📚 Documentation

See [docs](docs/index.md) for full details.

## ▶️ Run via Gradle

- **Pass arguments**:

```bash
./gradlew -q :cli:run --args "hello"
./gradlew -q :cli:run --args "version --json"
```

- **Pipe stdin to the CLI** (stdin is forwarded to the app):

```bash
echo '{"name":"world"}' | ./gradlew -q :cli:run --args "hello"
```

- **Notes**:
  - The top-level command is `ktools`; when running via Gradle you only pass subcommand args (e.g., `hello`, `version --json`).
  - For cleaner output, add `-q` or `--quiet` to Gradle.
