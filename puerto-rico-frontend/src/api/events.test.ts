import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { SessionEvent } from "./types";
import { subscribeToGameEvents } from "./events";

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  url: string;
  onmessage: ((event: { data: string }) => void) | null = null;
  closed = false;

  constructor(url: string) {
    this.url = url;
    FakeEventSource.instances.push(this);
  }

  emit(data: unknown) {
    this.onmessage?.({ data: JSON.stringify(data) });
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
});
