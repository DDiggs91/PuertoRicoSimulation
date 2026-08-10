# CLAUDE.md

Guidance for Claude Code (and other AI-assisted development) in this repo.

## Project

A software simulation of the board game *Puerto Rico*, with an eventual
goal of AI opponents at varying skill levels. All seven modules are now
built: `puerto-rico-model` (rules engine), `puerto-rico-session`
(orchestrator), `puerto-rico-lobby` (seating + session ownership),
`puerto-rico-ai` (a random-play engine), `puerto-rico-contract` (the
OpenAPI wire contract, generating both Java and TypeScript), `puerto-rico-web`
(the Spring Boot backend, Human Actor Adapter, SSE), and
`puerto-rico-frontend` (React lobby + live spectator view). The one thing
deliberately not built yet is the frontend's click-to-move interaction
UI for human players — everything server-side it needs (`HumanActor`, seat
tokens, the moves endpoint) already exists and is tested; see
`puerto-rico-frontend/README.md`'s Status section.

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
- Maven, multi-module: root `puerto-rico-parent` (packaging `pom`) → seven
  child modules — `puerto-rico-model`, `puerto-rico-session`,
  `puerto-rico-ai`, `puerto-rico-lobby`, `puerto-rico-contract`,
  `puerto-rico-web`, and `puerto-rico-frontend`. Each child declares its own
  internal dependencies on the others it needs (see their `pom.xml`s); the
  root POM pins their versions via `dependencyManagement`.
  `puerto-rico-model` itself still declares nothing beyond
  `parent`/`groupId`/`artifactId` — it inherits all third-party
  dependencies and plugins from the root. Add new third-party
  dependencies to the root `pom.xml`'s `dependencyManagement`/
  `dependencies` unless they're genuinely module-specific; internal
  module-to-module dependencies belong in the child module's own POM.
  `puerto-rico-model`, `puerto-rico-session`, `puerto-rico-lobby`, and
  `puerto-rico-ai` each add only TestNG and a surefire provider of their
  own (see below); everything else they inherit. `puerto-rico-web` uses
  JUnit 5 instead (see below) and adds `spring-boot-starter-webmvc-test`
  and `spring-boot-starter-validation`.
  See [docs/architecture.md](docs/architecture.md) for what each module
  is responsible for and how they connect.
- Spring Boot 4.1.0 — `puerto-rico-web` is the `@SpringBootApplication`
  module, built as an executable jar via `spring-boot-maven-plugin`
  (`repackage`). **Runs on Jackson 3** (`tools.jackson.*`), not the classic
  Jackson 2 (`com.fasterxml.jackson.databind`) — `spring-boot-starter-web`
  pulls in `tools.jackson.core:jackson-databind:3.x`, and `jackson-annotations`
  is the one piece that keeps its `com.fasterxml.jackson.annotation` package
  across both major versions. Anything that hand-imports an `ObjectMapper`
  or a Jackson `Module` needs the `tools.jackson.*` variant and, for
  `JsonNullable` support, `JsonNullableJackson3Module` specifically — not
  the plain `JsonNullableModule`, which targets Jackson 2 and won't
  register against a Jackson 3 `ObjectMapper`. MockMvc/`@AutoConfigureMockMvc`
  also moved packages in Spring Boot 4 (now
  `org.springframework.boot.webmvc.test.autoconfigure`) and needs its own
  starter (`spring-boot-starter-webmvc-test`) — it's no longer bundled into
  `spring-boot-starter-test`.
- The wire contract between `puerto-rico-web` and `puerto-rico-frontend` is
  generated, not hand-written: `puerto-rico-contract` holds one OpenAPI
  spec, `openapi-generator-maven-plugin` (generator `spring`) produces the
  Java interfaces/DTOs, `openapi-typescript` produces the TS types. Never
  hand-edit generated output (`puerto-rico-contract`'s
  `target/generated-sources`, or `puerto-rico-frontend/src/api/schema.d.ts`)
  — edit `puerto-rico-contract/src/main/resources/openapi/puerto-rico.yaml`
  instead. See that module's README for why `PlayerAction`/`SessionEvent`
  use `allOf` + `discriminator` rather than `oneOf`.
- `puerto-rico-frontend`: React + TypeScript + Vite, built via
  `frontend-maven-plugin` (installs its own isolated Node under
  `~/.cache/frontend-maven-plugin-node`, so no system Node is required for
  `./mvnw verify`; the devcontainer image pre-warms this path at build
  time — see Devcontainer section). Vitest + React
  Testing Library for unit/component tests; Playwright for functional tests
  against the real packaged app (not run by `./mvnw verify` — see its own
  CI job and README).
- Lombok, on the compiler's annotation processor path and on the compile
  classpath at `provided` scope so the annotations are importable
- google-java-format via `fmt-maven-plugin`, bound to `process-sources` —
  runs automatically on every build
- TestNG for `puerto-rico-model`, `puerto-rico-session`, `puerto-rico-lobby`,
  and `puerto-rico-ai`'s tests. JUnit 5 also reaches every module via the
  inherited `spring-boot-starter-test`, and surefire runs exactly one
  provider, so each of those modules names the TestNG provider explicitly
  in its own `pom.xml` rather than leaving it to auto-detection. Surefire's
  version is pinned in the root `pluginManagement` — importing the Spring
  Boot BOM brings `dependencyManagement` only, so it would otherwise fall
  back to Maven's default. `puerto-rico-web` is the one exception: JUnit 5 +
  `@SpringBootTest`/MockMvc are Spring Boot's own idiomatic test tools, so
  fighting that with TestNG isn't worth it there — see that module's README.

