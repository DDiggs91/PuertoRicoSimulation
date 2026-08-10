import type { PlayerStateView } from "../api/types";

export interface PlayerBoardProps {
  player: PlayerStateView;
  isGovernor: boolean;
  isActing: boolean;
}

export function PlayerBoard({ player, isGovernor, isActing }: PlayerBoardProps) {
  return (
    <section
      data-testid={`player-board-${player.seat}`}
      aria-current={isActing ? "true" : undefined}
    >
      <h3>
        {player.name}
        {isGovernor && <span aria-label="Governor">⛨</span>}
      </h3>
      <dl>
        <dt>Doubloons</dt>
        <dd data-testid={`player-${player.seat}-doubloons`}>{player.doubloons}</dd>
        <dt>Victory points</dt>
        <dd data-testid={`player-${player.seat}-victory-points`}>{player.victoryPoints}</dd>
        <dt>Island tiles</dt>
        <dd data-testid={`player-${player.seat}-island-count`}>{player.island.length}</dd>
        <dt>Buildings</dt>
        <dd data-testid={`player-${player.seat}-building-count`}>{player.buildings.length}</dd>
        <dt>Colonists in San Juan</dt>
        <dd data-testid={`player-${player.seat}-colonists`}>{player.colonistsInSanJuan}</dd>
      </dl>
    </section>
  );
}
