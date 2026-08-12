import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import type { PlayerAction } from "../../api/types";
import { makeBuilding, makePlayer, makeState } from "../../test/fixtures";
import { BuilderPicker } from "./BuilderPicker";
import { CaptainPicker } from "./CaptainPicker";
import { CaptainStoragePicker } from "./CaptainStoragePicker";
import { CraftsmanBonusPicker } from "./CraftsmanBonusPicker";
import { MayorPicker } from "./MayorPicker";
import { RoleSelectionPicker } from "./RoleSelectionPicker";
import { SettlerPicker } from "./SettlerPicker";
import { TraderPicker } from "./TraderPicker";
import type { PickerProps } from "./pickerTypes";

/**
 * The pickers share one contract, so most of what's worth asserting is shared too: they draw one
 * button per option they were handed, and hand the *same object* back on click. That identity is
 * the whole safety argument — an action the client assembled itself could differ from the one the
 * server offered, and `HumanActor.offer` compares by equality.
 */
function props(options: PlayerAction[], overrides: Partial<PickerProps> = {}): PickerProps {
  return {
    options,
    state: makeState(),
    seat: 0,
    submitting: false,
    onChoose: vi.fn(),
    ...overrides,
  };
}

describe("RoleSelectionPicker", () => {
  it("draws one card per offered role, with the doubloons piled on it", () => {
    const p = props(
      [
        { type: "SELECT_ROLE", seat: 0, role: "BUILDER" },
        { type: "SELECT_ROLE", seat: 0, role: "CAPTAIN" },
      ],
      {
        state: makeState({
          roles: {
            cards: [
              { role: "BUILDER", doubloons: 0, takenBySeat: null },
              { role: "CAPTAIN", doubloons: 3, takenBySeat: null },
              // Already taken this round, so it isn't among the options and isn't drawn.
              { role: "SETTLER", doubloons: 0, takenBySeat: 1 },
            ],
          },
        }),
      },
    );

    render(<RoleSelectionPicker {...p} />);

    expect(screen.getAllByTestId(/^action-select-role-/)).toHaveLength(2);
    expect(screen.getByTestId("role-coins-CAPTAIN")).toBeInTheDocument();
    expect(screen.queryByTestId("role-coins-BUILDER")).not.toBeInTheDocument();
    expect(screen.queryByTestId("action-select-role-SETTLER")).not.toBeInTheDocument();
  });

  it("hands back the option object it was given", async () => {
    const user = userEvent.setup();
    const option: PlayerAction = { type: "SELECT_ROLE", seat: 0, role: "TRADER" };
    const p = props([option], {
      state: makeState({ roles: { cards: [{ role: "TRADER", doubloons: 1, takenBySeat: null }] } }),
    });

    render(<RoleSelectionPicker {...p} />);
    await user.click(screen.getByTestId("action-select-role-TRADER"));

    expect(p.onChoose).toHaveBeenCalledWith(option);
  });
});

describe("SettlerPicker", () => {
  it("draws the face-up row by index, plus the quarry and pass options", async () => {
    const user = userEvent.setup();
    const tile: PlayerAction = { type: "TAKE_FACE_UP_TILE", seat: 0, faceUpIndex: 1 };
    const p = props([tile, { type: "TAKE_QUARRY", seat: 0 }, { type: "PASS_SETTLING", seat: 0 }], {
      state: makeState({
        tiles: {
          faceUp: ["CORN", "COFFEE", "SUGAR"],
          quarriesRemaining: 5,
          faceDownCount: 10,
          discardedCount: 0,
        },
      }),
    });

    render(<SettlerPicker {...p} />);

    // Only index 1 was offered, so only index 1 is clickable — the row is not drawn wholesale.
    expect(screen.queryByTestId("action-take-tile-0")).not.toBeInTheDocument();
    expect(screen.getByTestId("action-take-tile-1")).toHaveTextContent("Coffee plantation");
    expect(screen.getByTestId("action-take-quarry")).toHaveTextContent("5 left");
    expect(screen.getByTestId("action-pass")).toBeInTheDocument();

    await user.click(screen.getByTestId("action-take-tile-1"));
    expect(p.onChoose).toHaveBeenCalledWith(tile);
  });

  it("offers the Hacienda's take-or-skip when that is the decision", () => {
    const p = props([
      { type: "TAKE_HACIENDA_TILE", seat: 0 },
      { type: "SKIP_HACIENDA", seat: 0 },
    ]);

    render(<SettlerPicker {...p} />);

    expect(screen.getByTestId("action-take-hacienda-tile")).toBeInTheDocument();
    expect(screen.getByTestId("action-skip-hacienda")).toBeInTheDocument();
  });
});

describe("MayorPicker", () => {
  const player = makePlayer({
    seat: 0,
    colonistsInSanJuan: 2,
    island: [
      { type: "CORN", occupied: false },
      { type: "INDIGO", occupied: true },
    ],
    buildings: [makeBuilding("HOSPICE", { colonists: 0, capacity: 1, victoryPoints: 2 })],
  });

  it("makes clickable exactly the slots the server offered, and draws the rest inert", async () => {
    const user = userEvent.setup();
    const island: PlayerAction = {
      type: "PLACE_COLONIST",
      seat: 0,
      slot: { type: "ISLAND", index: 0 },
    };
    const building: PlayerAction = {
      type: "PLACE_COLONIST",
      seat: 0,
      slot: { type: "BUILDING", index: 0 },
    };
    const p = props([island, building, { type: "END_COLONIST_PLACEMENT", seat: 0 }], {
      state: makeState({ players: [player], phase: { type: "MAYOR", actorSeat: 0 } }),
    });

    render(<MayorPicker {...p} />);

    expect(screen.getByTestId("colonists-in-hand")).toHaveTextContent("2 in San Juan");
    expect(screen.getByTestId("action-place-colonist-ISLAND-0")).toBeInTheDocument();
    // Index 1 is already staffed, so it was not offered — drawn, but not a button.
    expect(screen.queryByTestId("action-place-colonist-ISLAND-1")).not.toBeInTheDocument();

    await user.click(screen.getByTestId("action-place-colonist-BUILDING-0"));
    expect(p.onChoose).toHaveBeenCalledWith(building);
  });
});

