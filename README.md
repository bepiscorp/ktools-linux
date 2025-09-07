# 🚀 ktools-linux

## 📘 Overview

Modular Kotlin CLI tools for Linux with git-style subcommands.

## 🔧 Setup

- ⬇️ Clone the repo
- 🏗️ Run `./gradlew build`

## 🧪 Testing

- ✅ `./gradlew test`

## 🔄 CI

- 🏗️ Build, test, integration test, and publish via GitHub Actions

## 📚 Documentation

See [docs](docs/index.md) for full details. Product and integration specs live in [docs/requirements.md](docs/requirements.md).

## 📖 Docs (MkDocs)

- Location: `mkdocs.yml` and `docs/`
- Install:

```bash
uv venv .venv
source .venv/bin/activate
uv pip install --upgrade mkdocs pymdown-extensions mkdocs-material mkdocs-mermaid2-plugin
```

- Serve locally with live reload:

```bash
mkdocs serve
```

- Build static site:

```bash
mkdocs build --clean
```

## 💿 Install (use `ktools` from your shell)

Install from a built distribution or from source. The produced launcher is named `cli`; the steps below create a `ktools` shim so you can run commands as `ktools ...`.

### macOS

- From source (local user install):

```bash
./gradlew :cli:installDist
mkdir -p "$HOME/.local/bin"
ln -sf "$PWD/cli/build/install/cli/bin/cli" "$HOME/.local/bin/ktools"
echo 'export PATH="$HOME/.local/bin:$PATH"' >> "$HOME/.zshrc"
exec $SHELL
```

- System-wide (requires sudo):

```bash
./gradlew :cli:distTar
sudo mkdir -p /opt/ktools
sudo tar -x -f cli/build/distributions/cli-*.tar -C /opt/ktools --strip-components=1
sudo ln -sf /opt/ktools/bin/cli /usr/local/bin/ktools
```

### Debian/Ubuntu

- From source (local user install):

```bash
./gradlew :cli:installDist
mkdir -p "$HOME/.local/bin"
ln -sf "$PWD/cli/build/install/cli/bin/cli" "$HOME/.local/bin/ktools"
echo 'export PATH="$HOME/.local/bin:$PATH"' >> "$HOME/.bashrc"
exec $SHELL
```

- System-wide (requires sudo):

```bash
./gradlew :cli:distTar
sudo install -d /opt/ktools
sudo tar -x -f cli/build/distributions/cli-*.tar -C /opt/ktools --strip-components=1
sudo ln -sf /opt/ktools/bin/cli /usr/local/bin/ktools
```

### Windows

- From source using the distribution ZIP:

```powershell
./gradlew :cli:distZip
Expand-Archive -Path "cli/build/distributions/cli-*.zip" -DestinationPath "$Env:ProgramFiles\ktools-temp"
if (Test-Path "$Env:ProgramFiles\ktools") { Remove-Item -Recurse -Force "$Env:ProgramFiles\ktools" }
Rename-Item "$Env:ProgramFiles\ktools-temp\cli-*" "$Env:ProgramFiles\ktools"
Copy-Item "$Env:ProgramFiles\ktools\bin\cli.bat" "$Env:ProgramFiles\ktools\bin\ktools.bat" -Force
setx PATH "$Env:PATH;$Env:ProgramFiles\ktools\bin"
```

- Verify:

```bash
ktools version
```

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

## 🧩 md2pdf quick start

Examples (full spec in [requirements](docs/requirements.md)):

```bash
# Single file → PDF
ktools md2pdf README.md -o README.pdf

# Directory (mirrors structure under output dir)
ktools md2pdf docs/ -O pdf/

# Globs (shell expanded)
ktools md2pdf docs/**/*.md -O pdf/

# Stdin → Stdout
echo "# Hello" | ktools md2pdf - -o - > hello.pdf

# Remote URL (requires explicit allow)
ktools md2pdf https://example.com/guide.md --allow-remote -o guide.pdf

# Theme, CSS, margins, template
ktools md2pdf file.md --theme github --css styles/print.css --margin "20mm,15mm,20mm,15mm" --template tpl.html -o file.pdf

# Batch with parallelism and resume
ktools md2pdf docs/ --parallel 4 --resume -O pdf/

# Dry-run (no conversion)
ktools md2pdf docs/ --dry-run
```

### 🔧 Engine selection

- Auto (default): `--engine auto` tries native (Node ≥ 18) then Docker.
- Force native: `--engine native` (requires Node ≥ 18)
- Force Docker: `--engine docker` (uses `realdennis/md2pdf`; supports `--reuse`)
- Discover: `ktools md2pdf engines` shows detected engines and versions.
- Diagnostics: `ktools md2pdf --about` and `ktools md2pdf --version`.

### ⚙️ Config precedence

1. `--config <file>`
2. Project: `.ktools/md2pdf.yaml` or `ktools.yaml`
3. User: `~/.config/ktools/md2pdf.yaml` or `~/.config/ktools/config.yaml`

CLI flags always override config values.

### 📦 I/O semantics

- Inputs: files, directories, globs, `-` (stdin), or remote URLs with `--allow-remote`.
- Outputs: `-o <file>` or `-o -` (stdout). For directories, defaults to mirrored paths; customize via `-O/--output-dir` or `--output-pattern`.

### 🚦 Exit codes

- 0: success
- 1: input validation error
- 2: conversion error (native or Docker)
- 3: Docker start/health-check failure
- 4: neither Node nor Docker available

### 📝 Logging & metrics

- JSON output: `--json` (for overall CLI); structured logs: `--log-json`
- Verbosity: `--verbose` (forward engine logs), `--quiet`
- Progress: compact TTY output with plain-text fallback
- Performance: `--parallel N`, `--metrics` (time, memory), `--timeout <sec>`

More usage examples: `ktools md2pdf examples` and [docs/commands.md](docs/commands.md).
