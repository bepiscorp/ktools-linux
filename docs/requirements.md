## 📋 System Requirements

This document captures the system requirements for ktools-linux. It includes the original scaffold goals and the md2pdf integration requirements so they are trackable and easy to reference.

### 🎯 Initial Scaffold Requirements

The initial scaffold requirements are merged into the Functional and Non-Functional Requirements sections below (IDs prefixed with SFR- and SNFR-).

### 🧰 md2pdf Integration Requirements

### ✅ Functional Requirements

- FR-1 — CLI command: Add sub-command `ktools md2pdf <input…> [options]`. Inputs support files, directories, globs (e.g., `docs/**/*.md`), `.md` and `.markdown` extensions, `-` for stdin, and remote URLs when `--allow-remote` is set. Multiple paths are accepted without `--batch`.
- FR-2 — Engine selection: Unify engine flags as `--engine <auto | native | docker>` (default: `auto`). Provide `ktools md2pdf engines` to display detected engines and versions. Conflicting legacy flags are replaced by `--engine`.
- FR-3 — Native library mode: Fork md2pdf and add a Node module export `module.exports.convert(markdown:string, options:Object): Promise<Buffer>`. The Kotlin wrapper invokes this via a Node bridge when Node (≥ 18) is available.
- FR-4 — Docker mode: If `--engine docker` or Node is unavailable, run the md2pdf Docker image (`realdennis/md2pdf`). The image must include a health-check that returns HTTP 200 on `/health` when ready. The wrapper polls (max 30 s) with retries/backoff before conversion. Support `--reuse/--no-reuse`; default to auto-stop after idle unless `--reuse` is set.
- FR-5 — CLI options: `-o/--output`, `-O/--output-dir`, `--output-pattern`, `--overwrite/--no-clobber`, `--page-size`, `--margin`, `--css`, `--theme`, `--template`, `--toc`, `--var key=val` (repeatable), `--batch`, `--parallel <N>`, `--resume`, `--fail-fast/--keep-going`, `--timeout <seconds>`, `--verbose`, `--quiet`, `--color <auto | always | never>`, `--log-json`, `--metrics`, `--dry-run`, `--engine`, `--config`, `--about`, `--version`, `--allow-remote`.
- FR-6 — Input & output semantics: `stdin` supported with `-`; `stdout` supported with `-o -`. Globs expand per shell; directories convert all `*.md`/`*.markdown`. Remote URLs require `--allow-remote`.
- FR-7 — File validation: Verify each input exists (or is `-`/remote when allowed), is readable, and has a supported extension. On failure return exit code 1 with `ERROR 1:` and the offending flag/value.
- FR-8 — Batch processing semantics: For directories, write outputs to mirrored structure under `--output-dir` (default: sibling `pdf/`), or use `--output-pattern`. For file lists, place outputs next to sources unless overridden. Emit progress `[n/m]` and final summary (converted, skipped, failed, elapsed). Honor `--resume`, `--parallel`, `--fail-fast/--keep-going`.
- FR-9 — Error handling & exit codes: `0` success, `1` input validation, `2` conversion error, `3` Docker start/health-check failure, `4` neither Node nor Docker available. Prefix messages with `ERROR <code>:` and include offending flag/value and next-step hint.
- FR-10 — Progress, logging, and TTY behavior: Compact, colorized status lines and per-file progress bars on TTY; plain text fallback. `--log-json` produces machine-readable logs. `--quiet` reduces noise; `--verbose` forwards md2pdf logs.
- FR-11 — Mode announcement: Print “Using native mode”, “Using docker mode”, or the forced engine at startup. First-run Docker indicates image pull progress and suggests `--engine docker` when Node is missing.
- FR-12 — Custom template & content: Template supports `{{content}}` and optionally `{{header}}`, `{{footer}}`, `{{title}}`, `{{author}}`, `{{date}}`, `{{toc}}`. YAML front-matter populates variables; `--var key=val` overrides.
- FR-13 — Help & discoverability: Extend `ktools --help` and `ktools md2pdf --help`. Provide `--help all` and `ktools md2pdf examples`. Group options by area (input/output, formatting, performance, Docker, config, diagnostics).
- FR-14 — Version and about: `ktools md2pdf --version` prints md2pdf version (npm for native; Docker tag for fallback). `--about` prints engine, versions, resolved config files, theme, and environment details.
- FR-16 — Dry-run support: `--dry-run` lists files to be processed and targets; shows engine selection, output resolution, concurrency plan.
- FR-17 — Configuration file: Precedence: `--config` > project local (`.ktools/md2pdf.yaml` or `ktools.yaml`) > user (`~/.config/ktools/md2pdf.yaml` or `~/.config/ktools/config.yaml`). Flags override config values. Provide `ktools config init md2pdf` to generate starter config.

