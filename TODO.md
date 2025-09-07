# 📋 TODO

- ✅ Scaffold multimodule Gradle project with core, cli, and command modules
- ✅ Add CLI entrypoint registering `hello` and `version` commands
- ✅ Implement config management layer with global and per-command HOCON files
- ✅ Wire dependency injection with Koin
- ✅ Integrate Koog for structured logging
- ✅ Support `--json` flag for all commands using kotlinx.serialization
- ✅ Configure Git hooks for commitlint, spotless + ktlint, and markdownlint
- ✅ Add test scaffolding using kotest, mockk, and testcontainers
- ✅ Set up documentation with Dokka and MkDocs (Mermaid enabled)
- ✅ Document code style (Google Kotlin Style Guide → JetBrains fallback) and contribution guide
- 🚧 Future plugin system using ServiceLoader for dynamic command loading
- ✅ Rename package prefix to `com.bepiscorp.ktools` and set Gradle group
- ✅ Add AGENTS.md with LLM contributor guidelines
- ✅ Fix DockerBridge to copy files instead of invalid volume mount and cache engine name
- ✅ Document docker cp usage for md2pdf Docker mode
