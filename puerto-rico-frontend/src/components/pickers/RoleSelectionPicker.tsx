import { ROLE_NAMES, ROLE_SUMMARIES } from "../art/labels";
import { Doubloon } from "../art/Pieces";
import { RoleIcon } from "../art/RoleIcon";
import { ActionButton } from "./ActionButton";
import { optionsOfType } from "./pickerTypes";
import type { PickerProps } from "./pickerTypes";

/**
 * The role tiles, as cards. Each shows the doubloons piled on it — the whole reason a weak role is
 * ever worth taking — and what taking it does, since the privilege is the thing a new player is
 * most likely to miss.
 *
 * A role already taken this round simply isn't in `options`, so it isn't drawn.
 */
export function RoleSelectionPicker({ options, state, submitting, onChoose }: PickerProps) {
  const choices = optionsOfType(options, "SELECT_ROLE");
  const doubloonsOn = new Map(state.roles.cards.map((card) => [card.role, card.doubloons]));

  return (
    <ul className="picker picker--roles" aria-label="Choose a role">
      {choices.map((action) => {
        const coins = doubloonsOn.get(action.role) ?? 0;
        return (
          <li key={action.role}>
            <ActionButton
              testId={`action-select-role-${action.role}`}
              variant="card"
              disabled={submitting}
              label={`Take the ${ROLE_NAMES[action.role]}${coins > 0 ? ` with ${coins} doubloons` : ""}`}
              onClick={() => onChoose(action)}
            >
              <RoleIcon role={action.role} />
              <span className="option__title">{ROLE_NAMES[action.role]}</span>
              <span className="option__detail">{ROLE_SUMMARIES[action.role]}</span>
              {coins > 0 && (
                <span className="option__coins" data-testid={`role-coins-${action.role}`}>
                  <Doubloon count={coins} />
                </span>
              )}
            </ActionButton>
          </li>
        );
      })}
    </ul>
  );
}
