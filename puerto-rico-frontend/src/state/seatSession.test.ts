import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearSeat, loadSeat, saveSeat } from "./seatSession";

describe("seatSession", () => {
  beforeEach(() => window.localStorage.clear());
  afterEach(() => vi.unstubAllGlobals());

  it("round-trips a seat", () => {
    saveSeat({ gameId: "g1", seat: 2, token: "abc", name: "Dani" });

    expect(loadSeat("g1")).toEqual({ gameId: "g1", seat: 2, token: "abc", name: "Dani" });
  });

  /** A token is only ever valid for the game it was minted for, so it is stored per game. */
  it("keeps seats at different tables apart", () => {
    saveSeat({ gameId: "g1", seat: 0, token: "one", name: "Dani" });
    saveSeat({ gameId: "g2", seat: 3, token: "two", name: "Dani" });

    expect(loadSeat("g1")?.token).toBe("one");
    expect(loadSeat("g2")?.seat).toBe(3);
    expect(loadSeat("g3")).toBeNull();
  });

  it("forgets a seat when asked", () => {
    saveSeat({ gameId: "g1", seat: 0, token: "abc", name: "Dani" });

    clearSeat("g1");

    expect(loadSeat("g1")).toBeNull();
  });

  /**
   * A half-written or hand-edited entry is treated as no entry. A bad token would simply fail the
   * server's check, but a bad seat number would have the UI claim a seat that isn't the player's.
   */
  it("treats a malformed entry as absent", () => {
    window.localStorage.setItem("puerto-rico.seat.g1", "{not json");
    expect(loadSeat("g1")).toBeNull();

    window.localStorage.setItem("puerto-rico.seat.g2", JSON.stringify({ token: "abc" }));
    expect(loadSeat("g2")).toBeNull();

    window.localStorage.setItem("puerto-rico.seat.g3", JSON.stringify({ seat: 1 }));
    expect(loadSeat("g3")).toBeNull();
  });

  it("defaults a missing name rather than rejecting the seat", () => {
    window.localStorage.setItem(
      "puerto-rico.seat.g1",
      JSON.stringify({ gameId: "g1", seat: 1, token: "abc" }),
    );

    expect(loadSeat("g1")).toEqual({ gameId: "g1", seat: 1, token: "abc", name: "You" });
  });

  /**
   * A browser with site data blocked throws on the property access itself. Losing reload recovery
   * is acceptable; crashing the app mid-game is not.
   */
  it("degrades quietly when storage is unavailable", () => {
    vi.stubGlobal("window", {
      get localStorage(): Storage {
        throw new Error("blocked");
      },
    });

    expect(() => saveSeat({ gameId: "g1", seat: 0, token: "abc", name: "Dani" })).not.toThrow();
    expect(loadSeat("g1")).toBeNull();
    expect(() => clearSeat("g1")).not.toThrow();
  });
});
