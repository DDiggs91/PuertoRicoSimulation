# Puerto Rico Simulation

A software simulation of the board game *Puerto Rico*, with an eventual goal
of AI opponents at varying skill levels.

![Java](https://img.shields.io/badge/Java-25-orange)
![Maven](https://img.shields.io/badge/Maven-3.9.16-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)
![CI](https://github.com/DDiggs91/PuertoRicoSimulation/actions/workflows/ci.yml/badge.svg)

> This is an unofficial, fan-made project created for personal/educational
> purposes. It is not affiliated with, endorsed by, or sponsored by Rio
> Grande Games, Ravensburger, alea, or Andreas Seyfarth (designer of Puerto
> Rico). "Puerto Rico" and its associated game mechanics, names, and artwork
> are the property of their respective rights holders. The MIT license
> below applies only to this repository's original source code, not to any
> Puerto Rico game content it describes.

## Status

Early scaffolding. The devcontainer and build tooling are set up, but no
game logic or model code has been written yet.

## Tech stack

- Java 25 (Eclipse Temurin)
- Maven 3.9.16 (multi-module, via the included Maven Wrapper)
- Spring Boot 4.1.0
- Lombok
- google-java-format (via `fmt-maven-plugin`, runs automatically on build)
- Frontend (`puerto-rico-frontend` module): TypeScript-based browser
  client; framework and build tooling not yet chosen

## Prerequisites

- [Docker](https://www.docker.com/) and [VS Code](https://code.visualstudio.com/)
  with the [Dev Containers](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)
  extension, **or** a [GitHub Codespace](https://github.com/features/codespaces)
- Alternatively, a local Java 25 JDK — the bundled `./mvnw` wrapper handles
  Maven itself

## Getting started

1. Clone the repo and open it in VS Code.
2. Reopen in container (or launch a Codespace). First boot runs
   `.devcontainer/post-create.sh`, which generates an SSH key if one doesn't
   already exist and prints a public key to add at
   [github.com/settings/ssh/new](https://github.com/settings/ssh/new) —
   do that once so `git push`/`git pull` work over SSH.
3. Build and test:
   ```bash
   ./mvnw clean install
   ./mvnw -pl puerto-rico-model test
   ```
   Code formatting runs automatically as part of the build — no manual
   formatting step needed.

## Project structure

```
.
├── .devcontainer/          # Dev container (Temurin 25 + Maven)
├── .github/workflows/      # CI
├── docs/
│   ├── architecture.md     # Component/module architecture outline
│   └── game-rules.md       # Puerto Rico rules reference for implementers
├── puerto-rico-model/       # Maven module — Game Engine (Rules Core)
├── puerto-rico-session/     # Maven module — Game Session (Orchestrator)
├── puerto-rico-ai/          # Maven module — AI Engine Plugins
├── puerto-rico-lobby/       # Maven module — Lobby / Matchmaking Manager
├── puerto-rico-web/         # Maven module — Web/API Layer (Spring Boot app)
├── frontend/                # Maven module — Web Frontend (browser client)
├── CLAUDE.md                # Guidance for AI-assisted development
├── LICENSE
├── pom.xml                  # Parent/reactor POM
└── README.md
```

Each module has its own README describing its intent; see
[docs/architecture.md](docs/architecture.md) for how they fit together.

## Documentation

- [CLAUDE.md](CLAUDE.md) — project conventions and commands for AI-assisted
  development
- [docs/architecture.md](docs/architecture.md) — component/module
  architecture outline
- [docs/game-rules.md](docs/game-rules.md) — Puerto Rico rules reference

## License

[MIT](LICENSE) — see the disclaimer above regarding the underlying game's IP.
