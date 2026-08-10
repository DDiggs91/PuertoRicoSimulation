import type { SessionEvent } from "./types";

/** Subscribes to a game's SSE stream; returns an unsubscribe function. */
export function subscribeToGameEvents(
  gameId: string,
  onEvent: (event: SessionEvent) => void,
): () => void {
  const source = new EventSource(`/api/games/${gameId}/events`);
  source.onmessage = (message) => {
    onEvent(JSON.parse(message.data) as SessionEvent);
  };
  return () => source.close();
}
