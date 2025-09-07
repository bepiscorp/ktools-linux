# 🧩 Commands

- `hello`
- `version`

> 📘 For creating new commands see [Adding Commands](adding-command.md).

## ▶️ Run examples

- **Hello**:

```bash
./gradlew -q :cli:run --args "hello"
```

- **Version (JSON)**:

```bash
./gradlew -q :cli:run --args "version --json"
```

- **With stdin**:

```bash
echo data | ./gradlew -q :cli:run --args "hello"
```
