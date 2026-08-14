# puerto-rico-contract — Wire Contract (OpenAPI)

See [docs/architecture.md](../docs/architecture.md) for how this module fits
into the whole system.

## Intent

Holds one file — [`puerto-rico.yaml`](src/main/resources/openapi/puerto-rico.yaml)
— and generates the Java side of it. Nothing in this module is hand-written
except that spec: it exists specifically so `puerto-rico-web` and
`puerto-rico-frontend` generate their wire types from one shared source of
truth instead of either side hand-maintaining DTOs that can silently drift
apart.

## Contracts

Defines the **Lobby**, **Game**, and **Ai** operations `puerto-rico-web`
exposes: create/list/join a game, seat a human or an AI engine, start a
game, read state, submit a move, and stream live events. Named per
`docs/architecture.md`'s Lobby, Event/broadcast, and Command/Query
contracts, all in one file because they're all part of the same wire
boundary a browser client talks to.

**Numbers the client displays travel over the wire, computed.** `Phase`
carries `buildOptions` and `goodPrices` in the builder and trader phases, and
`PlacedBuilding` carries its printed `capacity` and `victoryPoints`. Those
could all be re-derived client-side from a copy of the building table and the
discount rules — which is exactly the point of putting them here instead. A
build cost is the builder's privilege and the quarry discount applied
(`GameEngine.buildCost`), a sale price is the trader's privilege and market
bonuses (`GameEngine.sellPrice`); both are rules, and rules live in
`puerto-rico-model`. Both lists are *priced, not filtered*: every building
and every good appears, affordable or not, because which of them may actually
be chosen is `Decision.options`' job.

## Depends on

Nothing in this reactor. It has no Java of its own beyond what
`openapi-generator-maven-plugin` produces at build time into
`target/generated-sources/openapi`.

## Java generation

`openapi-generator-maven-plugin`, generator `spring`, `interfaceOnly=true`:
emits DTO classes (records-flavored via Jackson annotations, not actual
Java records — see below) plus one `@RequestMapping`-annotated interface
per tag (`LobbyApi`, `GameApi`, `AiApi`). `puerto-rico-web`'s controllers
`implement` those interfaces directly, so editing the spec without updating
a controller **fails the build at compile time** rather than surfacing as a
runtime surprise — that's the entire reason this is a generator step and
not a hand-drawn diagram.

**`PlayerAction` and `SessionEvent` are discriminated hierarchies**, one
schema per sealed variant (`SelectRoleAction`, `GameStartedEvent`, etc.),
using classic OpenAPI `discriminator` + `allOf` inheritance rather than a
top-level `oneOf` list. That choice was deliberate and specifically
Java-friendly: openapi-generator turns it into a concrete base class per
hierarchy with `@JsonTypeInfo`/`@JsonSubTypes` already wired, and each
variant as a subclass — full polymorphic (de)serialization with zero manual
Jackson configuration. `Phase`, by contrast, is flattened into one schema
with optional fields: the client only ever *renders* a `Phase`, never
constructs one, so a discriminated hierarchy would add machinery without
adding safety.

**`GameConfigView` omits `seed`, `TileSupplyView` omits the draw/discard
piles.** Both are the base game's only hidden information — the whole
reason `puerto-rico-session`'s `GameView` redacts them — and the wire
schema simply has no field to leak them into, independent of whatever the
mapper in `puerto-rico-web` does.

**One exception:** `GET /games/{gameId}/events` (Server-Sent Events) is
tagged `GameStream` — its own tag, generating a `GameStreamApi` interface
that nothing implements. `SseEmitter`, what an actual streaming controller
method must return, has no natural OpenAPI response shape; `GameController`
implements this route by hand at the same path instead. The operation stays
in the spec purely for documentation and for `puerto-rico-frontend`'s
TypeScript generation.

## TypeScript generation

`puerto-rico-frontend` runs `openapi-typescript` against this same YAML
file (`npm run generate:api`) to produce `src/api/schema.d.ts` — gitignored,
regenerated on every build, never hand-edited. See that module's README for
the one place the two generators' output doesn't perfectly match: a small
hand-written union type is needed to turn the individual
`PlayerAction`/`SessionEvent` variant types into a real discriminated
union, since `openapi-typescript` only synthesizes that automatically from
`oneOf`, not from `allOf` + `discriminator`.

## Building

```bash
./mvnw -pl puerto-rico-contract verify   # regenerates + compiles the Java side
```

There's nothing to hand-test here — `puerto-rico-web`'s `WireMappingTest`
and `puerto-rico-frontend`'s Vitest suite are what actually exercise the
generated types.
