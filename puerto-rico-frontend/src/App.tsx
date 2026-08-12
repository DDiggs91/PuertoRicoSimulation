import { useCallback, useEffect, useReducer, useState } from "react";
import { ApiError, client, unwrap } from "./api/client";
import { subscribeToGameEvents } from "./api/events";
import { asPlayerActions } from "./api/types";
import type { PlayerAction } from "./api/types";
import { GameBoard } from "./components/GameBoard";
import { LobbyScreen } from "./components/LobbyScreen";
import { gameReducer, initialGameState } from "./state/gameReducer";
import { loadSeat } from "./state/seatSession";
import type { SeatSession } from "./state/seatSession";

export function App() {
  // A minimal stand-in for routing: the vertical slice has exactly one navigable destination
  // besides the lobby (a specific game), so a query param carries it rather than pulling in a
  // router. Lets a spectator's URL be shared/reopened directly onto a running game.
  const [activeGameId, setActiveGameId] = useState<string | null>(() =>
    new URLSearchParams(window.location.search).get("game"),
  );
  const [state, dispatch] = useReducer(gameReducer, initialGameState);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [disconnected, setDisconnected] = useState(false);
  // Null while spectating. Restored from storage on the `?game=` path, so reopening or reloading
  // a game this browser holds a seat at comes back as a player, not a spectator.
  const [seat, setSeat] = useState<SeatSession | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function startWatching(gameId: string, seated: SeatSession | null) {
    window.history.replaceState(null, "", `?game=${gameId}`);
    setSeat(seated);
    setActiveGameId(gameId);
  }

  function returnToLobby() {
    window.history.replaceState(null, "", window.location.pathname);
    setLoadError(null);
    setDisconnected(false);
    setSeat(null);
    setActiveGameId(null);
  }

  /**
   * Fetches the current board. Used both to bootstrap a spectator opening mid-game — the SSE
   * stream only carries events from the moment it's subscribed — and to resync after a dropped
   * stream, since events emitted during the gap are gone and nothing replays them.
   */
  const loadSnapshot = useCallback(
    async (gameId: string, gameChanged: boolean, isCancelled: () => boolean) => {
      try {
        const view = unwrap(
          await client.GET("/games/{gameId}/state", { params: { path: { gameId } } }),
        );
        if (!isCancelled()) {
          dispatch({ type: "SNAPSHOT_LOADED", view, gameChanged });
          setLoadError(null);
        }
      } catch (error) {
        // An unknown id, a game that exists but was never started (that endpoint 404s), or a
        // network blip all land here. Without this the render below waits on a view that is
        // never coming and the user sees "Loading game…" forever.
        if (!isCancelled()) {
          setLoadError(error instanceof Error ? error.message : String(error));
        }
      }
    },
    [],
  );

  /**
   * Fetches whatever decision the session is currently waiting on. `DECISION_REQUESTED` is emitted
   * once, at the moment the wait starts, so a client that arrives (or reloads) after that never
   * hears about it and would sit on a board with no way to act. A 404 here is the ordinary
   * "nothing pending" answer — a finished game, or the instant between two decisions — not an
   * error worth showing anyone.
   */
  const loadPendingDecision = useCallback(async (gameId: string, isCancelled: () => boolean) => {
    try {
      const decision = unwrap(
        await client.GET("/games/{gameId}/decision", { params: { path: { gameId } } }),
      );
      if (!isCancelled()) {
        dispatch({
          type: "DECISION_LOADED",
          pending: {
            seat: decision.seat,
            requestId: decision.requestId,
            options: asPlayerActions(decision.options),
            // The decision's own board, not the one /state returned a moment earlier — a game with
            // AI seats moves between the two calls.
            state: decision.view.state,
          },
        });
      }
    } catch {
      if (!isCancelled()) {
        dispatch({ type: "DECISION_LOADED", pending: null });
      }
    }
  }, []);

  useEffect(() => {
    if (!activeGameId) {
      return;
    }
    let cancelled = false;
    setLoadError(null);
    setDisconnected(false);
    // A reload arrives with no React state at all, so the seat has to come back from storage
    // before anything decides whether this client can act.
    setSeat((current) => current ?? loadSeat(activeGameId));
    void loadSnapshot(activeGameId, true, () => cancelled).then(() => {
      if (!cancelled) {
        return loadPendingDecision(activeGameId, () => cancelled);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [activeGameId, loadSnapshot, loadPendingDecision]);

  useEffect(() => {
    if (!activeGameId) {
      return;
    }
    let cancelled = false;
    const unsubscribe = subscribeToGameEvents(activeGameId, {
      onEvent: dispatch,
      onConnectionChange: (connected) => {
        setDisconnected(!connected);
        if (connected) {
          // Recovered: whatever happened while the stream was down is only recoverable from
          // /state, and the decision now pending was announced during the gap.
          void loadSnapshot(activeGameId, false, () => cancelled).then(() => {
            if (!cancelled) {
              return loadPendingDecision(activeGameId, () => cancelled);
            }
          });
        }
      },
    });
    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [activeGameId, loadSnapshot, loadPendingDecision]);

  /**
   * Offers a move for this client's seat. A 202 is not confirmation the move landed — the session
   * applies it on its own thread — so nothing is updated optimistically here; the resulting
   * `ACTION_APPLIED` over SSE is what moves the board, and clearing `submitting` on the way out
   * just re-enables the buttons for whatever comes next.
   */
  async function submitMove(action: PlayerAction) {
    if (!activeGameId || !seat || !state.pending) {
      return;
    }
    setSubmitting(true);
    try {
      unwrap(
        await client.POST("/games/{gameId}/moves", {
          params: {
            path: { gameId: activeGameId },
            header: { "X-Seat-Token": seat.token },
          },
          body: { requestId: state.pending.requestId, action },
        }),
      );
    } catch (error) {
      // 400 (the decision moved on, or the action is no longer legal), 403 (not this client's
      // seat) and 404 all arrive as a Problem; the panel says so rather than failing silently.
      const detail =
        error instanceof ApiError
          ? (error.problem.detail ?? error.problem.title)
          : "Could not send that move.";
      dispatch({ type: "MOVE_FAILED", detail });
    } finally {
      setSubmitting(false);
    }
  }

  if (!activeGameId) {
    return <LobbyScreen onWatchGame={startWatching} />;
  }

  if (loadError) {
    return (
      <div className="screen screen--message">
        <p role="alert" data-testid="load-error">
          Could not load that game: {loadError}
        </p>
        <button
          type="button"
          className="button"
          data-testid="back-to-lobby"
          onClick={returnToLobby}
        >
          Back to the lobby
        </button>
      </div>
    );
  }

  if (!state.view) {
    return (
      <div className="screen screen--message">
        <p role="status">Loading game…</p>
      </div>
    );
  }

  return (
    <div className="screen">
      <header className="topbar">
        <button
          type="button"
          className="button"
          data-testid="back-to-lobby"
          onClick={returnToLobby}
        >
          Back to the lobby
        </button>
        {seat && (
          <span className="topbar__seat" data-testid="seated-as">
            Seated as {seat.name} (seat {seat.seat})
          </span>
        )}
        {disconnected && (
          <p className="topbar__warning" role="status" data-testid="connection-lost">
            Live updates disconnected — reconnecting.
          </p>
        )}
      </header>

      <GameBoard
        view={state.view}
        events={state.events}
        standings={state.standings}
        failure={state.failure}
        mySeat={seat?.seat ?? null}
        pending={state.pending}
        moveError={state.moveError}
        submitting={submitting}
        onChoose={submitMove}
      />
    </div>
  );
}
