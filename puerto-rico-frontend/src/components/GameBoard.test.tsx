import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { ScoreBreakdown, SessionEvent } from "../api/types";
import { makePlayer, makeState, makeView } from "../test/fixtures";
import { GameBoard } from "./GameBoard";

describe("GameBoard", () => {
  it("announces the current phase and acting seat as a live status", () => {
    const view = makeView({
      state: makeState({
        governorSeat: 0,
        phase: {
          type: "SETTLER",
          actorSeat: 1,
          chooserSeat: 0,
          queue: [1, 2, 0],
          haciendaOffered: false,
        },
      }),
    });

    render(<GameBoard view={view} events={[]} standings={null} />);

    const status = screen.getByTestId("game-phase");
    expect(status).toHaveAttribute("role", "status");
    expect(status).toHaveTextContent(/settler/i);
    expect(status).toHaveTextContent("1");
  });

  it("renders a PlayerBoard per player, marking the governor and the acting seat", () => {
    const view = makeView({
      state: makeState({
        players: [
          makePlayer({ seat: 0, name: "Ana" }),
          makePlayer({ seat: 1, name: "Bo" }),
          makePlayer({ seat: 2, name: "Coco" }),
        ],
        governorSeat: 0,
        phase: {
          type: "SETTLER",
          actorSeat: 1,
          chooserSeat: 0,
          queue: [1, 2, 0],
          haciendaOffered: false,
        },
      }),
    });

    render(<GameBoard view={view} events={[]} standings={null} />);

    expect(screen.getByTestId("player-board-0")).toBeInTheDocument();
    expect(screen.getByTestId("player-board-1")).toBeInTheDocument();
    expect(screen.getByTestId("player-board-2")).toBeInTheDocument();
    expect(screen.getByTestId("player-board-1")).toHaveAttribute("aria-current", "true");
    expect(screen.getByTestId("player-board-0")).not.toHaveAttribute("aria-current", "true");
  });

  it("renders the tile supply and trading house summary", () => {
    const view = makeView({
      state: makeState({
        tiles: {
          faceUp: ["CORN", "INDIGO"],
          quarriesRemaining: 6,
          faceDownCount: 30,
          discardedCount: 2,
        },
        tradingHouse: { goods: ["COFFEE", "SUGAR"] },
      }),
    });

    render(<GameBoard view={view} events={[]} standings={null} />);

    expect(screen.getByTestId("quarries-remaining")).toHaveTextContent("6");
    expect(screen.getByTestId("trading-house-goods")).toHaveTextContent("2");
  });

  it("renders the event log", () => {
    const events: SessionEvent[] = [{ type: "GAME_STARTED", view: makeView(), seatNames: ["Ana"] }];

    render(<GameBoard view={makeView()} events={events} standings={null} />);

    expect(screen.getByTestId("event-log")).toBeInTheDocument();
    expect(screen.getAllByTestId("event-log-entry")).toHaveLength(1);
  });

  it("shows final standings only once the game has ended", () => {
    const { rerender } = render(<GameBoard view={makeView()} events={[]} standings={null} />);
    expect(screen.queryByTestId("final-standings")).not.toBeInTheDocument();

    const standings: ScoreBreakdown[] = [
      {
        seat: 0,
        name: "Ana",
        chips: 15,
        buildingPoints: 20,
        bonusPoints: 5,
        tiebreak: 2,
        total: 40,
      },
      {
        seat: 1,
        name: "Bo",
        chips: 20,
        buildingPoints: 10,
        bonusPoints: 0,
        tiebreak: 5,
        total: 30,
      },
    ];
    rerender(<GameBoard view={makeView()} events={[]} standings={standings} />);

    const rows = screen.getAllByTestId("standing-row");
    expect(rows).toHaveLength(2);
    expect(rows[0]).toHaveTextContent("Ana");
    expect(rows[1]).toHaveTextContent("Bo");
  });

  // The server ranks by total and then the rulebook tiebreak; re-sorting here on total alone would
  // silently reorder a tie and contradict the ranking the same payload carries.
  it("keeps the server's ranking for players tied on total", () => {
    const standings: ScoreBreakdown[] = [
      {
        seat: 1,
        name: "Bo",
        chips: 20,
        buildingPoints: 10,
        bonusPoints: 0,
        tiebreak: 9,
        total: 30,
      },
      {
        seat: 0,
        name: "Ana",
        chips: 20,
        buildingPoints: 10,
        bonusPoints: 0,
        tiebreak: 3,
        total: 30,
      },
    ];

    render(<GameBoard view={makeView()} events={[]} standings={standings} />);

    const rows = screen.getAllByTestId("standing-row");
    expect(rows[0]).toHaveTextContent("Bo");
    expect(rows[1]).toHaveTextContent("Ana");
  });

  it("shows a session failure as an alert", () => {
    render(<GameBoard view={makeView()} events={[]} standings={null} failure="actor gave up" />);

    const alert = screen.getByTestId("session-failure");
    expect(alert).toHaveAttribute("role", "alert");
    expect(alert).toHaveTextContent("actor gave up");
  });

  it("shows no failure alert on a healthy game", () => {
    render(<GameBoard view={makeView()} events={[]} standings={null} failure={null} />);

    expect(screen.queryByTestId("session-failure")).not.toBeInTheDocument();
  });
});
