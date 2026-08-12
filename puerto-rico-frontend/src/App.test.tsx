import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";
import { makeState, makeView } from "./test/fixtures";

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  url: string;
  onmessage: ((event: { data: string }) => void) | null = null;
  onerror: (() => void) | null = null;
  closed = false;

  constructor(url: string) {
    this.url = url;
    FakeEventSource.instances.push(this);
  }

  emit(data: unknown) {
    this.onmessage?.({ data: JSON.stringify(data) });
  }

  fail() {
    this.onerror?.();
  }

  close() {
    this.closed = true;
  }
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

/** Points the app at a game id via the query param App uses instead of a router. */
function openGame(gameId: string | null) {
  const search = gameId === null ? "" : `?game=${gameId}`;
  window.history.replaceState(null, "", `/${search}`);
}

beforeEach(() => {
  FakeEventSource.instances = [];
  vi.stubGlobal("EventSource", FakeEventSource);
  window.localStorage.clear();
  openGame(null);
});

afterEach(() => {
  vi.unstubAllGlobals();
  openGame(null);
});

describe("App", () => {
  it("renders the lobby when no game is in the URL", () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => jsonResponse(200, [])),
    );

    render(<App />);

    expect(screen.getByTestId("lobby-create-game")).toBeInTheDocument();
  });

  it("bootstraps a game from ?game= by fetching its state and rendering the board", async () => {
    const view = makeView({ state: makeState({ governorSeat: 0 }) });
    const requested: string[] = [];
    const fetchMock = vi.fn(async (input: string | URL | Request) => {
      requested.push(String(input instanceof Request ? input.url : input));
      return jsonResponse(200, view);
    });
    vi.stubGlobal("fetch", fetchMock);
    openGame("abc-123");

    render(<App />);

    await waitFor(() => expect(screen.getByTestId("game-phase")).toBeInTheDocument());
    expect(requested.join(" ")).toContain("/api/games/abc-123/state");
  });

  /**
   * The regression this file exists for: before, a rejected bootstrap left `view` null forever and
   * the app sat on its loading placeholder with no way out.
   */
  it("renders an error, not a permanent spinner, when the state fetch fails", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        jsonResponse(404, {
          title: "Game not found",
          status: 404,
          reason: "GAME_NOT_FOUND",
        }),
      ),
    );
    openGame("nope");

    render(<App />);

    const alert = await screen.findByTestId("load-error");
    expect(alert).toHaveAttribute("role", "alert");
    expect(screen.queryByText(/loading game/i)).not.toBeInTheDocument();
  });

  it("offers a way back to the lobby from the error state", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = String(input instanceof Request ? input.url : input);
        return url.includes("/state")
          ? jsonResponse(404, { title: "Game not found", status: 404 })
          : jsonResponse(200, []);
      }),
    );
    openGame("nope");

    render(<App />);
    await screen.findByTestId("back-to-lobby");
    await user.click(screen.getByTestId("back-to-lobby"));

    await waitFor(() => expect(screen.getByTestId("lobby-create-game")).toBeInTheDocument());
    expect(window.location.search).toBe("");
  });

  /**
   * EventSource retries silently, so without this the board would sit there looking live while
   * quietly missing every event, and nothing would ever re-fetch the state it lost.
   */
  it("says so when the live stream drops, and resyncs on recovery", async () => {
    let fetches = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        fetches++;
        return jsonResponse(200, makeView());
      }),
    );
    openGame("abc-123");

    render(<App />);
    await waitFor(() => expect(FakeEventSource.instances).toHaveLength(1));
    await screen.findByTestId("game-phase");
    const afterBootstrap = fetches;

    FakeEventSource.instances[0]!.fail();

    await screen.findByTestId("connection-lost");

    FakeEventSource.instances[0]!.emit({
      type: "GAME_STARTED",
      view: makeView(),
      seatNames: ["Ana"],
    });

    await waitFor(() => expect(screen.queryByTestId("connection-lost")).not.toBeInTheDocument());
    await waitFor(() => expect(fetches).toBeGreaterThan(afterBootstrap));
  });

  it("shows no disconnection notice on a healthy stream", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => jsonResponse(200, makeView())),
    );
    openGame("abc-123");

    render(<App />);
    await screen.findByTestId("game-phase");

    expect(screen.queryByTestId("connection-lost")).not.toBeInTheDocument();
  });

  it("subscribes to the game's event stream and closes it on unmount", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => jsonResponse(200, makeView())),
    );
    openGame("abc-123");

    const { unmount } = render(<App />);
    await waitFor(() => expect(FakeEventSource.instances).toHaveLength(1));
    expect(FakeEventSource.instances[0]?.url).toBe("/api/games/abc-123/events");

    unmount();

    expect(FakeEventSource.instances[0]?.closed).toBe(true);
  });

  // --- holding a seat, which is what separates a player from a spectator ---

  /** Routes /state, /decision and /moves; everything else 404s so an unexpected call is loud. */
  function stubGameServer({
    decision = null as unknown,
    onMove = (_body: unknown, _token: string | null) => jsonResponse(202, null),
  } = {}) {
    const fetchMock = vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
      const url = String(input instanceof Request ? input.url : input);
      const method = init?.method ?? (input instanceof Request ? input.method : "GET");
      if (url.includes("/state")) {
        return jsonResponse(200, makeView());
      }
      if (url.includes("/decision")) {
        return decision === null
          ? jsonResponse(404, { title: "No pending decision", status: 404 })
          : jsonResponse(200, decision);
      }
      if (url.includes("/moves") && method === "POST") {
        const rawBody =
          init?.body ?? (input instanceof Request ? await input.clone().text() : undefined);
        const headers = init?.headers ?? (input instanceof Request ? input.headers : new Headers());
        const token =
          headers instanceof Headers
            ? headers.get("X-Seat-Token")
            : ((headers as Record<string, string>)["X-Seat-Token"] ?? null);
        return onMove(JSON.parse(rawBody as string), token);
      }
      return jsonResponse(404, { title: "Unhandled", status: 404 });
    });
    vi.stubGlobal("fetch", fetchMock);
    return fetchMock;
  }

  const pendingDecision = {
    seat: 1,
    view: makeView(),
    requestId: 42,
    options: [{ type: "SELECT_ROLE", seat: 1, role: "BUILDER" }],
  };

  function seatMe(gameId: string, seat = 1) {
    window.localStorage.setItem(
      `puerto-rico.seat.${gameId}`,
      JSON.stringify({ gameId, seat, token: "tok", name: "Dani" }),
    );
  }

  /**
   * The reload case. A seat token is minted once and never re-issued, so without this a refresh
   * mid-game leaves a player watching a seat the server still thinks is theirs.
   */
  it("restores a stored seat on the ?game= path and offers the pending decision", async () => {
    stubGameServer({ decision: pendingDecision });
    seatMe("abc-123");
    openGame("abc-123");

    render(<App />);

    expect(await screen.findByTestId("seated-as")).toHaveTextContent("Dani");
    expect(await screen.findByTestId("your-turn")).toBeInTheDocument();
    expect(screen.getByTestId("action-select-role-BUILDER")).toBeInTheDocument();
  });

  /**
   * DECISION_REQUESTED is emitted once, at the moment the wait starts. A client arriving after
   * that never hears it, which is exactly the window `GET /decision` closes.
   */
  it("shows no action panel for a spectator, however the decision arrived", async () => {
    stubGameServer({ decision: pendingDecision });
    openGame("abc-123");

    render(<App />);

    await screen.findByTestId("game-phase");
    expect(screen.queryByTestId("action-panel")).not.toBeInTheDocument();
    expect(screen.queryByTestId("seated-as")).not.toBeInTheDocument();
  });

  it("shows no action panel when the pending decision belongs to another seat", async () => {
    stubGameServer({ decision: pendingDecision });
    seatMe("abc-123", 2);
    openGame("abc-123");

    render(<App />);

    await screen.findByTestId("game-phase");
    expect(screen.queryByTestId("action-panel")).not.toBeInTheDocument();
  });

  it("treats a 404 from /decision as nothing pending, not as a load failure", async () => {
    stubGameServer({ decision: null });
    seatMe("abc-123");
    openGame("abc-123");

    render(<App />);

    await screen.findByTestId("game-phase");
    expect(screen.queryByTestId("load-error")).not.toBeInTheDocument();
    expect(screen.queryByTestId("action-panel")).not.toBeInTheDocument();
  });

  it("posts the chosen move with the seat token and the decision's request id", async () => {
    const user = userEvent.setup();
    const moves: { body: unknown; token: string | null }[] = [];
    stubGameServer({
      decision: pendingDecision,
      onMove: (body, token) => {
        moves.push({ body, token });
        return jsonResponse(202, null);
      },
    });
    seatMe("abc-123");
    openGame("abc-123");

    render(<App />);
    await user.click(await screen.findByTestId("action-select-role-BUILDER"));

    await waitFor(() => expect(moves).toHaveLength(1));
    expect(moves[0]!.token).toBe("tok");
    expect(moves[0]!.body).toEqual({
      requestId: 42,
      action: { type: "SELECT_ROLE", seat: 1, role: "BUILDER" },
    });
  });

  it("surfaces a refused move instead of failing silently", async () => {
    const user = userEvent.setup();
    stubGameServer({
      decision: pendingDecision,
      onMove: () =>
        jsonResponse(403, {
          status: 403,
          title: "Invalid seat token",
          detail: "This token does not authorize seat 1.",
        }),
    });
    seatMe("abc-123");
    openGame("abc-123");

    render(<App />);
    await user.click(await screen.findByTestId("action-select-role-BUILDER"));

    expect(await screen.findByTestId("action-panel-error")).toHaveTextContent(
      "This token does not authorize seat 1.",
    );
  });

  /** The live path: the event carries the options, so no fetch is needed to start acting. */
  it("opens the panel from a DECISION_REQUESTED arriving over the stream", async () => {
    stubGameServer({ decision: null });
    seatMe("abc-123");
    openGame("abc-123");

    render(<App />);
    await waitFor(() => expect(FakeEventSource.instances).toHaveLength(1));
    await screen.findByTestId("game-phase");

    FakeEventSource.instances[0]!.emit({
      type: "DECISION_REQUESTED",
      view: makeView(),
      seat: 1,
      requestId: 7,
      options: [{ type: "PASS_BUILDING", seat: 1 }],
    });

    expect(await screen.findByTestId("action-panel")).toBeInTheDocument();
  });
});
