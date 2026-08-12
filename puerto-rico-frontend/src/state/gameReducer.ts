import { asPlayerActions } from "../api/types";
import type {
  GameStateView,
  GameView,
  PlayerAction,
  ScoreBreakdown,
  SessionEvent,
} from "../api/types";

/**
 * A decision the session is waiting on. Only ever populated from the server's own legal-action
 * list, never assembled client-side — see `ActionPanel`, which builds every button out of
 * `options`, so an action the server would refuse is not something the UI can offer.
 *
 * Carries the board those options were computed against, not just the options. A legal-action list
 * only means anything alongside the phase it came from: pair it with a board from a different
 * moment — `/state` and `/decision` are two calls, and a resync can land between two events — and
 * the panel draws a picker for the wrong phase, which has nothing to offer and leaves the player
 * staring at an empty panel with no way to move. Both `DecisionRequestedEvent` and `Decision`
 * carry their own view for exactly this reason, so it costs nothing to keep the pair together.
 */
export interface PendingDecision {
  seat: number;
  requestId: number;
  options: PlayerAction[];
  /** The board the options are legal on. */
  state: GameStateView;
}

export interface GameUiState {
  view: GameView | null;
  events: SessionEvent[];
  standings: ScoreBreakdown[] | null;
  failure: string | null;
  pending: PendingDecision | null;
  /** Set when a submitted move came back refused; cleared the moment the game moves on. */
  moveError: string | null;
}

export const initialGameState: GameUiState = {
  view: null,
  events: [],
  standings: null,
  failure: null,
  pending: null,
  moveError: null,
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

/**
 * The `GET /decision` counterpart to `SNAPSHOT_LOADED`. `DECISION_REQUESTED` fires once, over SSE,
 * at the moment the session starts waiting — so a client that connects (or reloads) after that
 * moment never sees it and would sit on a board with no way to act. Fetching the pending decision
 * on bootstrap closes exactly that window.
 */
export interface DecisionLoaded {
  type: "DECISION_LOADED";
  pending: PendingDecision | null;
}

/** A submitted move the server refused. Display-only; the decision itself is still pending. */
export interface MoveFailed {
  type: "MOVE_FAILED";
  detail: string;
}

export type GameUiAction = SessionEvent | SnapshotLoaded | DecisionLoaded | MoveFailed;

export function gameReducer(state: GameUiState, action: GameUiAction): GameUiState {
  if (action.type === "SNAPSHOT_LOADED") {
    // A snapshot is either the initial bootstrap or a resync after the stream dropped. Either way
    // it re-establishes the board; the log, standings and any pending decision belong to whatever
    // game it describes, so switching games must not carry the previous one's over.
    return action.gameChanged
      ? { ...initialGameState, view: action.view }
      : { ...state, view: action.view };
  }

  if (action.type === "DECISION_LOADED") {
    return { ...state, pending: action.pending };
  }

  if (action.type === "MOVE_FAILED") {
    return { ...state, moveError: action.detail };
  }

  const appended = [...state.events, action];
  const events =
    appended.length > MAX_RETAINED_EVENTS ? appended.slice(-MAX_RETAINED_EVENTS) : appended;

  switch (action.type) {
    case "GAME_STARTED":
      return { ...state, view: action.view, events };
    case "DECISION_REQUESTED":
      // The event already carries everything a picker needs, so a live client never has to fetch
      // the decision it was just told about.
      return {
        ...state,
        view: action.view,
        events,
        pending: {
          seat: action.seat,
          requestId: action.requestId,
          options: asPlayerActions(action.options),
          state: action.view.state,
        },
        moveError: null,
      };
    case "ACTION_APPLIED":
    case "ACTION_REJECTED":
      // Whatever was pending has been answered (or refused outright); the session emits the next
      // DECISION_REQUESTED immediately after, which is what re-populates `pending`. Clearing here
      // rather than waiting for that is what stops a picker from briefly offering a stale
      // requestId the server would reject.
      return { ...state, view: action.view, events, pending: null, moveError: null };
    case "GAME_ENDED":
      return {
        ...state,
        view: action.view,
        standings: action.standings,
        events,
        pending: null,
        moveError: null,
      };
    case "SESSION_FAILED":
      return {
        ...state,
        view: action.view,
        failure: action.detail,
        events,
        pending: null,
        moveError: null,
      };
    default: {
      const exhaustive: never = action;
      throw new Error(`Unhandled SessionEvent: ${JSON.stringify(exhaustive)}`);
    }
  }
}
