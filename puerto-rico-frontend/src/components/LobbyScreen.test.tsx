import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { GameTableSummary } from "../api/types";
import { LobbyScreen } from "./LobbyScreen";

/** A minimal in-memory fake of the Lobby endpoints this screen actually calls. */
function installFakeServer() {
  const games = new Map<string, GameTableSummary>();
  let nextId = 1;

  const fetchMock = vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
    const rawUrl = input instanceof Request ? input.url : input.toString();
    const url = new URL(rawUrl, "http://localhost").pathname;
    const method = init?.method ?? (input instanceof Request ? input.method : "GET");
    const rawBody =
      init?.body ?? (input instanceof Request ? await input.clone().text() : undefined);
    const body = rawBody ? JSON.parse(rawBody as string) : undefined;

    if (url === "/api/games" && method === "GET") {
      return jsonResponse(200, Array.from(games.values()));
    }
    if (url === "/api/games" && method === "POST") {
      const id = `game-${nextId++}`;
      games.set(id, { id, seats: [], status: "OPEN" });
      return jsonResponse(201, { gameId: id });
    }
    const gameMatch = url.match(/^\/api\/games\/([^/]+)$/);
    if (gameMatch && method === "GET") {
      return jsonResponse(200, games.get(gameMatch[1]!));
    }
    const seatMatch = url.match(/^\/api\/games\/([^/]+)\/seats$/);
    if (seatMatch && method === "POST") {
      const game = games.get(seatMatch[1]!)!;
      const seatIndex = game.seats.length;
      game.seats.push({ name: body.name, kind: body.kind });
      // The real server mints a token for a human seat and omits it for an AI one — the
      // difference this screen depends on to know whether it can act.
      return jsonResponse(200, {
        seatIndex,
        ...(body.kind === "HUMAN" ? { seatToken: `token-${seatIndex}` } : {}),
      });
    }
    const startMatch = url.match(/^\/api\/games\/([^/]+)\/start$/);
    if (startMatch && method === "POST") {
      const game = games.get(startMatch[1]!)!;
      game.status = "STARTED";
      return jsonResponse(200, game);
    }
    throw new Error(`Unhandled request: ${method} ${url}`);
  });

  vi.stubGlobal("fetch", fetchMock);
  return { games };
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

/** The server installed for the current test, for the cases that assert on what it recorded. */
let server: ReturnType<typeof installFakeServer>;

beforeEach(() => {
  // Seat tokens persist across page loads by design, so they persist across tests too unless
  // cleared — one test's seat would otherwise disable the next test's "Take a seat" button.
  window.localStorage.clear();
  server = installFakeServer();
});
afterEach(() => vi.unstubAllGlobals());

