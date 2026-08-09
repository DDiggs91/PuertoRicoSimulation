# Architecture Outline

> This is a component-level outline, not an implementation design. It names
> the parts the system will need, what each is responsible for, and which
> contracts connect them — it does not define interfaces, classes, or
> message shapes. Those are separate, later decisions.

## Target capabilities

The architecture below is scoped to support:

1. Hosting multiple AI engines playing against each other, spectatable in a
   web browser.
2. 1-4 human players connecting via a web browser and playing each other.
3. Humans and AI playing together in the same browser-hosted game.

## Scoping decisions

Two decisions were made up front, since they determine which parts are
needed at all:

- **AI integration is in-process.** AI opponents are Java plugins running
  in the same JVM as the game engine, not separate processes/services
  talking over a network protocol. A networked/out-of-process bot protocol
  is explicitly out of scope for now.
- **Persistence is in-memory only.** Game state lives in server memory for
  the lifetime of the process. Durable storage (resume after restart,
  replays, history) is explicitly deferred, not designed for here.

## Components

### 1. Game Engine (Rules Core)

Maps to the existing `puerto-rico-model` module. Owns game state, turn and
phase sequencing, role-card resolution, scoring, and end-condition
detection. Pure domain logic — no I/O, no awareness of the web layer, and
no concept of "human" vs. "AI." It's the single source of truth for
whether a move is legal and what happens as a result.

### 2. AI Engine Plugins

One implementation per AI opponent, starting simple (e.g. random or
greedy heuristic) with room to add stronger engines later. Each plugin
fulfills the same decision-making role, so the rest of the system treats
every AI interchangeably regardless of its internal sophistication.
Responsible only for producing a choice when asked — no game-state
mutation, no I/O. Registered somewhere discoverable so a specific engine
(by name/skill level) can be picked when seating a game.

### 3. Human Actor Adapter

Lets a human player act like any other decision-maker from the Game
Session's point of view. Bridges the asynchronous, request/response
nature of a browser connection (wait for a move to arrive over the
network) into the same decision-making role the AI plugins fulfill, so
the orchestration layer never has to special-case humans vs. AI.

### 4. Game Session (Orchestrator)

Manages one running game's lifecycle: which actors (human or AI) are
seated, whose turn it is, routing decision requests to the right actor,
applying the result to the Game Engine, and broadcasting the resulting
state to anyone watching. This is the bridge between the pure rules
engine and the outside world, and the piece that makes mixed human/AI
seating possible.

### 5. Lobby / Matchmaking Manager

Tracks, in memory, the set of joinable and in-progress games. Handles
creating a game, seating 1-4 human players and/or AI engines into open
seats, and starting a game once seated. The entry point before a Game
Session exists.

### 6. Web/API Layer (Spring Boot backend)

The only component that talks to browsers. Translates HTTP requests into
Lobby actions, and a real-time channel (e.g. WebSocket) into in-game
moves. Pushes game state/event updates back out to connected clients,
whether they're seated players or spectators.

### 7. Web Frontend (browser client)

Renders the player board, central board, role selection, lobby/join
screens, and a spectator view. The same frontend serves both "I am
playing" and "I am only watching AIs play" — the two differ only in
whether the client is allowed to submit a move.

## Contracts

Named here to show how components connect; none are designed yet.

| Contract | Between | Purpose |
|---|---|---|
| Command/Query contract | Game Session ↔ Game Engine | Submit a player action; get back the updated state or a rejection. |
| Decision contract | Game Session ↔ Human Actor Adapter / AI Engine Plugin | "Given this state and these legal options, what do you choose?" — same shape whether a human or AI answers. |
| Event/broadcast contract | Game Session → Web/API Layer | Notifies of state changes so they can be pushed to every connected client. |
| Lobby contract | Web/API Layer ↔ Lobby Manager | Create/list/join a game; seat a human or an AI engine into a seat. |
| Wire contract | Web/API Layer ↔ Web Frontend | The browser-facing protocol: request/response for lobby actions, a real-time channel for live play and spectating. |

## Deployment target

Spring Boot runs the app. The shipped artifact is a single small Docker
image running one Spring Boot process — the frontend's production build
output is bundled into that process's static resources rather than
deployed as a separate service. There is one runtime component, not a
constellation of them; the module split below is a source-organization
decision, not a deployment topology.

## Module mapping

The components above are broken out into several Maven modules so the
work can proceed independently per module, and so `./mvnw verify` remains
the single command that validates the whole repo. Each module has its own
README with the same accomplishes/contracts framing as above:

| Component | Module | Depends on |
|---|---|---|
| Game Engine (Rules Core) | [puerto-rico-model](../puerto-rico-model/README.md) | — |
| Game Session (Orchestrator) | [puerto-rico-session](../puerto-rico-session/README.md) | puerto-rico-model |
| AI Engine Plugins | [puerto-rico-ai](../puerto-rico-ai/README.md) | puerto-rico-model, puerto-rico-session |
| Lobby / Matchmaking Manager | [puerto-rico-lobby](../puerto-rico-lobby/README.md) | puerto-rico-session |
| Web Frontend | [frontend](../frontend/README.md) (`puerto-rico-frontend`) | — |
| Web/API Layer + Human Actor Adapter | [puerto-rico-web](../puerto-rico-web/README.md) | puerto-rico-lobby, puerto-rico-ai, puerto-rico-frontend |

The Human Actor Adapter component is folded into `puerto-rico-web` rather
than given its own module: it has no existence independent of a browser
connection, so it lives with the layer that owns that connection. The
Actor/Decision contract it implements is still defined in
`puerto-rico-session`, alongside the AI plugins that implement the same
contract.

`puerto-rico-frontend` is a Maven module even though its actual content is
TypeScript/JS, not Java: Maven wraps the frontend toolchain (e.g. a
plugin that runs a linter/type-check over the TypeScript) so the module
builds as part of the same reactor, and — as an ordinary Maven artifact —
can be depended on directly by `puerto-rico-web` rather than glued in
through an ad hoc copy step.

## Open questions (not decided here)

- **Frontend framework.** Repo placement and deployment shape are now
  settled (lives in this repo as `/frontend`, bundled into the
  `puerto-rico-web` Docker image), but the specific framework/build
  tooling is still undecided.
- **Player/spectator permission boundary.** Who can submit a move vs.
  only observe on the wire contract is a real design concern, left for
  when that contract is actually designed.

Persistence and a networked/out-of-process AI protocol remain deliberately
out of scope per the scoping decisions above — not open questions, just
not being designed for right now.
