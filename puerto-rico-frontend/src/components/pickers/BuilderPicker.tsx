import { BUILDING_NAMES } from "../art/labels";
import { BuildingCard } from "../art/BuildingCard";
import { ActionButton } from "./ActionButton";
import { optionsOfType } from "./pickerTypes";
import type { PickerProps } from "./pickerTypes";

/**
 * The buildings this player can actually afford and has room for, each priced.
 *
 * The price is `Phase.buildOptions`', not this component's: the builder's privilege and the quarry
 * discount are engine rules, and the server quotes the number it will charge (see
 * `GameEngine.buildCost`). Which buildings appear is `options`' doing, as everywhere else.
 */
export function BuilderPicker({ options, state, submitting, onChoose }: PickerProps) {
  const builds = optionsOfType(options, "BUILD_BUILDING");
  const pass = optionsOfType(options, "PASS_BUILDING");
  const priced = new Map((state.phase.buildOptions ?? []).map((o) => [o.buildingType, o]));

  return (
    <div className="picker picker--builder">
      <ul className="picker__row picker__row--wrap" aria-label="Buildings you can build">
        {builds.map((action) => {
          const quote = priced.get(action.buildingType);
          return (
            <li key={action.buildingType}>
              <ActionButton
                testId={`action-build-${action.buildingType}`}
                variant="card"
                disabled={submitting}
                label={`Build the ${BUILDING_NAMES[action.buildingType]}${
                  quote ? ` for ${quote.cost} doubloons` : ""
                }`}
                onClick={() => onChoose(action)}
              >
                <BuildingCard
                  type={action.buildingType}
                  cost={quote?.cost}
                  victoryPoints={quote?.victoryPoints ?? 0}
                  capacity={quote?.colonistCapacity ?? 1}
                />
              </ActionButton>
            </li>
          );
        })}
      </ul>

      <div className="picker__row">
        {pass.map((action) => (
          <ActionButton
            key="pass"
            testId="action-pass"
            variant="pass"
            disabled={submitting}
            onClick={() => onChoose(action)}
          >
            <span className="option__title">Build nothing</span>
          </ActionButton>
        ))}
      </div>
    </div>
  );
}
