## 📋 System Requirements

This document captures the system requirements for ktools-linux. It includes the original scaffold goals and the md2pdf integration requirements so they are trackable and easy to reference.

### 🎯 Initial Scaffold Requirements

| ID    | Requirement                                                                                                                   |
| ----- | ----------------------------------------------------------------------------------------------------------------------------- |
| SR-1  | Language: Kotlin 2+                                                                                                           |
| SR-2  | Build system: Gradle 9+ with Kotlin DSL (`build.gradle.kts`)                                                                  |
| SR-3  | Plugins: `application`, Kotlin, Kotlin serialization, Dokka                                                                   |
| SR-4  | Libraries: Clikt, Koin, kotlinx.coroutines, Ktor, Koog, kotlinx.serialization, HOCON (`com.typesafe:config` + Kotlin wrapper) |
| SR-5  | Config: Global config at `~/.config/ktools/config.conf`                                                                       |
| SR-6  | Config: Per-command configs at `~/.config/ktools/<command>.conf`                                                              |
| SR-7  | Config: Sensible defaults when config files are missing                                                                       |
| SR-8  | CLI UX: Runs on macOS, Debian, and inside CI; user-friendly errors                                                            |
| SR-9  | CLI UX: All commands support `--json` output via kotlinx.serialization                                                        |
| SR-10 | Versioning: Semantic Versioning; `version` command prints Gradle `version`                                                    |
| SR-11 | Commits & hooks: Conventional Commits; git hooks for commitlint, spotless+ktlint, markdownlint                                |
| SR-12 | Code style: Google Kotlin Style Guide; JetBrains style fallback; rules documented                                             |
| SR-13 | Testing: `kotest`, `mockk`, `testcontainers`                                                                                  |
| SR-14 | Docs: Dokka for API docs; MkDocs with Mermaid; include coding style, contribution guide, adding commands                      |
| SR-15 | Architecture: Multimodule Gradle project (`:core`, `:cli`, `:commands:hello`, `:commands:version`)                            |
| SR-16 | Architecture: Commands future-ready for plugin loading; Koin for DI wiring                                                    |
| SR-17 | Deliverable: Gradle project structure with `settings.gradle.kts`                                                              |
| SR-18 | Deliverable: `Main.kt` in `:cli` that discovers and registers commands                                                        |
| SR-19 | Deliverable: Example commands `hello` and `version`                                                                           |
| SR-20 | Deliverable: Config management layer in `:core` (global + per-command via HOCON)                                              |
| SR-21 | Deliverable: DI setup with Koin (logger, config loader, services)                                                             |
| SR-22 | Deliverable: Logging integration with Koog (structured levels)                                                                |
| SR-23 | Deliverable: `--json` supported by all commands                                                                               |
| SR-24 | Deliverable: Git hooks configured (commitlint, spotless+ktlint, markdownlint)                                                 |
| SR-25 | Deliverable: Basic test scaffolding with kotest + mockk + testcontainers                                                      |
| SR-26 | Deliverable: Documentation setup with Dokka + MkDocs (Mermaid enabled)                                                        |
| SR-27 | Deliverable: Code comments explaining adding commands, wiring Koin, extending configs, and future plugin loading location     |

### 🧰 md2pdf Integration Requirements

1. Functional Requirements

