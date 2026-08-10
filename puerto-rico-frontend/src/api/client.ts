import createClient from "openapi-fetch";
import type { paths } from "./schema";
import type { Problem } from "./types";

// An absolute URL, not just "/api": openapi-fetch builds a real Request internally, and
// Request — unlike browser fetch("/api/...") — has no page URL to resolve a relative path
// against, so a bare "/api" throws under Node's fetch (as used by Vitest). window.location.origin
// covers both real browsers and jsdom, which stubs a real origin (http://localhost:3000 by
// default) even in tests.
const baseUrl = `${window.location.origin}/api`;

export const client = createClient<paths>({
  baseUrl,
  // openapi-fetch defaults its fetch implementation to `globalThis.fetch` as of the moment
  // createClient() runs (module load), not per-request — so a test's vi.stubGlobal("fetch", ...)
  // in a beforeEach would silently have no effect without this indirection forcing a fresh
  // globalThis.fetch lookup on every call.
  fetch: (...args: Parameters<typeof fetch>) => globalThis.fetch(...args),
});

export class ApiError extends Error {
  readonly problem: Problem;

  constructor(problem: Problem) {
    super(problem.title);
    this.problem = problem;
  }
}

/**
 * openapi-fetch never throws on an HTTP error status — it returns `{ data, error }` and leaves
 * the check to the caller. This is the one place that check happens, so every call site gets a
 * plain value-or-throw instead of repeating the `if (error)` branch.
 */
export function unwrap<T>(result: { data?: T; error?: Problem }): T {
  if (result.error) {
    throw new ApiError(result.error);
  }
  return result.data as T;
}
