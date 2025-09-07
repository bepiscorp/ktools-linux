# 🤖 AGENTS

Guidelines for LLM contributors. Follows [AGENTS.md spec](https://github.com/openai/agents.md).

## 🧭 General

- 🧬 Keep package prefix and Gradle group `com.bepiscorp.ktools`.
- 📦 Use Semantic Versioning for releases.

## 🛠️ Development

- 📐 Follow Google Kotlin Style Guide (JetBrains style fallback).
- 🧹 Format code with `./gradlew spotlessApply`; verify with `./gradlew spotlessCheck`.
- 🧪 Run `./gradlew test` for code changes; skip only for docs or comments.
- 🏗️ Build with `./gradlew build` when needed.
- 🔍 Prefer `rg` for searches; avoid `grep -R` and `ls -R`.
- 🚫 Do not create new git branches; commit on main.

## 📚 Documentation

- 📄 Place docs in `README.md`, `CHANGELOG.md`, or `docs/`; store images in `assets/`.
- 🔤 Prefix headings and list items with at most one relevant emoji.
- ✍️ Record user tasks in `TODO.md` with `🚧` or `✅` status.

## 💬 Commits & PRs

- 📝 Follow Conventional Commits (e.g., `feat(cli): add init command`).
- 📣 Reference files with line numbers in PR summaries.

## 🤖 LLM Tips

- ⏳ Wait for commands to finish before continuing.
- 📚 Read nested `AGENTS.md` files for additional local rules.
