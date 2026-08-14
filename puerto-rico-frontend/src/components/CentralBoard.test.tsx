import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { makeCatalogEntry, makeState } from "../test/fixtures";
import { CentralBoard } from "./CentralBoard";

describe("CentralBoard", () => {
  it("renders the role track with doubloons and who took what", () => {
    const state = makeState({
      roles: {
        cards: [
          { role: "SETTLER", doubloons: 0, takenBySeat: 1 },
          { role: "CAPTAIN", doubloons: 2, takenBySeat: null },
        ],
      },
    });

    render(<CentralBoard state={state} />);

    const settler = screen.getByTestId("role-card-SETTLER");
    expect(settler).toHaveAttribute("data-taken", "true");
    expect(settler).toHaveTextContent("seat 1");

    const captain = screen.getByTestId("role-card-CAPTAIN");
    expect(captain).toHaveAttribute("data-taken", "false");
    expect(captain).toHaveTextContent("Captain");
    // What the role does, not just its name — the same line the selection picker shows.
    expect(captain).toHaveTextContent("Everyone ships goods");
    // The coins are a drawn piece, so the count is on its label rather than in the card's text.
    expect(screen.getByLabelText("2 doubloons")).toBeInTheDocument();
  });

  it("shows the building display, with the copies still for sale", () => {
    const state = makeState({
      config: {
        playerNames: ["Ana", "Bo", "Coco"],
        buildingCatalog: [makeCatalogEntry("HOSPICE", { cost: 4, victoryPoints: 2, copies: 2 })],
      },
      buildings: { HOSPICE: 1 },
    });

    render(<CentralBoard state={state} />);

    expect(screen.getByTestId("building-display-panel")).toBeInTheDocument();
    expect(screen.getByTestId("building-supply-HOSPICE")).toHaveTextContent("1 of 2 left");
  });

  /**
   * The ships are drawn, so the cargo kind rides on the ship's accessible label; the loaded/capacity
   * count stays as text beside it, which is what a spectator scans for.
   */
  it("renders each cargo ship's capacity, cargo, and load", () => {
    const state = makeState({
      ships: [
        { capacity: 4, cargo: "CORN", loaded: 3 },
        { capacity: 5, cargo: null, loaded: 0 },
      ],
    });

    render(<CentralBoard state={state} />);

    expect(screen.getByTestId("cargo-ship-0")).toHaveTextContent("3 / 4");
    expect(screen.getByLabelText("Ship with 4 holds, 3 loaded with Corn")).toBeInTheDocument();
    expect(screen.getByTestId("cargo-ship-1")).toHaveTextContent("0 / 5");
    expect(screen.getByLabelText("Empty ship with 5 holds")).toBeInTheDocument();
  });

  it("renders the face-up plantation row", () => {
    const state = makeState({
      tiles: {
        faceUp: ["CORN", "INDIGO", "QUARRY"],
        quarriesRemaining: 6,
        faceDownCount: 30,
        discardedCount: 2,
      },
    });

    render(<CentralBoard state={state} />);

    expect(screen.getAllByTestId("face-up-tile")).toHaveLength(3);
    expect(screen.getByTestId("tile-INDIGO")).toBeInTheDocument();
    expect(screen.getByLabelText("Indigo plantation")).toBeInTheDocument();
    expect(screen.getByTestId("face-down-count")).toHaveTextContent("30");
  });

  it("renders which goods are at the trading house, and its empty slots", () => {
    const state = makeState({ tradingHouse: { goods: ["COFFEE", "SUGAR"] } });

    render(<CentralBoard state={state} />);

    const contents = screen
      .getAllByTestId("trading-house-good")
      .map((li) => li.getAttribute("title"));
    expect(contents).toEqual(["Coffee", "Sugar"]);
    // Two sold, two slots still open — the phase ends the moment the fourth fills.
    expect(screen.getAllByLabelText("Empty slot")).toHaveLength(2);
    expect(screen.getByTestId("trading-house-goods")).toHaveTextContent("2 / 4");
  });

  it("renders the shared supplies", () => {
    const state = makeState({
      colonistSupply: 40,
      colonistsOnShip: 3,
      victoryPointSupply: 61,
      tiles: { faceUp: [], quarriesRemaining: 5, faceDownCount: 12, discardedCount: 1 },
    });

    render(<CentralBoard state={state} />);

    expect(screen.getByTestId("colonist-supply")).toHaveTextContent("40");
    expect(screen.getByTestId("colonists-on-ship")).toHaveTextContent("3");
    expect(screen.getByTestId("victory-point-supply")).toHaveTextContent("61");
    expect(screen.getByTestId("quarries-remaining")).toHaveTextContent("5");
  });

  it("flags the final round only once it has started", () => {
    const { rerender } = render(<CentralBoard state={makeState({ finalRound: false })} />);
    expect(screen.queryByTestId("final-round")).not.toBeInTheDocument();

    rerender(<CentralBoard state={makeState({ finalRound: true })} />);

    expect(screen.getByTestId("final-round")).toHaveAttribute("role", "status");
  });
});
