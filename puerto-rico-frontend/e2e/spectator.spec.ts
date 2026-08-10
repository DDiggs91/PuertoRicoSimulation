import { expect, test } from "@playwright/test";

test("a spectator opening mid-game gets the current board, not just future events", async ({
  page,
  browser,
}) => {
  await page.goto("/");
  await page.getByTestId("lobby-create-game").click();
  await page.getByTestId("add-ai-seat").click();
  await page.getByTestId("add-ai-seat").click();
  await page.getByTestId("add-ai-seat").click();
  await page.getByTestId("start-game").click();

  // Let the game actually progress a little before anyone else joins, so this is a genuine
  // mid-game join rather than a start-game race.
  await expect(page.getByTestId("event-log-entry").nth(2)).toBeVisible({ timeout: 30_000 });
  const gameUrl = page.url();

  const spectatorContext = await browser.newContext();
  const spectatorPage = await spectatorContext.newPage();
  try {
    await spectatorPage.goto(gameUrl);

    // /state bootstraps the board immediately — this must not depend on waiting for the next
    // SSE event, which is exactly what a naive events-only implementation would get wrong.
    await expect(spectatorPage.getByTestId("player-board-0")).toBeVisible();
    await expect(spectatorPage.getByTestId("game-phase")).toBeVisible();
  } finally {
    await spectatorContext.close();
  }
});
