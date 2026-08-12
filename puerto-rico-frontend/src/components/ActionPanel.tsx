import type { GameStateView, PlayerAction } from "../api/types";
import type { PendingDecision } from "../state/gameReducer";

type PhaseType = GameStateView["phase"]["type"];
import { BuilderPicker } from "./pickers/BuilderPicker";
import { CaptainPicker } from "./pickers/CaptainPicker";
import { CaptainStoragePicker } from "./pickers/CaptainStoragePicker";
import { CraftsmanBonusPicker } from "./pickers/CraftsmanBonusPicker";
import { MayorPicker } from "./pickers/MayorPicker";
import { RoleSelectionPicker } from "./pickers/RoleSelectionPicker";
import { SettlerPicker } from "./pickers/SettlerPicker";
import { TraderPicker } from "./pickers/TraderPicker";
import type { PickerProps } from "./pickers/pickerTypes";

export interface ActionPanelProps {
  /** The seat this client holds a token for; null for a spectator. */
  mySeat: number | null;
  pending: PendingDecision | null;
  submitting: boolean;
  moveError: string | null;
  onChoose: (action: PlayerAction) => void;
}

const PROMPTS: Record<string, string> = {
  ROLE_SELECTION: "Choose a role",
  SETTLER: "Take a plantation",
  MAYOR: "Place your colonists",
  BUILDER: "Build a building",
  CRAFTSMAN_BONUS: "Take your extra barrel",
  TRADER: "Sell a good",
  CAPTAIN_LOADING: "Load a ship",
  CAPTAIN_STORAGE: "Choose what to keep",
};

/**
 * The one place a human's move is chosen.
 *
 * Renders only when there is a decision pending *for this client's seat* — a spectator, and a
 * player whose turn it isn't, both get nothing, which is what makes the panel's presence the "it's
 * your turn" signal rather than something the board has to say separately.
 *
 * Each phase gets its own picker, but they all receive the same thing: the slice of the server's
 * legal-action list matching that phase. No picker builds an action of its own, so what the UI can
 * offer and what the session will accept are the same set by construction.
 *
 * The board comes from the decision, not from the surrounding `GameBoard`. Those two can differ
 * for a moment — a resync re-fetches `/state` on its own schedule — and a picker chosen by one
 * board for options computed against another is a picker with nothing to draw.
 */
export function ActionPanel({
  mySeat,
  pending,
  submitting,
  moveError,
  onChoose,
}: ActionPanelProps) {
  if (mySeat === null || pending === null || pending.seat !== mySeat) {
    return null;
  }

  const props: PickerProps = {
    options: pending.options,
    state: pending.state,
    seat: mySeat,
    submitting,
    onChoose,
  };
  const phase = pending.state.phase.type;

  return (
    <section
      className="action-panel"
      data-testid="action-panel"
      data-phase={phase}
      aria-label="Your move"
    >
      <header className="action-panel__header">
        <p className="action-panel__prompt" role="status" data-testid="your-turn">
          Your turn — {PROMPTS[phase] ?? "choose a move"}
        </p>
        {submitting && (
          <span className="action-panel__pending" role="status" data-testid="move-submitting">
            Sending…
          </span>
        )}
      </header>

      {moveError && (
        <p className="notice notice--alert" role="alert" data-testid="action-panel-error">
          {moveError}
        </p>
      )}

      {picker(phase, props)}
    </section>
  );
}

function picker(phase: PhaseType, props: PickerProps) {
  switch (phase) {
    case "ROLE_SELECTION":
      return <RoleSelectionPicker {...props} />;
    case "SETTLER":
      return <SettlerPicker {...props} />;
    case "MAYOR":
      return <MayorPicker {...props} />;
    case "BUILDER":
      return <BuilderPicker {...props} />;
    case "CRAFTSMAN_BONUS":
      return <CraftsmanBonusPicker {...props} />;
    case "TRADER":
      return <TraderPicker {...props} />;
    case "CAPTAIN_LOADING":
      return <CaptainPicker {...props} />;
    case "CAPTAIN_STORAGE":
      return <CaptainStoragePicker {...props} />;
    case "GAME_OVER":
      // The session stops asking for decisions once the game is over, so this is unreachable in
      // practice; naming it keeps the switch exhaustive rather than falling through to nothing.
      return null;
  }
}
