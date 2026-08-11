import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { SessionEvent } from "./types";
import { subscribeToGameEvents } from "./events";

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

  emitRaw(data: string) {
    this.onmessage?.({ data });
  }

  fail() {
    this.onerror?.();
  }

  close() {
    this.closed = true;
  }
}

beforeEach(() => {
  FakeEventSource.instances = [];
  vi.stubGlobal("EventSource", FakeEventSource);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("subscribeToGameEvents", () => {
  it("connects to the game's event stream path", () => {
    subscribeToGameEvents("abc-123", () => {});

    expect(FakeEventSource.instances).toHaveLength(1);
    expect(FakeEventSource.instances[0]?.url).toBe("/api/games/abc-123/events");
  });

  it("parses incoming messages and forwards them as SessionEvent", () => {
    const received: SessionEvent[] = [];
    subscribeToGameEvents("abc-123", (event) => received.push(event));

    const event: SessionEvent = {
      type: "GAME_STARTED",
      view: { state: { config: { playerNames: [] } } } as unknown as SessionEvent["view"],
      seatNames: ["Ana"],
    };
    FakeEventSource.instances[0]?.emit(event);

    expect(received).toEqual([event]);
  });

  it("closes the underlying EventSource on unsubscribe", () => {
    const unsubscribe = subscribeToGameEvents("abc-123", () => {});

    unsubscribe();

    expect(FakeEventSource.instances[0]?.closed).toBe(true);
  });

  /**
   * EventSource reconnects on its own, but events emitted during the gap are gone — nothing
   * replays them — so the caller has to be told in order to re-fetch /state.
   */
  it("reports a dropped connection and its recovery", () => {
    const changes: boolean[] = [];
    subscribeToGameEvents("abc-123", {
      onEvent: () => {},
      onConnectionChange: (connected) => changes.push(connected),
    });
    const source = FakeEventSource.instances[0]!;

    source.fail();
    source.emit({ type: "GAME_STARTED", seatNames: [] });

    expect(changes).toEqual([false, true]);
  });

  it("does not report the same connection state twice", () => {
    const changes: boolean[] = [];
    subscribeToGameEvents("abc-123", {
      onEvent: () => {},
      onConnectionChange: (connected) => changes.push(connected),
    });
    const source = FakeEventSource.instances[0]!;

    source.fail();
    source.fail();

    expect(changes).toEqual([false]);
  });

  it("survives a frame it cannot parse", () => {
    const received: SessionEvent[] = [];
    subscribeToGameEvents("abc-123", (event) => received.push(event));
    const source = FakeEventSource.instances[0]!;

    source.emitRaw("not json");
    source.emit({ type: "GAME_STARTED", seatNames: ["Ana"] });

    expect(received).toHaveLength(1);
  });
});
