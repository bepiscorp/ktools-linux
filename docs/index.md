# 🏠 Home

Welcome to ktools-linux.

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
