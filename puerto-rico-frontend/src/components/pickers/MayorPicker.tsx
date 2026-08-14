import { useState } from "react";
import type { PlayerAction, PlayerStateView } from "../../api/types";
import { TILE_NAMES } from "../art/labels";
import { BuildingCard } from "../art/BuildingCard";
import { Colonist, PlantationTile } from "../art/Pieces";
import { ActionButton } from "./ActionButton";
import type { PickerProps } from "./pickerTypes";

interface Arrangement {
  /** Index-aligned with `player.island`. */
  island: boolean[];
  /** Index-aligned with `player.buildings`, each a colonist count rather than a flag. */
  buildings: number[];
}

function arrangementOf(player: PlayerStateView | undefined): Arrangement {
  return {
    island: (player?.island ?? []).map((tile) => tile.occupied),
    buildings: (player?.buildings ?? []).map((building) => building.colonists),
  };
}

function sanJuanRemaining(player: PlayerStateView | undefined, arrangement: Arrangement): number {
  const before =
    (player?.island ?? []).filter((t) => t.occupied).length +
    (player?.buildings ?? []).reduce((sum, b) => sum + b.colonists, 0);
  const after =
    arrangement.island.filter(Boolean).length +
    arrangement.buildings.reduce((sum, n) => sum + n, 0);
  return (player?.colonistsInSanJuan ?? 0) - (after - before);
}

/**
 * Colonist placement, staged entirely in the browser. Clicking a circle toggles it locally —
 * placing or lifting a colonist is instant, free, and reversible any number of times — and nothing
 * reaches the server until Finalize sends the whole finished arrangement as one
 * `SetColonistPlacementAction`. See `pickerTypes.ts` for why this is the one picker allowed to
 * construct an action rather than hand back an object the server offered verbatim.
 *
 * The board this stages from is `player.island`/`player.buildings` as they stood when this seat's
 * turn began — untouched by anything staged here, since nothing is sent until Finalize. A fresh
 * `MayorPicker` mounts once per turn (the action panel unmounts between decisions), so seeding state
 * from props on mount is exactly "start from what's actually on the board."
 */
export function MayorPicker({ state, seat, submitting, onChoose }: PickerProps) {
  const player = state.players.find((p) => p.seat === seat);
  const [arrangement, setArrangement] = useState<Arrangement>(() => arrangementOf(player));
  const remaining = sanJuanRemaining(player, arrangement);

  const toggleIsland = (index: number) => {
    setArrangement((current) => {
      const occupied = current.island[index];
      if (!occupied && remaining <= 0) {
        return current;
      }
      const island = current.island.slice();
      island[index] = !occupied;
      return { ...current, island };
    });
  };

  const placeBuilding = (index: number, capacity: number) => {
    setArrangement((current) => {
      if (current.buildings[index] >= capacity || remaining <= 0) {
        return current;
      }
      const buildings = current.buildings.slice();
      buildings[index] += 1;
      return { ...current, buildings };
    });
  };

  const removeBuilding = (index: number) => {
    setArrangement((current) => {
      if (current.buildings[index] <= 0) {
        return current;
      }
      const buildings = current.buildings.slice();
      buildings[index] -= 1;
      return { ...current, buildings };
    });
  };

  const finalize = () => {
    const action: PlayerAction = {
      type: "SET_COLONIST_PLACEMENT",
      seat,
      islandOccupied: arrangement.island,
      buildingColonists: arrangement.buildings,
    };
    onChoose(action);
  };

  return (
    <div className="picker picker--mayor">
      <p className="picker__hint" data-testid="colonists-in-hand">
        <Colonist size={18} />
        {remaining} in San Juan — click an empty circle to staff it, a filled one to take that
        colonist back. Nothing is sent until you finalize.
      </p>

      <ul className="picker__row" aria-label="Your island">
        {(player?.island ?? []).map((tile, index) => {
          const occupied = arrangement.island[index];
          const clickable = occupied || remaining > 0;
          return (
            <li key={index}>
              {clickable ? (
                <ActionButton
                  testId={`stage-colonist-ISLAND-${index}`}
                  variant="tile"
                  submits={false}
                  disabled={submitting}
                  label={
                    occupied
                      ? `Take the colonist back from the ${TILE_NAMES[tile.type]}`
                      : `Staff the ${TILE_NAMES[tile.type]}`
                  }
                  onClick={() => toggleIsland(index)}
                >
                  <PlantationTile type={tile.type} occupied={occupied} />
                </ActionButton>
              ) : (
                // Empty and nothing left to put there: still part of the island being read, so it
                // stays drawn rather than disappearing and reflowing the row.
                <span className="option option--tile option--inert">
                  <PlantationTile type={tile.type} occupied={occupied} />
                </span>
              )}
            </li>
          );
        })}
      </ul>

      <ul className="picker__row" aria-label="Your city">
        {(player?.buildings ?? []).map((building, index) => {
          const colonists = arrangement.buildings[index];
          const canPlace = colonists < building.capacity && remaining > 0;
          const canRemove = colonists > 0;
          return (
            <li key={index}>
              {/* The card is a frame, not a button — its circles are the targets, so it is only
                  dimmed when neither direction is available. */}
              <span
                className={`option option--card ${canPlace || canRemove ? "option--host" : "option--inert"}`}
              >
                <BuildingCard
                  type={building.type}
                  victoryPoints={building.victoryPoints}
                  capacity={building.capacity}
                  colonists={colonists}
                  slotId={`BUILDING-${index}`}
                  disabled={submitting}
                  onPlace={canPlace ? () => placeBuilding(index, building.capacity) : undefined}
                  onRemove={canRemove ? () => removeBuilding(index) : undefined}
                />
              </span>
            </li>
          );
        })}
      </ul>

      <div className="picker__row">
        <ActionButton
          testId="action-end-colonist-placement"
          variant="pass"
          disabled={submitting}
          onClick={finalize}
        >
          <span className="option__title">Finalize colonist placements</span>
        </ActionButton>
      </div>
    </div>
  );
}
