import { defineConfig, devices } from "@playwright/test";

// Runs against the real, packaged Spring Boot app — not a mocked API — so these tests catch
// wire-contract drift the unit suite can't see. `webServer` boots `java -jar` itself; build the
// jar first with `../mvnw -pl puerto-rico-web -am package -DskipTests`.
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: "html",
  use: {
    baseURL: "http://localhost:8080",
    trace: "on-first-retry",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: {
    // A much shorter AI think-time than production's 300ms default: these tests care that a
    // full game plays out and renders correctly, not that it's paced for a human to watch.
    command:
      "java -Dapp.ai.think-time-ms=20 -jar ../puerto-rico-web/target/puerto-rico-web-0.0.1.jar",
    url: "http://localhost:8080/api/ai/engines",
    reuseExistingServer: !process.env.CI,
    timeout: 60_000,
    stdout: "pipe",
  },
});
