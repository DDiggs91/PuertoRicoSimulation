import type { Role } from "../../api/types";
import { ROLE_NAMES } from "./labels";

/**
 * The emblem printed on each role tile, as an original inline-SVG motif. Decorative — the role's
 * name is always rendered alongside it — so this is `aria-hidden` and the label lives on the card.
 */
export function RoleIcon({ role, size = 30 }: { role: Role; size?: number }) {
  return (
    <svg
      className="role-icon"
      width={size}
      height={size}
      viewBox="0 0 32 32"
      aria-hidden="true"
      focusable="false"
      data-testid={`role-icon-${role}`}
    >
      <title>{ROLE_NAMES[role]}</title>
      {motif(role)}
    </svg>
  );
}

function motif(role: Role) {
  switch (role) {
    // A settler's wheelbarrow, tipped over a fresh plot.
    case "SETTLER":
      return (
        <g stroke="#4a3418" strokeWidth="2" fill="none" strokeLinecap="round">
          <path d="M5 20h15l4-8H9z" fill="#8a6b3a" />
          <circle cx="11" cy="24" r="3.5" fill="#c9962c" />
          <path d="M20 20l6 5" />
        </g>
      );
    // The mayor's colonists, arriving as a group.
    case "MAYOR":
      return (
        <g fill="#7c4a24" stroke="#3a2110" strokeWidth="1.6">
          <circle cx="11" cy="13" r="5" />
          <circle cx="21" cy="13" r="5" />
          <circle cx="16" cy="23" r="5" />
        </g>
      );
    // A builder's trowel and brick course.
    case "BUILDER":
      return (
        <g stroke="#4a3418" strokeWidth="2" strokeLinecap="round">
          <path d="M6 26h20" />
          <path d="M8 22h7v-4H8zM17 22h7v-4h-7z" fill="#b5643a" />
          <path d="M22 14l5-8-9 4z" fill="#9a9089" />
        </g>
      );
    // The craftsman's press, turning plantation into barrels.
    case "CRAFTSMAN":
      return (
        <g stroke="#4a3418" strokeWidth="1.8">
          <rect x="7" y="6" width="18" height="5" rx="1.5" fill="#8a6b3a" />
          <path d="M16 11v6" />
          <rect x="9" y="17" width="14" height="9" rx="2" fill="#c9962c" />
        </g>
      );
    // The trader's scales.
    case "TRADER":
      return (
        <g stroke="#4a3418" strokeWidth="2" fill="none" strokeLinecap="round">
          <path d="M16 5v20M8 27h16" />
          <path d="M6 11h20" />
          <path d="M3 11l3 7 3-7zM23 11l3 7 3-7z" fill="#c9962c" />
        </g>
      );
    // The captain's ship's wheel.
    case "CAPTAIN":
      return (
        <g stroke="#4a3418" strokeWidth="2" fill="none">
          <circle cx="16" cy="16" r="8.5" fill="#8a6b3a" />
          <circle cx="16" cy="16" r="3" fill="#4a3418" />
          <path d="M16 3v5M16 24v5M3 16h5M24 16h5M7 7l3.5 3.5M21.5 21.5L25 25M25 7l-3.5 3.5M10.5 21.5L7 25" />
        </g>
      );
    // The prospector's pick, and the doubloon it turns up.
    case "PROSPECTOR":
      return (
        <g stroke="#4a3418" strokeWidth="2" strokeLinecap="round">
          <path d="M9 25L23 9" />
          <path d="M17 5c5 0 9 4 9 9-3-3-6-3-9-6s-3-3 0-3z" fill="#9a9089" />
          <circle cx="10" cy="24" r="4" fill="#c9962c" />
        </g>
      );
  }
}
