import type { GoodsMap, PlayerStateView } from "../api/types";
import { BuildingCard } from "./art/BuildingCard";
import { GOOD_NAMES, GOOD_ORDER } from "./art/labels";
import { Colonist, Doubloon, GoodBarrel, PlantationTile } from "./art/Pieces";

export interface PlayerBoardProps {
  player: PlayerStateView;
  isGovernor: boolean;
  isActing: boolean;
  /** True for the seat this client holds, so a player can find their own board at a glance. */
  isYou?: boolean;
}

/** Goods in the order the trading house lists them, skipping kinds the player holds none of. */
export function heldGoods(goods: GoodsMap): [string, number][] {
  return GOOD_ORDER.map((kind) => [kind, goods[kind] ?? 0] as [string, number]).filter(
    ([, count]) => count > 0,
  );
}

export function PlayerBoard({ player, isGovernor, isActing, isYou = false }: PlayerBoardProps) {
  const goods = heldGoods(player.goods);

  return (
    <section
      className="panel player-board"
      data-testid={`player-board-${player.seat}`}
      data-you={isYou}
      data-acting={isActing}
      aria-current={isActing ? "true" : undefined}
    >
      <header className="player-board__header">
        <h3 className="player-board__name">
          {player.name}
          {isGovernor && (
            <span className="badge badge--governor" aria-label="Governor">
              ⛨ Governor
            </span>
          )}
          {isYou && (
            <span className="badge badge--you" data-testid={`player-${player.seat}-is-you`}>
              You
            </span>
          )}
        </h3>
        <dl className="player-board__tally">
          <div>
            <dt>Doubloons</dt>
            <dd data-testid={`player-${player.seat}-doubloons`}>
              <Doubloon count={player.doubloons} size={20} />
            </dd>
          </div>
          <div>
            <dt>Victory points</dt>
            <dd data-testid={`player-${player.seat}-victory-points`}>{player.victoryPoints}</dd>
          </div>
          <div>
            <dt>Island tiles</dt>
            <dd data-testid={`player-${player.seat}-island-count`}>{player.island.length}</dd>
          </div>
          <div>
            <dt>Buildings</dt>
            <dd data-testid={`player-${player.seat}-building-count`}>{player.buildings.length}</dd>
          </div>
          <div>
            <dt>Colonists in San Juan</dt>
            <dd data-testid={`player-${player.seat}-colonists`}>
              <Colonist size={18} />
              {player.colonistsInSanJuan}
            </dd>
          </div>
        </dl>
      </header>

      {/* Which tiles and buildings, not just how many — a plantation is only productive once a
          colonist is on it, so occupancy is as much a part of the board as the tile itself. */}
      <ul
        className="player-board__island"
        data-testid={`player-${player.seat}-island`}
        aria-label={`${player.name}'s island`}
      >
        {player.island.map((tile, index) => (
          <li
            key={index}
            data-testid={`player-${player.seat}-island-tile`}
            data-occupied={tile.occupied}
          >
            <PlantationTile type={tile.type} occupied={tile.occupied} size={46} />
          </li>
        ))}
      </ul>

      <ul
        className="player-board__city"
        data-testid={`player-${player.seat}-buildings`}
        aria-label={`${player.name}'s city`}
      >
        {player.buildings.map((building, index) => (
          <li key={index} data-testid={`player-${player.seat}-building`}>
            <BuildingCard
              type={building.type}
              victoryPoints={building.victoryPoints}
              capacity={building.capacity}
              colonists={building.colonists}
            />
          </li>
        ))}
      </ul>

      <ul
        className="player-board__goods"
        data-testid={`player-${player.seat}-goods`}
        aria-label={`${player.name}'s goods`}
      >
        {goods.map(([kind, count]) => (
          <li key={kind} data-testid={`player-${player.seat}-good-${kind}`}>
            <GoodBarrel good={kind as keyof typeof GOOD_NAMES} size={20} />
            <span>× {count}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}
