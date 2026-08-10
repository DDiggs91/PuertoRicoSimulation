import type { GameView, ScoreBreakdown, SessionEvent } from "../api/types";
import { EventLog } from "./EventLog";
import { PlayerBoard } from "./PlayerBoard";

export interface GameBoardProps {
  view: GameView;
  events: SessionEvent[];
  standings: ScoreBreakdown[] | null;
}

export function GameBoard({ view, events, standings }: GameBoardProps) {
  const { state } = view;
  const { phase } = state;

  return (
    <div>
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

      <dl aria-label="Central board">
        <dt>Quarries remaining</dt>
        <dd data-testid="quarries-remaining">{state.tiles.quarriesRemaining}</dd>
        <dt>Trading house</dt>
        <dd data-testid="trading-house-goods">{state.tradingHouse.goods.length} / 4</dd>
      </dl>

      <EventLog events={events} />

      {standings && (
        <ol data-testid="final-standings">
          {[...standings]
            .sort((a, b) => b.total - a.total)
            .map((score) => (
              <li key={score.seat} data-testid="standing-row">
                {score.name}: {score.total}
              </li>
            ))}
        </ol>
      )}
    </div>
  );
}
