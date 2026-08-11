import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, client, unwrap } from "./client";
import type { Problem } from "./types";

afterEach(() => vi.unstubAllGlobals());

describe("client", () => {
  /**
   * `client` is created once at module load. openapi-fetch would capture `globalThis.fetch` at that
   * moment, so a stub installed later — as every test here and in the component tests does — would
   * be ignored; `client.ts` passes a wrapper that re-reads `globalThis.fetch` per call instead.
   * Stubbing *after* the import above is the whole point of this test.
   */
  it("re-reads globalThis.fetch on every call rather than capturing it at module load", async () => {
    const fetchMock = vi.fn(
      async () =>
        new Response(JSON.stringify([]), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await client.GET("/games");

    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("resolves paths against an absolute base URL", async () => {
    const seen: string[] = [];
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        seen.push(String(input instanceof Request ? input.url : input));
        return new Response(JSON.stringify({ gameId: "abc" }), {
          status: 201,
          headers: { "Content-Type": "application/json" },
        });
      }),
    );

    await client.POST("/games");

    expect(seen[0]).toBe(`${window.location.origin}/api/games`);
  });
});

describe("unwrap", () => {
  it("returns data when there is no error", () => {
    const result = unwrap({ data: { gameId: "abc" }, error: undefined });

    expect(result).toEqual({ gameId: "abc" });
  });

  it("throws an ApiError carrying the problem when there is an error", () => {
    const problem: Problem = { status: 409, title: "Request rejected", reason: "TABLE_FULL" };

    expect(() => unwrap({ data: undefined, error: problem })).toThrow(ApiError);
    try {
      unwrap({ data: undefined, error: problem });
    } catch (e) {
      expect(e).toBeInstanceOf(ApiError);
      expect((e as ApiError).problem).toEqual(problem);
      expect((e as ApiError).message).toBe("Request rejected");
    }
  });
});
