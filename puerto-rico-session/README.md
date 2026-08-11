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

- Consumes the **Command/Query contract** exposed by `puerto-rico-model`:
  `GameSetup.create`, `GameEngine.legalActions`, `GameEngine.apply`.
- Defines the **Decision contract**: `Actor.decide(Decision) ->
  CompletableFuture<PlayerAction>`, implemented by AI plugins in
  `puerto-rico-ai` and by the Human Actor Adapter in `puerto-rico-web`.
- Defines the **Event/broadcast contract**: `SessionListener.onEvent`,
  consumed by `puerto-rico-web` to push state changes to connected clients.

## Depends on

`puerto-rico-model`.

## The central design problem

The model's engine is a pure function; a session is not. Three things
shape this module:

1. **AI answers in nanoseconds, a human in minutes.** `Actor.decide`
   returns a `CompletableFuture<PlayerAction>` so both fit one shape: an AI
   returns `completedFuture(...)`, a human adapter returns an incomplete
   future and completes it once a move arrives over the network.
2. **State mutation must stay serialized** even though HTTP threads will
   read the board concurrently.
3. **An all-AI game completes every future synchronously**, so naively
   chaining `thenCompose` calls would blow the stack partway through a
   game.

The module splits in two to address all three:

- **`GameSession`** — a synchronous, single-object state machine. No
  threads, no futures held internally. `submit` is `synchronized`; every
  other read (`state()`, `viewFor`, `status()`, `pendingDecision()`,
  `standings()`) is a lock-free read of one immutable snapshot, so HTTP
  threads never block on game logic.
- **`SessionRunner`** — a thin driver that asks the actor at the head of
  the queue for a decision and hands the result off with
  `answer.whenCompleteAsync(..., executor)`. That single call is the
  trampoline and the only thing bounding stack depth. `onAnswer` and
  `handleFailure` do call `step()` **directly**; what keeps an all-AI game
  off the call stack is that `step()` ends at `whenCompleteAsync`, which
  queues onto the executor instead of running inline even when the future
  is already complete — so the stack unwinds once per decision. Weakening
  that to `whenComplete`, or "fixing" the direct `step()` calls without
  understanding this, reintroduces the overflow.

## Packages

| Package | Holds |
|---|---|
| `com.PRS.session` | `GameSession`, `SessionRunner`, `SubmitOutcome`, `SessionStatus` |
| `com.PRS.session.actors` | `Actor`, `Decision`, `SeatedActor`, `ActorKind` |
| `com.PRS.session.view` | `GameView` — the redacted state handed to actors and listeners |
| `com.PRS.session.events` | `SessionEvent` (sealed), `SessionListener` |

## Design notes

**Actors never see the model's hidden information.** The base game's only
hidden state is the face-down plantation draw and discard piles, and the
seed that would let a search reconstruct their order. `GameView.of`
scrubs both before a `GameState` is ever handed to an `Actor` or a
`SessionListener` — every player and spectator gets the same view, since
the base game hides nothing on a per-seat basis. `GameSession.state()` is
the one deliberate exception: it returns the raw, unredacted state, for
the lobby and for tests that need to assert on the real board.

**`requestId` guards every submission.** Each `Decision` carries a
monotonic token; `submit` refuses anything that doesn't echo the token on
the currently pending decision as `Stale`. This is what makes a duplicate
socket frame, a double-click, or an answer that arrives after the session
already moved on harmless rather than a state-corrupting race — without
it, retrofitting the guard later would be a breaking change to both
`puerto-rico-ai` and the Human Actor Adapter.

**Rejection is a value here too**, following the model's lead:
`SubmitOutcome` is `Applied | Refused | Stale`, never an exception.
`Refused` reuses the model's `RejectionReason` rather than inventing a
parallel enum — the reasons a move is illegal don't change just because a
session is asking instead of a test.

**A broken actor can't wedge a game forever.** `SessionRunner` retries an
actor whose future fails, returns `null`, or gets `Refused` by the engine,
up to three times, then calls `GameSession.fail(...)` and lets
`completion()` finish exceptionally. This is what stops a buggy AI plugin
from spinning a game (and a CPU core) indefinitely.

**Explicitly out of scope**, matching the project's in-memory scoping
decision: per-decision timeouts, disconnect/reconnect handling,
pause/resume, and persistence. `requestId` is the hook that makes adding
a timeout safe later without changing the contract.

## Testing

TestNG with AssertJ assertions. `FakeActors` (test-only) supplies actor
stand-ins — `firstLegal` (the simplest possible AI), `failing`,
`illegal`, and `deferred` (a controllable stand-in for a human, answered
by test code rather than a network).

```bash
./mvnw -pl puerto-rico-session -am verify
./mvnw -pl puerto-rico-session test -Dtest=SessionRunnerTest
```

`SessionRunnerTest` includes a regression test that drives a full 5-player
all-AI game to completion specifically to catch stack-overflow
regressions in the trampoline — every decision there resolves
synchronously, which is exactly the case that would blow the stack if a
future step were ever chained inline instead of re-posted to the
executor.
