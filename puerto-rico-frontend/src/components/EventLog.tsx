import type { SessionEvent } from "../api/types";

export interface EventLogProps {
  events: SessionEvent[];
}

export function EventLog({ events }: EventLogProps) {
  return (
    <ul data-testid="event-log" aria-live="polite">
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