Package convention: `com.PRS.model.*` for the rules engine (e.g.
`com.PRS.model.boards`, `.buildings`, `.goods`, `.rolecards`, `.game`,
`.actions`, `.engine`, `.scoring`), `com.PRS.session.*` for the orchestrator
(`com.PRS.session`, `.actors`, `.view`, `.events`), `com.PRS.lobby` (flat) for
the lobby, `com.PRS.ai` (flat) for AI engines, and `com.PRS.web.*` for the
backend (`com.PRS.web`, `.api`, `.actors`, `.events`, `.wire`). Generated
Java lives under `com.PRS.contract.*` (`.api`, `.model`) — never hand-edited,
never a place to add code. See each module's README for what its packages
hold and the design decisions behind them.

## Commands

Use the bundled wrapper (`./mvnw`) so the Maven version always matches what
CI and the devcontainer use — don't rely on a bare `mvn` unless the wrapper
is unavailable. The wrapper pins Maven **3.9.16**, matching the version
installed in `.devcontainer/Dockerfile`.

```bash
./mvnw verify                                # full reactor build + tests (matches CI), incl. frontend Vitest
./mvnw clean install                         # build and install all modules to ~/.m2
./mvnw -pl puerto-rico-model verify           # build/test just the model module
./mvnw -pl puerto-rico-model test -Dtest=FooTest             # single test class
./mvnw -pl puerto-rico-model test -Dtest=FooTest#barMethod   # single test method
./mvnw fmt:format                            # run the formatter explicitly (rarely needed, see below)
./mvnw -pl puerto-rico-web spring-boot:run    # run the app locally: http://localhost:8080

cd puerto-rico-frontend
npm run dev            # Vite dev server, proxies /api to localhost:8080
npm run test:unit      # Vitest + React Testing Library
npm run typecheck
../mvnw -pl puerto-rico-web -am package -DskipTests && npm run test:e2e   # Playwright, needs the real jar built first
```

Run `./mvnw verify` before considering any code task complete. `./mvnw
verify` does **not** run Playwright — see CI & dependency updates below.

## CI & dependency updates

- `.github/workflows/ci.yml` has two jobs. `build` runs on every push/PR to
  `main`: checkout → `setup-java` (temurin 25, maven cache) →
  `./mvnw -B verify` — the exact command local `./mvnw verify` is meant to
  match, and includes the frontend's Vitest suite via
  `frontend-maven-plugin`. `e2e` packages the real app
  (`./mvnw -B -DskipTests package`), then runs Playwright against it —
  kept as a separate job because it needs Node + browser binaries and
  boots a real server, unlike everything `build` covers.
- `.github/dependabot.yml` opens weekly update PRs for the `maven`,
  `github-actions`, and `npm` (in `puerto-rico-frontend`) ecosystems
  automatically — unprompted dependency-bump PRs are expected automation,
  not something to action manually unless asked.

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
  structure. `puerto-rico-model`, `puerto-rico-session`, `puerto-rico-lobby`,
  and `puerto-rico-ai` use TestNG (`@Test`, `@DataProvider`) with AssertJ
  assertions; AssertJ and Mockito arrive transitively via
  `spring-boot-starter-test`. Rules tables belong in a `@DataProvider` so
  each row is asserted individually. `puerto-rico-web` uses JUnit 5
  (`@Test`, `@ParameterizedTest`) instead — `@SpringBootTest`/MockMvc are
  Spring's own idiomatic test tooling and there's no benefit to fighting
  that with TestNG.
- `puerto-rico-frontend` tests live in `src/**/*.test.ts(x)` next to the
  code they cover (Vitest convention, not `src/test/java`'s mirrored-tree
  layout) plus `e2e/*.spec.ts` for Playwright. Every element a test
  locates gets a stable `data-testid`; interactive/status elements also
  get real ARIA roles, so the same locators work for Playwright and for a
  screen reader — this is designed in from the start, not retrofitted
  after the fact.

## Devcontainer

- Base image `eclipse-temurin:25-jdk-noble`; Maven and Node (22 LTS) are
  both installed manually in the Dockerfile (not via apt) — Maven to avoid
  pulling in a second, conflicting JDK; Node so `puerto-rico-frontend`'s
  day-to-day `npm run dev`/`test:unit` work without going through Maven.
  `./mvnw verify` itself doesn't depend on this system Node —
  `frontend-maven-plugin` installs its own isolated copy under
  `~/.cache/frontend-maven-plugin-node`. The Dockerfile pre-warms that
  exact path at image-build time (running the plugin's
  `install-node-and-npm` goal as the `ubuntu` user against the two
  `pom.xml` files it needs — see the build context note in
  `devcontainer.json` and `../.dockerignore`), so a fresh container
  already has it before the first real `./mvnw verify` runs.
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
- Likewise the ESLint VS Code extension (`dbaeumer.vscode-eslint`) is
  installed, but `puerto-rico-frontend` has no ESLint config
  (`eslint.config.js`) yet — TypeScript's own `strict` compiler settings
  are what currently catches issues. Confirm before adding an ESLint
  config or assuming lint rules are enforced anywhere.
