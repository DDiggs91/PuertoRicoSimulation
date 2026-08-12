import type { SessionEvent } from "../api/types";
import { ACTION_NAMES } from "./art/labels";

export interface EventLogProps {
  events: SessionEvent[];
}

export function EventLog({ events }: EventLogProps) {
  return (
    <section className="panel event-log">
      <h2 className="panel__title">Log</h2>
      {/* role="log" with aria-relevant="additions" tells a screen reader this is an append-only
          feed and to announce only what is new. A bare aria-live list re-reads the whole thing,
          which for a game log hundreds of entries long is unusable. */}
      <ul
        className="event-log__list"
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
    </section>
  );
}

export function describeEvent(event: SessionEvent): string {
  switch (event.type) {
    case "GAME_STARTED":
      return `Game started with ${event.seatNames.join(", ")}`;
    case "DECISION_REQUESTED":
      return `${seatName(event, event.seat)} is deciding`;
    case "ACTION_APPLIED":
      return `${seatName(event, event.seat)} ${ACTION_NAMES[event.action.type] ?? event.action.type}`;
    case "ACTION_REJECTED":
      return `${seatName(event, event.seat)}'s move was rejected: ${event.detail}`;
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

/**
 * Every event carries the board it happened on, so a seat can be named rather than numbered
 * without the log needing state of its own. Falls back to the number if the seat somehow isn't on
 * the board the event carried.
 */
function seatName(event: SessionEvent, seat: number): string {
  return event.view.state.players.find((player) => player.seat === seat)?.name ?? `Seat ${seat}`;
}
