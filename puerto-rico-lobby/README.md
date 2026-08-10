# puerto-rico-lobby — Lobby / Matchmaking Manager

See [docs/architecture.md](../docs/architecture.md) for how this module fits
into the whole system.

## Intent

Tracks, in memory, the set of joinable and in-progress games. Handles
creating a game, seating human or AI actors into open seats, and starting a
game once seated. This is the entry point before a Game Session exists —
nothing in `puerto-rico-session` gets created until a table here is seated
and started.

Per the project's current scoping decisions, this is **in-memory only**:
no durable storage of lobby or game state. A restart loses whatever games
were open. A persistence layer is explicitly deferred, not designed for
here.

## Contracts

Exposes the **Lobby contract** to `puerto-rico-web`, implemented by the
`Lobby` class:

```java
GameId createGame();
JoinOutcome join(GameId id, Actor actor, ActorKind kind);
StartOutcome start(GameId id, long seed);
StartOutcome start(GameId id, long seed, List<SessionListener> listeners);
List<GameTableSummary> listGames();
Optional<GameTableSummary> find(GameId id);
Optional<GameSession> sessionFor(GameId id);   // present once started
void close();                                  // AutoCloseable
```

The listener-taking overload exists because it's the *only* point at which
a listener can be attached before a game starts: an all-AI game can finish
before the two-argument `start` even returns, so anything registered
afterward may see nothing. `puerto-rico-web`'s `GameEventStream` uses it to
attach its SSE fan-out listener before `GameSession.start()` fires its
first event.

Consumes `puerto-rico-session`'s `GameSession`/`SessionRunner`/`Actor`
surface directly to create and drive a game once a table is ready.

## Depends on

`puerto-rico-session` (and transitively `puerto-rico-model`).

## Two design decisions

**The lobby seats pre-built `Actor`s — it never constructs one itself.**
`join` takes an already-built `Actor` and an `ActorKind`; the lobby stays
agnostic about whether that actor is an AI plugin or a human adapter, and
stays exactly as small as its job description: seating bookkeeping, nothing
about *answering* decisions. That matches
[docs/architecture.md](../docs/architecture.md)'s description of
`puerto-rico-web` as the component that "implements the Decision contract
(via the Human Actor Adapter)... and calls into `puerto-rico-ai`" — web
builds actors, lobby just seats whatever it's handed. The alternative (the
lobby owning a built-in placeholder actor for humans) would duplicate
machinery the Human Actor Adapter is explicitly scoped to own; architecture.md
is explicit that the adapter "has no existence independent of a browser
session," so it doesn't belong here.

**`start` builds the `GameSession` *and* spins its `SessionRunner`.** A table
that's "owned" but not actually being driven forward isn't a meaningfully
started game, so `GameTable.start` calls `GameSession.create` →
`session.start()` → `SessionRunner.drive(...)` in one step, and holds onto the
runner so `Lobby.close()` can shut every in-flight runner down cleanly —
mirroring `SessionRunner`'s own `AutoCloseable` shape one level up.

## Packages

Flat under `com.PRS.lobby` — the module's surface is small enough that
session's topic-subpackage split (`actors`/`view`/`events`) isn't warranted
here.

| Type | Holds |
|---|---|
| `Lobby` | The public registry over tables: create, join, start, list, find, close |
| `GameTable` | Package-private: one table's seats and, once started, its `GameSession`/`SessionRunner` |
| `GameId`, `GameTableStatus`, `SeatSummary`, `GameTableSummary` | Value types for tracking and listing tables |
| `LobbyRejectionReason`, `JoinOutcome`, `StartOutcome` | Rejection as a value — a separate enum from the model's `RejectionReason`, since this rejects table admission, not an in-game action |

## Design notes

**Seat admission never throws for caller-triggerable input.** An unknown
game id, a full table (5 seats), too few seats to start (below 3), or acting
on an already-started table are all returned as `Rejected(...)` values,
following the "rejection is a value" convention that now runs through the
model's `RejectionReason`, the session's `SubmitOutcome`, and this module's
`JoinOutcome`/`StartOutcome`. `SetupTable.MIN_PLAYERS`/`MAX_PLAYERS` from
`puerto-rico-model` are the source of truth for the 3–5 range, so the lobby's
admission checks exist to give a friendly rejection before ever reaching
`GameConfig`'s constructor, not to duplicate its validation.

**Concurrency mirrors `GameSession`.** `Lobby` holds a
`ConcurrentHashMap<GameId, GameTable>`; each `GameTable`'s own `synchronized`
`join`/`start` methods serialize activity on *that* table, so concurrent
activity on different tables never contends, and `summary()` is a lock-free
read of an immutable snapshot.

**Explicitly deferred, not designed for here**, consistent with the
project's in-memory scoping decision: leaving a seat before start (no
README/architecture support for it, and seat-index renumbering semantics
aren't obviously right — better left unbuilt than half-specified),
spectator-only joins, and any notion of listing/resuming a table after
process restart.

## Testing

TestNG with AssertJ assertions. `StubActors` (test-only) supplies actor
stand-ins — `named` (the simplest possible actor) and `deferred` (a
controllable stand-in for a human, answered by test code rather than a
network), mirroring `puerto-rico-session`'s `FakeActors`.

```bash
./mvnw -pl puerto-rico-lobby -am verify
./mvnw -pl puerto-rico-lobby test -Dtest=LobbySessionLifecycleTest
```

`LobbySessionLifecycleTest` drives a full 3-seat all-AI game to `FINISHED`
purely through `Lobby`'s public API — `createGame` → `join` → `start` — to
confirm the `SessionRunner` the lobby spins up on `start` actually makes the
game progress, not just that a `GameSession` object gets created.
