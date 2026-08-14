import { expect, test } from "@playwright/test";
import type { Page } from "@playwright/test";

/**
 * A human seat played end to end against the real packaged app — the half of this module that had
 * no client until now. The unit suite covers each picker against fixtures; what only a real server
 * can prove is that every action family the engine actually produces reaches a picker that can
 * submit it, with the wire shapes matching in both directions.
 */

/** Creates a table, seats this browser as a human plus `bots` AI opponents, and starts it. */
async function seatAndStart(page: Page, bots: number) {
  await page.goto("/");
  await page.getByTestId("lobby-create-game").click();

  await page.getByTestId("human-name-input").fill("Dani");
  await page.getByTestId("take-human-seat").click();
  await expect(page.getByTestId("your-seat-badge")).toContainText("seat 0");

  for (let i = 0; i < bots; i++) {
    await page.getByTestId("add-ai-seat").click();
  }

  await expect(page.getByTestId("start-game")).toBeEnabled();
  await page.getByTestId("start-game").click();
}

test("a seated human is told it is their turn and can choose a role", async ({ page }) => {
  test.setTimeout(60_000);

  await seatAndStart(page, 2);

  // Seat 0 is the governor of a fresh game, so the first decision is this player's.
  await expect(page.getByTestId("your-turn")).toBeVisible();
  await expect(page.getByTestId("action-panel")).toHaveAttribute("data-phase", "ROLE_SELECTION");
  await expect(page.getByTestId("player-board-0")).toHaveAttribute("data-you", "true");

  // Every option is built from the server's own legal list, so any role shown is takeable.
  const firstRole = page.getByTestId(/^action-select-role-/).first();
  await expect(firstRole).toBeVisible();
  await firstRole.click();

  // A 202 is not confirmation — the board moving on is. Role selection is over for this player,
  // so whatever comes next is a different phase (or another seat's turn entirely).
  await expect(page.getByTestId("action-panel")).not.toHaveAttribute(
    "data-phase",
    "ROLE_SELECTION",
    { timeout: 30_000 },
  );
});

/**
 * The reload case §1 calls out: a seat token is minted once and never re-issued, so losing it
 * means being locked out of a seat the server still considers yours.
 */
test("a mid-game reload keeps the seat and the pending decision", async ({ page }) => {
  test.setTimeout(60_000);

  await seatAndStart(page, 2);
  await expect(page.getByTestId("your-turn")).toBeVisible();

  await page.reload();

  await expect(page.getByTestId("seated-as")).toContainText("Dani");
  // Restored from GET /decision, not from the stream: DECISION_REQUESTED fired before this page
  // existed and nothing replays it.
  await expect(page.getByTestId("your-turn")).toBeVisible();
  await expect(page.getByTestId(/^action-select-role-/).first()).toBeVisible();
});

/**
 * A game played by clicking whatever is legal, asserting on the way through that every action
 * family the engine actually asks for has a picker able to answer it. Clicking "the first option"
 * is not strategy — it is the cheapest way to reach every phase a real game passes through, which
 * is what this is here to cover.
 *
 * Every click is a real browser round trip, so this plays for a fixed budget and asserts on the
 * phases it got through rather than on reaching the final score; `game.spec.ts` already covers a
 * game played out to standings, where the AI seats need no browser at all.
 */
test("a human can play through every phase family the game reaches", async ({ page }) => {
  test.setTimeout(180_000);

  await seatAndStart(page, 2);

  const panel = page.getByTestId("action-panel");
  // Disabled means a move is already in flight for this decision — clicking again would offer a
  // request id the server has moved past.
  const nextOption = panel.locator("[data-action-option]:not([disabled])").first();
  const standings = page.getByTestId("final-standings");
  const phasesSeen = new Set<string>();

  // Which role this player still wants. Taking roles deliberately rather than clicking whatever
  // comes first is what makes the phases this test reaches its own choice instead of a bet on what
  // the random AIs happen to pick — the difference between a deterministic run and a coin flip.
  const wanted = ["SETTLER", "MAYOR", "BUILDER", "CRAFTSMAN", "TRADER", "CAPTAIN"];

  // The phases every round passes through, and so the ones this player is guaranteed to reach by
  // taking those roles. Play stops as soon as all four have been played rather than running the
  // game out — the point is that each picker can answer a real decision, which a fifth builder
  // turn says nothing more about.
  const required = ["ROLE_SELECTION", "SETTLER", "MAYOR", "BUILDER"];
  const covered = () => required.every((phase) => phasesSeen.has(phase));
  const deadline = Date.now() + 120_000;

  while (Date.now() < deadline && !covered() && !(await standings.isVisible())) {
    // Either it's this player's turn or an AI is thinking; both are ordinary, so this polls rather
    // than waiting on a panel that may not be coming for a second or two.
    if (!(await nextOption.isVisible())) {
      await page.waitForTimeout(50);
      continue;
    }

    const phase = await panel.getAttribute("data-phase");
    let option = nextOption;
    if (phase === "ROLE_SELECTION") {
      // The first role still wanted that is still on the table; roles return to the pool each
      // round, so a role missed this round comes back.
      for (const role of wanted) {
        const card = panel.getByTestId(`action-select-role-${role}`);
        if (await card.isVisible()) {
          option = card;
          wanted.splice(wanted.indexOf(role), 1);
          break;
        }
      }
    }
    if (phase === "MAYOR") {
      // Mayor is staged locally rather than submitted move by move: the circle buttons here don't
      // carry `data-action-option` at all (see `pickerTypes.ts` and `ActionButton`'s `submits`
      // prop), so `nextOption` never lands on one — only Finalize does. Placing the colonists this
      // turn dealt is what makes finalizing legal, so that staging has to happen here explicitly. A
      // "Staff …" accessible name is a placement in either picker (an empty island tile or a
      // building circle); it disappears on its own once nothing is left to place.
      const staff = panel.getByRole("button", { name: /^Staff /, disabled: false });
      while (
        await staff
          .first()
          .isVisible()
          .catch(() => false)
      ) {
        await staff.first().click({ timeout: 5_000 });
      }
      option = panel.getByTestId("action-end-colonist-placement");
    }

    try {
      await option.click({ timeout: 5_000 });
    } catch {
      // The decision was answered and the panel re-rendered out from under the click. Nothing to
      // recover — the loop re-reads whatever is on offer now.
      continue;
    }
    if (phase) {
      phasesSeen.add(phase);
    }

    // A rejected move would mean the UI offered something the server refused — the one outcome
    // building every button from `Decision.options` is supposed to make impossible.
    await expect(page.getByTestId("action-panel-error")).toHaveCount(0);
  }

  for (const phase of required) {
    expect(
      phasesSeen,
      `expected to have played a ${phase} decision; played: ${[...phasesSeen].join(", ")}`,
    ).toContain(phase);
  }
});