describe("LobbyScreen", () => {
  it("creates a game and lists it", async () => {
    const user = userEvent.setup();
    render(<LobbyScreen onWatchGame={() => {}} />);

    await user.click(screen.getByTestId("lobby-create-game"));

    await waitFor(() => expect(screen.getByTestId("game-list")).toHaveTextContent("OPEN"));
  });

  it("seats AI players and enables start once three are seated", async () => {
    const user = userEvent.setup();
    render(<LobbyScreen onWatchGame={() => {}} />);
    await user.click(screen.getByTestId("lobby-create-game"));
    await waitFor(() => screen.getByTestId("add-ai-seat"));

    expect(screen.getByTestId("start-game")).toBeDisabled();

    await user.click(screen.getByTestId("add-ai-seat"));
    await user.click(screen.getByTestId("add-ai-seat"));
    await user.click(screen.getByTestId("add-ai-seat"));

    await waitFor(() => expect(screen.getByTestId("start-game")).toBeEnabled());
  });

  it("starting the game calls onWatchGame with the game id", async () => {
    const user = userEvent.setup();
    const onWatchGame = vi.fn();
    render(<LobbyScreen onWatchGame={onWatchGame} />);
    await user.click(screen.getByTestId("lobby-create-game"));
    await waitFor(() => screen.getByTestId("add-ai-seat"));
    await user.click(screen.getByTestId("add-ai-seat"));
    await user.click(screen.getByTestId("add-ai-seat"));
    await user.click(screen.getByTestId("add-ai-seat"));
    await waitFor(() => expect(screen.getByTestId("start-game")).toBeEnabled());

    await user.click(screen.getByTestId("start-game"));

    // No seat taken in this tab, so it opens as a spectator.
    await waitFor(() => expect(onWatchGame).toHaveBeenCalledWith("game-1", null));
  });

  it("takes a human seat and keeps the token it is handed", async () => {
    const user = userEvent.setup();
    const onWatchGame = vi.fn();
    render(<LobbyScreen onWatchGame={onWatchGame} />);
    await user.click(screen.getByTestId("lobby-create-game"));
    await waitFor(() => screen.getByTestId("take-human-seat"));

    await user.clear(screen.getByTestId("human-name-input"));
    await user.type(screen.getByTestId("human-name-input"), "Dani");
    await user.click(screen.getByTestId("take-human-seat"));

    await waitFor(() => expect(screen.getByTestId("your-seat-badge")).toHaveTextContent("seat 0"));
    // One seat per browser per table: taking a second would strand the first token.
    expect(screen.getByTestId("take-human-seat")).toBeDisabled();

    const stored = JSON.parse(window.localStorage.getItem("puerto-rico.seat.game-1")!);
    expect(stored).toMatchObject({ gameId: "game-1", seat: 0, token: "token-0", name: "Dani" });
  });

  /** Starting a game you are seated at opens it as a player, not as a spectator. */
  it("hands the seat to the game view when starting a game it holds one at", async () => {
    const user = userEvent.setup();
    const onWatchGame = vi.fn();
    render(<LobbyScreen onWatchGame={onWatchGame} />);
    await user.click(screen.getByTestId("lobby-create-game"));
    await waitFor(() => screen.getByTestId("take-human-seat"));
    await user.click(screen.getByTestId("take-human-seat"));
    await waitFor(() => screen.getByTestId("your-seat-badge"));
    await user.click(screen.getByTestId("add-ai-seat"));
    await user.click(screen.getByTestId("add-ai-seat"));
    await waitFor(() => expect(screen.getByTestId("start-game")).toBeEnabled());

    await user.click(screen.getByTestId("start-game"));

    await waitFor(() =>
      expect(onWatchGame).toHaveBeenCalledWith(
        "game-1",
        expect.objectContaining({ seat: 0, token: "token-0" }),
      ),
    );
  });

  /**
   * Two seatings in flight together each read the table before the other lands, so both would
   * number their bot the same — and a double-click on "Take a seat" would claim two seats, the
   * second token overwriting the first in storage.
   */
  it("numbers each AI seat distinctly, and seats one player per click", async () => {
    const user = userEvent.setup();
    render(<LobbyScreen onWatchGame={() => {}} />);
    await user.click(screen.getByTestId("lobby-create-game"));
    await waitFor(() => screen.getByTestId("take-human-seat"));

    await user.click(screen.getByTestId("take-human-seat"));
    await waitFor(() => screen.getByTestId("your-seat-badge"));
    await user.click(screen.getByTestId("add-ai-seat"));
    await user.click(screen.getByTestId("add-ai-seat"));

    await waitFor(() => expect(screen.getByTestId("game-list")).toHaveTextContent("3 seated"));
    expect(server.games.get("game-1")!.seats.map((seat) => seat.name)).toEqual([
      "You",
      "Bot 2",
      "Bot 3",
    ]);
  });

  /**
   * Before this the only way to watch a game you did not start was to be handed its ?game= URL.
   */
  it("lets you watch a started game straight from the list", async () => {
    const user = userEvent.setup();
    const { games } = installFakeServer();
    games.set("game-9", { id: "game-9", seats: [], status: "STARTED" });
    const onWatchGame = vi.fn();
    render(<LobbyScreen onWatchGame={onWatchGame} />);

    await waitFor(() => screen.getByTestId("watch-game-game-9"));
    await user.click(screen.getByTestId("watch-game-game-9"));

    expect(onWatchGame).toHaveBeenCalledWith("game-9", null);
  });

  /** A game this browser already holds a seat at is re-entered as that player, token and all. */
  it("re-enters a game it holds a stored seat at as the player, not a spectator", async () => {
    const user = userEvent.setup();
    const { games } = installFakeServer();
    games.set("game-9", { id: "game-9", seats: [], status: "STARTED" });
    window.localStorage.setItem(
      "puerto-rico.seat.game-9",
      JSON.stringify({ gameId: "game-9", seat: 2, token: "kept", name: "Dani" }),
    );
    const onWatchGame = vi.fn();
    render(<LobbyScreen onWatchGame={onWatchGame} />);

    await waitFor(() => screen.getByTestId("watch-game-game-9"));
    await user.click(screen.getByTestId("watch-game-game-9"));

    expect(onWatchGame).toHaveBeenCalledWith(
      "game-9",
      expect.objectContaining({ seat: 2, token: "kept" }),
    );
  });

  it("offers no watch button for a game that has not started", async () => {
    const { games } = installFakeServer();
    games.set("game-8", { id: "game-8", seats: [], status: "OPEN" });
    render(<LobbyScreen onWatchGame={() => {}} />);

    await waitFor(() => screen.getByTestId("game-list-item-game-8"));

    expect(screen.queryByTestId("watch-game-game-8")).not.toBeInTheDocument();
  });

  it("polls so games seated at another table show up without a reload", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    try {
      const { games } = installFakeServer();
      render(<LobbyScreen onWatchGame={() => {}} />);
      await waitFor(() => screen.getByTestId("game-list"));

      games.set("game-7", { id: "game-7", seats: [], status: "STARTED" });
      await vi.advanceTimersByTimeAsync(3500);

      await waitFor(() => expect(screen.getByTestId("game-list")).toHaveTextContent("game-7"));
    } finally {
      vi.useRealTimers();
    }
  });
});
