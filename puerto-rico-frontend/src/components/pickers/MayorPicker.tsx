import { BUILDING_NAMES, TILE_NAMES } from "../art/labels";
import { BuildingCard } from "../art/BuildingCard";
import { Colonist, PlantationTile } from "../art/Pieces";
import { ActionButton } from "./ActionButton";
import { optionsOfType } from "./pickerTypes";
import type { PickerProps } from "./pickerTypes";

/**
 * Colonist placement, as the board itself rather than a list: the player's own island tiles and
 * city buildings, each clickable exactly when it has room for another colonist.
 *
 * A `PlaceColonist` option carries a `ColonistSlot` whose `index` addresses `island[i]` or
 * `buildings[i]` directly, so the option list maps straight onto what's drawn — no lookup table,
 * and no way to click a space the server didn't offer.
 */
export function MayorPicker({ options, state, seat, submitting, onChoose }: PickerProps) {
  const placements = optionsOfType(options, "PLACE_COLONIST");
  const end = optionsOfType(options, "END_COLONIST_PLACEMENT");
  const player = state.players.find((p) => p.seat === seat);

  const islandSlots = new Map(
    placements.filter((a) => a.slot.type === "ISLAND").map((a) => [a.slot.index, a]),
  );
  const buildingSlots = new Map(
    placements.filter((a) => a.slot.type === "BUILDING").map((a) => [a.slot.index, a]),
  );

  return (
    <div className="picker picker--mayor">
      <p className="picker__hint" data-testid="colonists-in-hand">
        <Colonist size={18} />
        {player?.colonistsInSanJuan ?? 0} in San Juan — place them where they will work.
      </p>

      <ul className="picker__row" aria-label="Your island">
        {(player?.island ?? []).map((tile, index) => {
          const action = islandSlots.get(index);
          return (
            <li key={index}>
              {action ? (
                <ActionButton
                  testId={`action-place-colonist-ISLAND-${index}`}
                  variant="tile"
                  disabled={submitting}
                  label={`Staff the ${TILE_NAMES[tile.type]}`}
                  onClick={() => onChoose(action)}
                >
                  <PlantationTile type={tile.type} occupied={tile.occupied} />
                </ActionButton>
              ) : (
                // Drawn but not offered: a full tile is still part of the island the player is
                // reading, and hiding it would make the board jump around as spaces fill.
                <span className="option option--tile option--inert">
                  <PlantationTile type={tile.type} occupied={tile.occupied} />
                </span>
              )}
            </li>
          );
        })}
      </ul>

      <ul className="picker__row" aria-label="Your city">
        {(player?.buildings ?? []).map((building, index) => {
          const action = buildingSlots.get(index);
          return (
            <li key={index}>
              {action ? (
                <ActionButton
                  testId={`action-place-colonist-BUILDING-${index}`}
                  variant="card"
                  disabled={submitting}
                  label={`Staff the ${BUILDING_NAMES[building.type]}`}
                  onClick={() => onChoose(action)}
                >
                  <BuildingCard
                    type={building.type}
                    victoryPoints={building.victoryPoints}
                    capacity={building.capacity}
                    colonists={building.colonists}
                  />
                </ActionButton>
              ) : (
                <span className="option option--card option--inert">
                  <BuildingCard
                    type={building.type}
                    victoryPoints={building.victoryPoints}
                    capacity={building.capacity}
                    colonists={building.colonists}
                  />
                </span>
              )}
            </li>
          );
        })}
      </ul>

      <div className="picker__row">
        {end.map((action) => (
          <ActionButton
            key="end"
            testId="action-end-colonist-placement"
            variant="pass"
            disabled={submitting}
            onClick={() => onChoose(action)}
          >
            <span className="option__title">Done placing colonists</span>
          </ActionButton>
        ))}
      </div>
    </div>
  );
}
