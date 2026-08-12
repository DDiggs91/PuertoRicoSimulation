# Architecture

How the system is put together: what each component is responsible for,
which contracts connect them, and how those map onto Maven modules. Every
component described here is built and tested; each module's own README
carries the design decisions behind it.

## Capabilities

The system supports:

1. Hosting multiple AI engines playing against each other, spectatable in a
   web browser.
2. Human players connecting via a web browser and playing each other.
3. Humans and AI playing together in the same browser-hosted game.

A game seats 3-5 players, any mix of human and AI — the base game's range,
with no 2-player variant. All three are built: a human takes a seat in the
lobby, and the frontend's action panel turns each pending `Decision` into a
per-phase picker, submitting the chosen move through the Human Actor Adapter.
Only one AI engine exists so far, and it plays at random.

## Scoping decisions

Two constraints shape which parts exist at all:

- **AI integration is in-process.** AI opponents are Java plugins running
  in the same JVM as the game engine, not separate processes/services
  talking over a network protocol. A networked/out-of-process bot protocol
  is out of scope.
- **Persistence is in-memory only.** Game state lives in server memory for
  the lifetime of the process. Durable storage (resume after restart,
  replays, history) is deferred. Finished and abandoned tables are reclaimed
  after a retention window rather than kept forever — see
  `puerto-rico-lobby`'s README.

## Components

### 1. Game Engine (Rules Core)

`puerto-rico-model`. Owns game state, turn and phase sequencing, role-card
resolution, scoring, and end-condition detection. Pure domain logic — no
I/O, no awareness of the web layer, and no concept of "human" vs. "AI." It
is the single source of truth for whether a move is legal and what happens
as a result. Its jar has no third-party compile dependencies at all.

### 2. AI Engine Plugins

`puerto-rico-ai`. One implementation per AI opponent; today that is a
random-play engine, with room for stronger ones. Each plugin fulfills the
same decision-making role, so the rest of the system treats every AI
interchangeably. Responsible only for producing a choice when asked — no
game-state mutation, no I/O. Registered in an `AiRegistry` so a specific
engine can be picked by id when seating a game.

### 3. Human Actor Adapter

`HumanActor`, in `puerto-rico-web`. Lets a human player act like any other
decision-maker from the Game Session's point of view, bridging the
asynchronous request/response nature of a browser connection into the same
`Actor` role the AI plugins fulfill, so the orchestration layer never
special-cases humans vs. AI.

### 4. Game Session (Orchestrator)

`puerto-rico-session`. Manages one running game's lifecycle: which actors
are seated, whose turn it is, routing decision requests to the right actor,
applying the result to the Game Engine, and broadcasting the resulting
state to anyone watching. `GameSession` is a synchronous state machine;
`SessionRunner` is what drives it, re-posting each step to a
single-threaded executor so an instantly-answering AI cannot overflow the
stack. Views handed out are scrubbed of hidden information — the face-down
tile piles and every seed that would reproduce them.

### 5. Lobby / Matchmaking Manager

`puerto-rico-lobby`. Tracks, in memory, the set of joinable and in-progress
games. Handles creating a game, seating 3-5 human players and/or AI engines
into open seats, starting a game once seated, and reclaiming tables that
have finished or were never used. The entry point before a Game Session
exists.

### 6. Web/API Layer (Spring Boot backend)

`puerto-rico-web`. The only component that talks to browsers. Translates
HTTP requests into Lobby actions, and HTTP moves into in-game actions.
Pushes game state/event updates back out over Server-Sent Events, whether
the client is a seated player or a spectator, and completes those streams
when a game ends. SSE rather than WebSocket: the broadcast contract is
already one-directional (session → listener), SSE is plain HTTP that
reconnects natively, and moves already have their own request/response
endpoint with no need for a second channel to carry them.

The player/spectator boundary is a per-seat token, minted when a human
seats and required (via an `X-Seat-Token` header) to submit a move for that
seat. Everyone can read every endpoint; only a token holder can act as a
given seat.

### 7. Web Frontend (browser client)

