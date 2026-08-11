import { useCallback, useEffect, useReducer, useState } from "react";
import { client, unwrap } from "./api/client";
import { subscribeToGameEvents } from "./api/events";
import { GameBoard } from "./components/GameBoard";
import { LobbyScreen } from "./components/LobbyScreen";
import { gameReducer, initialGameState } from "./state/gameReducer";

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

  function startWatching(gameId: string) {
    window.history.replaceState(null, "", `?game=${gameId}`);
    setActiveGameId(gameId);
  }

  function returnToLobby() {
    window.history.replaceState(null, "", window.location.pathname);
    setLoadError(null);
    setDisconnected(false);
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

  useEffect(() => {
    if (!activeGameId) {
      return;
    }
    let cancelled = false;
    setLoadError(null);
    setDisconnected(false);
    void loadSnapshot(activeGameId, true, () => cancelled);
    return () => {
      cancelled = true;
    };
  }, [activeGameId, loadSnapshot]);

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
          // Recovered: whatever happened while the stream was down is only recoverable from /state.
          void loadSnapshot(activeGameId, false, () => cancelled);
        }
      },
    });
    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [activeGameId, loadSnapshot]);

  if (!activeGameId) {
    return <LobbyScreen onWatchGame={startWatching} />;
  }

  if (loadError) {
    return (
      <div>
        <p role="alert" data-testid="load-error">
          Could not load that game: {loadError}
        </p>
        <button type="button" data-testid="back-to-lobby" onClick={returnToLobby}>
          Back to the lobby
        </button>
      </div>
    );
  }

  if (!state.view) {
    return <p role="status">Loading game…</p>;
  }

  return (
    <>
      {disconnected && (
        <p role="status" data-testid="connection-lost">
          Live updates disconnected — reconnecting.
        </p>
      )}
      <button type="button" data-testid="back-to-lobby" onClick={returnToLobby}>
        Back to the lobby
      </button>
      <GameBoard
        view={state.view}
        events={state.events}
        standings={state.standings}
        failure={state.failure}
      />
    </>
  );
}
