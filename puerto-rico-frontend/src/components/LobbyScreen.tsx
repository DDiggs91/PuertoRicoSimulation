import { useCallback, useEffect, useState } from "react";
import { ApiError, client, unwrap } from "../api/client";
import type { GameTableSummary } from "../api/types";

export interface LobbyScreenProps {
  /**
   * Opens a game's spectator view. Called both for a game this tab just started and for one picked
   * out of the list, which is why it isn't named for starting.
   */
  onWatchGame: (gameId: string) => void;
}

const MAX_SEATS = 5;
const MIN_SEATS_TO_START = 3;

/** Other tabs seat players and start games; without polling none of that is ever visible here. */
const REFRESH_INTERVAL_MS = 3000;

export function LobbyScreen({ onWatchGame }: LobbyScreenProps) {
  const [games, setGames] = useState<GameTableSummary[]>([]);
  const [activeGameId, setActiveGameId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refreshGames = useCallback(async () => {
    try {
      setGames(unwrap(await client.GET("/games")));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Failed to load games.");
    }
  }, []);

  useEffect(() => {
    void refreshGames();
    const timer = setInterval(() => void refreshGames(), REFRESH_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [refreshGames]);

  const activeGame = games.find((g) => g.id === activeGameId) ?? null;
  const seatCount = activeGame?.seats.length ?? 0;

  async function createGame() {
    try {
      const { gameId } = unwrap(await client.POST("/games"));
      setActiveGameId(gameId);
      await refreshGames();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Failed to create a game.");
    }
  }

  async function addAiSeat() {
    if (!activeGameId) {
      return;
    }
    try {
      unwrap(
        await client.POST("/games/{gameId}/seats", {
          params: { path: { gameId: activeGameId } },
          body: { name: `Bot ${seatCount + 1}`, kind: "AI", engineId: "random" },
        }),
      );
      await refreshGames();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Failed to seat an AI.");
    }
  }

  async function startGame() {
    if (!activeGameId) {
      return;
    }
    try {
      unwrap(
        await client.POST("/games/{gameId}/start", {
          params: { path: { gameId: activeGameId } },
        }),
      );
      onWatchGame(activeGameId);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Failed to start the game.");
    }
  }

  return (
    <div>
      {error && <p role="alert">{error}</p>}

      <button data-testid="lobby-create-game" onClick={createGame}>
        Create game
      </button>

      <ul data-testid="game-list" aria-label="Games">
        {games.map((game) => (
          <li key={game.id} data-testid={`game-list-item-${game.id}`}>
            {/* A started game is watchable by anyone; before this the only way in was to be handed
                its ?game= URL by whoever created it. */}
            {game.status === "OPEN" ? (
              <span>
                {game.id} — {game.status} — {game.seats.length} seated
              </span>
            ) : (
              <button
                type="button"
                data-testid={`watch-game-${game.id}`}
                onClick={() => onWatchGame(game.id)}
              >
                Watch {game.id} — {game.status} — {game.seats.length} seated
              </button>
            )}
          </li>
        ))}
      </ul>

      {activeGameId && (
        <div data-testid="active-game-panel">
          <button data-testid="add-ai-seat" onClick={addAiSeat} disabled={seatCount >= MAX_SEATS}>
            Seat a random AI
          </button>
          <button
            data-testid="start-game"
            onClick={startGame}
            disabled={seatCount < MIN_SEATS_TO_START}
          >
            Start game
          </button>
        </div>
      )}
    </div>
  );
}
