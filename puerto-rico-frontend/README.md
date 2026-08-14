# puerto-rico-frontend — Web Frontend (browser client)

See [docs/architecture.md](../docs/architecture.md) for how this module fits
into the whole system.

## Intent

Renders a running game — the central board (role track, ships, face-up
plantations, trading house, supplies), every player's board, a running
event log, and final standings — plus the lobby screen used to create a
table, take a seat at it, and seat AI opponents. A seated human plays
through per-phase action pickers; a visitor with no seat watches the same
board without them.

## Stack

React 19 + TypeScript + Vite. Vitest + React Testing Library for unit
tests; Playwright for functional tests against the real packaged app.
`openapi-fetch` for the API client, typed from `openapi-typescript`'s
generated schema — see below.

## Contracts

Speaks the wire contract generated from `puerto-rico-contract`'s OpenAPI
spec, exposed by `puerto-rico-web`: request/response for lobby actions,
plus a Server-Sent Events channel for live game events.

## Depends on

Nothing in this reactor via Maven (it has no Java dependency edges); at
build time, `npm run generate:api` reads
`../puerto-rico-contract/src/main/resources/openapi/puerto-rico.yaml`
directly off disk.

## Project layout

| Path                       | Holds                                                                                                   |
| -------------------------- | ------------------------------------------------------------------------------------------------------- |
| `src/api/schema.d.ts`      | **Generated**, gitignored — run `npm run generate:api`                                                  |
| `src/api/types.ts`         | Hand-written union aliases for `PlayerAction`/`SessionEvent` (see below) plus re-exports of the rest    |
| `src/api/client.ts`        | The configured `openapi-fetch` client, plus `unwrap`/`ApiError`                                         |
| `src/api/events.ts`        | `EventSource` subscription → typed `SessionEvent`                                                       |
| `src/state/gameReducer.ts` | Pure `(state, event) => state` — the UI state, including the pending decision                           |
| `src/state/seatSession.ts` | The seat token, persisted per game so a reload doesn't strand a player                                  |
| `src/components/`          | `LobbyScreen`, `GameBoard`, `CentralBoard`, `BuildingDisplay`, `PlayerBoard`, `EventLog`, `ActionPanel` |
| `src/components/pickers/`  | One picker per action family, plus the shared `ActionButton` and `PickerProps`                          |
| `src/components/art/`      | Inline-SVG pieces (`Pieces`, `RoleIcon`, `BuildingCard`) and the names/colours in `labels.ts`           |
| `src/styles/`              | `tokens.css` (palette and type) and `app.css` (layout)                                                  |
| `src/App.tsx`              | `?game=` routing, the `/state` + `/decision` bootstrap, SSE lifecycle, move submission                  |
| `src/main.tsx`             | The React entry point — mounts `App` and imports the stylesheet                                         |
| `src/test/`                | `setup.ts` (jest-dom matchers) and `fixtures.ts` (minimal-but-valid `GameStateView` builders)           |
| `e2e/`                     | Playwright specs                                                                                        |

## Design notes

**Generated types, one hand-written seam.** `openapi-typescript` correctly
generates every individual `PlayerAction`/`SessionEvent` variant — literal
`type` field included — but doesn't synthesize a union out of them the way
it would from a spec using `oneOf` instead of `allOf` + `discriminator`
(the shape `puerto-rico-contract`'s spec deliberately uses instead, since
it's what the Java generator handles most reliably — see that module's
README). `src/api/types.ts` closes that one gap with a hand-written union
alias per hierarchy. Nothing about a variant's _shape_ is hand-maintained,
only the grouping.

**`openapi-fetch` needs an absolute `baseUrl`, not `/api`.** It builds a
real `Request` internally, and `Request` — unlike a browser calling
`fetch("/api/...")` — has no page to resolve a relative path against;
`new Request("/api/games")` throws under Node's fetch (which is what runs
under Vitest, jsdom or not). `client.ts` uses `window.location.origin` so
this works identically under test and in a real browser.

**The client's `fetch` is looked up lazily, not defaulted once.**
`openapi-fetch`'s own default is `fetch: globalThis.fetch` evaluated at
`createClient()` call time — module load, in practice — so a test's
`vi.stubGlobal("fetch", ...)` in `beforeEach` has no effect on a client
created before that stub runs. `client.ts` passes `fetch: (...args) =>
globalThis.fetch(...args)` instead, forcing a fresh lookup on every call.

**Test ids are structural, ARIA roles are semantic — both by design, not
retrofitted.** Every element a test locates has a stable `data-testid`
(`lobby-create-game`, `player-board-2`, `event-log`, `final-standings`,
...); interactive/status elements additionally carry real roles
(`role="status"` with `aria-live` on the phase indicator, `role="alert"` on
errors) so the same locators work for a screen reader and for Playwright.

