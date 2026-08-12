import type { Good } from "../../api/types";
import { GOOD_NAMES } from "../art/labels";
import { GoodBarrel } from "../art/Pieces";
import { ActionButton } from "./ActionButton";
import { optionsOfType } from "./pickerTypes";
import type { PickerProps } from "./pickerTypes";

/**
 * What the player keeps when the ships sail; everything not kept spoils.
 *
 * The server offers whole legal combinations — each `StoreGoods` option is a complete answer, its
 * `warehouseKinds` sized to the warehouses the player actually owns plus the single barrel anyone
 * may hold back. Presenting them as finished choices rather than as a build-your-own picker is
 * deliberate: a combination picker could compose a selection the rules forbid, and then the only
 * thing standing between the player and a rejected move would be a check this UI would have to
 * duplicate.
 */
export function CaptainStoragePicker({ options, submitting, onChoose }: PickerProps) {
  const stores = optionsOfType(options, "STORE_GOODS");

  return (
    <ul className="picker picker--storage" aria-label="What to keep">
      {stores.map((action, index) => {
        const kinds = action.warehouseKinds;
        const single = action.singleBarrel ?? null;
        return (
          <li key={index}>
            <ActionButton
              testId={`action-store-${index}`}
              variant="card"
              disabled={submitting}
              label={describe(kinds, single)}
              onClick={() => onChoose(action)}
            >
              <span className="option__title">{describe(kinds, single)}</span>
              <span className="option__barrels">
                {kinds.map((good) => (
                  <GoodBarrel key={`all-${good}`} good={good} size={22} />
                ))}
                {single && (
                  <span className="option__single">
                    <GoodBarrel good={single} size={22} />
                  </span>
                )}
              </span>
            </ActionButton>
          </li>
        );
      })}
    </ul>
  );
}

function describe(warehouseKinds: Good[], singleBarrel: Good | null): string {
  const parts: string[] = [];
  if (warehouseKinds.length > 0) {
    parts.push(`all your ${warehouseKinds.map((g) => GOOD_NAMES[g]).join(" and ")}`);
  }
  if (singleBarrel) {
    parts.push(`one ${GOOD_NAMES[singleBarrel]}`);
  }
  return parts.length === 0 ? "Keep nothing — let it all spoil" : `Keep ${parts.join(", plus ")}`;
}
