import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

// Dev server proxies /api to the Spring Boot backend so `npm run dev` can hit a locally
// running puerto-rico-web without a CORS setup; the production build is served *by*
// puerto-rico-web itself (see puerto-rico-web/pom.xml's dependency on this module), so no
// proxy exists there.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
  build: {
    outDir: "target/classes/META-INF/resources",
    emptyOutDir: true,
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    css: false,
    // e2e/ holds Playwright specs, which define their own test() — Vitest's default include
    // glob would otherwise pick those files up too and collide with Playwright's runner.
    exclude: ["e2e/**", "node_modules/**"],
  },
});