**Every option comes from the server's legal-action list, unchanged — with
one sanctioned exception.** A picker never constructs a `PlayerAction`; it
filters `Decision.options` (or the identical list on
`DecisionRequestedEvent`) by variant and hands the very same object back on
click. `HumanActor.offer` checks membership by equality, so "that action is
not currently legal" isn't a race a fast clicker can win — it's unreachable
from ordinary use. That is also why the pickers are split per phase rather
than one form: each one only has to present a list it was given.

`MayorPicker` is the exception: a colonist arrangement is a configuration,
not a choice from a list, so the mayor phase offers exactly one option (a
greedy fill) and the picker stages its own arrangement locally instead —
placing and lifting colonists freely, with nothing sent until Finalize. It
constructs a `SetColonistPlacementAction` to match what was staged, and
`HumanActor.offer` admits that one variant by asking the engine whether it
is legal (the same check `GameSession.submit` would make next) rather than
by list membership. See `pickerTypes.ts` and `HumanActor`'s doc-comment.

**The pending decision carries the board it was computed against.** A
legal-action list means nothing without the phase it came from, and the board
arrives by other routes too — `/state` on bootstrap, and again on a resync
after the stream drops. Pairing options from one moment with a board from
another draws a picker for the wrong phase, which has nothing to offer and
strands the player. `DecisionRequestedEvent` and `Decision` both carry their
own `view`, so `PendingDecision` keeps it and `ActionPanel` reads its phase
from there rather than from the surrounding `GameBoard`.

**The seat token is persisted; it cannot be re-fetched.** `SeatTokens` mints
one at seating time and has no lookup — being able to ask for a token would
defeat what holding one proves. So a token kept only in React state is gone
on reload, locking a player out of a seat the server still considers theirs.
`seatSession.ts` stores `{gameId, seat, token, name}` in `localStorage` keyed
by game id, and `App.tsx` restores it on the `?game=` path. Storage access is
wrapped: a browser with site data blocked loses reload recovery, not the app.

**`GET /decision` closes the window `DECISION_REQUESTED` leaves open.** That
event fires once, when the session starts waiting. A client that arrives —
or reloads — after that never hears it, and nothing replays it. So the
bootstrap fetches the pending decision alongside the state, and treats a 404
as the ordinary "nothing pending" answer rather than a load failure.

**Numbers on cards are quoted by the server, never computed here.** A
building's discounted cost and a good's sale price are engine rules
(`GameEngine.buildCost` / `sellPrice`), so `Phase.buildOptions` and
`Phase.goodPrices` carry the results and the pickers display them. What lives
client-side is only what the server has no opinion about: English names and a
palette, in `components/art/labels.ts`.

**The art is inline SVG, not image files.** Nothing to fetch or cache-bust,
and a piece recolours and resizes from its props. It is original work
evoking the boxed game's look — this is an unaffiliated fan project (see the
root README), so no published artwork is reproduced.

**Routing is one query parameter, not a router.** This slice has exactly
one navigable destination besides the lobby — a specific game — so `App.tsx`
reads `?game=<id>` rather than pulling in a routing library. Starting a
game rewrites the URL to match, so a spectator's link is shareable/reloadable.

## Testing

```bash
npm install
npm run generate:api      # writes src/api/schema.d.ts
npm run test:unit         # Vitest + React Testing Library
npm run typecheck
npm run build              # outputs to target/classes/META-INF/resources — what puerto-rico-web serves
npm run test:e2e           # Playwright — needs the real app built and packaged first, see below
```

Playwright runs against the **real, packaged Spring Boot app** — not a
mocked API — so it catches wire-contract drift the unit suite can't see.
Its `webServer` config boots the jar itself:

```bash
../mvnw -pl puerto-rico-web -am package -DskipTests
npm run test:e2e
```

`npm run test:unit`/`npm run build` regenerate `schema.d.ts` automatically
via `pretest:unit`/`prebuild` npm hooks, so neither needs
`generate:api` run first by hand.

## Status

Built and tested — unit, component, and end-to-end: the lobby (create, list,
take a human seat, seat AI opponents, start), the live board (central board,
per-player boards, event log, final standings), and human play — an action
picker for each of the eight action families, move submission with the seat
token, and seat persistence across a reload. Framework/toolchain choice is
settled (this stack).

Not built yet: the frontend half of timeout handling, which needs a
server-side per-decision timeout that does not exist either (explicitly
deferred — see `puerto-rico-session`'s README). Colonists are placed and
lifted by clicking a card's colonist circles, not by dragging.
