import type { SessionEvent } from "./types";

export interface GameEventHandlers {
  onEvent: (event: SessionEvent) => void;
  /**
   * Connection state. `EventSource` reconnects on its own, but events emitted while it was down
   * are gone for good — there is no replay — so the caller has to re-fetch `/state` on recovery.
   * Called with false on a drop and true once a message proves the stream is live again.
   */
  onConnectionChange?: (connected: boolean) => void;
}

/** Subscribes to a game's SSE stream; returns an unsubscribe function. */
export function subscribeToGameEvents(
  gameId: string,
  handlers: GameEventHandlers | ((event: SessionEvent) => void),
): () => void {
  const { onEvent, onConnectionChange } =
    typeof handlers === "function"
      ? { onEvent: handlers, onConnectionChange: undefined }
      : handlers;

  const source = new EventSource(`/api/games/${gameId}/events`);
  let connected = true;

  function setConnected(next: boolean) {
    if (next !== connected) {
      connected = next;
      onConnectionChange?.(next);
    }
  }

  source.onmessage = (message) => {
    setConnected(true);
    try {
      onEvent(JSON.parse(message.data) as SessionEvent);
    } catch {
      // A frame we can't parse is one event lost, not a reason to tear the stream down.
    }
  };

  source.onerror = () => {
    // EventSource fires this both for a transient drop (it will retry) and for a closed stream —
    // which is what the server does deliberately once a game ends. Either way the UI should say so.
    setConnected(false);
  };

  return () => source.close();
}
