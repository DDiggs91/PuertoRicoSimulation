# TODO — Remaining Work

A forward-looking roadmap of features and capabilities that are **not yet
built**, as distinct from [REVIEW.md](REVIEW.md), which audits what *is* built
for bugs, dead code, and gaps against its own documented scope. Where an item
here overlaps a REVIEW.md finding, that's noted rather than duplicated.

Sections are priority order. Within a section, roughly implementation order.

---

## 1. Human play — the click-to-move UI (P0)

The single largest gap in the project. Everything server-side is built and
tested (`HumanActor`, seat tokens, `POST /moves`) — see
`puerto-rico-frontend/README.md`'s Status section — but there is no client UI
that lets a seated human actually choose and submit a move. Today, seating a
human produces a seat with no way to act.

- **Per-phase action pickers.** `GET /decision` already returns the exact
  `PlayerAction` list a seat may choose from (`Decision.options`); the UI's
  job is presenting them comprehensibly rather than as raw JSON. Each of the
  eight action families needs its own presentation:
  - Role selection — clickable role cards showing accumulated doubloons
  - Settler — clickable face-up tiles, a quarry option, Hacienda take/skip, pass
  - Mayor — drag/click colonists from San Juan onto empty circles, end-placement
  - Builder — a buildable-buildings list showing discounted cost, pass
  - Craftsman bonus — pick a produced good, pass
  - Trader — sellable goods with price, pass
  - Captain — loadable ships per good, Wharf option
  - Captain storage — warehouse-kind and single-barrel picker
- **Submit flow.** `POST /games/{id}/moves` with `X-Seat-Token` and the chosen
  action's `requestId`; handle the 202 (offer accepted, wait for the event
  stream) versus 400/403/404 (`ApiError`) paths already modeled in
  `api/client.ts`.
- **"It's your turn" affordance.** The board already knows the acting seat
  (`phase.actorSeat`) — surface that distinctly for the seat the client holds
  a token for, not just in the existing spectator-facing phase line.
- **Seat token persistence.** Confirmed absent: no `localStorage`/
  `sessionStorage` use anywhere in `puerto-rico-frontend/src`. A human's seat
  token exists only in React state, so a page reload mid-game strands the
  player with no way to re-authenticate as their seat (the server-side
  `SeatTokens` entry is still valid — the client just no longer has it).
  Persist `{gameId, seat, token}` client-side keyed by game id, restored on
  the `?game=` bootstrap path in `App.tsx`.
- **Legal-options-only enforcement in the UI**, not just the server. `HumanActor.offer`
  already rejects an action not in `pending.options()` — the picker should
  only ever construct options from that same list, so a rejection is
  unreachable from normal use rather than a race the user can trigger.

## 2. AI skill levels & a training module (P0)

Only one AI exists today: `RandomAi`, which samples uniformly from
`GameEngine.legalActions`. `puerto-rico-ai/README.md` already frames this as
deliberate — *"starting simple … with room to add stronger engines later
without changing anything else in the system"* — and `AiRegistry` is already
built to list multiple engines by id (`available()` returns a `List`, `create`
switches on `engineId`), so adding engines is additive, not a redesign.

Recommended progression, cheapest first:

### 2.1 Heuristic engines (no training required)

- A **greedy** engine: for each legal action, apply it via `GameEngine.apply`
  (the model is a pure function — cheap to try every option) and pick the one
  that maximizes an immediate heuristic (e.g. `Scorer.score(...).total()` plus
  a doubloon/goods-on-hand term, since `Scorer` only values what's already
  locked in as VP chips/buildings). This alone is a meaningfully stronger
  "Medium" opponent and needs nothing beyond `puerto-rico-ai`.
- Register it in `AiRegistry.available()` alongside `random`, with its own
  `AiEngineInfo` id/description, so it's selectable in the lobby the same way
  `random` is today — no lobby or web changes needed beyond that.

### 2.2 Search-based engine ("Hard")

