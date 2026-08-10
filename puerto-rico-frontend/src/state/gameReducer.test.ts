import { describe, expect, it } from "vitest";
import type { SessionEvent } from "../api/types";
import { makeView } from "../test/fixtures";
import { gameReducer, initialGameState } from "./gameReducer";

describe("gameReducer", () => {
  it("GAME_STARTED sets the view and records the event", () => {
    const event: SessionEvent = {
      type: "GAME_STARTED",
      view: makeView(),
      seatNames: ["Ana", "Bo", "Coco"],
    };

    const next = gameReducer(initialGameState, event);

    expect(next.view).toBe(event.view);
    expect(next.events).toEqual([event]);
  });

  it("DECISION_REQUESTED sets the view and records the event", () => {
    const event: SessionEvent = {
      type: "DECISION_REQUESTED",
      view: makeView(),
      seat: 0,
      options: [],
      requestId: 1,
    };

    const next = gameReducer(initialGameState, event);

    expect(next.view).toBe(event.view);
    expect(next.events).toEqual([event]);
  });

  it("ACTION_APPLIED sets the post-action view and records the event", () => {
    const event: SessionEvent = {
      type: "ACTION_APPLIED",
      view: makeView(),
      seat: 1,
      action: { type: "PASS_BUILDING", seat: 1 },
    };

    const next = gameReducer(initialGameState, event);

    expect(next.view).toBe(event.view);
    expect(next.events).toEqual([event]);
  });

  it("ACTION_REJECTED updates the view and records the event without touching standings", () => {
    const event: SessionEvent = {
      type: "ACTION_REJECTED",
      view: makeView(),
      seat: 2,
      action: { type: "PASS_TRADING", seat: 2 },
      reason: "WRONG_PHASE",
      detail: "not the trader phase",
    };

    const next = gameReducer(initialGameState, event);

    expect(next.view).toBe(event.view);
    expect(next.standings).toBeNull();
    expect(next.events).toEqual([event]);
  });

  it("GAME_ENDED records standings and the final view", () => {
    const event: SessionEvent = {
      type: "GAME_ENDED",
      view: makeView(),
      standings: [
        { seat: 0, name: "Ana", chips: 10, buildingPoints: 5, bonusPoints: 0, tiebreak: 3, total: 15 },
      ],
    };

    const next = gameReducer(initialGameState, event);

    expect(next.standings).toEqual(event.standings);
    expect(next.view).toBe(event.view);
  });

  it("SESSION_FAILED records the failure detail and the view at the time of failure", () => {
    const failed: SessionEvent = {
      type: "SESSION_FAILED",
      view: makeView(),
      detail: "Seat 1's actor kept failing",
    };

    const next = gameReducer(initialGameState, failed);

    expect(next.failure).toBe("Seat 1's actor kept failing");
    expect(next.view).toBe(failed.view);
  });

  it("SNAPSHOT_LOADED sets the view without recording it as an event", () => {
    const view = makeView();

    const next = gameReducer(initialGameState, { type: "SNAPSHOT_LOADED", view });

    expect(next.view).toBe(view);
    expect(next.events).toEqual([]);
  });

  it("accumulates events across multiple dispatches in order", () => {
    const first: SessionEvent = { type: "GAME_STARTED", view: makeView(), seatNames: ["Ana"] };
    const second: SessionEvent = {
      type: "DECISION_REQUESTED",
      view: makeView(),
      seat: 0,
      options: [],
      requestId: 1,
    };

    const afterFirst = gameReducer(initialGameState, first);
    const afterSecond = gameReducer(afterFirst, second);

    expect(afterSecond.events).toEqual([first, second]);
  });
});
