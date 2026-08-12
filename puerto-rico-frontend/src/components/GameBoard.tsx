import type { GameView, PlayerAction, ScoreBreakdown, SessionEvent } from "../api/types";
import type { PendingDecision } from "../state/gameReducer";
import { ActionPanel } from "./ActionPanel";
import { PHASE_NAMES } from "./art/labels";
import { CentralBoard } from "./CentralBoard";
import { EventLog } from "./EventLog";
import { PlayerBoard } from "./PlayerBoard";

export interface GameBoardProps {
  view: GameView;
  events: SessionEvent[];
  standings: ScoreBreakdown[] | null;
  /** Set when the session aborted; the board is frozen and this is the only explanation of why. */
  failure?: string | null;
  /** The seat this client holds a token for; null while spectating. */
  mySeat?: number | null;
  pending?: PendingDecision | null;
  moveError?: string | null;
  submitting?: boolean;
  onChoose?: (action: PlayerAction) => void;
}

export function GameBoard({
  view,
  events,
  standings,
  failure,
  mySeat = null,
  pending = null,
  moveError = null,
  submitting = false,
  onChoose,
}: GameBoardProps) {
  const { state } = view;
  const { phase } = state;
  const myTurn = mySeat !== null && pending !== null && pending.seat === mySeat;
  // Absent once the game is over, where actorSeat is -1 by contract.
  const actor = state.players.find((player) => player.seat === phase.actorSeat);

  return (
    <div className="board" data-my-turn={myTurn}>
      {failure && (
        <p className="notice notice--alert" role="alert" data-testid="session-failure">
          This game stopped: {failure}
        </p>
      )}

      <p className="phase-line" data-testid="game-phase" role="status" aria-live="polite">
        {PHASE_NAMES[phase.type] ?? phase.type}
        {actor && ` — ${actor.name} to act`}
      </p>

      {/* Ahead of the board, not below it: when it is your turn this is the thing to look at, and
          a panel that has to be scrolled to is a panel that gets missed. */}
      {onChoose && (
        <ActionPanel
          mySeat={mySeat}
          pending={pending}
          submitting={submitting}
          moveError={moveError}
          onChoose={onChoose}
        />
      )}

      <CentralBoard state={state} />

      <section className="board__players" aria-label="Players">
        {state.players.map((player) => (
          <PlayerBoard
            key={player.seat}
            player={player}
            isGovernor={player.seat === state.governorSeat}
            isActing={player.seat === phase.actorSeat}
            isYou={player.seat === mySeat}
          />
        ))}
      </section>

      <EventLog events={events} />

      {/*
        Rendered in the order received. Scorer.finalStandings already ranks by total and then the
        rulebook tiebreak (doubloons + goods), which a re-sort on `total` alone would discard.
      */}
      {standings && (
        <section className="panel standings">
          <h2 className="panel__title">Final standings</h2>
          <ol data-testid="final-standings" className="standings__list">
            {standings.map((score) => (
              <li key={score.seat} data-testid="standing-row" className="standings__row">
                <span className="standings__name">{score.name}</span>
                <span className="standings__total">{score.total}</span>
              </li>
            ))}
          </ol>
        </section>
      )}
    </div>
  );
}
