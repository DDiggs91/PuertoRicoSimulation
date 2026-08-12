import { GOOD_NAMES } from "../art/labels";
import { Doubloon, GoodBarrel } from "../art/Pieces";
import { ActionButton } from "./ActionButton";
import { optionsOfType } from "./pickerTypes";
import type { PickerProps } from "./pickerTypes";

/**
 * What this player may sell into the trading house, and what each sale pays.
 *
 * As with the builder, the price comes off the wire (`Phase.goodPrices`, from
 * `GameEngine.sellPrice`) — list price plus the trader's privilege plus market bonuses is a rule,
 * and rules live in the model.
 */
export function TraderPicker({ options, state, submitting, onChoose }: PickerProps) {
  const sales = optionsOfType(options, "SELL_GOOD");
  const pass = optionsOfType(options, "PASS_TRADING");
  const priced = new Map((state.phase.goodPrices ?? []).map((p) => [p.good, p.price]));

  return (
    <div className="picker picker--goods">
      <ul className="picker__row" aria-label="Goods you can sell">
        {sales.map((action) => {
          const price = priced.get(action.good);
          return (
            <li key={action.good}>
              <ActionButton
                testId={`action-sell-${action.good}`}
                variant="tile"
                disabled={submitting}
                label={`Sell ${GOOD_NAMES[action.good]}${
                  price === undefined ? "" : ` for ${price} doubloons`
                }`}
                onClick={() => onChoose(action)}
              >
                <GoodBarrel good={action.good} size={34} />
                <span className="option__title">{GOOD_NAMES[action.good]}</span>
                {price !== undefined && (
                  <span className="option__coins" data-testid={`sell-price-${action.good}`}>
                    <Doubloon count={price} />
                  </span>
                )}
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
            <span className="option__title">Sell nothing</span>
          </ActionButton>
        ))}
      </div>
    </div>
  );
}
