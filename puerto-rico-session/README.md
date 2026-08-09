# puerto-rico-session — Game Session (Orchestrator)

See [docs/architecture.md](../docs/architecture.md) for how this module fits
into the whole system.

## Intent

Manages one running game's lifecycle: which actors (human or AI) are
seated, whose turn it is, routing decision requests to the right actor,
applying the result to the Game Engine, and broadcasting the resulting
state to anyone watching. This is the bridge between the pure rules engine
in `puerto-rico-model` and the outside world, and the piece that makes
mixed human/AI seating possible in the same game.

This module also owns the **Decision contract** itself — the shape that
both an AI engine and a human player must fulfill to participate in a
game. It lives here (rather than in `puerto-rico-model` or in
`puerto-rico-web`) because the Session is the component that asks "given
this state and these legal options, what do you choose?" and needs a
single answer shape regardless of who's answering.

## Contracts

- Consumes the **Command/Query contract** exposed by `puerto-rico-model`.
- Defines the **Decision contract**, implemented by AI plugins in
  `puerto-rico-ai` and by the Human Actor Adapter in `puerto-rico-web`.
- Produces the **Event/broadcast contract** consumed by `puerto-rico-web`,
  so state changes can be pushed out to connected clients (players and
  spectators alike).

## Depends on

`puerto-rico-model`.

## Status

Scaffolding only. No classes exist yet — the Actor/Decision contract
described above is a documented intent, not yet an interface.
