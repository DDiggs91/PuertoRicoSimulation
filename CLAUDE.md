# CLAUDE.md

Guidance for Claude Code (and other AI-assisted development) in this repo.

## Project

A software simulation of the board game *Puerto Rico*, with an eventual
goal of AI opponents at varying skill levels. `puerto-rico-model` holds a
complete base-game rules engine, `puerto-rico-session` orchestrates a
running game on top of it (the Decision and Event/broadcast contracts), and
`puerto-rico-lobby` seats actors into a table and creates the Game Session
(the Lobby contract); the remaining three modules are still empty
scaffolding, so don't assume any AI, web, or frontend code exists until you
find it.

Git history prior to commit `9afca52` reflects abandoned early work and
should be disregarded — treat this repository's current state as the
effective starting point rather than reconstructing intent from old
commits.

For Puerto Rico's rules, see [docs/game-rules.md](docs/game-rules.md). It is
derived from the official rulebook kept alongside it at
[docs/puerto-rico-rules-en.pdf](docs/puerto-rico-rules-en.pdf), so its numbers
are authoritative — check the PDF only for wording or worked examples the
reference leaves out. Scope is the base game, 3–5 players; there is no
2-player variant.

[README.md](README.md) carries an explicit disclaimer that this is an
unofficial fan project, not affiliated with Rio Grande Games, Ravensburger,
alea, or designer Andreas Seyfarth. Keep that in mind for any user-facing
copy or documentation — don't imply official affiliation or endorsement.

## Tech stack

- Java 25 (Eclipse Temurin)
- Maven, multi-module: root `puerto-rico-parent` (packaging `pom`) → six
  child modules — `puerto-rico-model`, `puerto-rico-session`,
  `puerto-rico-ai`, `puerto-rico-lobby`, `puerto-rico-web`, and
  `puerto-rico-frontend`. Each child declares its own internal
  dependencies on the others it needs (see their `pom.xml`s); the root
  POM pins their versions via `dependencyManagement`.
  `puerto-rico-model` itself still declares nothing beyond
  `parent`/`groupId`/`artifactId` — it inherits all third-party
  dependencies and plugins from the root. Add new third-party
  dependencies to the root `pom.xml`'s `dependencyManagement`/
  `dependencies` unless they're genuinely module-specific; internal
  module-to-module dependencies belong in the child module's own POM.
  `puerto-rico-model`, `puerto-rico-session`, and `puerto-rico-lobby` each
  add only TestNG and a surefire provider of their own (see below);
  everything else they inherit.
  See [docs/architecture.md](docs/architecture.md) for what each module
  is responsible for and how they connect.
- Spring Boot 4.1.0 — currently BOM/starters only (`spring-boot-starter-web`,
  `spring-boot-starter-test`); no `@SpringBootApplication` class or
  controller exists yet. Its eventual role is decided, though:
  `puerto-rico-web` is the intended application module, and the target
  deployable is a single small Docker image running that Spring Boot app
  — don't assume the controller/WebSocket layer already exists, but do
  assume that's where it's headed.
- Lombok, on the compiler's annotation processor path and on the compile
  classpath at `provided` scope so the annotations are importable
- google-java-format via `fmt-maven-plugin`, bound to `process-sources` —
  runs automatically on every build
- TestNG for `puerto-rico-model`, `puerto-rico-session`, and
  `puerto-rico-lobby`'s tests. JUnit 5 also reaches every module via the
  inherited `spring-boot-starter-test`, and surefire runs exactly one
  provider, so each of those modules names the TestNG provider explicitly
  in its own `pom.xml` rather than leaving it to auto-detection. Surefire's
  version is pinned in the root `pluginManagement` — importing the Spring
  Boot BOM brings `dependencyManagement` only, so it would otherwise fall
  back to Maven's default.

Package convention: `com.PRS.model.*` for the rules engine (e.g.
`com.PRS.model.boards`, `.buildings`, `.goods`, `.rolecards`, `.game`,
`.actions`, `.engine`, `.scoring`), `com.PRS.session.*` for the orchestrator
(`com.PRS.session`, `.actors`, `.view`, `.events`), and `com.PRS.lobby` (flat,
no subpackages) for the lobby. See
[puerto-rico-model/README.md](puerto-rico-model/README.md),
[puerto-rico-session/README.md](puerto-rico-session/README.md), and
[puerto-rico-lobby/README.md](puerto-rico-lobby/README.md) for what each
package holds and the design decisions behind them.

## Commands

Use the bundled wrapper (`./mvnw`) so the Maven version always matches what
CI and the devcontainer use — don't rely on a bare `mvn` unless the wrapper
is unavailable. The wrapper pins Maven **3.9.16**, matching the version
installed in `.devcontainer/Dockerfile`.

```bash
./mvnw verify                                # full reactor build + tests (matches CI)
./mvnw clean install                         # build and install all modules to ~/.m2
./mvnw -pl puerto-rico-model verify           # build/test just the model module
./mvnw -pl puerto-rico-model test -Dtest=FooTest             # single test class
./mvnw -pl puerto-rico-model test -Dtest=FooTest#barMethod   # single test method
./mvnw fmt:format                            # run the formatter explicitly (rarely needed, see below)
```

Run `./mvnw verify` before considering any code task complete.

## CI & dependency updates

- `.github/workflows/ci.yml` runs on every push/PR to `main`: checkout →
  `setup-java` (temurin 25, maven cache) → `./mvnw -B verify`. This is the
  exact command local `./mvnw verify` is meant to match.
- `.github/dependabot.yml` opens weekly update PRs for the `maven` and
  `github-actions` ecosystems automatically — unprompted dependency-bump
  PRs are expected automation, not something to action manually unless
  asked.

## Code style

- Formatting is automatic: `fmt-maven-plugin`'s `format` goal runs on every
  build and rewrites files to match google-java-format. This is
  intentional — it auto-fixes rather than failing the build on violations,
  and CI (`./mvnw verify`) relies on that same auto-fix behavior rather
  than a check-and-fail step. Don't hand-format Java code, and don't
  propose switching this to a check/fail mode — it's a deliberate,
  settled choice.
- Prefer Lombok annotations over hand-written boilerplate. For the immutable
  records the model is built from, `@Builder(toBuilder = true)` is the one
  that earns its keep — it replaces long chains of `withX` copy methods.
- Tests live under `src/test/java`, mirroring the `src/main/java` package
  structure. `puerto-rico-model`, `puerto-rico-session`, and
  `puerto-rico-lobby` use TestNG (`@Test`, `@DataProvider`) with AssertJ
  assertions; AssertJ and Mockito arrive transitively via
  `spring-boot-starter-test`. Rules tables belong in a `@DataProvider` so
  each row is asserted individually.

## Devcontainer

- Base image `eclipse-temurin:25-jdk-noble`; Maven is installed manually in
  the Dockerfile (not via apt) specifically to avoid pulling in a second,
  conflicting JDK.
- Runs as the non-root `ubuntu` user with passwordless sudo.
- `~/.claude` and `~/.ssh` are persisted across rebuilds via named Docker
  volumes.
- First boot runs `.devcontainer/post-create.sh`, which generates an
  ed25519 SSH key if missing (prints the public key to add to GitHub) and
  pre-trusts GitHub's host key.

## Open questions — do not assume

- The devcontainer includes the Cucumber VS Code extension
  (`cucumberopen.cucumber-official`), but no Cucumber/Gherkin Maven
  dependency or `.feature` files exist. Treat BDD tooling as anticipated,
  not committed — confirm before adding `cucumber-java`/feature-file
  scaffolding.