| ID    | Requirement                                                                                                                                                                                                                                                                                                                                                                                                              |
| ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| FR-1  | CLI command – Add sub-command `ktools md2pdf <input…> [options]`. Inputs support files, directories, globs (e.g., `docs/**/*.md`), `.md` and `.markdown` extensions, `-` for stdin, and remote URLs when `--allow-remote` is set. Multiple paths are accepted without `--batch`.                                                                                                                                         |
| FR-2  | Engine selection – Unify engine flags as `--engine <auto                                                                                                                                                                                                                                                                                                                                                                 | native | docker>`(default:`auto`). Provide `ktools md2pdf engines`to display detected engines and versions. Conflicting legacy flags are replaced by`--engine`. |
| FR-3  | Native library mode – Fork md2pdf and add a Node module export `module.exports.convert(markdown:string, options:Object): Promise<Buffer>`. The Kotlin wrapper invokes this via a Node bridge when Node (≥ 18) is available.                                                                                                                                                                                              |
| FR-4  | Docker mode – If `--engine docker` or Node is unavailable, run the md2pdf Docker image (`realdennis/md2pdf`). The image must include a health-check that returns HTTP 200 on `/health` when ready. The wrapper polls (max 30 s) with retries/backoff before conversion. Support `--reuse/--no-reuse` to keep or tear down a warm container between conversions; default to auto-stop after idle unless `--reuse` is set. |
| FR-5  | CLI options (all optional unless otherwise noted): `-o/--output`, `-O/--output-dir`, `--output-pattern`, `--overwrite/--no-clobber`, `--page-size`, `--margin`, `--css`, `--theme`, `--template`, `--toc`, `--var key=val` (repeatable), `--batch`, `--parallel <N>`, `--resume`, `--fail-fast/--keep-going`, `--timeout <seconds>`, `--verbose`, `--quiet`, `--color <auto                                              | always | never>`, `--log-json`, `--metrics`, `--dry-run`, `--engine`, `--config`, `--about`, `--version`, `--allow-remote`.                                     |
| FR-6  | Input & output semantics – `stdin` supported with `-`; `stdout` supported with `-o -`. Globs expand per shell; directories convert all `*.md`/`*.markdown`. Remote URLs require `--allow-remote`.                                                                                                                                                                                                                        |
| FR-7  | File validation – Verify each input exists (or is `-`/remote when allowed), is readable, and has a supported extension. On failure return exit code 1 with `ERROR 1:` and the offending flag/value.                                                                                                                                                                                                                      |
| FR-8  | Batch processing semantics – For directories, write outputs to mirrored structure under `--output-dir` (default: sibling `pdf/`), or use `--output-pattern`. For file lists, place outputs next to sources unless overridden. Emit progress `[n/m]` and final summary (converted, skipped, failed, elapsed). Honor `--resume`, `--parallel`, `--fail-fast/--keep-going`.                                                 |
| FR-9  | Error handling & exit codes – `0` success, `1` input validation, `2` conversion error, `3` Docker start/health-check failure, `4` neither Node nor Docker available. Prefix messages with `ERROR <code>:` and include offending flag/value and next-step hint.                                                                                                                                                           |
| FR-10 | Progress, logging, and TTY behavior – Compact, colorized status lines and per-file progress bars on TTY; plain text fallback. `--log-json` produces machine-readable logs. `--quiet` reduces noise; `--verbose` forwards md2pdf logs.                                                                                                                                                                                    |
| FR-11 | Mode announcement – Print “Using native mode”, “Using docker mode”, or the forced engine at startup. First-run Docker indicates image pull progress and suggests `--engine docker` when Node is missing.                                                                                                                                                                                                                 |
| FR-12 | Custom template & content – Template supports `{{content}}` and optionally `{{header}}`, `{{footer}}`, `{{title}}`, `{{author}}`, `{{date}}`, `{{toc}}`. YAML front-matter populates variables; `--var key=val` overrides.                                                                                                                                                                                               |
| FR-13 | Help & discoverability – Extend `ktools --help` and `ktools md2pdf --help`. Provide `--help all` and `ktools md2pdf examples`. Group options by area (input/output, formatting, performance, Docker, config, diagnostics).                                                                                                                                                                                               |
| FR-14 | Version and about – `ktools md2pdf --version` prints md2pdf version (npm for native; Docker tag for fallback). `--about` prints engine, versions, resolved config files, theme, and environment details.                                                                                                                                                                                                                 |
| FR-16 | Dry-run support – `--dry-run` lists files to be processed and targets; shows engine selection, output resolution, concurrency plan.                                                                                                                                                                                                                                                                                      |
| FR-17 | Configuration file – Precedence: `--config` > project local (`.ktools/md2pdf.yaml` or `ktools.yaml`) > user (`~/.config/ktools/md2pdf.yaml` or `~/.config/ktools/config.yaml`). Flags override config values. Provide `ktools config init md2pdf` to generate starter config.                                                                                                                                            |

2. Non-Functional Requirements

| ID     | Requirement                                                                                                                                                                                                         |
| ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| NFR-1  | Cross-platform – macOS, Linux, Windows via Gradle. Path normalization and quoting are documented and tested.                                                                                                        |
| NFR-2  | Performance – Convert a typical 10 KB markdown file in ≤ 5 s. `--metrics` reports time and memory; `--parallel` improves throughput.                                                                                |
| NFR-3  | Reliability – Defined exit codes and actionable hints. Docker health-checks with retries/backoff; containers auto-stop unless `--reuse`.                                                                            |
| NFR-4  | Security – Docker runs with read-only filesystem (except mounted output), no host network by default, least privileges.                                                                                             |
| NFR-5  | Maintainability – Integration code resides in `cli/src/main/kotlin/com/bepiscorp/ktools/cli/md2pdf/` (or command module) and follows Google Kotlin Style; common flags shared.                                      |
| NFR-6  | Dependency management – Gradle task `npmInstallMd2Pdf` clones/builds native export and caches artifacts.                                                                                                            |
| NFR-7  | Documentation – Update README, commands, and index with new command, options, engine selection, config precedence, examples (incl. PowerShell), batch semantics, quick-start, and diagnostics (`engines`, `about`). |
| NFR-8  | CI integration – CI runs tests and Docker integration test; parses logs via `--log-json`; exercises stdin/stdout, Windows path normalization, and `--parallel`.                                                     |
| NFR-9  | Extensibility – New md2pdf options added by extending options map passed to native or Docker without changing wrapper core; discoverable via `--list-options`.                                                      |
| NFR-10 | Usability – Auto-completion, man page, rich `--help`, examples, and `engines` commands. Colorized TTY output and `--quiet`.                                                                                         |
| NFR-11 | Accessibility – Clear language; progress bars have plain-text fallbacks; dry-run and summaries assist limited terminal visibility.                                                                                  |

### 🔗 Traceability

- The above requirements map to implementation in `:commands:md2pdf` and CLI wiring in `:cli`.
- Cross-cutting concerns (logging, DI, config) are implemented in `:core` and shared with commands.
