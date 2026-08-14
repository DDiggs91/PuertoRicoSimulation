import type {
  BuildingCatalogEntry,
  BuildingType,
  GameStateView,
  GameView,
  PlacedBuilding,
  PlayerAction,
  PlayerStateView,
} from "../api/types";
import type { PendingDecision } from "../state/gameReducer";

/** Minimal-but-valid fixtures for tests that don't care about full board fidelity. */

export function makePlayer(overrides: Partial<PlayerStateView> = {}): PlayerStateView {
  return {
    seat: 0,
    name: "Ana",
    doubloons: 0,
    victoryPoints: 0,
    island: [],
    islandSpaces: 12,
    buildings: [],
    citySpaces: 12,
    citySpacesUsed: 0,
    colonistsInSanJuan: 0,
    goods: {},
    ...overrides,
  };
}

/**
 * A placed building with the card numbers the wire carries. Defaults are deliberately
 * unremarkable — a test that cares about capacity or victory points states them.
 */
export function makeBuilding(
  type: BuildingType,
  overrides: Partial<PlacedBuilding> = {},
): PlacedBuilding {
  return { type, colonists: 0, capacity: 1, victoryPoints: 1, ...overrides };
}

/**
 * A catalog entry with the card's printed numbers. Empty by default in `makeState` — a test that
 * renders the building display names the handful of buildings it cares about.
 */
export function makeCatalogEntry(
  type: BuildingType,
  overrides: Partial<BuildingCatalogEntry> = {},
): BuildingCatalogEntry {
  return { type, cost: 1, victoryPoints: 1, colonistCapacity: 1, copies: 2, ...overrides };
}

export function makeState(overrides: Partial<GameStateView> = {}): GameStateView {
  return {
    config: { playerNames: ["Ana", "Bo", "Coco"], buildingCatalog: [] },
    players: [
      makePlayer({ seat: 0, name: "Ana" }),
      makePlayer({ seat: 1, name: "Bo" }),
      makePlayer({ seat: 2, name: "Coco" }),
    ],
    governorSeat: 0,
    roles: { cards: [] },
    tiles: { faceUp: [], quarriesRemaining: 8, faceDownCount: 42, discardedCount: 0 },
    goods: {},
    buildings: {},
    tradingHouse: { goods: [] },
    ships: [],
    colonistSupply: 55,
    colonistsOnShip: 3,
    victoryPointSupply: 75,
    phase: { type: "ROLE_SELECTION", actorSeat: 0 },
    finalRound: false,
    ...overrides,
  };
}

export function makeView(overrides: Partial<GameView> = {}): GameView {
  return { state: makeState(), ...overrides };
}

/**
 * A decision awaiting this seat, with whatever options the test wants offered. Carries a board
 * like the real thing does — the options and the phase they are legal in travel together.
 */
export function makePending(
  options: PlayerAction[],
  overrides: Partial<PendingDecision> = {},
): PendingDecision {
  return { seat: 0, requestId: 1, options, state: makeState(), ...overrides };
}
