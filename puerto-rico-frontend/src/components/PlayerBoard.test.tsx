import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { makePlayer } from "../test/fixtures";
import { PlayerBoard } from "./PlayerBoard";

describe("PlayerBoard", () => {
  it("renders the player's name, doubloons, and victory points", () => {
    const player = makePlayer({ seat: 1, name: "Bo", doubloons: 7, victoryPoints: 3 });

    render(<PlayerBoard player={player} isGovernor={false} isActing={false} />);

    const board = screen.getByTestId("player-board-1");
    expect(board).toHaveTextContent("Bo");
    expect(screen.getByTestId("player-1-doubloons")).toHaveTextContent("7");
    expect(screen.getByTestId("player-1-victory-points")).toHaveTextContent("3");
  });

  it("marks the governor with an accessible label", () => {
    const player = makePlayer({ seat: 0 });

    render(<PlayerBoard player={player} isGovernor={true} isActing={false} />);

    expect(screen.getByLabelText(/governor/i)).toBeInTheDocument();
  });

  it("does not show a governor label for a non-governor", () => {
    const player = makePlayer({ seat: 1 });

    render(<PlayerBoard player={player} isGovernor={false} isActing={false} />);

    expect(screen.queryByLabelText(/governor/i)).not.toBeInTheDocument();
  });

  it("marks the currently-acting seat as a live status region", () => {
    const player = makePlayer({ seat: 2 });

    render(<PlayerBoard player={player} isGovernor={false} isActing={true} />);

    expect(screen.getByTestId("player-board-2")).toHaveAttribute("aria-current", "true");
  });

  it("renders island tile and building counts", () => {
    const player = makePlayer({
      seat: 0,
      island: [
        { type: "CORN", occupied: true },
        { type: "INDIGO", occupied: false },
      ],
      buildings: [{ type: "SMALL_MARKET", colonists: 1 }],
    });

    render(<PlayerBoard player={player} isGovernor={false} isActing={false} />);

    expect(screen.getByTestId("player-0-island-count")).toHaveTextContent("2");
    expect(screen.getByTestId("player-0-building-count")).toHaveTextContent("1");
  });

  it("lists each island tile with whether it is staffed", () => {
    const player = makePlayer({
      seat: 0,
      island: [
        { type: "CORN", occupied: true },
        { type: "QUARRY", occupied: false },
      ],
    });

    render(<PlayerBoard player={player} isGovernor={false} isActing={false} />);

    const tiles = screen.getAllByTestId("player-0-island-tile");
    expect(tiles).toHaveLength(2);
    expect(tiles[0]).toHaveTextContent("CORN");
    expect(tiles[0]).toHaveAttribute("data-occupied", "true");
    expect(tiles[1]).toHaveTextContent("QUARRY");
    expect(tiles[1]).toHaveAttribute("data-occupied", "false");
  });

  it("lists each building with how many colonists are on it", () => {
    const player = makePlayer({
      seat: 1,
      buildings: [
        { type: "SMALL_MARKET", colonists: 1 },
        { type: "WHARF", colonists: 0 },
      ],
    });

    render(<PlayerBoard player={player} isGovernor={false} isActing={false} />);

    const buildings = screen.getAllByTestId("player-1-building");
    expect(buildings[0]).toHaveTextContent("SMALL_MARKET — 1 colonist");
    expect(buildings[1]).toHaveTextContent("WHARF — 0 colonists");
  });

  it("lists goods held, in trading-house order, omitting kinds held none of", () => {
    const player = makePlayer({ seat: 2, goods: { COFFEE: 2, CORN: 3, SUGAR: 0 } });

    render(<PlayerBoard player={player} isGovernor={false} isActing={false} />);

    const goods = screen.getByTestId("player-2-goods");
    expect(goods.textContent).toBe("CORN × 3COFFEE × 2");
    expect(screen.queryByTestId("player-2-good-SUGAR")).not.toBeInTheDocument();
  });
});
