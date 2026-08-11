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
});
