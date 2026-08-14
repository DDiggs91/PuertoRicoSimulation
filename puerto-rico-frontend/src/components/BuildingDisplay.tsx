import type { BuildingCatalogEntry, BuildingSupplyMap } from "../api/types";
import { BuildingCard } from "./art/BuildingCard";

export interface BuildingDisplayProps {
  /** The printed building table, off `state.config.buildingCatalog`. */
  catalog: BuildingCatalogEntry[];
  /** Copies still for sale, off `state.buildings`. */
  supply: BuildingSupplyMap;
}

/** The display's four columns, which are also the quarry-discount caps. */
const COLUMNS = [1, 2, 3, 4];

/**
 * The building display, arranged the way the printed board arranges it: four columns by victory
 * point value, cheapest first, each card stacked with the copies still available.
 *
 * A building's column *is* its VP value, and that number caps how many occupied quarries may
 * discount it — so the columns are not decoration, they are the rule a player reads off the board
 * when working out what a purchase will actually cost.
 *
 * Every number here comes off the wire: the card faces from `config.buildingCatalog`, the counts
 * from `state.buildings`. Prices shown are the printed ones — what a particular player would pay
 * today is the builder phase's business (`Phase.buildOptions`), quoted on the cards in that picker.
 */
export function BuildingDisplay({ catalog, supply }: BuildingDisplayProps) {
  return (
    <div className="building-display" data-testid="building-display" aria-label="Building display">
      {COLUMNS.map((column) => (
        <ul
          key={column}
          className="building-display__column"
          data-testid={`building-display-column-${column}`}
          aria-label={`Buildings worth ${column} victory point${column === 1 ? "" : "s"}`}
        >
          {catalog
            .filter((entry) => entry.victoryPoints === column)
            .map((entry) => {
              const remaining = supply[entry.type] ?? 0;
              return (
                <li
                  key={entry.type}
                  data-testid={`building-supply-${entry.type}`}
                  data-sold-out={remaining === 0}
                >
                  <BuildingCard
                    type={entry.type}
                    cost={entry.cost}
                    victoryPoints={entry.victoryPoints}
                    capacity={entry.colonistCapacity}
                    detail
                  />
                  <span className="building-display__stock">
                    {remaining} of {entry.copies} left
                  </span>
                </li>
              );
            })}
        </ul>
      ))}
    </div>
  );
}
