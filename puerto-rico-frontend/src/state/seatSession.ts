/**
 * Where a human player's seat token lives between page loads.
 *
 * The server mints a seat token once, at seating time, and never re-issues it — `SeatTokens` has
 * no "look mine up again" path, deliberately, since being able to ask for a token would defeat what
 * the token proves. So a token held only in React state is lost for good on a reload, stranding a
 * player in a seat the server still considers theirs. Persisting it here is what makes a reload
 * mid-game recoverable.
 *
 * Keyed by game id: one browser can hold seats at several tables at once, and a token is only ever
 * valid for the game it was minted for.
 */
export interface SeatSession {
  gameId: string;
  seat: number;
  token: string;
  name: string;
}

const KEY_PREFIX = "puerto-rico.seat.";

function keyFor(gameId: string): string {
  return `${KEY_PREFIX}${gameId}`;
}

/**
 * Every access goes through this. `localStorage` is absent under some server-side/test
 * environments and *throws on access* in a browser with site data blocked — including from the
 * mere property read — so a missing store has to degrade to "this player can't recover from a
 * reload", never to a crash that costs them the game they're in the middle of.
 */
function storage(): Storage | null {
  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

export function saveSeat(session: SeatSession): void {
  try {
    storage()?.setItem(keyFor(session.gameId), JSON.stringify(session));
  } catch {
    // A full or read-only store costs reload recovery, nothing more.
  }
}

export function loadSeat(gameId: string): SeatSession | null {
  try {
    const raw = storage()?.getItem(keyFor(gameId));
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as Partial<SeatSession>;
    // Anything hand-edited, half-written, or left by an older version of this shape is treated as
    // absent rather than trusted — a bad token would fail the server's check anyway, but a bad
    // `seat` would have the UI claim a seat that isn't the player's.
    if (typeof parsed.seat !== "number" || typeof parsed.token !== "string") {
      return null;
    }
    return {
      gameId,
      seat: parsed.seat,
      token: parsed.token,
      name: typeof parsed.name === "string" ? parsed.name : "You",
    };
  } catch {
    return null;
  }
}

export function clearSeat(gameId: string): void {
  try {
    storage()?.removeItem(keyFor(gameId));
  } catch {
    // Nothing to do: the entry outliving its game is harmless, since the token is game-scoped.
  }
}
