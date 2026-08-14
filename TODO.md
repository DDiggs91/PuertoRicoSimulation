# TODO — Remaining Work

A forward-looking roadmap of features and capabilities that are **not yet
built**, as distinct from [REVIEW.md](REVIEW.md), which audits what *is* built
for bugs, dead code, and gaps against its own documented scope. Where an item
here overlaps a REVIEW.md finding, that's noted rather than duplicated.

Sections are priority order. Within a section, roughly implementation order.

---

## 1. AI skill levels & a training module (P0)

Only one AI exists today: `RandomAi`, which samples uniformly from
`GameEngine.legalActions`. `puerto-rico-ai/README.md` already frames this as
deliberate — *"starting simple … with room to add stronger engines later
without changing anything else in the system"* — and `AiRegistry` is already
built to list multiple engines by id (`available()` returns a `List`, `create`
switches on `engineId`), so adding engines is additive, not a redesign.

Recommended progression, cheapest first:

### 1.1 Heuristic engines (no training required)

- A **greedy** engine: for each legal action, apply it via `GameEngine.apply`
  (the model is a pure function — cheap to try every option) and pick the one
  that maximizes an immediate heuristic (e.g. `Scorer.score(...).total()` plus
  a doubloon/goods-on-hand term, since `Scorer` only values what's already
  locked in as VP chips/buildings). This alone is a meaningfully stronger
  "Medium" opponent and needs nothing beyond `puerto-rico-ai`.
- Register it in `AiRegistry.available()` alongside `random`, with its own
  `AiEngineInfo` id/description, so it's selectable in the lobby the same way
  `random` is today — no lobby or web changes needed beyond that.

### 1.2 Search-based engine ("Hard")

- A bounded-depth **minimax/expectimax or Monte Carlo Tree Search** engine,
  using `GameEngine.legalActions`/`apply` as the simulator and a heuristic
  evaluation function (extending 1.1's) as the leaf value. `GameState` being
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

### 1.3 Trained/learned engine and a training module

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
  1.2's search) tuned via self-play plus a simple optimizer (hill-climbing,
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
  `displayName`/`description`) instead of hardcoding `engineId: "random"` in
  `addAiSeat`.

Update `puerto-rico-ai/README.md` and `docs/architecture.md`'s AI Engine
Plugins section once new engines land — both already anticipate this
happening, so it's an addition, not a rewrite.

## 2. Frontend polish beyond the current board

The board renders every shared component and every player's island and city,
a seated human can play all eight action families, and the whole thing is
styled. What is left is refinement rather than absence:

- **Drag-and-drop colonist placement.** Placing and lifting are clicks on a
  card's colonist circles today, which the rules need nothing more than;
  dragging a colonist from San Juan onto a circle is the physical gesture the
  board suggests.
- **Reading the board mid-decision.** The action panel is sticky at the
  bottom and can cover a good deal of a five-player table on a short viewport.
- **A move history worth reading.** The event log names players and describes
  actions in words, but not *what* was taken or built — "Ana built a
  building", not "Ana built the Hospice for 2".

## 3. Reliability & production-readiness (explicitly deferred scoping decisions)

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
  alongside the timeout item above, same README. The client half now exists —
  a seat token survives a reload and `GET /decision` restores the pending
  move — but the server still has no notion of a seat being away, so a
  disconnected player simply blocks their seat until they return.
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

## 4. Explicitly out of scope (naming these so nobody assumes they're planned)

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

1. **§1.1 (greedy AI)** first — a human can play now, but only against
   opponents that move at random, so a real opponent is what the game most
   visibly lacks.
2. **§1.2 → §1.3** (search, then training) once §1.1 proves the extensibility
   path end-to-end.
3. **§2** (frontend polish) whenever it's in the way; none of it blocks play.
4. **§3** once the project has real users to protect against — timeouts and
   eviction matter most exactly when someone leaves a tab open or a game
   unattended.
5. **§4** only if a concrete need arises; treat this section as "decide before
   building," not a backlog.
