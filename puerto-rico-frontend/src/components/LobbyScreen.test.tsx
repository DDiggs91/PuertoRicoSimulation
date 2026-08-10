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
    const rawBody = init?.body ?? (input instanceof Request ? await input.clone().text() : undefined);
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
    render(<LobbyScreen onGameStarted={() => {}} />);

    await user.click(screen.getByTestId("lobby-create-game"));

    await waitFor(() => expect(screen.getByTestId("game-list")).toHaveTextContent("OPEN"));
  });

  it("seats AI players and enables start once three are seated", async () => {
    const user = userEvent.setup();
    render(<LobbyScreen onGameStarted={() => {}} />);
    await user.click(screen.getByTestId("lobby-create-game"));
    await waitFor(() => screen.getByTestId("add-ai-seat"));

    expect(screen.getByTestId("start-game")).toBeDisabled();

    await user.click(screen.getByTestId("add-ai-seat"));
    await user.click(screen.getByTestId("add-ai-seat"));
    await user.click(screen.getByTestId("add-ai-seat"));

    await waitFor(() => expect(screen.getByTestId("start-game")).toBeEnabled());
  });

  it("starting the game calls onGameStarted with the game id", async () => {
    const user = userEvent.setup();
    const onGameStarted = vi.fn();
    render(<LobbyScreen onGameStarted={onGameStarted} />);
    await user.click(screen.getByTestId("lobby-create-game"));
    await waitFor(() => screen.getByTestId("add-ai-seat"));
    await user.click(screen.getByTestId("add-ai-seat"));
    await user.click(screen.getByTestId("add-ai-seat"));
    await user.click(screen.getByTestId("add-ai-seat"));
    await waitFor(() => expect(screen.getByTestId("start-game")).toBeEnabled());

    await user.click(screen.getByTestId("start-game"));

    await waitFor(() => expect(onGameStarted).toHaveBeenCalledWith("game-1"));
  });
});
