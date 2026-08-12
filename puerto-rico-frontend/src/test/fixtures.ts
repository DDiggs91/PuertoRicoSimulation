import type {
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
    buildings: [],
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

export function makeState(overrides: Partial<GameStateView> = {}): GameStateView {
  return {
    config: { playerNames: ["Ana", "Bo", "Coco"] },
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
