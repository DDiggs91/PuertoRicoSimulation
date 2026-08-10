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
  // A full 3-player game at production AI think-time can take a while — the assertion timeout
  // below only bounds that one wait; the overall test needs headroom too, or Playwright's own
  // default (30s) test timeout kills it first regardless of the assertion's timeout.
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

  // A full 3-player game at production AI think-time can take a while; this is the one place
  // that's genuinely worth a long timeout rather than a flake-prone short one.
  await expect(page.getByTestId("final-standings")).toBeVisible({ timeout: 120_000 });
  const standingRows = page.getByTestId("standing-row");
  await expect(standingRows).toHaveCount(3);
});
