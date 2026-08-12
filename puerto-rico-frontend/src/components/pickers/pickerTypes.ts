import type { GameStateView, PlayerAction } from "../../api/types";

/**
 * What every picker gets. `options` is always a slice of the server's own legal-action list — the
 * pickers never construct an action, they present the ones they were handed and pass the very same
 * object back to `onChoose`. That is what makes `HumanActor.offer`'s "that action is not currently
 * legal" rejection unreachable from ordinary use rather than a race a fast clicker can win.
 */
export interface PickerProps {
  options: PlayerAction[];
  state: GameStateView;
  /** The seat this client holds — always the acting seat, or the panel would not be rendered. */
  seat: number;
  /** True while a move is in flight, so a second click can't offer a decision already answered. */
  submitting: boolean;
  onChoose: (action: PlayerAction) => void;
}

/** Narrows the option list to one action variant, keeping the original objects. */
export function optionsOfType<T extends PlayerAction["type"]>(
  options: PlayerAction[],
  type: T,
): Extract<PlayerAction, { type: T }>[] {
  return options.filter(
    (option): option is Extract<PlayerAction, { type: T }> =>
      // The generated variants carry a literal `type`, so this predicate is the only place the
      // narrowing happens and every picker below reads as if the list were already typed.
      (option.type as string) === (type as string),
  );
}
