import type { SessionEvent } from "../api/types";

export interface EventLogProps {
  events: SessionEvent[];
}

export function EventLog({ events }: EventLogProps) {
  return (
    // role="log" with aria-relevant="additions" tells a screen reader this is an append-only feed
    // and to announce only what is new. A bare aria-live list re-reads the whole thing, which for a
    // game log hundreds of entries long is unusable.
    <ul
      data-testid="event-log"
      role="log"
      aria-label="Event log"
      aria-live="polite"
      aria-relevant="additions"
    >
      {events.map((event, index) => (
        <li key={index} data-testid="event-log-entry">
          {describeEvent(event)}
        </li>
      ))}
    </ul>
  );
}

export function describeEvent(event: SessionEvent): string {
  switch (event.type) {
    case "GAME_STARTED":
      return `Game started with ${event.seatNames.join(", ")}`;
    case "DECISION_REQUESTED":
      return `Seat ${event.seat} is deciding`;
    case "ACTION_APPLIED":
      return `Seat ${event.seat} played ${event.action.type}`;
    case "ACTION_REJECTED":
      return `Seat ${event.seat}'s move was rejected: ${event.detail}`;
    case "GAME_ENDED":
      return "Game ended";
    case "SESSION_FAILED":
      return `Session failed: ${event.detail}`;
    default: {
      const exhaustive: never = event;
      throw new Error(`Unhandled SessionEvent: ${JSON.stringify(exhaustive)}`);
    }
  }
}
