import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { makeBuilding, makePlayer } from "../test/fixtures";
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
      buildings: [makeBuilding("SMALL_MARKET", { colonists: 1 })],
    });

    render(<PlayerBoard player={player} isGovernor={false} isActing={false} />);

    expect(screen.getByTestId("player-0-island-count")).toHaveTextContent("2");
    expect(screen.getByTestId("player-0-building-count")).toHaveTextContent("1");
  });

  /**
   * The tiles are drawn, not spelled, so occupancy rides on the tile's own accessible label and on
   * the `data-occupied` attribute a test (or a stylesheet) can key off.
   */
  it("draws each island tile with whether it is staffed", () => {
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
    expect(tiles[0]).toHaveAttribute("data-occupied", "true");
    expect(tiles[1]).toHaveAttribute("data-occupied", "false");
    expect(screen.getByLabelText("Corn field, staffed")).toBeInTheDocument();
    expect(screen.getByLabelText("Quarry, idle")).toBeInTheDocument();
  });

  it("draws each building as a card with its filled and empty colonist circles", () => {
    const player = makePlayer({
      seat: 1,
      buildings: [
        makeBuilding("SMALL_MARKET", { colonists: 1, capacity: 1, victoryPoints: 1 }),
        makeBuilding("WHARF", { colonists: 0, capacity: 1, victoryPoints: 3 }),
      ],
    });

    render(<PlayerBoard player={player} isGovernor={false} isActing={false} />);

    expect(screen.getAllByTestId("player-1-building")).toHaveLength(2);
    expect(screen.getByTestId("building-card-SMALL_MARKET")).toHaveTextContent("Small Market");
    expect(screen.getByTestId("building-card-WHARF")).toHaveTextContent("3 VP");
    expect(screen.getByLabelText("1 of 1 colonist spaces filled")).toBeInTheDocument();
    expect(screen.getByLabelText("0 of 1 colonist spaces filled")).toBeInTheDocument();
  });

  it("lists goods held, in trading-house order, omitting kinds held none of", () => {
    const player = makePlayer({ seat: 2, goods: { COFFEE: 2, CORN: 3, SUGAR: 0 } });

    render(<PlayerBoard player={player} isGovernor={false} isActing={false} />);

    const goods = screen.getByTestId("player-2-goods");
    expect(goods.textContent).toBe("× 3× 2");
    expect(screen.getByTestId("player-2-good-CORN")).toHaveTextContent("× 3");
    expect(screen.getByTestId("player-2-good-COFFEE")).toHaveTextContent("× 2");
    expect(screen.queryByTestId("player-2-good-SUGAR")).not.toBeInTheDocument();
  });

  it("marks this client's own board so a player can find themselves", () => {
    const player = makePlayer({ seat: 1 });

    render(<PlayerBoard player={player} isGovernor={false} isActing={false} isYou={true} />);

    expect(screen.getByTestId("player-board-1")).toHaveAttribute("data-you", "true");
    expect(screen.getByTestId("player-1-is-you")).toBeInTheDocument();
  });
});
