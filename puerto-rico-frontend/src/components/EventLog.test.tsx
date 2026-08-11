import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { SessionEvent } from "../api/types";
import { makeView } from "../test/fixtures";
import { describeEvent, EventLog } from "./EventLog";

describe("describeEvent", () => {
  it("describes GAME_STARTED", () => {
    const text = describeEvent({
      type: "GAME_STARTED",
      view: makeView(),
      seatNames: ["Ana", "Bo"],
    });
    expect(text).toContain("Ana");
    expect(text).toContain("Bo");
  });

  it("describes ACTION_APPLIED with seat and action type", () => {
    const text = describeEvent({
      type: "ACTION_APPLIED",
      view: makeView(),
      seat: 1,
      action: { type: "PASS_BUILDING", seat: 1 },
    });
    expect(text).toContain("Seat 1");
    expect(text).toContain("PASS_BUILDING");
  });

  it("describes ACTION_REJECTED with the rejection detail", () => {
    const text = describeEvent({
      type: "ACTION_REJECTED",
      view: makeView(),
      seat: 2,
      action: { type: "PASS_TRADING", seat: 2 },
      reason: "WRONG_PHASE",
      detail: "not the trader phase",
    });
    expect(text).toContain("not the trader phase");
  });

  it("describes GAME_ENDED", () => {
    const text = describeEvent({ type: "GAME_ENDED", view: makeView(), standings: [] });
    expect(text.toLowerCase()).toContain("ended");
  });

  it("describes SESSION_FAILED with the failure detail", () => {
    const text = describeEvent({ type: "SESSION_FAILED", view: makeView(), detail: "boom" });
    expect(text).toContain("boom");
  });
});

describe("EventLog", () => {
  it("renders one entry per event, most recent last, in a live region", () => {
    const events: SessionEvent[] = [
      { type: "GAME_STARTED", view: makeView(), seatNames: ["Ana"] },
      {
        type: "ACTION_APPLIED",
        view: makeView(),
        seat: 0,
        action: { type: "PASS_BUILDING", seat: 0 },
      },
    ];

    render(<EventLog events={events} />);

    const log = screen.getByTestId("event-log");
    expect(log).toHaveAttribute("aria-live", "polite");
    const entries = screen.getAllByTestId("event-log-entry");
    expect(entries).toHaveLength(2);
    expect(entries[1]).toHaveTextContent("PASS_BUILDING");
  });

  it("renders nothing but a container when there are no events yet", () => {
    render(<EventLog events={[]} />);

    expect(screen.getByTestId("event-log")).toBeInTheDocument();
    expect(screen.queryAllByTestId("event-log-entry")).toHaveLength(0);
  });
});
