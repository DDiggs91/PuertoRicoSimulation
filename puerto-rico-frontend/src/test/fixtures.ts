import type { GameStateView, GameView, PlayerStateView } from "../api/types";

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
