# 🏠 Home

Welcome to ktools-linux.

See Requirements for product and integration specs: [requirements](requirements.md).

## ▶️ Running

- **Via Gradle**:

```bash
./gradlew :cli:run --args "hello"
./gradlew :cli:run --args "version --json"
```

- **With stdin**:

```bash
printf 'input' | ./gradlew -q :cli:run --args "hello"
```

Tip: Use `-q` for quieter Gradle output.
