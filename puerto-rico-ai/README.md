# puerto-rico-ai — AI Engine Plugins

See [docs/architecture.md](../docs/architecture.md) for how this module fits
into the whole system.

## Intent

Houses the individual AI opponent implementations — starting simple (e.g.
random or greedy heuristic play), with room to add stronger engines later
without changing anything else in the system. Each engine fulfills the
same decision-making role, so the rest of the system treats every AI
interchangeably regardless of its internal sophistication. An engine's
only job is to produce a choice when asked — no game-state mutation, no
I/O.

AI integration is **in-process**: engines are Java plugins running in the
same JVM as everything else, not separate services talking over a network
protocol. That's a deliberate scoping decision, not a placeholder.

## Contracts

Implements the **Decision contract** defined by `puerto-rico-session`.
Reads game state and legal-option types from `puerto-rico-model`.

Engines need to be discoverable by name/skill level so a specific one can
be picked when seating a game (e.g. from `puerto-rico-lobby`) — the
registration/discovery mechanism itself is left for implementation.

## Depends on

`puerto-rico-model`, `puerto-rico-session`.

## Status

Scaffolding only. No engine implementations exist yet.
