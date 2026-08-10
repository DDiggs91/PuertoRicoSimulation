# Critical Review — Puerto Rico Simulation

A module-by-module audit of correctness, dead code, test quality, documentation
accuracy, and completeness. Every finding names the file and the evidence. This
document is written to be executed: work top-down, and treat each **Fix** as the
acceptance criterion.

Run `./mvnw verify` before starting to establish a green baseline, and again
after each section.

Legend — **[BUG]** wrong behaviour · **[DEAD]** unused or unexplained code ·
**[TEST]** missing or hollow coverage · **[DOC]** documentation contradicts code
· **[GAP]** incomplete feature.

---

## 1. `puerto-rico-model` — rules engine

### 1.1 [BUG] Goods sold to the trading house are destroyed, never returned to the supply

`TradingHouse.clearIfFull()` ([goods/TradingHouse.java:37](puerto-rico-model/src/main/java/com/PRS/model/goods/TradingHouse.java#L37))
returns `empty()`, and `TraderPhaseHandler.advance`
([engine/TraderPhaseHandler.java:83-91](puerto-rico-model/src/main/java/com/PRS/model/engine/TraderPhaseHandler.java#L83-L91))
applies it without touching `state.goods()`. A sale moves a barrel off the
player (`player.plusGoods(good, -1)`) and onto the trading house; when the house
clears, that barrel vanishes from the game.

Every other route returns barrels correctly — `unloadFullShips`
(CaptainPhaseHandler.java:321-331), the Wharf (:178), storage overflow (:301-307)
— so the trading house is the sole leak.

Effect: up to 4 barrels per completed trader phase are permanently removed.
Coffee and tobacco start at 9 barrels each; a long game can silently exhaust a
kind, after which `CraftsmanPhaseHandler.begin` produces none of it and players
appear to lose production for no visible reason.

**Fix:** in `TraderPhaseHandler.advance`, when the house clears, put each barrel
it held back into `GoodsSupply`. Prefer making the accounting explicit — e.g.
have the clear step return the emptied goods so the handler can `supply.put(...)`
each one — rather than recomputing from the house's contents at the call site.

**Verify:** see the conservation test in [3.10](#310-test-no-test-asserts-goods-conservation).

### 1.2 [BUG] The Wharf is compulsory when it is a player's only loading option

`docs/game-rules.md` §5 (Captain → Wharf): *"Usable once per captain phase, at a
moment of the owner's choosing, and using it is optional even though loading is
otherwise compulsory."*

`CaptainPhaseHandler.canLoad`
([engine/CaptainPhaseHandler.java:94-105](puerto-rico-model/src/main/java/com/PRS/model/engine/CaptainPhaseHandler.java#L94-L105))
returns `true` when `mayUseWharf(...)` is true, so a player whose goods no ship
can take is still handed the turn — and `legalActions` (:107-125) then offers
only `LoadWharf`. There is no way to decline. `CaptainPhaseTest.theWharfShipsAKindStraightToTheSupply`
(CaptainPhaseTest.java:211) constructs exactly this state and asserts only
`.contains(...)`, so the forcing is neither locked in nor caught.

**Fix:** add a decline path so a Wharf owner with no ship-loadable goods may end
their turn. Options: a `PlayerAction.PassWharf` variant offered only when every
other option is a `LoadWharf`, or treat the Wharf as non-qualifying for
`canLoad` and offer it purely as an extra choice on a turn the player already
has. Whichever is chosen, `legalActions` must never consist solely of
`LoadWharf`. Update `PlayerAction`, the OpenAPI spec's `PlayerAction`
discriminator mapping, `ActionMapper`, and `src/api/types.ts` if a new variant is
added.

**Verify:** a test where the only ship is full of another kind and the player
holds goods must show a legal action that is not `LoadWharf`, and taking it must
advance the phase without shipping.

### 1.3 [BUG] The Wharf's 11-barrel capacity is not enforced

`docs/game-rules.md` §6: *"Capacity 11 barrels."* `apply`'s `LoadWharf` branch
(CaptainPhaseHandler.java:164-187) ships `player.goodsCount(good)` with no cap.
Unreachable today only because no kind has more than 11 barrels in the game — an
invariant held by `Good`'s supply table, not by this code.

**Fix:** cap at 11 explicitly, as a named constant on `BuildingType.WHARF` or in
the handler, with a comment tying it to the rulebook.

### 1.4 [DEAD] `RejectionReason.LOADING_IS_MANDATORY` is never produced

[engine/RejectionReason.java:23](puerto-rico-model/src/main/java/com/PRS/model/engine/RejectionReason.java#L23)
and [puerto-rico.yaml:393](puerto-rico-contract/src/main/resources/openapi/puerto-rico.yaml#L393).
Grep finds zero producers. It is vestigial: the captain phase skips players who
cannot load rather than rejecting a pass, so the rejection can never fire.

**Fix:** either delete it from the enum and the OpenAPI spec, or make it the
rejection returned by the decline path added in [1.2](#12-bug-the-wharf-is-compulsory-when-it-is-a-players-only-loading-option)
when a player who *can* load tries to skip. Do not leave it unreferenced.

### 1.5 [DEAD] `TileSupply.create` duplicates the face-up-row rule

[boards/TileSupply.java:48](puerto-rico-model/src/main/java/com/PRS/model/boards/TileSupply.java#L48)
calls `refillFaceUp(playerCount + 1)` while `SettlerPhaseHandler.advance` uses
`SetupTable.faceUpPlantations(state.playerCount())` for the same rule.

**Fix:** route setup through `SetupTable.faceUpPlantations` so the rule has one
home. (`SetupTable` already lives in `com.PRS.model.game`, which `boards` does
not depend on — pass the count in from `GameSetup` rather than adding the edge.)

### 1.6 [DOC] The README's "known simplifications" list is incomplete

[puerto-rico-model/README.md:78](puerto-rico-model/README.md#L78) names two.
Missing: the two Prospector cards are auto-resolved to whichever carries more
doubloons (`RoleTrack.indexToTake`, rolecards/RoleTrack.java:82-93 — correct, but
it *is* a decision the rules nominally leave to the player), and, until
[1.2](#12-bug-the-wharf-is-compulsory-when-it-is-a-players-only-loading-option)
and [1.3](#13-bug-the-wharfs-11-barrel-capacity-is-not-enforced) land, the Wharf
deviations.

**Fix:** after the engine fixes, add the Prospector auto-pick to the list. Remove
any Wharf entry once the engine matches the rulebook.

### 1.7 [DOC] `RandomPlay` is a public entry point the README does not mention

[README.md:21](puerto-rico-model/README.md#L21) says the Command/Query contract
is *"Three entry points, all pure functions"*, and the package table
([README.md:55](puerto-rico-model/README.md#L55)) lists `com.PRS.model.engine` as
holding *"`GameEngine`, `ActionResult`, `RejectionReason`, and one
package-private handler per phase"*. `RandomPlay` is public, lives in that
package, and is the class `puerto-rico-ai`'s `RandomAi` is built on — it is a
fourth public surface, not an internal.

**Fix:** name `RandomPlay` in the package table and explain why a random selector
ships in the rules module (it is shared with `puerto-rico-ai` deliberately —
`puerto-rico-ai/README.md` already argues this; the model README should agree).

---

## 2. `puerto-rico-session` — orchestrator

### 2.1 [BUG] `GameView` leaks the seed that reconstructs the entire tile shuffle

[view/GameView.java:15-21](puerto-rico-session/src/main/java/com/PRS/session/view/GameView.java#L15-L21)
scrubs `TileSupply.drawPile`, `discardPile`, and `TileSupply.seed` — but leaves
`GameState.config().seed()` intact. `GameSetup.create` derives the initial
shuffle entirely from that seed (`TileSupply.create(playerCount, config.seed())`,
game/GameSetup.java:39), so any `Actor` or `SessionListener` holding a `GameView`
can replay the shuffle and know every face-down tile.

Three things say this is wrong:

- `puerto-rico-session/README.md`, Design notes: *"the seed that would let a
  search reconstruct their order. `GameView.of` scrubs **both** before a
  `GameState` is ever handed to an `Actor`."*
- The OpenAPI spec got it right: `GameConfigView`
  ([puerto-rico.yaml:525-536](puerto-rico-contract/src/main/resources/openapi/puerto-rico.yaml#L525-L536))
  *"Omits GameConfig.seed on purpose."* So the wire is clean and the in-process
  view is not.
- `GameViewTest.scrubsDrawAndDiscardPilesAndTheSeed`
  ([GameViewTest.java:19](puerto-rico-session/src/test/java/com/PRS/session/view/GameViewTest.java#L19))
  is named for the seed but asserts only `tiles().seed()`.

**Fix:** scrub `config().seed()` too — e.g. rebuild the `GameConfig` with seed `0`
inside `GameView.of`. Then extend the test to assert
`view.state().config().seed()` is zero, and keep
`GameViewTest.preservesPublicInformation` passing (player names must survive).

### 2.2 [BUG] `GameSession.start()` mutates state outside the lock and is not idempotent

[GameSession.java:69-74](puerto-rico-session/src/main/java/com/PRS/session/GameSession.java#L69-L74).
`submit` is `synchronized` precisely so transitions serialize, but `start` calls
`requestNextDecision` — which reassigns `snapshot` and bumps `requestIdGen` —
without holding the same monitor. The javadoc says *"Call once"* but nothing
enforces it; a second call re-emits `GameStarted` and invalidates the outstanding
`requestId`, quietly stranding any pending human decision.

**Fix:** make `start()` `synchronized` and guard against a second invocation
(track a `started` flag; a repeat call should be a no-op or throw, and the choice
should be stated in the javadoc).

### 2.3 [BUG] `ActionApplied` is broadcast before `snapshot` advances

[GameSession.java:169-178](puerto-rico-session/src/main/java/com/PRS/session/GameSession.java#L169-L178):
`emit(ActionApplied(broadcastView(next), ...))` runs while `snapshot` still holds
the pre-action state; only `requestNextDecision`/the `GameEnded` branch install
`next`. Listeners run inline on the session thread, so an SSE client that
receives `ACTION_APPLIED` and immediately re-reads `GET /api/games/{id}/state`
can get a board older than the event it just saw.

Low severity — the frontend renders from the event's own `view` — but it makes
`state()` and the event stream disagree at a moment a client can observe.

**Fix:** install the new `snapshot` before emitting `ActionApplied`, so every
emitted view is one a concurrent reader could also have fetched.

### 2.4 [DEAD] `FakeActors.scripted` is never used

[FakeActors.java:190](puerto-rico-session/src/test/java/com/PRS/session/FakeActors.java#L190).
`puerto-rico-session/README.md`'s Testing section lists it among the stand-ins
the suite supplies.

**Fix:** delete it and drop it from the README, or write the test that needs it —
a scripted actor is the natural way to cover a specific action sequence the
`firstLegal` fixture can't reach.

### 2.5 [DEAD] `SessionRunner.handleFailure` has an unreachable branch structure

[SessionRunner.java:115-125](puerto-rico-session/src/main/java/com/PRS/session/SessionRunner.java#L115-L125):
both the `>= MAX_CONSECUTIVE_FAILURES` branch and the fall-through end in
`step()`, so the `return` separates two identical tails.

**Fix:** collapse to `if (consecutiveFailures >= MAX) session.fail(...); step();`.

### 2.6 [DOC] The README's trampoline explanation invites a wrong "fix"

`puerto-rico-session/README.md`: *"no matter how that future completes, re-posts
the next step to a single-threaded executor rather than chaining inline."* The
behaviour is correct, but the mechanism is subtler than the sentence implies:
`onAnswer` and `handleFailure` call `step()` **directly** (SessionRunner.java:108,
121, 124); the stack unwinds because `step()` ends at
`answer.whenCompleteAsync(..., executor)` (:92), which queues rather than
recurses. A future reader who takes the README literally may "correct" the direct
calls, or worse, replace `whenCompleteAsync` with `whenComplete` and reintroduce
the overflow.

**Fix:** reword to name `whenCompleteAsync` as the trampoline point, and add a
short comment at SessionRunner.java:92 saying the executor hand-off is what
bounds the stack.

---

## 3. `puerto-rico-lobby`, `puerto-rico-ai`, `puerto-rico-contract`

### 3.1 [BUG] A finished game is listed as `STARTED` forever

`GameTableStatus` has only `OPEN` and `STARTED`
([GameTableStatus.java:4-7](puerto-rico-lobby/src/main/java/com/PRS/lobby/GameTableStatus.java#L4-L7),
mirrored at [puerto-rico.yaml:239-241](puerto-rico-contract/src/main/resources/openapi/puerto-rico.yaml#L239-L241)).
`GameTable.start` sets `STARTED` and nothing ever moves it on, even though the
table owns a `GameSession` whose `status()` reaches `FINISHED` or `FAILED`. The
lobby list therefore shows completed games as in-progress, and `LobbyScreen`
renders that status verbatim.

**Fix:** add `FINISHED` (and consider `FAILED`) to the enum and the spec, and
derive the summary status from the live session in `GameTable.summary()` rather
than from the stored snapshot. Update `LobbyMapper`, `src/api/types.ts` consumers,
and `LobbyApiTest`.

### 3.2 [BUG] `Lobby.tables` grows without bound

[Lobby.java:23](puerto-rico-lobby/src/main/java/com/PRS/lobby/Lobby.java#L23) is a
`ConcurrentHashMap` with `put` but no `remove`. Every created table — including
ones abandoned before seating — is retained for the process's lifetime, along
with its `GameSession`, full `GameState` history, and `SessionRunner` thread.
`puerto-rico-lobby/README.md`'s "Explicitly deferred" list covers seat-leaving,
spectator joins, and restart-resume; it does not mention eviction, so this reads
as an oversight rather than a decision.

**Fix:** add eviction — the smallest useful version is a `Lobby.remove(GameId)`
plus a sweep of finished/abandoned tables (close the runner, drop the entry) —
and record whatever policy is chosen in the README's deferred/decided list.

### 3.3 [BUG] `SeatTokens` grows without bound

[SeatTokens.java:17](puerto-rico-web/src/main/java/com/PRS/web/actors/SeatTokens.java#L17):
minted tokens are never removed, and a token stays valid after its game ends.

**Fix:** evict a game's tokens when its table is evicted per [3.2](#32-bug-lobbytables-grows-without-bound);
key the map by `GameId` so the sweep is a single removal.

### 3.4 [DOC] `puerto-rico-ai/README.md` contains raw Javadoc markup

Design notes, second bullet: *"`RandomAi` schedules its answer via {@code
CompletableFuture.delayedExecutor}"* — `{@code ...}` renders literally in
Markdown.

**Fix:** use backticks.

---

## 4. `puerto-rico-web` — backend

### 4.1 [BUG] `POST /api/games/{id}/start` with `{"seed": null}` returns 500

[LobbyController.java:88-91](puerto-rico-web/src/main/java/com/PRS/web/api/LobbyController.java#L88-L91):

```java
long seed = startRequest == null
    ? ThreadLocalRandom.current().nextLong()
    : startRequest.getSeed().orElse(ThreadLocalRandom.current().nextLong());
```

`StartRequest.seed` is generated as `JsonNullable<Long>` (verified in
`puerto-rico-contract/target/generated-sources/.../StartRequest.java`). An
explicit JSON `null` deserializes to `JsonNullable.of(null)` — *present*, holding
`null` — so `orElse` returns `null` and the unboxing to `long` throws
`NullPointerException`. Only an **omitted** field takes the fallback. Nothing in
`LobbyApiTest` sends the field, so this is uncaught.

**Fix:** treat present-and-null as absent, e.g.
`Optional.ofNullable(startRequest).map(StartRequest::getSeed).filter(JsonNullable::isPresent).map(JsonNullable::get).filter(Objects::nonNull).orElseGet(...)`,
or a small `seedOrRandom(StartRequest)` helper. Add a MockMvc test posting
`{"seed":null}` and one posting `{"seed":42}` (asserting the game is reproducible).

### 4.2 [BUG] SSE streams are never closed and their subscriber lists never evicted

[GameEventStream.java](puerto-rico-web/src/main/java/com/PRS/web/events/GameEventStream.java):
`subscribe` creates `new SseEmitter(0L)` (never times out) and `listenerFor`
fans out, but nothing calls `emitter.complete()` when `GameEnded` or
`SessionFailed` arrives, and `subscribers` (:24) never drops a game's entry. Every
spectator holds an open connection indefinitely after the game finishes, and the
map retains a list per game forever.

**Fix:** on a terminal event, send it, then complete every emitter for that game
and remove the map entry. Coordinate with [3.2](#32-bug-lobbytables-grows-without-bound)
so lobby eviction and stream eviction agree.

### 4.3 [DEAD] Inline fully-qualified `java.io.IOException`

[GameEventStream.java:44](puerto-rico-web/src/main/java/com/PRS/web/events/GameEventStream.java#L44).
The FQN on `com.PRS.contract.model.SessionEvent` (:40) is justified — it collides
with `com.PRS.session.events.SessionEvent`. `java.io.IOException` collides with
nothing.

**Fix:** import it.

### 4.4 [DEAD] `puerto-rico-model` compiles and ships against Spring Web

The root [pom.xml:106-109](pom.xml#L106-L109) declares `spring-boot-starter-web`
in `<dependencies>`, not `<dependencyManagement>`, so **every** module inherits it
at compile scope — including `puerto-rico-model`, whose README says *"This is
pure domain logic: no I/O, no knowledge of the web layer"* and whose
`docs/architecture.md` entry says the same. `puerto-rico-model`'s jar drags
Spring Web and Tomcat onto the classpath of anything that depends on it.

**Fix:** move `spring-boot-starter-web` out of the root `<dependencies>` and
declare it in `puerto-rico-web/pom.xml` only. Check what else the domain modules
were silently relying on — `slf4j` reaches `puerto-rico-session` this way
(`GameSession.java:21-22`), so `puerto-rico-session` needs an explicit
`slf4j-api` dependency once the starter is gone. `spring-boot-starter-test`
(test scope) can stay inherited; it is what supplies AssertJ everywhere.
Then update CLAUDE.md's Tech-stack paragraph, which currently says
`puerto-rico-model` *"inherits all third-party dependencies and plugins from the
root"* without noting that this includes the web stack.

### 4.5 [DEAD] Unused `slf4j.version` property

[pom.xml:22](pom.xml#L22) is never referenced; slf4j arrives via the Spring Boot
BOM.

**Fix:** delete it (or, if [4.4](#44-dead-puerto-rico-model-compiles-and-ships-against-spring-web)
adds an explicit `slf4j-api` dependency, use it there).

### 4.6 [GAP] `puerto-rico-web` has no `src/main/resources` at all

No `application.properties`, no `application-test.properties`, no logging
configuration. Consequences visible today:

- Every `@SpringBootTest` that starts a game runs the AI at the production
  300 ms think-time (`WebConfig.java:28`), leaving real games grinding in
  background threads for the rest of the suite. Only Playwright overrides it
  (`playwright.config.ts:22`).
- There is nowhere to set server/port/logging defaults, and the SSE and think-time
  knobs are undiscoverable without reading `WebConfig`.

**Fix:** add `src/test/resources/application.properties` with
`app.ai.think-time-ms=0` so tests run instantly and deterministically, and
`src/main/resources/application.properties` documenting the app's own settings.

---

## 5. `puerto-rico-frontend`

### 5.1 [BUG] A failed `/state` fetch leaves the UI stuck on "Loading game…"

[App.tsx:117-133](puerto-rico-frontend/src/App.tsx#L117-L133): the bootstrap
effect calls `unwrap(...)` inside an un-awaited async IIFE with no `catch`. Any
failure — unknown id, a game that exists but was never started (the endpoint 404s
in that case), a network blip — becomes an unhandled promise rejection, and
`state.view` stays `null`, so [App.tsx:146-148](puerto-rico-frontend/src/App.tsx#L146-L148)
renders "Loading game…" forever. Opening `?game=<anything-invalid>` reproduces it.

**Fix:** catch, store the error, and render it with `role="alert"` and a way back
to the lobby.

### 5.2 [BUG] `SESSION_FAILED` is captured and never shown

`gameReducer` maintains `failure`
([state/gameReducer.ts:6,46](puerto-rico-frontend/src/state/gameReducer.ts#L46))
and `gameReducer.test.ts` asserts it is set — but `App.tsx` never reads it and
`GameBoard` has no such prop
([App.tsx:150](puerto-rico-frontend/src/App.tsx#L150)). A session that aborts
leaves the board frozen with no explanation. (The event does appear in the event
log via `describeEvent`, buried among every other event.)

**Fix:** surface `failure` as a prominent `role="alert"` banner, with a
`data-testid` and a component test.

### 5.3 [BUG] Client-side standings sort discards the tiebreak

[GameBoard.tsx:44](puerto-rico-frontend/src/components/GameBoard.tsx#L44) re-sorts
by `total` only. The server already sorts with the rulebook tiebreak (doubloons +
goods) in `Scorer.finalStandings`, and the wire carries `tiebreak` for exactly
this reason. Two players on the same total render in an arbitrary order that
contradicts the server's ranking.

**Fix:** render `standings` in the order received; delete the client sort. If a
sort is kept for defensiveness, it must apply `total` then `tiebreak`, both
descending.

### 5.4 [GAP] The spectator board renders counts, not a board

`docs/architecture.md` §7: *"Renders the player board, central board, role
selection, lobby/join screens, and a spectator view."* What is actually rendered:

| Rendered | Not rendered |
|---|---|
| Phase type + acting seat | The role track — which roles remain, who took what, doubloons sitting on cards |
| Per player: doubloons, VP, **counts** of island tiles / buildings / San Juan colonists | Which tiles and which buildings a player owns; whether they're occupied; goods held |
| Quarries remaining | The face-up plantation row |
| Trading house occupancy `n / 4` | Which goods are in the trading house |
| Event log, final standings | Cargo ships (capacity, cargo kind, load), colonist ship, colonist supply, VP supply, `finalRound` |

Every one of these fields is already on the wire in `GameStateView` — this is
purely a rendering gap.

**Fix:** extend `PlayerBoard` to list island tiles (kind + occupied) and
buildings (type + colonists + capacity) and goods held; add a `CentralBoard`
component for ships, the face-up row, the trading house contents, the role track,
and the supplies. Every element a test locates gets a stable `data-testid`, and
interactive/status elements get real ARIA roles — this repo designs that in from
the start rather than retrofitting (`puerto-rico-frontend/README.md`, Design
notes). Extend the Vitest component tests and add Playwright assertions for the
new regions.

### 5.5 [GAP] You cannot spectate a game from the lobby list

[LobbyScreen.tsx:79-85](puerto-rico-frontend/src/components/LobbyScreen.tsx#L79-L85)
renders games as inert `<li>` text. The only way to watch a game you did not
start is to be handed its `?game=<id>` URL. `README.md`'s Status section sells
*"open a second tab mid-game as a spectator"*, which works only via URL sharing.
The list also never refreshes after the initial load, so seats filling at another
table are invisible.

**Fix:** make each row a link/button that calls `onGameStarted(game.id)`
(rename it — `onWatchGame` describes both paths), and poll `GET /games` on an
interval while the lobby is mounted. Cover both with component tests and add a
Playwright case that joins an in-progress game from the list.

### 5.6 [GAP] No SSE error handling or reconnect; the event array grows unbounded

[api/events.ts](puerto-rico-frontend/src/api/events.ts) sets `onmessage` only —
no `onerror`, no reconnect signalling, no way for the UI to show "connection
lost". `EventSource` reconnects on its own, but events emitted during the gap are
lost with nothing to resynchronise from (a re-fetch of `/state` would fix it).
Separately, `gameReducer` appends every event forever
(gameReducer.ts:35); a full 5-player game produces thousands, all rendered by
`EventLog` inside an `aria-live="polite"` list — which a screen reader will read
continuously.

**Fix:** add `onerror` handling that surfaces a disconnected state and re-fetches
`/state` on recovery; cap the retained log (keep the last N, or render a
windowed view) and give `EventLog` `role="log"` with `aria-live` scoped so it
announces new entries rather than the whole list.

### 5.7 [GAP] The application has no styling whatsoever

No CSS or styling solution anywhere in `puerto-rico-frontend/src`
(`vite.config.ts` even sets `css: false` for tests). The delivered UI is
unstyled default-browser HTML.

**Fix:** add a minimal stylesheet — this does not need a framework, but a
Puerto Rico board with no visual structure is not a usable spectator view. Keep
it theme-aware and keep every existing `data-testid`/role intact so no test
breaks.

### 5.8 [DEAD] `@vitest/coverage-v8` is installed with no script that uses it

[package.json:33](puerto-rico-frontend/package.json#L33). No `coverage` script,
no `coverage` block in `vite.config.ts`.

**Fix:** add `"test:unit:coverage": "vitest run --coverage"` and a coverage config
(which would also make the gaps in section 6 measurable), or drop the dependency.

### 5.9 [DOC] The README's project-layout table omits real files

[puerto-rico-frontend/README.md:37-46](puerto-rico-frontend/README.md#L37-L46)
lists `src/api/*`, `src/state/*`, `src/components/`, `e2e/` — but not
`src/App.tsx` (which owns routing, bootstrap, and subscription), `src/main.tsx`,
or `src/test/` (`fixtures.ts`, `setup.ts`).

**Fix:** add them.

---

## 6. Test quality

### 6.1 [TEST] `GameEventStreamTest` tests almost nothing this module owns

[GameEventStreamTest.java](puerto-rico-web/src/test/java/com/PRS/web/events/GameEventStreamTest.java):

- `subscribeReturnsAUsableEmitter` (:43) asserts that Spring's own
  `SseEmitter.complete()` does not throw. It exercises no `GameEventStream` logic.
- `aCompletedEmitterIsDroppedWithoutPropagating` (:31) asserts only that no
  exception escapes. It never checks the emitter was actually **dropped**, which
  is the behaviour `puerto-rico-web/README.md` promises (*"an emitter that throws
  … is dropped from that game's subscriber list rather than allowed to wedge the
  others"*).
- The happy path — a live subscriber receives the mapped wire event — is untested
  entirely.

**Fix:** delete `subscribeReturnsAUsableEmitter`. Rewrite the drop test to assert
a subsequent event does not reach the dead emitter and that a second, live
emitter still receives it. Add a test that a subscriber receives a
`SessionEventMapper`-mapped payload with the right discriminator.

### 6.2 [TEST] `WireMappingTest` covers 2 of 9 `Phase` variants

[WireMappingTest.java:120-144](puerto-rico-web/src/test/java/com/PRS/web/wire/WireMappingTest.java#L120-L144)
covers `ROLE_SELECTION` and `SETTLER`. Untested: `MAYOR`, `BUILDER`,
`CRAFTSMAN_BONUS`, `TRADER`, `CAPTAIN_LOADING`, `CAPTAIN_STORAGE`, `GAME_OVER` —
including the branches that set `craftsmanOptions`, `wharfUsed`, and `bonusUsed`,
which no other test touches. `puerto-rico-web/README.md` claims this class proves
*"Every mapper … is total and correct"*.

**Fix:** parameterise over all nine variants, asserting the discriminator and each
variant's own fields.

### 6.3 [TEST] Nothing asserts the wire discriminator is actually serialized

`ActionMapper.toWire` never sets `type`
([ActionMapper.java:31-130](puerto-rico-web/src/main/java/com/PRS/web/wire/ActionMapper.java#L31-L130));
correct JSON depends entirely on the generated `@JsonTypeInfo`/`@JsonSubTypes` on
`PlayerAction`. The round-trip test (:71-78) goes object → object and never
touches JSON, so a generator-config change that drops the annotations would break
every client while the suite stays green.

**Fix:** add a serialization test asserting
`{"type":"SELECT_ROLE","seat":0,"role":"SETTLER"}`-shaped output for a
representative action and event, using the Jackson 3 `ObjectMapper` from the
Spring context.

### 6.4 [TEST] `LobbyApiTest` never covers `ALREADY_STARTED`

`puerto-rico-web/README.md` claims it covers *"every `LobbyRejectionReason` maps
to its status and body"*. `GAME_NOT_FOUND`, `TABLE_FULL`, and `TOO_FEW_SEATS` are
covered; `ALREADY_STARTED` is not (only `LobbyTest` covers it, at the lobby layer,
not the HTTP mapping).

**Fix:** add MockMvc cases for seating after start and starting twice, both
asserting 409 and `$.reason == "ALREADY_STARTED"`.

### 6.5 [TEST] Two engine rejection reasons are produced but never asserted

`TILE_UNAVAILABLE` (`SettlerPhaseHandler.java:78`) and `WHARF_UNAVAILABLE`
(`CaptainPhaseHandler.java:167`) have zero assertions in
`puerto-rico-model/src/test`.

**Fix:** add the two cases — an out-of-range `faceUpIndex`, and a `LoadWharf`
submitted by a player with no occupied Wharf or one already spent.

### 6.6 [TEST] `App.tsx` has no test at all

It is the only frontend source file without a sibling test, and it owns the
`?game=` routing, the `/state` bootstrap, the SSE subscribe/unsubscribe lifecycle,
and the loading state — including the bug in
[5.1](#51-bug-a-failed-state-fetch-leaves-the-ui-stuck-on-loading-game).

**Fix:** add `src/App.test.tsx` covering: no `?game` renders the lobby; `?game=x`
fetches `/state` and renders the board; a failed fetch renders an error, not a
permanent spinner; unmounting closes the `EventSource`.

### 6.7 [TEST] `client.test.ts` never exercises the client it is named for

[api/client.test.ts](puerto-rico-frontend/src/api/client.test.ts) tests `unwrap`
only. The lazy-`fetch` indirection that `puerto-rico-frontend/README.md` devotes a
whole design note to — *"forcing a fresh `globalThis.fetch` lookup on every
call"* — is only incidentally exercised by `LobbyScreen.test.tsx`. If someone
reverted `client.ts` to openapi-fetch's default, no test named for `client` would
fail.

**Fix:** add a test that stubs `globalThis.fetch` in `beforeEach` (after module
load) and asserts a `client.GET` call reaches the stub.

### 6.8 [TEST] No test asserts goods conservation

Nothing checks the game's most basic invariant, which is exactly why
[1.1](#11-bug-goods-sold-to-the-trading-house-are-destroyed-never-returned-to-the-supply)
went unnoticed.

**Fix:** add to `GameEngineContractTest` a check, run after every step of the
random-play fuzz loop, that for each `Good`:

```
supply + Σ players' holdings + Σ ships carrying that kind + trading-house count
  == good.barrelSupply()
```

This must fail before the [1.1](#11-bug-goods-sold-to-the-trading-house-are-destroyed-never-returned-to-the-supply)
fix and pass after. Consider the analogous invariants for colonists
(supply + on ship + on boards + in San Juan == `SetupTable.colonistSupply`) and
for building copies.

### 6.9 [DEAD] `TestGames.actor(...)` is never called

[TestGames.java:137](puerto-rico-model/src/test/java/com/PRS/model/TestGames.java#L137).

**Fix:** delete it, or use it in place of the `state.phase().actorSeat()` calls
scattered through the phase tests.

---

## 7. Documentation & repo hygiene

### 7.1 [DOC] `docs/architecture.md` is still written as a forward-looking outline

The document describes a system that does not yet exist, while every part of it
is built and tested:

- Header (:3-6): *"It names the parts the system **will need** … it does not
  define interfaces, classes, or message shapes. Those are separate, later
  decisions."* All of them are defined.
- Contracts (:94): *"Named here to show how components connect; **none are
  designed yet**."* All five are designed, generated, and implemented.
- A trailing section, *"Decisions made since this outline was written"* (:144),
  narrates history rather than stating current state — the same pattern the rest
  of the doc set avoids.

**Fix:** rewrite it in the present tense as a description of the system as it
stands. Fold the "Decisions made since" content into the sections it belongs to
(frontend stack into §7, seat tokens into §6, OpenAPI into the Contracts table)
and delete the historical framing. Keep the scoping decisions (in-process AI,
in-memory persistence) — those are current constraints, not history.

### 7.2 [DOC] "1-4 human players" contradicts the 3–5 player scope

[docs/architecture.md:14](docs/architecture.md#L14) — *"1-4 human players
connecting via a web browser"* — and :71 — *"seating 1-4 human players and/or AI
engines into open seats."* The game is 3–5 players (`docs/game-rules.md`,
`SetupTable.MIN_PLAYERS`/`MAX_PLAYERS`, CLAUDE.md, both READMEs), and the lobby
accepts up to 5 seats of any kind, all of which may be human.

**Fix:** state 3–5 seats total, any mix of human and AI.

### 7.3 [DOC] Frontend claims in architecture.md overstate what is rendered

§7 (:86-91) — see [5.4](#54-gap-the-spectator-board-renders-counts-not-a-board).
Once 5.4 lands, §7 becomes true; until then, the two disagree.

**Fix:** land 5.4, then confirm §7 reads accurately (in particular, "role
selection" must mean a rendered role track, not just the phase name).

### 7.4 [DOC] `e2e` comments contradict `playwright.config.ts`

[e2e/game.spec.ts:21-23](puerto-rico-frontend/e2e/game.spec.ts#L21-L23) and
:43-44 both say *"A full 3-player game **at production AI think-time** can take a
while"*, but `playwright.config.ts:19-22` boots the jar with
`-Dapp.ai.think-time-ms=20` and explains it is *"A much shorter AI think-time
than production's 300ms default."*

**Fix:** correct the spec comments to say the timeout covers a full game at the
20 ms e2e think-time (and note the `reuseExistingServer` case, where a
locally-running server may in fact be at 300 ms — which is presumably where the
stale comment came from).

### 7.5 [DOC] `.editorconfig` prescribes 4-space indentation for Java and TypeScript

[.editorconfig:9](.editorconfig#L9) sets `indent_size = 4` under `[*]`, overridden
to 2 only for `yml/yaml/json/md`. Java is formatted by google-java-format at 2
spaces (enforced automatically on every build) and the TypeScript is 2-space
throughout. Any editor honouring `.editorconfig` fights the formatter on every
new Java file.

**Fix:** add `[*.{java,ts,tsx,js,jsx,css,html}] indent_size = 2`, or flip the `[*]`
default to 2 and keep 4 only where something actually uses it.

### 7.6 [DOC] `.devcontainer/Dockerfile` has no trailing newline

Violates the repo's own `.editorconfig` `insert_final_newline = true`.

**Fix:** add one.

### 7.7 [DEAD] A Gradle extension in a Maven-only project

`.devcontainer/devcontainer.json` installs `vscjava.vscode-gradle`. There is no
Gradle anywhere in the repo.

**Fix:** remove it. (`cucumberopen.cucumber-official` and `dbaeumer.vscode-eslint`
are already flagged in CLAUDE.md's "Open questions" as anticipated-not-committed —
leave those alone unless the ESLint question in
[5.8](#58-dead-vitestcoverage-v8-is-installed-with-no-script-that-uses-it)'s
neighbourhood is settled.)

### 7.8 [DOC] Root README's project structure omits two tracked paths

[README.md:75-93](README.md#L75-L93) shows `docs/` with two files (the
authoritative rulebook PDF is missing) and no `.dockerignore` — which the
devcontainer build genuinely depends on, and which CLAUDE.md's Devcontainer
section explicitly references.

**Fix:** add both to the tree.

### 7.9 [GAP] No lint or formatting config for the frontend

CLAUDE.md already records this as an open question. It has a visible cost:
`src/test/fixtures.ts:22` and `src/components/LobbyScreen.tsx:64` run well past
the line length every other file keeps to, because nothing enforces it. The Java
side has this solved (google-java-format auto-fixing on every build).

**Fix:** decide the question. The consistent choice is Prettier wired into the
`frontend-maven-plugin` build the same way `fmt-maven-plugin` is wired into the
Java build — auto-fix on build, not check-and-fail — so both halves of the repo
behave identically. Then update CLAUDE.md's "Open questions" section.

---

## Suggested order

1. **Correctness first:** 1.1, 2.1, 4.1, 4.2, 5.1, 5.2 — each is a user-visible
   or invariant-breaking defect, and each is small.
2. **Invariant tests that lock the fixes in:** 6.8, then 6.1–6.7.
3. **Rules fidelity:** 1.2, 1.3, 1.4 (they interact — the decline path decides
   whether `LOADING_IS_MANDATORY` lives or dies).
4. **Lifecycle and leaks:** 3.1, 3.2, 3.3, 2.2, 2.3, 4.6.
5. **Frontend completion:** 5.4, 5.5, 5.6, 5.7, 5.3.
6. **Dead code and build hygiene:** 4.4 (largest blast radius — do it with a full
   `./mvnw verify` after), 4.5, 1.5, 2.4, 2.5, 4.3, 5.8, 6.9, 7.7.
7. **Documentation last,** so it describes the finished state: 7.1–7.6, 7.8, 7.9,
   1.6, 1.7, 2.6, 3.4, 5.9.

## Verification

- `./mvnw verify` after every section — it runs the full reactor including the
  frontend's Vitest suite and auto-formats Java.
- `./mvnw -pl puerto-rico-web -am package -DskipTests && cd puerto-rico-frontend
  && npm run test:e2e` for anything touching the wire contract, the SSE stream,
  or the frontend.
- `./mvnw -pl puerto-rico-web spring-boot:run` and drive a real game in a browser
  for the frontend items — the unit suite cannot show that the board reads well.
- For [1.1](#11-bug-goods-sold-to-the-trading-house-are-destroyed-never-returned-to-the-supply)
  specifically: confirm the conservation assertion from
  [6.8](#68-test-no-test-asserts-goods-conservation) **fails on the current code**
  before applying the fix. A fix whose test never failed first has proved nothing.
