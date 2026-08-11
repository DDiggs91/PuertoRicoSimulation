import { expect, test } from "@playwright/test";

test("lobby lists a created game and reflects seats as they fill", async ({ page }) => {
  await page.goto("/");

  await page.getByTestId("lobby-create-game").click();
  const gameList = page.getByTestId("game-list");
  await expect(gameList).toContainText("OPEN");
  await expect(gameList).toContainText("0 seated");

  await page.getByTestId("add-ai-seat").click();
  await expect(gameList).toContainText("1 seated");

  await page.getByTestId("add-ai-seat").click();
  await expect(gameList).toContainText("2 seated");
});

test("creating, seating three AIs, and starting a game plays it to final standings", async ({
  page,
}) => {
  // This covers a full 3-player game at the e2e think-time playwright.config.ts starts the server
  // with (20ms), not production's 300ms. The headroom is for the reuseExistingServer case, where a
  // server already running locally may well be at the production default. The assertion timeout
  // below only bounds that one wait; the overall test needs its own, or Playwright's default (30s)
  // kills it first regardless.
  test.setTimeout(150_000);

  await page.goto("/");

  await page.getByTestId("lobby-create-game").click();
  await page.getByTestId("add-ai-seat").click();
  await page.getByTestId("add-ai-seat").click();
  await page.getByTestId("add-ai-seat").click();

  await expect(page.getByTestId("start-game")).toBeEnabled();
  await page.getByTestId("start-game").click();

  // The board renders live as the game progresses.
  await expect(page.getByTestId("player-board-0")).toBeVisible();
  await expect(page.getByTestId("player-board-1")).toBeVisible();
  await expect(page.getByTestId("player-board-2")).toBeVisible();
  await expect(page.getByTestId("game-phase")).toBeVisible();
  await expect(page.getByTestId("event-log")).toBeVisible();

  // The central board: everything shared, which a spectator needs as much as the player boards.
  await expect(page.getByTestId("central-board")).toBeVisible();
  await expect(page.getByTestId("role-track")).toBeVisible();
  await expect(page.getByTestId("cargo-ships")).toBeVisible();
  await expect(page.getByTestId("cargo-ship-0")).toBeVisible();
  await expect(page.getByTestId("face-up-tiles")).toBeVisible();
  await expect(page.getByTestId("colonist-supply")).toBeVisible();
  await expect(page.getByTestId("victory-point-supply")).toBeVisible();

  // And a player's own board in detail, not just counts.
  await expect(page.getByTestId("player-0-island")).toBeVisible();
  await expect(page.getByTestId("player-0-island-tile").first()).toBeVisible();

  // A whole game, even at the 20ms e2e think-time, is the one place genuinely worth a long
  // timeout rather than a flake-prone short one.
  await expect(page.getByTestId("final-standings")).toBeVisible({ timeout: 120_000 });
  const standingRows = page.getByTestId("standing-row");
  await expect(standingRows).toHaveCount(3);
});

test("a game started in one tab can be watched from the lobby list in another", async ({
  browser,
}) => {
  test.setTimeout(150_000);

  const host = await browser.newPage();
  await host.goto("/");
  await host.getByTestId("lobby-create-game").click();
  for (let i = 0; i < 3; i++) {
    await host.getByTestId("add-ai-seat").click();
  }
  await expect(host.getByTestId("start-game")).toBeEnabled();
  await host.getByTestId("start-game").click();
  await expect(host.getByTestId("game-phase")).toBeVisible();

  // A second visitor who was never handed the ?game= URL: the lobby polls, so the started game
  // turns up on its own, and its row is what gets them into the spectator view.
  const spectator = await browser.newPage();
  await spectator.goto("/");
  const watchButton = spectator.getByTestId(/^watch-game-/).first();
  await expect(watchButton).toBeVisible({ timeout: 30_000 });
  await watchButton.click();

  await expect(spectator.getByTestId("central-board")).toBeVisible();
  await expect(spectator.getByTestId("player-board-0")).toBeVisible();

  await host.close();
  await spectator.close();
});