#### 🚀 Scaffold Functional Requirements (SFR)

- SFR-1 — CLI scaffold: Git-style subcommands using Clikt; modules `:cli`, `:core`, `:commands:hello`, `:commands:version`.
- SFR-2 — Hello command: Prints “Hello from ktools!”.
- SFR-3 — Version command: Prints current project version from Gradle `version`.
- SFR-4 — Configuration system: Global `~/.config/ktools/config.conf` and per-command `~/.config/ktools/<command>.conf` with sensible defaults (HOCON).
- SFR-5 — Dependency injection: Koin wires logger, config loader, and services.
- SFR-6 — Logging: Structured logging with Koog.
- SFR-7 — JSON output: All commands support `--json` via kotlinx.serialization.
- SFR-8 — Extensibility: Architecture prepared for future plugin loading (e.g., ServiceLoader discovery placeholder).

### 🧪 Non-Functional Requirements

- NFR-1 — Cross-platform: macOS, Linux, Windows via Gradle. Path normalization and quoting are documented and tested.
- NFR-2 — Performance: Convert a typical 10 KB markdown file in ≤ 5 s. `--metrics` reports time and memory; `--parallel` improves throughput.
- NFR-3 — Reliability: Defined exit codes and actionable hints. Docker health-checks with retries/backoff; containers auto-stop unless `--reuse`.
- NFR-4 — Security: Docker runs with read-only filesystem (except mounted output), no host network by default, least privileges.
- NFR-5 — Maintainability: Integration code resides in `cli/src/main/kotlin/com/bepiscorp/ktools/cli/md2pdf/` (or command module) and follows Google Kotlin Style; common flags shared.
- NFR-6 — Dependency management: Gradle task `npmInstallMd2Pdf` clones/builds native export and caches artifacts.
- NFR-7 — Documentation: Update README, commands, and index with new command, options, engine selection, config precedence, examples (incl. PowerShell), batch semantics, quick-start, and diagnostics (`engines`, `about`).
- NFR-8 — CI integration: CI runs tests and Docker integration test; parses logs via `--log-json`; exercises stdin/stdout, Windows path normalization, and `--parallel`.
- NFR-9 — Extensibility: New md2pdf options added by extending options map passed to native or Docker without changing wrapper core; discoverable via `--list-options`.
- NFR-10 — Usability: Auto-completion, man page, rich `--help`, examples, and `engines` commands. Colorized TTY output and `--quiet`.
- NFR-11 — Accessibility: Clear language; progress bars have plain-text fallbacks; dry-run and summaries assist limited terminal visibility.

#### 🧱 Scaffold Non-Functional Requirements (SNFR)

- SNFR-1 — Baseline: Kotlin 2+, Gradle 9+ with Kotlin DSL; plugins `application`, Kotlin, Kotlin serialization, Dokka.
- SNFR-2 — Libraries: Clikt, Koin, kotlinx.coroutines, Ktor, Koog, kotlinx.serialization, HOCON (`com.typesafe:config` + Kotlin wrapper).
- SNFR-3 — Code style and commits: Conventional Commits; Git hooks for commitlint, spotless+ktlint, markdownlint; follow Google Kotlin Style Guide with JetBrains fallback; document code style.
- SNFR-4 — Testing stack: kotest, mockk, testcontainers with basic scaffolding.
- SNFR-5 — Documentation: Dokka for API docs; MkDocs (Mermaid enabled) with coding style standards, contribution guide, and instructions for adding new commands.
- SNFR-6 — Multimodule architecture: `:core` for config/DI/logging/utils; `:cli` for entrypoint; `:commands:*` for subcommands; commands designed for future plugin loading.
- SNFR-7 — Deliverables: Gradle project structure, `Main.kt` command discovery, config in `:core`, DI with Koin, Koog logging, test scaffolding, documentation setup, and code comments guiding extension points.
- SNFR-8 — CLI UX: Runs on macOS, Debian, and CI with clear error messages.
- SNFR-9 — Versioning: Semantic Versioning for releases.

### 🔗 Traceability

- The above requirements map to implementation in `:commands:md2pdf` and CLI wiring in `:cli`.
- Cross-cutting concerns (logging, DI, config) are implemented in `:core` and shared with commands.
