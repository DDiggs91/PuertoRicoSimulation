# CLAUDE.md

Guidance for Claude Code (and other AI-assisted development) in this repo.

## Project

A software simulation of the board game *Puerto Rico*, with an eventual
goal of AI opponents at varying skill levels. Currently early scaffolding:
build tooling and devcontainer are set up, but no game logic or model code
exists yet — don't assume any model/board/role classes exist until you
find them.

Git history prior to commit `9afca52` reflects abandoned early work and
should be disregarded — treat this repository's current state as the
effective starting point rather than reconstructing intent from old
commits.

For Puerto Rico's rules, see [docs/game-rules.md](docs/game-rules.md) —
treat it as a strong starting reference, not ground truth (it carries its
own accuracy caveat).

## Tech stack

- Java 25 (Eclipse Temurin)
- Maven, multi-module: root `puerto-rico-parent` (packaging `pom`) →
  `puerto-rico-model` (packaging `jar`)
- Spring Boot 4.1.0 — currently BOM/starters only (`spring-boot-starter-web`,
  `spring-boot-starter-test`); no `@SpringBootApplication` class or
  controller exists yet, don't assume a running web server
- Lombok, wired into the compiler's annotation processor path
- google-java-format via `fmt-maven-plugin`, bound to `process-sources` —
  runs automatically on every build

Package convention (established, not yet populated): `com.PRS.model.*`,
e.g. `com.PRS.model.boards`, `.buildings`, `.goods`, `.rolecards`. Treat
this as a naming convention to continue, not an architecture mandate —
actual class design is a separate, not-yet-made decision.

## Commands

Use the bundled wrapper (`./mvnw`) so the Maven version always matches what
CI and the devcontainer use — don't rely on a bare `mvn` unless the wrapper
is unavailable.

```bash
./mvnw verify                                # full reactor build + tests (matches CI)
./mvnw clean install                         # build and install all modules to ~/.m2
./mvnw -pl puerto-rico-model verify           # build/test just the model module
./mvnw -pl puerto-rico-model test -Dtest=FooTest             # single test class
./mvnw -pl puerto-rico-model test -Dtest=FooTest#barMethod   # single test method
./mvnw fmt:format                            # run the formatter explicitly (rarely needed, see below)
```

Run `./mvnw verify` before considering any code task complete.

## Code style

- Formatting is automatic: `fmt-maven-plugin`'s `format` goal runs on every
  build and rewrites files to match google-java-format. This is
  intentional — it auto-fixes rather than failing the build on violations,
  and CI (`./mvnw verify`) relies on that same auto-fix behavior rather
  than a check-and-fail step. Don't hand-format Java code, and don't
  propose switching this to a check/fail mode — it's a deliberate,
  settled choice.
- Prefer Lombok annotations (`@Getter`, `@RequiredArgsConstructor`,
  `@Builder`, etc.) over hand-written boilerplate once model classes exist.
- Tests live under `src/test/java`, mirroring the `src/main/java` package
  structure. JUnit 5, AssertJ, and Mockito are available transitively via
  `spring-boot-starter-test`.

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
- Spring Boot's eventual role (backing a REST API / UI, vs. just DI
  convenience) is undecided — don't assume an application entry point or
  controller layer should exist.
