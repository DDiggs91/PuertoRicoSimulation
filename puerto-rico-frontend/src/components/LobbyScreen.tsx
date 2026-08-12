import { useCallback, useEffect, useState } from "react";
import { ApiError, client, unwrap } from "../api/client";
import type { GameTableSummary } from "../api/types";
import { loadSeat, saveSeat } from "../state/seatSession";
import type { SeatSession } from "../state/seatSession";

export interface LobbyScreenProps {
  /**
   * Opens a game's view. Called both for a game this tab just started and for one picked out of
   * the list, which is why it isn't named for starting. The seat is passed alongside: null means
   * "watch this", anything else means "play it as that seat".
   */
  onWatchGame: (gameId: string, seat: SeatSession | null) => void;
}

const MAX_SEATS = 5;
const MIN_SEATS_TO_START = 3;

/** Other tabs seat players and start games; without polling none of that is ever visible here. */
const REFRESH_INTERVAL_MS = 3000;

export function LobbyScreen({ onWatchGame }: LobbyScreenProps) {
  const [games, setGames] = useState<GameTableSummary[]>([]);
  const [activeGameId, setActiveGameId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [playerName, setPlayerName] = useState("You");
  // The seat this browser holds at the table being set up, if any. Read back from storage rather
  // than tracked only here, so the button stays correct across a reload of the lobby itself.
  const [mySeat, setMySeat] = useState<SeatSession | null>(null);
  // Seating is one request at a time. Two in flight together each read the table before the other
  // has landed, so both would number their bot the same — and an impatient double-click on "Take a
  // seat" would claim two seats, the second of whose tokens replaces the first in storage.
  const [seating, setSeating] = useState(false);

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
      setMySeat(loadSeat(gameId));
      await refreshGames();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Failed to create a game.");
    }
  }

  /**
   * Seats this browser as a human. The response carries the one and only copy of the seat token
   * the server will ever hand out, so it goes straight to storage — losing it means losing the
   * ability to act, with no way to ask for it again.
   */
  async function takeSeat() {
    if (!activeGameId || seating) {
      return;
    }
    const name = playerName.trim() || "You";
    setSeating(true);
    try {
      const response = unwrap(
        await client.POST("/games/{gameId}/seats", {
          params: { path: { gameId: activeGameId } },
          body: { name, kind: "HUMAN" },
        }),
      );
      if (!response.seatToken) {
        // The server only omits this for an AI seat, so reaching it means the contract changed
        // underneath us — better to say so than to seat a player who silently cannot move.
        setError("That seat came back without a token, so it cannot be played.");
        return;
      }
      const session: SeatSession = {
        gameId: activeGameId,
        seat: response.seatIndex,
        token: response.seatToken,
        name,
      };
      saveSeat(session);
      setMySeat(session);
      await refreshGames();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Failed to take a seat.");
    } finally {
      setSeating(false);
    }
  }

  async function addAiSeat() {
    if (!activeGameId || seating) {
      return;
    }
    setSeating(true);
    try {
      // Numbered from the table's live seat count, not from the polled list, which lags behind by
      // up to a refresh interval and would name two different bots the same.
      const table = unwrap(
        await client.GET("/games/{gameId}", { params: { path: { gameId: activeGameId } } }),
      );
      unwrap(
        await client.POST("/games/{gameId}/seats", {
          params: { path: { gameId: activeGameId } },
          body: { name: `Bot ${table.seats.length + 1}`, kind: "AI", engineId: "random" },
        }),
      );
      await refreshGames();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Failed to seat an AI.");
    } finally {
      setSeating(false);
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
      onWatchGame(activeGameId, mySeat);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Failed to start the game.");
    }
  }

  return (
    <div className="screen lobby">
      <header className="lobby__header">
        <h1 className="lobby__title">Puerto Rico</h1>
        <p className="lobby__subtitle">Take a seat, or watch the colonists work.</p>
      </header>

      {error && (
        <p className="notice notice--alert" role="alert">
          {error}
        </p>
      )}

      <section className="panel lobby__panel">
        <button
          type="button"
          className="button button--primary"
          data-testid="lobby-create-game"
          onClick={createGame}
        >
          Create game
        </button>

        <ul className="lobby__games" data-testid="game-list" aria-label="Games">
          {games.map((game) => (
            <li className="lobby__game" key={game.id} data-testid={`game-list-item-${game.id}`}>
              {/* A started game is watchable by anyone; before this the only way in was to be handed
                  its ?game= URL by whoever created it. */}
              {game.status === "OPEN" ? (
                <span>
                  {game.id} — {game.status} — {game.seats.length} seated
                </span>
              ) : (
                <button
                  type="button"
                  className="button"
                  data-testid={`watch-game-${game.id}`}
                  onClick={() => onWatchGame(game.id, loadSeat(game.id))}
                >
                  Watch {game.id} — {game.status} — {game.seats.length} seated
                </button>
              )}
            </li>
          ))}
        </ul>
      </section>

      {activeGameId && (
        <section className="panel lobby__panel" data-testid="active-game-panel">
          <div className="lobby__seating">
            <label className="lobby__field" htmlFor="player-name">
              Your name
              <input
                id="player-name"
                className="input"
                data-testid="human-name-input"
                value={playerName}
                onChange={(e) => setPlayerName(e.target.value)}
                disabled={mySeat !== null}
              />
            </label>
            <button
              type="button"
              className="button button--primary"
              data-testid="take-human-seat"
              onClick={takeSeat}
              disabled={seating || mySeat !== null || seatCount >= MAX_SEATS}
            >
              Take a seat
            </button>
            {mySeat && (
              <span className="badge" data-testid="your-seat-badge">
                You are seat {mySeat.seat}
              </span>
            )}
          </div>

          <div className="lobby__seating">
            <button
              type="button"
              className="button"
              data-testid="add-ai-seat"
              onClick={addAiSeat}
              disabled={seating || seatCount >= MAX_SEATS}
            >
              Seat a random AI
            </button>
            <button
              type="button"
              className="button button--primary"
              data-testid="start-game"
              onClick={startGame}
              disabled={seatCount < MIN_SEATS_TO_START}
            >
              Start game
            </button>
          </div>
        </section>
      )}
    </div>
  );
}
