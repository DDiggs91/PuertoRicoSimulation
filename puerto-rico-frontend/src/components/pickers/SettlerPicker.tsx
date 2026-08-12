import { TILE_NAMES } from "../art/labels";
import { PlantationTile } from "../art/Pieces";
import { ActionButton } from "./ActionButton";
import { optionsOfType } from "./pickerTypes";
import type { PickerProps } from "./pickerTypes";

/**
 * The face-up plantation row, plus the three settler-phase choices that aren't a tile: the
 * quarry (the settler's own privilege, or a Construction Hut owner's), the Hacienda's extra
 * face-down tile, and passing.
 *
 * The Hacienda offer is a separate decision the engine raises *before* the tile choice, so those
 * two option sets never appear together; rendering both branches unconditionally is still right,
 * because whichever isn't live simply has no options to draw.
 */
export function SettlerPicker({ options, state, submitting, onChoose }: PickerProps) {
  const tiles = optionsOfType(options, "TAKE_FACE_UP_TILE");
  const quarry = optionsOfType(options, "TAKE_QUARRY");
  const hacienda = optionsOfType(options, "TAKE_HACIENDA_TILE");
  const skipHacienda = optionsOfType(options, "SKIP_HACIENDA");
  const pass = optionsOfType(options, "PASS_SETTLING");

  return (
    <div className="picker picker--settler">
      {tiles.length > 0 && (
        <ul className="picker__row" aria-label="Face-up plantations">
          {tiles.map((action) => {
            const type = state.tiles.faceUp[action.faceUpIndex];
            return (
              <li key={action.faceUpIndex}>
                <ActionButton
                  testId={`action-take-tile-${action.faceUpIndex}`}
                  variant="tile"
                  disabled={submitting}
                  label={`Take the ${type ? TILE_NAMES[type] : "plantation"}`}
                  onClick={() => onChoose(action)}
                >
                  {type && <PlantationTile type={type} />}
                  <span className="option__title">{type ? TILE_NAMES[type] : "Plantation"}</span>
                </ActionButton>
              </li>
            );
          })}
        </ul>
      )}

      <div className="picker__row">
        {quarry.map((action) => (
          <ActionButton
            key="quarry"
            testId="action-take-quarry"
            variant="tile"
            disabled={submitting}
            label="Take a quarry instead of a plantation"
            onClick={() => onChoose(action)}
          >
            <PlantationTile type="QUARRY" />
            <span className="option__title">Quarry</span>
            <span className="option__detail">{state.tiles.quarriesRemaining} left</span>
          </ActionButton>
        ))}

        {hacienda.map((action) => (
          <ActionButton
            key="hacienda"
            testId="action-take-hacienda-tile"
            variant="card"
            disabled={submitting}
            onClick={() => onChoose(action)}
          >
            <span className="option__title">Use the Hacienda</span>
            <span className="option__detail">Take an extra plantation, unseen, from the pile</span>
          </ActionButton>
        ))}

        {skipHacienda.map((action) => (
          <ActionButton
            key="skip-hacienda"
            testId="action-skip-hacienda"
            variant="pass"
            disabled={submitting}
            onClick={() => onChoose(action)}
          >
            <span className="option__title">Skip the Hacienda</span>
          </ActionButton>
        ))}

        {pass.map((action) => (
          <ActionButton
            key="pass"
            testId="action-pass"
            variant="pass"
            disabled={submitting}
            onClick={() => onChoose(action)}
          >
            <span className="option__title">Take no plantation</span>
          </ActionButton>
        ))}
      </div>
    </div>
  );
}
