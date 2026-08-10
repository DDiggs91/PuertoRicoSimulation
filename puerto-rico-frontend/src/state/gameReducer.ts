import type { GameView, ScoreBreakdown, SessionEvent } from "../api/types";

export interface GameUiState {
  view: GameView | null;
  events: SessionEvent[];
  standings: ScoreBreakdown[] | null;
  failure: string | null;
}

export const initialGameState: GameUiState = {
  view: null,
  events: [],
  standings: null,
  failure: null,
};

/**
 * A client-local counterpart to the initial `GET /state` fetch: not something the session ever
 * broadcasts, but needed so a spectator opening mid-game has a board to render before the next
 * SessionEvent arrives over SSE. Deliberately not recorded into `events` — it's a bootstrap, not
 * something that happened in the game.
 */
export interface SnapshotLoaded {
  type: "SNAPSHOT_LOADED";
  view: GameView;
}

export type GameUiAction = SessionEvent | SnapshotLoaded;

export function gameReducer(state: GameUiState, action: GameUiAction): GameUiState {
  if (action.type === "SNAPSHOT_LOADED") {
    return { ...state, view: action.view };
  }

  const events = [...state.events, action];

  switch (action.type) {
    case "GAME_STARTED":
    case "DECISION_REQUESTED":
    case "ACTION_APPLIED":
    case "ACTION_REJECTED":
      return { ...state, view: action.view, events };
    case "GAME_ENDED":
      return { ...state, view: action.view, standings: action.standings, events };
    case "SESSION_FAILED":
      return { ...state, view: action.view, failure: action.detail, events };
    default: {
      const exhaustive: never = action;
      throw new Error(`Unhandled SessionEvent: ${JSON.stringify(exhaustive)}`);
    }
  }
}
