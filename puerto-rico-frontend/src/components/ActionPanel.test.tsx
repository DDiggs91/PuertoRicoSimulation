import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import type { GameStateView, PlayerAction } from "../api/types";
import { makePending, makeState } from "../test/fixtures";
import { ActionPanel } from "./ActionPanel";

function renderPanel(
  options: PlayerAction[],
  {
    state = makeState(),
    mySeat = 0 as number | null,
    pendingSeat = 0,
    submitting = false,
    moveError = null as string | null,
  } = {},
) {
  const onChoose = vi.fn();
  render(
    <ActionPanel
      mySeat={mySeat}
      pending={makePending(options, { seat: pendingSeat, state })}
      submitting={submitting}
      moveError={moveError}
      onChoose={onChoose}
    />,
  );
  return onChoose;
}

describe("ActionPanel", () => {
  const selectRole: PlayerAction = { type: "SELECT_ROLE", seat: 0, role: "BUILDER" };

  it("shows nothing to a spectator", () => {
    render(
      <ActionPanel
        mySeat={null}
        pending={makePending([selectRole])}
        submitting={false}
        moveError={null}
        onChoose={vi.fn()}
      />,
    );

    expect(screen.queryByTestId("action-panel")).not.toBeInTheDocument();
  });

  it("shows nothing when the decision belongs to another seat", () => {
    renderPanel([selectRole], { mySeat: 0, pendingSeat: 2 });

    expect(screen.queryByTestId("action-panel")).not.toBeInTheDocument();
  });

  it("shows nothing when no decision is pending at all", () => {
    render(
      <ActionPanel
        mySeat={0}
        pending={null}
        submitting={false}
        moveError={null}
        onChoose={vi.fn()}
      />,
    );

    expect(screen.queryByTestId("action-panel")).not.toBeInTheDocument();
  });

  /**
   * The panel's presence *is* the "it's your turn" signal, so the announcement lives on it rather
   * than as a separate thing the board has to keep in sync.
   */
  it("announces the turn and names the phase it is for", () => {
    renderPanel([selectRole]);

    expect(screen.getByTestId("your-turn")).toHaveTextContent("Your turn — Choose a role");
    expect(screen.getByTestId("your-turn")).toHaveAttribute("role", "status");
    expect(screen.getByTestId("action-panel")).toHaveAttribute("data-phase", "ROLE_SELECTION");
  });

  it("passes the chosen action back unchanged, so it is the server's own option object", async () => {
    const user = userEvent.setup();
    const onChoose = renderPanel([selectRole]);

    await user.click(screen.getByTestId("action-select-role-BUILDER"));

    expect(onChoose).toHaveBeenCalledTimes(1);
    expect(onChoose.mock.calls[0]![0]).toBe(selectRole);
  });

  it("disables every option while a move is in flight", () => {
    renderPanel([selectRole], { submitting: true });

    expect(screen.getByTestId("action-select-role-BUILDER")).toBeDisabled();
    expect(screen.getByTestId("move-submitting")).toBeInTheDocument();
  });

  it("surfaces a refused move as an alert", () => {
    renderPanel([selectRole], { moveError: "This decision has already moved on." });

    expect(screen.getByTestId("action-panel-error")).toHaveTextContent(
      "This decision has already moved on.",
    );
    expect(screen.getByTestId("action-panel-error")).toHaveAttribute("role", "alert");
  });

  /**
   * One picker per phase, each drawing only from the options it was handed. The check that matters
   * is that every phase a session can wait in reaches a picker at all — a phase falling through to
   * nothing would strand the player with no way to move and no error to explain it.
   */
  const phases: [GameStateView["phase"]["type"], PlayerAction[], string][] = [
    ["ROLE_SELECTION", [selectRole], "action-select-role-BUILDER"],
    ["SETTLER", [{ type: "TAKE_QUARRY", seat: 0 }], "action-take-quarry"],
    ["MAYOR", [{ type: "END_COLONIST_PLACEMENT", seat: 0 }], "action-end-colonist-placement"],
    ["BUILDER", [{ type: "PASS_BUILDING", seat: 0 }], "action-pass"],
    [
      "CRAFTSMAN_BONUS",
      [{ type: "TAKE_CRAFTSMAN_BONUS", seat: 0, good: "CORN" }],
      "action-craftsman-bonus-CORN",
    ],
    ["TRADER", [{ type: "SELL_GOOD", seat: 0, good: "SUGAR" }], "action-sell-SUGAR"],
    ["CAPTAIN_LOADING", [{ type: "DECLINE_WHARF", seat: 0 }], "action-decline-wharf"],
    [
      "CAPTAIN_STORAGE",
      [{ type: "STORE_GOODS", seat: 0, warehouseKinds: [], singleBarrel: "CORN" }],
      "action-store-0",
    ],
  ];

  it.each(phases)("renders a picker for %s", async (phase, options, testId) => {
    const user = userEvent.setup();
    const onChoose = renderPanel(options, {
      state: makeState({ phase: { type: phase, actorSeat: 0 } }),
    });

    await user.click(screen.getByTestId(testId));

    expect(onChoose).toHaveBeenCalledWith(options[0]);
  });
});
