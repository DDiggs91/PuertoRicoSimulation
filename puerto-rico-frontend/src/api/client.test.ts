import { describe, expect, it } from "vitest";
import { ApiError, unwrap } from "./client";
import type { Problem } from "./types";

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
