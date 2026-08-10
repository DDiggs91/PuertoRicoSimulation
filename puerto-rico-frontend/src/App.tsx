import { useEffect, useReducer, useState } from "react";
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

  function startWatching(gameId: string) {
    window.history.replaceState(null, "", `?game=${gameId}`);
    setActiveGameId(gameId);
  }

  // Bootstraps a spectator opening mid-game: the SSE stream only carries events from the
  // moment it's subscribed, so /state fills in whatever already happened.
  useEffect(() => {
    if (!activeGameId) {
      return;
    }
    let cancelled = false;
    (async () => {
      const view = unwrap(
        await client.GET("/games/{gameId}/state", { params: { path: { gameId: activeGameId } } }),
      );
      if (!cancelled) {
        dispatch({ type: "SNAPSHOT_LOADED", view });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [activeGameId]);

  useEffect(() => {
    if (!activeGameId) {
      return;
    }
    return subscribeToGameEvents(activeGameId, dispatch);
  }, [activeGameId]);

  if (!activeGameId) {
    return <LobbyScreen onGameStarted={startWatching} />;
  }

  if (!state.view) {
    return <p role="status">Loading game…</p>;
  }

  return <GameBoard view={state.view} events={state.events} standings={state.standings} />;
}
