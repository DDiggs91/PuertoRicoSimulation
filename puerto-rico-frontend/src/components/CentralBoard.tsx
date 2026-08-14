import type { GameStateView } from "../api/types";
import { BuildingDisplay } from "./BuildingDisplay";
import { GOOD_NAMES, ROLE_NAMES, ROLE_SUMMARIES, TILE_NAMES } from "./art/labels";
import { Colonist, Doubloon, GoodBarrel, PlantationTile, ShipArt } from "./art/Pieces";
import { RoleIcon } from "./art/RoleIcon";

export interface CentralBoardProps {
  state: GameStateView;
}

const TRADING_HOUSE_SLOTS = 4;

/**
 * Everything on the table that isn't a player's own board: the role track, the cargo ships, the
 * face-up plantation row, the trading house, and the shared supplies. All of it already rides on
 * `GameStateView`; a spectator can't follow a game from per-player counts alone.
 */
export function CentralBoard({ state }: CentralBoardProps) {
  return (
    <section className="panel central-board" data-testid="central-board" aria-label="Central board">
      {/* Full width: the cards carry what each role does, so seven of them side by side is both
          how the real board reads and shorter than stacking them beside the ships. */}
      <div className="central-board__group central-board__group--wide">
        <h2 className="panel__title">Roles</h2>
        {/* Which roles are left, who took what, and the doubloons piled on the untaken ones — the
            single most important thing to see when deciding what a player will do next. */}
        <ul className="role-track" data-testid="role-track" aria-label="Role track">
          {state.roles.cards.map((card) => {
            const taken = card.takenBySeat !== null && card.takenBySeat !== undefined;
            return (
              <li
                key={card.role}
                className="role-card"
                data-testid={`role-card-${card.role}`}
                data-taken={taken}
              >
                <RoleIcon role={card.role} size={26} />
                <span className="role-card__name">{ROLE_NAMES[card.role]}</span>
                {/* The same one-liner the selection picker shows. A role is worth reading here
                    too: half of choosing well is knowing what the *other* roles would have done. */}
                <span className="role-card__detail">{ROLE_SUMMARIES[card.role]}</span>
                {card.doubloons > 0 && (
                  <span className="role-card__coins">
                    <Doubloon count={card.doubloons} size={18} />
                  </span>
                )}
                {taken && <span className="role-card__taken">seat {card.takenBySeat}</span>}
              </li>
            );
          })}
        </ul>
      </div>

      <div className="central-board__group">
        <h2 className="panel__title">Ships</h2>
        <ul className="cargo-ships" data-testid="cargo-ships" aria-label="Cargo ships">
          {state.ships.map((ship, index) => (
            <li key={index} data-testid={`cargo-ship-${index}`}>
              <ShipArt capacity={ship.capacity} loaded={ship.loaded} cargo={ship.cargo} />
              <span className="cargo-ships__count">
                {ship.loaded} / {ship.capacity}
              </span>
            </li>
          ))}
        </ul>
      </div>

      <div className="central-board__group">
        <h2 className="panel__title">Plantations</h2>
        <ul className="face-up-tiles" data-testid="face-up-tiles" aria-label="Face-up plantations">
          {state.tiles.faceUp.map((tile, index) => (
            <li key={index} data-testid="face-up-tile" title={TILE_NAMES[tile]}>
              <PlantationTile type={tile} size={44} />
            </li>
          ))}
        </ul>
      </div>

      <div className="central-board__group">
        <h2 className="panel__title">Trading house</h2>
        <ul className="trading-house" data-testid="trading-house" aria-label="Trading house">
          {state.tradingHouse.goods.map((good, index) => (
            <li key={index} data-testid="trading-house-good" title={GOOD_NAMES[good]}>
              <GoodBarrel good={good} size={26} />
            </li>
          ))}
          {/* The empty slots matter as much as the full ones: the phase ends the moment the
              fourth is filled, and the house is cleared. */}
          {Array.from({ length: TRADING_HOUSE_SLOTS - state.tradingHouse.goods.length }, (_, i) => (
            <li key={`empty-${i}`} className="trading-house__empty" aria-label="Empty slot" />
          ))}
        </ul>
      </div>

      {/* Collapsible, and open by default: it is the largest thing on the table and a player who
          has memorised the display would rather have the vertical space back. */}
      <details className="central-board__group" data-testid="building-display-panel" open>
        <summary className="panel__title">Buildings</summary>
        <BuildingDisplay catalog={state.config.buildingCatalog} supply={state.buildings} />
      </details>

      <dl className="central-board__supplies">
        <div>
          <dt>Trading house</dt>
          <dd data-testid="trading-house-goods">
            {state.tradingHouse.goods.length} / {TRADING_HOUSE_SLOTS}
          </dd>
        </div>
        <div>
          <dt>Quarries remaining</dt>
          <dd data-testid="quarries-remaining">{state.tiles.quarriesRemaining}</dd>
        </div>
        <div>
          <dt>Face-down plantations</dt>
          <dd data-testid="face-down-count">{state.tiles.faceDownCount}</dd>
        </div>
        <div>
          <dt>Colonist supply</dt>
          <dd data-testid="colonist-supply">
            <Colonist size={18} />
            {state.colonistSupply}
          </dd>
        </div>
        <div>
          <dt>Colonists on ship</dt>
          <dd data-testid="colonists-on-ship">{state.colonistsOnShip}</dd>
        </div>
        <div>
          <dt>Victory point supply</dt>
          <dd data-testid="victory-point-supply">{state.victoryPointSupply}</dd>
        </div>
      </dl>

      {state.finalRound && (
        <p className="notice notice--final" role="status" data-testid="final-round">
          Final round
        </p>
      )}
    </section>
  );
}
