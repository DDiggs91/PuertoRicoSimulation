# puerto-rico-frontend — Web Frontend (browser client)

See [docs/architecture.md](../docs/architecture.md) for how this module fits
into the whole system.

## Intent

Renders the player board, central board, role selection, lobby/join
screens, and a spectator view. The same client serves both "I am playing"
and "I am only watching AIs play" — the two differ only in whether the
client is allowed to submit a move.

## Contracts

Speaks the **Wire contract** exposed by `puerto-rico-web`: request/response
for lobby actions, plus a real-time channel for live game play and
spectating.

## Maven module rationale

The browser code itself is TypeScript/JS, not Java — but the module still
participates in the Maven reactor rather than living outside it. Even
before there's a real app to build, Maven can wrap the frontend toolchain
(e.g. via `frontend-maven-plugin` or `exec-maven-plugin` to install
Node/npm and run a TypeScript linter/type-check) so `./mvnw verify` stays
the one command that validates the whole repo, frontend included, and CI
doesn't need a second toolchain bolted on.

Being a Maven artifact (`com.PRS:puerto-rico-frontend`) also means it can
be depended on like any other module: `puerto-rico-web` declares a
dependency on it (see [../puerto-rico-web/pom.xml](../puerto-rico-web/pom.xml))
so the frontend's production build output can be pulled onto the classpath
and served as static content, rather than the two being wired together
through some ad hoc copy step.

## Depends on

Nothing else in this reactor.

## Status

Scaffolding only: a bare `pom.xml`, no Node/npm project, no TypeScript
source, and no linter configured yet. Framework choice (React, Vue, plain
TS, or otherwise), the npm toolchain, and the Maven plugin that wires it
into the build are all left for whoever picks this module up.