- A bounded-depth **minimax/expectimax or Monte Carlo Tree Search** engine,
  using `GameEngine.legalActions`/`apply` as the simulator and a heuristic
  evaluation function (extending 2.1's) as the leaf value. `GameState` being
  an immutable record tree with no defensive-copy cost
  (`puerto-rico-model/README.md`: *"callers get snapshots, replay, and
  lookahead search for free"*) is exactly what this needs and is already
  designed for it.
- Multiplayer scoring (not zero-sum) needs a search variant that handles
  N-player payoffs — paranoid/maxn search or MCTS with per-player reward are
  the standard choices; pick one and document the choice in a new
  `puerto-rico-ai` design note.
- Think-time budgeting: reuse the existing `thinkTime`-via-`Executor` pattern
  from `RandomAi` so a bounded search fits inside the same pacing mechanism
  rather than blocking the session thread.

### 2.3 Trained/learned engine and a training module

This is the biggest lift and the one genuinely new piece of infrastructure:

- **New module** (e.g. `puerto-rico-training`), offline tooling — not part of
  the Spring Boot runtime, not built by `./mvnw verify`'s default reactor path
  unless it's fast, similar in spirit to how Playwright is carved out of the
  main build today.
- **Self-play harness.** The pattern already exists and is proven at scale:
  `SessionRunnerTest`/`GameEngineContractTest` already drive full games
  synchronously with `Duration.ZERO` actors. A training harness is the same
  shape, run thousands of times, varying the AI's parameters between runs.
- **Parameter representation.** Start with a weighted linear evaluation
  function (interpretable, small, fast to evaluate — feeds directly into
  2.2's search) tuned via self-play plus a simple optimizer (hill-climbing,
  a genetic algorithm, or CMA-ES over the weight vector). A neural
  net/RL approach is a plausible "someday" upgrade but is a much larger
  undertaking (training infrastructure, GPU/tooling dependencies that don't
  fit this project's all-Java, no-external-services shape) — don't start
  there.
- **Persistence for trained parameters.** The project's current scoping
  decision is explicitly in-memory-only (`docs/architecture.md`: *"Persistence
  is in-memory only … explicitly deferred"*), but a trained engine's weights
  have to live *somewhere* durable to be loaded at runtime. This is a narrow,
  new exception to that decision, not a reversal of it: a versioned
  weights file (JSON/properties) checked into the repo or built as a
  resource, loaded once at `AiRegistry` construction — no database, no
  request-time persistence.
- **Evaluation/benchmarking tooling.** A CLI or test-only harness that plays N
  games between two engine versions and reports win rate, so "is the new
  training run actually better" has an answer before it ships as a lobby
  option.
- **Surface skill levels in the lobby UI.** Once `AiRegistry.available()`
  lists more than one engine, `LobbyScreen`'s "Seat a random AI" button needs
  to become an engine picker (dropdown/radio over `available()`'s
  `displayName`/`description`) instead of hardcoding `engineId: "random"`
  (`LobbyScreen.tsx:51`).

Update `puerto-rico-ai/README.md` and `docs/architecture.md`'s AI Engine
Plugins section once new engines land — both already anticipate this
happening, so it's an addition, not a rewrite.

## 3. Frontend completeness beyond move-submission

Already itemized with file/line detail in REVIEW.md — listed here only so
the roadmap is a complete picture in one read:

- Central board rendering (role track, ships, face-up tiles, trading-house
  contents) — [REVIEW.md §5.4](REVIEW.md#54-gap-the-spectator-board-renders-counts-not-a-board)
- Spectating a game from the lobby list, not just via a shared URL —
  [REVIEW.md §5.5](REVIEW.md#55-gap-you-cannot-spectate-a-game-from-the-lobby-list)
- SSE reconnect/error handling and a bounded event log —
  [REVIEW.md §5.6](REVIEW.md#56-gap-no-sse-error-handling-or-reconnect-the-event-array-grows-unbounded)
- Any styling at all — [REVIEW.md §5.7](REVIEW.md#57-gap-the-application-has-no-styling-whatsoever)

New, once move-submission exists: the per-phase action pickers from
[§1](#1-human-play--the-click-to-move-ui-p0) need the same `data-testid`/ARIA
rigor the spectator components already follow, plus Playwright coverage for
at least one full human-played turn per phase family.

## 4. Reliability & production-readiness (explicitly deferred scoping decisions)

These are named "out of scope" in `docs/architecture.md` and
`puerto-rico-session/README.md` today — real limitations, not oversights —
listed here as the roadmap for if/when the project needs to run for real,
unattended users rather than a demo:

- **Per-decision timeout for a human seat.** Confirmed: no timeout mechanism
  exists anywhere in `puerto-rico-session`/`puerto-rico-web` today — a human
  actor with a pending decision blocks that seat (and, once colonists/goods
  depend on turn order, arguably the table) indefinitely if the player
  never answers. `Decision.requestId` is explicitly the hook designed to make
  this safe to add later (`puerto-rico-session/README.md`: *"the hook that
  makes adding a timeout safe later without changing the contract"*) — an
  auto-pass or auto-play-random fallback after N seconds is the natural
  first version.
- **Disconnect/reconnect handling, pause/resume.** Explicitly deferred
  alongside the timeout item above, same README. Ties into the seat-token
  persistence in [§1](#1-human-play--the-click-to-move-ui-p0) — reconnect is
  only meaningful once the client can re-identify itself as a seat after
  losing in-memory state.
- **Durable persistence** (resume a game after a server restart, replay,
  history). Explicitly out of scope per `docs/architecture.md`'s scoping
  decisions. A real deployment loses every in-progress game on redeploy;
  worth a deliberate decision (and likely a lightweight embedded store, not a
  new external service, to keep the single-process deployment target from
  `docs/architecture.md` §"Deployment target") rather than continuing to defer
  it indefinitely.
- **Lobby/session/token eviction.** REVIEW.md already flags the concrete bugs
  ([§3.2](REVIEW.md#32-bug-lobbytables-grows-without-bound),
  [§3.3](REVIEW.md#33-bug-seattokens-grows-without-bound),
  [§4.2](REVIEW.md#42-bug-sse-streams-are-never-closed-and-their-subscriber-lists-never-evicted)),
  but the underlying gap — nothing ever reclaims a finished or abandoned
  table — is also a roadmap item for running this unattended: without it, an
  always-on deployment eventually exhausts memory purely from lobby history.

## 5. Explicitly out of scope (naming these so nobody assumes they're planned)

Per `CLAUDE.md` and `docs/architecture.md`'s stated scoping decisions, not
currently on any roadmap:

- A 2-player variant — the base game is 3–5 players only, and there is no
  official 2-player ruleset to implement against.
- Expansions (the game supports base-game rules only).
- An out-of-process/networked AI protocol — AI integration is deliberately
  in-process Java plugins in the same JVM (`docs/architecture.md`'s scoping
  decisions).
- A distributed/multi-process deployment — one Spring Boot process serving
  its own bundled frontend build is the stated deployment target.

If any of these become real requirements, they're substantial enough to
warrant their own design pass rather than an incremental addition to the
current architecture.

---

## Suggested sequencing

1. **§1 (human play)** and **§2.1 (greedy AI)** in parallel — independent,
   both unblock real gameplay, and §2.1 gives the human someone worth
   playing against.
2. **§3** (frontend completeness) alongside §1, since both touch the same
   components and are easiest to land together.
3. **§2.2 → §2.3** (search, then training) once §2.1 proves the extensibility
   path end-to-end.
4. **§4** once the project has real users to protect against — timeouts and
   eviction matter most exactly when someone leaves a tab open or a game
   unattended.
5. **§5** only if a concrete need arises; treat this section as "decide before
   building," not a backlog.
