import type { ReactNode } from "react";

export interface ActionButtonProps {
  /** Stable, specific id — `action-build-HOSPICE`, `action-load-ship-1-CORN`, and so on. */
  testId: string;
  onClick: () => void;
  disabled?: boolean;
  /** Screen-reader label, when the visible content is art or a bare number. */
  label?: string;
  variant?: "tile" | "card" | "plain" | "pass";
  children: ReactNode;
}

/**
 * Every clickable option in the action panel, so the affordances stay identical across eight very
 * different pickers.
 *
 * Carries `data-action-option` on top of its specific `data-testid`: an element can only have one
 * test id, but a test (and the Playwright spec that plays a whole game by clicking whatever is
 * offered) needs a way to say "any legal option, whatever this phase calls it". The specific id is
 * for asserting a particular choice; the marker attribute is for finding one at all.
 */
export function ActionButton({
  testId,
  onClick,
  disabled,
  label,
  variant = "plain",
  children,
}: ActionButtonProps) {
  return (
    <button
      type="button"
      className={`option option--${variant}`}
      data-testid={testId}
      data-action-option="true"
      aria-label={label}
      disabled={disabled}
      onClick={onClick}
    >
      {children}
    </button>
  );
}
