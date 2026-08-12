import type { BuildingType } from "../../api/types";
import { BUILDING_NAMES, buildingTint } from "./labels";
import { Colonist, Doubloon } from "./Pieces";

export interface BuildingCardProps {
  type: BuildingType;
  /**
   * Doubloons to buy it *now*, discounts already applied — straight off the wire
   * (`Phase.buildOptions`), never computed here. Omitted for a building already owned, which has
   * no price.
   */
  cost?: number;
  victoryPoints: number;
  /** Colonist circles printed on the card. */
  capacity: number;
  /** How many of those circles are filled. Omitted in the shop, where nothing is staffed yet. */
  colonists?: number;
}

/**
 * One building card, laid out the way the printed one is: name across the top, its price and
 * victory-point value in the corners, and its colonist circles along the bottom — filled or empty,
 * which is what says whether the building actually does anything yet.
 *
 * Chrome is CSS (the parchment, the border, the colour band); only the pieces sitting on it — the
 * coin and the colonists — are SVG.
 */
export function BuildingCard({
  type,
  cost,
  victoryPoints,
  capacity,
  colonists,
}: BuildingCardProps) {
  return (
    <span
      className="building-card"
      data-tint={buildingTint(type)}
      data-testid={`building-card-${type}`}
    >
      <span className="building-card__name">{BUILDING_NAMES[type]}</span>
      <span className="building-card__numbers">
        {cost !== undefined && (
          <span className="building-card__cost" data-testid={`building-cost-${type}`}>
            <Doubloon count={cost} size={20} />
          </span>
        )}
        <span className="building-card__vp" title={`${victoryPoints} victory points`}>
          {victoryPoints} VP
        </span>
      </span>
      <span
        className="building-card__circles"
        aria-label={
          colonists === undefined
            ? `${capacity} colonist spaces`
            : `${colonists} of ${capacity} colonist spaces filled`
        }
      >
        {Array.from({ length: capacity }, (_, i) => (
          <Colonist key={i} size={15} empty={colonists === undefined || i >= colonists} />
        ))}
      </span>
    </span>
  );
}
