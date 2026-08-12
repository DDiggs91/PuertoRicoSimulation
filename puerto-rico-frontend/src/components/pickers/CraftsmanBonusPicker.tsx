import { GOOD_NAMES } from "../art/labels";
import { GoodBarrel } from "../art/Pieces";
import { ActionButton } from "./ActionButton";
import { optionsOfType } from "./pickerTypes";
import type { PickerProps } from "./pickerTypes";

/**
 * The craftsman's privilege: one extra barrel of something they just produced. Only kinds the
 * player actually produced this round are offered, and only while the supply holds one — both
 * decided server-side, so this just draws what it was given.
 */
export function CraftsmanBonusPicker({ options, submitting, onChoose }: PickerProps) {
  const takes = optionsOfType(options, "TAKE_CRAFTSMAN_BONUS");
  const pass = optionsOfType(options, "PASS_CRAFTSMAN_BONUS");

  return (
    <div className="picker picker--goods">
      <ul className="picker__row" aria-label="Extra barrel">
        {takes.map((action) => (
          <li key={action.good}>
            <ActionButton
              testId={`action-craftsman-bonus-${action.good}`}
              variant="tile"
              disabled={submitting}
              label={`Take an extra ${GOOD_NAMES[action.good]} barrel`}
              onClick={() => onChoose(action)}
            >
              <GoodBarrel good={action.good} size={34} />
              <span className="option__title">{GOOD_NAMES[action.good]}</span>
            </ActionButton>
          </li>
        ))}
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
            <span className="option__title">Take no extra barrel</span>
          </ActionButton>
        ))}
      </div>
    </div>
  );
}
