# puerto-rico-model — Game Engine (Rules Core)

See [docs/architecture.md](../docs/architecture.md) for how this module fits
into the whole system.

## Intent

Owns game state, turn/phase sequencing, role-card resolution, scoring, and
end-condition detection for a single game of Puerto Rico. This is pure
domain logic: no I/O, no knowledge of the web layer, and no concept of
"human" vs. "AI" — it's the single source of truth for whether a move is
legal and what happens as a result.

Supports the base game at **3–5 players**, implemented against
[docs/game-rules.md](../docs/game-rules.md), which is derived from the
rulebook in [docs/puerto-rico-rules-en.pdf](../docs/puerto-rico-rules-en.pdf).

## Contracts

Exposes the **Command/Query contract** consumed by `puerto-rico-session`.
Three entry points, all pure functions:

```java
GameState          state   = GameSetup.create(new GameConfig(names, seed));
List<PlayerAction> options = GameEngine.legalActions(state);
ActionResult       result  = GameEngine.apply(state, action);
```

`ActionResult` is a sealed `Accepted(GameState) | Rejected(RejectionReason,
String)`. An illegal move is **always a return value, never an exception**,
so the layer above can turn it into a client error without catching anything.

`GameState` is an immutable record tree. `apply` never touches its argument,
so callers get snapshots, replay, and lookahead search for free — no
defensive copying. Games are reproducible from their `GameConfig` seed.

Two read-only companions price what those actions would cost, for the two
phases where a price is a rule rather than a printed number:

```java
int cost  = GameEngine.buildCost(state, BuildingType.HARBOR);  // builder phase only
int price = GameEngine.sellPrice(state, Good.COFFEE);          // trader phase only
```

They exist so a display can show the number without restating the rule —
`buildCost` applies the builder's privilege and the quarry discount,
`sellPrice` the trader's privilege and any market bonuses. Both price the
move without asserting it is legal; `legalActions` remains the only answer to
that. Called outside their own phase, where no such price exists, they throw
`IllegalStateException`.

One more public surface sits alongside those three: `RandomPlay.choose`,
which picks uniformly at random from a list of legal actions. It ships here
rather than in `puerto-rico-ai` because it is not really a strategy — it is
the reference way to walk the action space, used both by this module's own
fuzz tests and by `puerto-rico-ai`'s `RandomAi`. Keeping one copy means the
tests and the shipped engine cannot drift apart; `puerto-rico-ai`'s README
makes the same argument from the other side.

This module does not know about actors, decisions, sessions, or lobbies —
those concerns live upstream in `puerto-rico-session`.

## Depends on

Nothing else in this reactor. Everything else depends on this module,
directly or transitively.

## Packages

| Package | Holds |
|---|---|
| `com.PRS.model.goods` | `Good` (price, supply), `GoodsSupply`, `TradingHouse` |
| `com.PRS.model.buildings` | `BuildingType` (the 23 building cards), `BuildingCategory`, `PlacedBuilding`, `BuildingSupply` |
| `com.PRS.model.boards` | `TileType`, `IslandTile`, `PlayerState`, `TileSupply`, `CargoShip` |
| `com.PRS.model.rolecards` | `Role`, `RoleCard`, `RoleTrack` |
| `com.PRS.model.game` | `GameConfig`, `SetupTable`, `GameSetup`, `GameState`, `Phase` |
| `com.PRS.model.actions` | `PlayerAction` (sealed, one record per decision), `ColonistSlot` |
| `com.PRS.model.engine` | `GameEngine`, `ActionResult`, `RejectionReason`, `RandomPlay`, and one package-private handler per phase |
| `com.PRS.model.scoring` | `Scorer`, `ScoreBreakdown` |

## Design notes

**Only real decisions are actions.** Steps the rules leave no discretion
over are resolved by the engine rather than surfaced: goods production,
dealing colonists off the ship, the trader emptying a full trading house,
the captain unloading full ships, and the settler redealing the face-up row.
`legalActions` is therefore never empty until the game is over.

**`Phase` is a sealed interface**, one variant per phase, each carrying
exactly the transient state that phase needs (whose turn, who has used a
Wharf, whether the captain's bonus is spent). Nothing has to be inferred.

**Colonist placement lifts before it places.** The rules let a player
rearrange colonists already on their board. Modelling that as free
circle-to-circle moves lets a player shuffle forever, so the mayor phase
need never end. Instead a player's colonists are all recalled to San Juan
when their turn begins and placed from scratch: the same end positions are
reachable, but every action strictly drains San Juan, so the phase always
terminates.

**Three known simplifications**, all cases where the rules leave a choice
that is never rationally made either way:

- The mayor's extra colonist is taken automatically whenever the supply
  holds one. Declining could in principle delay the colonist-supply end
  trigger by a round.
- The prospector's doubloon is taken automatically.
- With two prospector cards in play (five players), the chooser is given
  whichever carries more doubloons rather than being asked
  (`RoleTrack.indexToTake`). The cards are otherwise identical, so the
  richer one is strictly better.

The Wharf is *not* on this list: the rulebook makes using it optional even
though loading is otherwise compulsory, and the engine honours that — a
player whose only remaining option is the Wharf is offered
`PlayerAction.DeclineWharf`. Declining when a cargo ship would still take
something is refused with `LOADING_IS_MANDATORY`.

**Where a tile or building sits does not matter.** Island tiles are an
unordered list capped at 12, and city space is tracked as a count — the
rules say placement position is irrelevant, buildings may be shuffled to
make room for a large one, and nothing is ever removed, so a large
building needs only `freeCitySpaces >= 2` rather than a modelled grid.

## Testing

TestNG with AssertJ assertions, mirroring the main package structure. Rules
tables (setup values per player count, the building catalogue, prices, the
Factory and Residence tables) are asserted row by row with `@DataProvider`.
`com.PRS.model.TestGames` builds boards in a known state without playing a
game out to reach them.

```bash
./mvnw -pl puerto-rico-model verify
./mvnw -pl puerto-rico-model test -Dtest=CaptainPhaseTest
```

`GameEngineContractTest` plays complete games by choosing uniformly at
random from `legalActions`, which checks that every offered action is
accepted, that no phase can stall, and that all three end conditions
actually fire.
