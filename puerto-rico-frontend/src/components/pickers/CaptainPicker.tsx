import { GOOD_NAMES } from "../art/labels";
import { GoodBarrel, ShipArt } from "../art/Pieces";
import { ActionButton } from "./ActionButton";
import { optionsOfType } from "./pickerTypes";
import type { PickerProps } from "./pickerTypes";

/**
 * Loading. Each option is a specific ship and a specific good, so the picker draws the ship it
 * would go onto — which matters here more than anywhere else, since the rules force the *most*
 * cargo possible and a player needs to see why one ship is offered and another isn't.
 *
 * The Wharf is separate: it takes any number of one good straight to the bank, and is the one
 * loading option the rulebook makes optional, which is why declining appears only alongside it.
 */
export function CaptainPicker({ options, state, submitting, onChoose }: PickerProps) {
  const loads = optionsOfType(options, "LOAD_SHIP");
  const wharf = optionsOfType(options, "LOAD_WHARF");
  const decline = optionsOfType(options, "DECLINE_WHARF");

  return (
    <div className="picker picker--captain">
      <ul className="picker__row picker__row--wrap" aria-label="Ships you can load">
        {loads.map((action) => {
          const ship = state.ships[action.shipIndex];
          return (
            <li key={`${action.shipIndex}-${action.good}`}>
              <ActionButton
                testId={`action-load-ship-${action.shipIndex}-${action.good}`}
                variant="card"
                disabled={submitting}
                label={`Load ${GOOD_NAMES[action.good]} onto the ${ship?.capacity ?? "?"}-hold ship`}
                onClick={() => onChoose(action)}
              >
                {ship && (
                  <ShipArt capacity={ship.capacity} loaded={ship.loaded} cargo={ship.cargo} />
                )}
                <span className="option__title">
                  <GoodBarrel good={action.good} size={20} />
                  Load {GOOD_NAMES[action.good]}
                </span>
                {ship && (
                  <span className="option__detail">
                    {ship.loaded} of {ship.capacity} holds full
                  </span>
                )}
              </ActionButton>
            </li>
          );
        })}
      </ul>

      <div className="picker__row">
        {wharf.map((action) => (
          <ActionButton
            key={action.good}
            testId={`action-load-wharf-${action.good}`}
            variant="card"
            disabled={submitting}
            label={`Use your Wharf to ship every ${GOOD_NAMES[action.good]} barrel`}
            onClick={() => onChoose(action)}
          >
            <GoodBarrel good={action.good} size={26} />
            <span className="option__title">Wharf: ship all {GOOD_NAMES[action.good]}</span>
          </ActionButton>
        ))}

        {decline.map((action) => (
          <ActionButton
            key="decline-wharf"
            testId="action-decline-wharf"
            variant="pass"
            disabled={submitting}
            onClick={() => onChoose(action)}
          >
            <span className="option__title">Don't use the Wharf</span>
          </ActionButton>
        ))}
      </div>
    </div>
  );
}
