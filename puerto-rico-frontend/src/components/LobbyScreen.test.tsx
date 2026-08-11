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
    const seatMatch = url.match(/^\/api\/games\/([^/]+)\/seats$/);
    if (seatMatch && method === "POST") {
      const game = games.get(seatMatch[1]!)!;
      const seatIndex = game.seats.length;
      game.seats.push({ name: body.name, kind: body.kind });
      return jsonResponse(200, { seatIndex });
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

beforeEach(() => installFakeServer());
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

    await waitFor(() => expect(onWatchGame).toHaveBeenCalledWith("game-1"));
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

    expect(onWatchGame).toHaveBeenCalledWith("game-9");
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