`puerto-rico-frontend`. React + Vite + TypeScript, with Vitest/React
Testing Library for unit and component tests and Playwright for functional
tests against the real packaged app. Renders the lobby/join screen, the
per-player boards (island tiles, buildings, colonists, goods), the central
board (role track, cargo ships, face-up plantation row, trading house,
shared supplies), the event log, and final standings. The same frontend
serves both "I am playing" and "I am only watching AIs play" — the two
differ only in whether the client is allowed to submit a move.

## Contracts

| Contract | Between | Purpose |
|---|---|---|
| Command/Query contract | Game Session ↔ Game Engine | Submit a player action; get back the updated state or a rejection. `GameEngine.apply`/`legalActions`, pure functions over an immutable `GameState`. |
| Decision contract | Game Session ↔ Human Actor Adapter / AI Engine Plugin | "Given this state and these legal options, what do you choose?" — the `Actor`/`Decision` pair, the same shape whether a human or an AI answers. |
| Event/broadcast contract | Game Session → Web/API Layer | `SessionEvent`/`SessionListener`: notifies of state changes so they can be pushed to every connected client. |
| Lobby contract | Web/API Layer ↔ Lobby Manager | Create/list/join a game; seat a human or an AI engine; start; evict. |
| Wire contract | Web/API Layer ↔ Web Frontend | The browser-facing protocol: request/response for lobby actions, an SSE channel for live play and spectating. One OpenAPI spec in `puerto-rico-contract` generates both the Java server interfaces and the TypeScript client types — neither side hand-writes DTOs. |

## Deployment target

Spring Boot runs the app. The shipped artifact is a single small Docker
image running one Spring Boot process — the frontend's production build
output is bundled into that process's static resources rather than
deployed as a separate service. There is one runtime component, not a
constellation of them; the module split below is a source-organization
decision, not a deployment topology.

## Module mapping

The components above are split into Maven modules so work can proceed
independently per module, and so `./mvnw verify` remains the single command
that validates the whole repo. Each module has its own README:

| Component | Module | Depends on |
|---|---|---|
| Game Engine (Rules Core) | [puerto-rico-model](../puerto-rico-model/README.md) | — |
| Game Session (Orchestrator) | [puerto-rico-session](../puerto-rico-session/README.md) | puerto-rico-model |
| AI Engine Plugins | [puerto-rico-ai](../puerto-rico-ai/README.md) | puerto-rico-model, puerto-rico-session |
| Lobby / Matchmaking Manager | [puerto-rico-lobby](../puerto-rico-lobby/README.md) | puerto-rico-session |
| Wire Contract (OpenAPI) | [puerto-rico-contract](../puerto-rico-contract/README.md) | — |
| Web Frontend | [puerto-rico-frontend](../puerto-rico-frontend/README.md) | — (generates TS from puerto-rico-contract's spec at build time) |
| Web/API Layer + Human Actor Adapter | [puerto-rico-web](../puerto-rico-web/README.md) | puerto-rico-lobby, puerto-rico-ai, puerto-rico-contract, puerto-rico-frontend |

The Human Actor Adapter is folded into `puerto-rico-web` rather than given
its own module: it has no existence independent of a browser connection, so
it lives with the layer that owns that connection. The `Actor`/`Decision`
contract it implements is still defined in `puerto-rico-session`, alongside
the AI plugins that implement the same contract.

`puerto-rico-frontend` is a Maven module even though its content is
TypeScript, not Java: `frontend-maven-plugin` runs the npm toolchain
(install, API type generation, Prettier, the production build, and the
Vitest suite) inside the reactor, and the module is an ordinary Maven
artifact that `puerto-rico-web` depends on directly rather than gluing in
through an ad hoc copy step.

Spring Web belongs to the modules that are actually web-facing —
`puerto-rico-web` and `puerto-rico-contract`, whose generated API
interfaces need it to compile. The four domain modules do not inherit it,
which is what keeps `puerto-rico-model`'s "pure domain logic" claim true of
its published jar and not just its source.

## Out of scope

Persistence and a networked/out-of-process AI protocol are excluded by the
scoping decisions above — not open questions, just not being designed for.
