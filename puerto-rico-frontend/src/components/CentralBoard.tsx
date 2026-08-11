import type { GameStateView } from "../api/types";

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
    <section data-testid="central-board" aria-label="Central board">
      <h2>Central board</h2>

      {/* Which roles are left, who took what, and the doubloons piled on the untaken ones — the
          single most important thing to see when deciding what a player will do next. */}
      <ul data-testid="role-track" aria-label="Role track">
        {state.roles.cards.map((card) => (
          <li
            key={card.role}
            data-testid={`role-card-${card.role}`}
            data-taken={card.takenBySeat !== null && card.takenBySeat !== undefined}
          >
            {card.role}
            {card.doubloons > 0 && ` +${card.doubloons}`}
            {card.takenBySeat !== null &&
              card.takenBySeat !== undefined &&
              ` — taken by seat ${card.takenBySeat}`}
          </li>
        ))}
      </ul>

      <ul data-testid="cargo-ships" aria-label="Cargo ships">
        {state.ships.map((ship, index) => (
          <li key={index} data-testid={`cargo-ship-${index}`}>
            {ship.loaded} / {ship.capacity}
            {ship.cargo ? ` ${ship.cargo}` : " empty"}
          </li>
        ))}
      </ul>

      <ul data-testid="face-up-tiles" aria-label="Face-up plantations">
        {state.tiles.faceUp.map((tile, index) => (
          <li key={index} data-testid="face-up-tile">
            {tile}
          </li>
        ))}
      </ul>

      <ul data-testid="trading-house" aria-label="Trading house">
        {state.tradingHouse.goods.map((good, index) => (
          <li key={index} data-testid="trading-house-good">
            {good}
          </li>
        ))}
      </ul>

      <dl>
        <dt>Trading house</dt>
        <dd data-testid="trading-house-goods">
          {state.tradingHouse.goods.length} / {TRADING_HOUSE_SLOTS}
        </dd>
        <dt>Quarries remaining</dt>
        <dd data-testid="quarries-remaining">{state.tiles.quarriesRemaining}</dd>
        <dt>Face-down plantations</dt>
        <dd data-testid="face-down-count">{state.tiles.faceDownCount}</dd>
        <dt>Colonist supply</dt>
        <dd data-testid="colonist-supply">{state.colonistSupply}</dd>
        <dt>Colonists on ship</dt>
        <dd data-testid="colonists-on-ship">{state.colonistsOnShip}</dd>
        <dt>Victory point supply</dt>
        <dd data-testid="victory-point-supply">{state.victoryPointSupply}</dd>
      </dl>

      {state.finalRound && (
        <p role="status" data-testid="final-round">
          Final round
        </p>
      )}
    </section>
  );
}
