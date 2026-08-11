import type { GameView, ScoreBreakdown, SessionEvent } from "../api/types";
import { CentralBoard } from "./CentralBoard";
import { EventLog } from "./EventLog";
import { PlayerBoard } from "./PlayerBoard";

export interface GameBoardProps {
  view: GameView;
  events: SessionEvent[];
  standings: ScoreBreakdown[] | null;
  /** Set when the session aborted; the board is frozen and this is the only explanation of why. */
  failure?: string | null;
}

export function GameBoard({ view, events, standings, failure }: GameBoardProps) {
  const { state } = view;
  const { phase } = state;

  return (
    <div>
      {failure && (
        <p role="alert" data-testid="session-failure">
          This game stopped: {failure}
        </p>
      )}

      <p data-testid="game-phase" role="status" aria-live="polite">
        {phase.type} — seat {phase.actorSeat} to act
      </p>

      <section aria-label="Players">
        {state.players.map((player) => (
          <PlayerBoard
            key={player.seat}
            player={player}
            isGovernor={player.seat === state.governorSeat}
            isActing={player.seat === phase.actorSeat}
          />
        ))}
      </section>

      <CentralBoard state={state} />

      <EventLog events={events} />

      {/*
        Rendered in the order received. Scorer.finalStandings already ranks by total and then the
        rulebook tiebreak (doubloons + goods), which a re-sort on `total` alone would discard.
      */}
      {standings && (
        <ol data-testid="final-standings">
          {standings.map((score) => (
            <li key={score.seat} data-testid="standing-row">
              {score.name}: {score.total}
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}
