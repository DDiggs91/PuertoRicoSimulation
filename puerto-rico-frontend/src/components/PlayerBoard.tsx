import type { GoodsMap, PlayerStateView } from "../api/types";

export interface PlayerBoardProps {
  player: PlayerStateView;
  isGovernor: boolean;
  isActing: boolean;
}

/** Goods in the order the trading house lists them, skipping kinds the player holds none of. */
export function heldGoods(goods: GoodsMap): [string, number][] {
  const order = ["CORN", "INDIGO", "SUGAR", "TOBACCO", "COFFEE"];
  return order
    .map((kind) => [kind, goods[kind] ?? 0] as [string, number])
    .filter(([, count]) => count > 0);
}

export function PlayerBoard({ player, isGovernor, isActing }: PlayerBoardProps) {
  const goods = heldGoods(player.goods);

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

      {/* Which tiles and buildings, not just how many — a plantation is only productive once a
          colonist is on it, so occupancy is as much a part of the board as the tile itself. */}
      <ul data-testid={`player-${player.seat}-island`} aria-label={`${player.name}'s island`}>
        {player.island.map((tile, index) => (
          <li
            key={index}
            data-testid={`player-${player.seat}-island-tile`}
            data-occupied={tile.occupied}
          >
            {tile.type}
            {tile.occupied ? " (staffed)" : " (idle)"}
          </li>
        ))}
      </ul>

      <ul data-testid={`player-${player.seat}-buildings`} aria-label={`${player.name}'s city`}>
        {player.buildings.map((building, index) => (
          <li key={index} data-testid={`player-${player.seat}-building`}>
            {building.type} — {building.colonists} colonist
            {building.colonists === 1 ? "" : "s"}
          </li>
        ))}
      </ul>

      <ul data-testid={`player-${player.seat}-goods`} aria-label={`${player.name}'s goods`}>
        {goods.map(([kind, count]) => (
          <li key={kind} data-testid={`player-${player.seat}-good-${kind}`}>
            {kind} × {count}
          </li>
        ))}
      </ul>
    </section>
  );
}
