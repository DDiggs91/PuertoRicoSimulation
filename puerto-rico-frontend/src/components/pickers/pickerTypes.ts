import type { GameStateView, PlayerAction } from "../../api/types";

/**
 * What every picker gets. `options` is always a slice of the server's own legal-action list, and
 * every picker but one presents exactly those objects back to `onChoose` unchanged — never
 * constructing an action of its own — which is what makes `HumanActor.offer`'s "that action is not
 * currently legal" rejection unreachable from ordinary use rather than a race a fast clicker can
 * win.
 *
 * `MayorPicker` is the sanctioned exception: a colonist arrangement is a configuration, not a
 * choice from a list, and `legalActions` offers exactly one option (a greedy fill) as a result. The
 * picker stages its own arrangement locally — instant, unlimited undo, nothing sent until Finalize
 * — and constructs a `SetColonistPlacementAction` to match it. `HumanActor.offer` admits that one
 * variant by asking the engine whether it is legal rather than by list membership; see its
 * doc-comment.
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
