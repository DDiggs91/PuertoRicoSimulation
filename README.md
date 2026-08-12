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

Playable end to end: create a table in a browser, take a seat, seat AI
opponents, and play the game out to final standings — or seat only AIs and
watch, from your own tab or a second one opened mid-game. A seated human
plays through a per-phase action picker for every one of the game's eight
action families. All seven modules are built — rules engine, orchestrator,
lobby, AI, the OpenAPI wire contract, the Spring Boot backend, and the React
frontend.

The main thing missing is a *good* opponent: the only AI engine plays at
random. See [TODO.md](TODO.md) for that and the rest of the roadmap.

## Tech stack

- Java 25 (Eclipse Temurin), Spring Boot 4.1.0 (Jackson 3)
- Maven 3.9.16 (multi-module, via the included Maven Wrapper)
- React 19 + TypeScript + Vite, generated from an OpenAPI spec shared with
  the Java side (`puerto-rico-contract`) — see that module's README
- Lombok
- google-java-format (via `fmt-maven-plugin`, runs automatically on build)
- TestNG (engine/domain modules) or JUnit 5 (`puerto-rico-web`, Spring's
  own idiomatic tooling); Vitest + React Testing Library + Playwright for
  the frontend

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
   ./mvnw clean install    # full reactor, all seven modules, incl. frontend Vitest
   ```
   Code formatting runs automatically as part of the build — no manual
   formatting step needed.
4. Run it:
   ```bash
   ./mvnw -pl puerto-rico-web spring-boot:run
   ```
   then open [http://localhost:8080](http://localhost:8080) — create a
   game, take a seat, seat a couple of random AIs, and start it.
   [docs/playing.md](docs/playing.md) walks through a full game as one human
   against two AIs.

## Project structure

```
.
├── .devcontainer/           # Dev container (Temurin 25 + Maven + Node)
├── .github/workflows/       # CI
├── docs/
│   ├── architecture.md      # How the modules fit together
│   ├── playing.md           # How to run it and play a game
│   ├── game-rules.md        # Puerto Rico rules reference for implementers
│   └── puerto-rico-rules-en.pdf  # The official rulebook game-rules.md derives from
├── puerto-rico-model/       # Maven module — Game Engine (Rules Core)
├── puerto-rico-session/     # Maven module — Game Session (Orchestrator)
├── puerto-rico-ai/          # Maven module — AI Engine Plugins
├── puerto-rico-lobby/       # Maven module — Lobby / Matchmaking Manager
├── puerto-rico-contract/    # Maven module — Wire Contract (OpenAPI)
├── puerto-rico-web/         # Maven module — Web/API Layer (Spring Boot app)
├── puerto-rico-frontend/    # Maven module — Web Frontend (React browser client)
├── .dockerignore            # Keeps the widened devcontainer build context small
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