describe("BuilderPicker", () => {
  it("shows the discounted cost the server quoted, not a price of its own", async () => {
    const user = userEvent.setup();
    const build: PlayerAction = { type: "BUILD_BUILDING", seat: 0, buildingType: "HOSPICE" };
    const p = props([build, { type: "PASS_BUILDING", seat: 0 }], {
      state: makeState({
        phase: {
          type: "BUILDER",
          actorSeat: 0,
          // Printed 4, quoted at 2 — a privilege and a quarry, arithmetic this component never does.
          buildOptions: [
            { buildingType: "HOSPICE", cost: 2, victoryPoints: 2, colonistCapacity: 1 },
          ],
        },
      }),
    });

    render(<BuilderPicker {...p} />);

    expect(screen.getByTestId("building-cost-HOSPICE")).toHaveTextContent("2");
    expect(screen.getByTestId("action-build-HOSPICE")).toHaveAccessibleName(
      "Build the Hospice for 2 doubloons",
    );

    await user.click(screen.getByTestId("action-build-HOSPICE"));
    expect(p.onChoose).toHaveBeenCalledWith(build);
  });
});

describe("CraftsmanBonusPicker", () => {
  it("offers only the goods the server listed, plus passing", async () => {
    const user = userEvent.setup();
    const take: PlayerAction = { type: "TAKE_CRAFTSMAN_BONUS", seat: 0, good: "TOBACCO" };
    const p = props([take, { type: "PASS_CRAFTSMAN_BONUS", seat: 0 }]);

    render(<CraftsmanBonusPicker {...p} />);

    expect(screen.getAllByTestId(/^action-craftsman-bonus-/)).toHaveLength(1);
    await user.click(screen.getByTestId("action-craftsman-bonus-TOBACCO"));
    expect(p.onChoose).toHaveBeenCalledWith(take);
  });
});

describe("TraderPicker", () => {
  it("prices each sale from the wire", async () => {
    const user = userEvent.setup();
    const sell: PlayerAction = { type: "SELL_GOOD", seat: 0, good: "COFFEE" };
    const p = props([sell, { type: "PASS_TRADING", seat: 0 }], {
      state: makeState({
        phase: {
          type: "TRADER",
          actorSeat: 0,
          goodPrices: [
            { good: "COFFEE", price: 6 },
            { good: "CORN", price: 1 },
          ],
        },
      }),
    });

    render(<TraderPicker {...p} />);

    expect(screen.getByTestId("sell-price-COFFEE")).toHaveTextContent("6");
    // Corn is priced on the wire but wasn't offered, so it isn't sellable here.
    expect(screen.queryByTestId("action-sell-CORN")).not.toBeInTheDocument();

    await user.click(screen.getByTestId("action-sell-COFFEE"));
    expect(p.onChoose).toHaveBeenCalledWith(sell);
  });
});

describe("CaptainPicker", () => {
  it("names the ship each load would go onto", async () => {
    const user = userEvent.setup();
    const load: PlayerAction = { type: "LOAD_SHIP", seat: 0, shipIndex: 1, good: "SUGAR" };
    const p = props([load], {
      state: makeState({
        ships: [
          { capacity: 4, cargo: "CORN", loaded: 2 },
          { capacity: 5, cargo: null, loaded: 0 },
        ],
      }),
    });

    render(<CaptainPicker {...p} />);

    expect(screen.getByTestId("action-load-ship-1-SUGAR")).toHaveTextContent("0 of 5 holds full");
    await user.click(screen.getByTestId("action-load-ship-1-SUGAR"));
    expect(p.onChoose).toHaveBeenCalledWith(load);
  });

  it("offers the Wharf and declining it together, since only that pairing is optional", () => {
    const p = props([
      { type: "LOAD_WHARF", seat: 0, good: "INDIGO" },
      { type: "DECLINE_WHARF", seat: 0 },
    ]);

    render(<CaptainPicker {...p} />);

    expect(screen.getByTestId("action-load-wharf-INDIGO")).toBeInTheDocument();
    expect(screen.getByTestId("action-decline-wharf")).toBeInTheDocument();
  });
});

describe("CaptainStoragePicker", () => {
  /**
   * Each option is a finished, legal combination from the server. Presenting them whole is what
   * keeps the UI from composing a selection the rules forbid.
   */
  it("spells out each whole combination it was offered", async () => {
    const user = userEvent.setup();
    const both: PlayerAction = {
      type: "STORE_GOODS",
      seat: 0,
      warehouseKinds: ["COFFEE", "TOBACCO"],
      singleBarrel: "CORN",
    };
    const nothing: PlayerAction = { type: "STORE_GOODS", seat: 0, warehouseKinds: [] };
    const p = props([both, nothing]);

    render(<CaptainStoragePicker {...p} />);

    expect(screen.getByTestId("action-store-0")).toHaveTextContent(
      "Keep all your Coffee and Tobacco, plus one Corn",
    );
    expect(screen.getByTestId("action-store-1")).toHaveTextContent(
      "Keep nothing — let it all spoil",
    );

    await user.click(screen.getByTestId("action-store-0"));
    expect(p.onChoose).toHaveBeenCalledWith(both);
  });
});
