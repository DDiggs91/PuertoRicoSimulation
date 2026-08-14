import type { BuildingType } from "../../api/types";
import { BUILDING_NAMES, BUILDING_SUMMARIES, buildingTint } from "./labels";
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
  /**
   * Spell out what the building does. On for the cards a player is choosing between — the display
   * and the builder's shop — and off for the small ones lining a city, which would triple in
   * height. Those keep the same text as a tooltip either way.
   */
  detail?: boolean;
  /**
   * Mayor phase only: stage the first empty circle as filled, or the last filled one as empty.
   * Given a handler, that one circle becomes a button — colonists are interchangeable, so exactly
   * one circle per direction is actionable and there is never a question of which one a click
   * meant. Neither handler submits anything; both only edit the arrangement `MayorPicker` is
   * staging locally, which is why these buttons carry no `data-action-option` marker.
   */
  onPlace?: () => void;
  onRemove?: () => void;
  /** Base for the circle buttons' test ids, e.g. `BUILDING-3`. Required alongside a handler. */
  slotId?: string;
  disabled?: boolean;
}

/**
 * One building card, laid out the way the printed one is: name across the top, its price and
 * victory-point value in the corners, what it does beneath that, and its colonist circles along
 * the bottom — filled or empty, which is what says whether the building actually does anything yet.
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
  detail = false,
  onPlace,
  onRemove,
  slotId,
  disabled = false,
}: BuildingCardProps) {
  const filled = colonists ?? 0;
  const placeIndex = onPlace && filled < capacity ? filled : -1;
  const removeIndex = onRemove && filled > 0 ? filled - 1 : -1;

  return (
    <span
      className="building-card"
      data-tint={buildingTint(type)}
      data-testid={`building-card-${type}`}
      title={BUILDING_SUMMARIES[type]}
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
      {detail && <span className="building-card__text">{BUILDING_SUMMARIES[type]}</span>}
      <span
        className="building-card__circles"
        aria-label={
          colonists === undefined
            ? `${capacity} colonist spaces`
            : `${colonists} of ${capacity} colonist spaces filled`
        }
      >
        {Array.from({ length: capacity }, (_, i) => {
          const art = <Colonist size={15} empty={colonists === undefined || i >= filled} />;
          if (i === placeIndex) {
            return (
              <button
                key={i}
                type="button"
                className="building-card__circle-button"
                data-testid={`stage-place-colonist-${slotId}`}
                disabled={disabled}
                aria-label={`Staff the ${BUILDING_NAMES[type]}`}
                onClick={onPlace}
              >
                {art}
              </button>
            );
          }
          if (i === removeIndex) {
            return (
              <button
                key={i}
                type="button"
                className="building-card__circle-button"
                data-testid={`stage-remove-colonist-${slotId}`}
                disabled={disabled}
                aria-label={`Take a colonist back from the ${BUILDING_NAMES[type]}`}
                onClick={onRemove}
              >
                {art}
              </button>
            );
          }
          return <span key={i}>{art}</span>;
        })}
      </span>
    </span>
  );
}
