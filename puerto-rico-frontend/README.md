# puerto-rico-frontend — Web Frontend (browser client)

See [docs/architecture.md](../docs/architecture.md) for how this module fits
into the whole system.

## Intent

Renders a live spectator view of a running game — role/phase status, every
player's board, the tile/trading-house summary, a running event log, and
final standings — plus the lobby screen used to create a table and seat AI
opponents into it. The same client serves both "I am playing" and "I am
only watching AIs play"; this pass covers the watching half in full (see
Status).

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

| Path                       | Holds                                                                                                |
| -------------------------- | ---------------------------------------------------------------------------------------------------- |
| `src/api/schema.d.ts`      | **Generated**, gitignored — run `npm run generate:api`                                               |
| `src/api/types.ts`         | Hand-written union aliases for `PlayerAction`/`SessionEvent` (see below) plus re-exports of the rest |
| `src/api/client.ts`        | The configured `openapi-fetch` client, plus `unwrap`/`ApiError`                                      |
| `src/api/events.ts`        | `EventSource` subscription → typed `SessionEvent`                                                    |
| `src/state/gameReducer.ts` | Pure `(state, event) => state` — the event-sourced UI state                                          |
| `src/components/`          | `LobbyScreen`, `GameBoard`, `CentralBoard`, `PlayerBoard`, `EventLog`                                |
| `src/App.tsx`              | `?game=` routing, the `/state` bootstrap and resync, and the SSE subscription lifecycle              |
| `src/main.tsx`             | The React entry point — mounts `App` and nothing else                                                |
| `src/test/`                | `setup.ts` (jest-dom matchers) and `fixtures.ts` (minimal-but-valid `GameStateView` builders)        |
| `e2e/`                     | Playwright specs                                                                                     |

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

**No client-side move-submission UI yet.** `LobbyScreen` seats AI engines
only — seating a human produces a seat with no way to act, since nothing
here calls `POST /moves` yet. That's the deliberately deferred half of this
module's scope; the backend (`HumanActor`, seat tokens, the moves endpoint)
is already built and tested in `puerto-rico-web` waiting for it.

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

The lobby screen (create/list/seat AI/start) and the live spectator board
(phase, per-player boards, event log, final standings) are built and
tested — unit, component, and end-to-end. Framework/toolchain choice is
settled (this stack). Not built yet: the click-to-move interaction UI for
human players, and the frontend half of reconnect/timeout handling (both
explicitly deferred — see `puerto-rico-web`'s README for what already
exists server-side to build it on).
