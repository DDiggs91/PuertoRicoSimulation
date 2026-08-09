# puerto-rico-lobby — Lobby / Matchmaking Manager

See [docs/architecture.md](../docs/architecture.md) for how this module fits
into the whole system.

## Intent

Tracks, in memory, the set of joinable and in-progress games. Handles
creating a game, seating 1-4 human players and/or AI engines into open
seats, and starting a game once seated. This is the entry point before a
Game Session exists — nothing in `puerto-rico-session` gets created until
the Lobby decides a game is ready to start.

Per the project's current scoping decisions, this is **in-memory only**:
no durable storage of lobby or game state. A restart loses whatever games
were open. A persistence layer is explicitly deferred, not designed for
here.

## Contracts

Exposes the **Lobby contract** to `puerto-rico-web`: create/list/join a
game, seat a human or an AI engine into a seat. Creates and owns Game
Session instances (via `puerto-rico-session`) once a game starts.

## Depends on

`puerto-rico-session` (and transitively `puerto-rico-model`).

## Status

Scaffolding only. No classes exist yet.
