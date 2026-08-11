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
 * How many log entries are kept. A full five-player game emits thousands of events; retaining and
 * rendering all of them grows the DOM without bound and gives a screen reader an ever-longer live
 * region to work through. The board itself always comes from the latest view, never from the log,
 * so dropping the oldest entries loses nothing but scrollback.
 */
export const MAX_RETAINED_EVENTS = 200;

/**
 * A client-local counterpart to the initial `GET /state` fetch: not something the session ever
 * broadcasts, but needed so a spectator opening mid-game has a board to render before the next
 * SessionEvent arrives over SSE. Deliberately not recorded into `events` — it's a bootstrap, not
 * something that happened in the game.
 */
export interface SnapshotLoaded {
  type: "SNAPSHOT_LOADED";
  view: GameView;
  /** True when this snapshot is for a different game, so the log and standings must be cleared. */
  gameChanged?: boolean;
}

export type GameUiAction = SessionEvent | SnapshotLoaded;

export function gameReducer(state: GameUiState, action: GameUiAction): GameUiState {
  if (action.type === "SNAPSHOT_LOADED") {
    // A snapshot is either the initial bootstrap or a resync after the stream dropped. Either way
    // it re-establishes the board; the log and standings belong to whatever game it describes, so
    // switching games must not carry the previous one's over.
    return action.gameChanged
      ? { ...initialGameState, view: action.view }
      : { ...state, view: action.view };
  }

  const appended = [...state.events, action];
  const events =
    appended.length > MAX_RETAINED_EVENTS ? appended.slice(-MAX_RETAINED_EVENTS) : appended;

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
