# puerto-rico-web — Web/API Layer + Human Actor Adapter

See [docs/architecture.md](../docs/architecture.md) for how this module fits
into the whole system.

## Intent

The only part of the system that talks to browsers. Translates HTTP
requests into Lobby actions, and a real-time channel (e.g. WebSocket) into
in-game moves; pushes game state/event updates back out to connected
clients — both seated players and spectators.

This module also owns the **Human Actor Adapter**: the piece that turns
"wait for a move to arrive over the network" into an answer to the
Decision contract defined by `puerto-rico-session`, so a human player is
just another actor from the Session's point of view. It lives here rather
than as a standalone module because it's inherently a web-connection
concern — it has no existence independent of a browser session.

## Contracts

- Implements the client-facing **Wire contract** consumed by
  `puerto-rico-frontend`: request/response for lobby actions, a
  real-time channel for live play and spectating.
- Consumes the **Lobby contract** exposed by `puerto-rico-lobby`.
- Consumes the **Event/broadcast contract** produced by `puerto-rico-session`.
- Implements the **Decision contract** (via the Human Actor Adapter)
  defined by `puerto-rico-session`, and calls into `puerto-rico-ai` to
  offer AI opponents when a game is being seated.

## Depends on

`puerto-rico-lobby`, `puerto-rico-ai`, `puerto-rico-frontend`, and
transitively `puerto-rico-session` and `puerto-rico-model`.

## Deployment

This is the Spring Boot application module — the eventual
`@SpringBootApplication` entry point lives here. The target deployable is
a single small Docker image running this Spring Boot app: the frontend's
production build output is pulled in as an ordinary Maven dependency on
`puerto-rico-frontend` and served as static content, so the whole system
ships as one process, one container, with no ad hoc copy step between
the two.

## Status

Scaffolding only. No application class, controllers, or WebSocket
configuration exist yet.
