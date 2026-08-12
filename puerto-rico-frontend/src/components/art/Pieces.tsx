import type { Good, TileType } from "../../api/types";
import { GOOD_COLORS, GOOD_NAMES, TILE_NAMES } from "./labels";

/**
 * The game's physical pieces, drawn as inline SVG.
 *
 * Inline rather than image files on purpose: there is nothing to fetch, nothing to cache-bust, and
 * a piece can be recoloured or resized from its props instead of needing another asset. Each one
 * is `role="img"` with a label, so a screen reader hears "Indigo barrel" where a sighted player
 * sees the barrel.
 *
 * The art is original — evocative of the boxed game's look, not traced from it.
 */

/** A barrel of goods, in that good's colour. */
export function GoodBarrel({ good, size = 22 }: { good: Good; size?: number }) {
  const color = GOOD_COLORS[good];
  return (
    <svg
      className="piece piece--barrel"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      role="img"
      aria-label={`${GOOD_NAMES[good]} barrel`}
      data-testid={`barrel-${good}`}
    >
      <ellipse cx="12" cy="4.6" rx="6.4" ry="2.2" fill={color} stroke="#2f2114" strokeWidth="1" />
      <path
        d="M5.6 4.6c0 6 -1.4 8.4 0 14.4 0 1.3 12.8 1.3 12.8 0 1.4 -6 0 -8.4 0 -14.4z"
        fill={color}
        stroke="#2f2114"
        strokeWidth="1"
        strokeLinejoin="round"
      />
      <path d="M4.8 9.2h14.4M4.8 14.6h14.4" stroke="#2f2114" strokeWidth="0.9" opacity="0.65" />
    </svg>
  );
}

/** A doubloon. Gold, milled edge, and a number when it stands for a pile rather than a coin. */
export function Doubloon({ count, size = 22 }: { count?: number; size?: number }) {
  const label = count === undefined ? "Doubloon" : `${count} doubloons`;
  return (
    <svg
      className="piece piece--coin"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      role="img"
      aria-label={label}
      data-testid="doubloon"
    >
      <circle cx="12" cy="12" r="10.2" fill="#c9962c" stroke="#7a5610" strokeWidth="1.2" />
      <circle cx="12" cy="12" r="7.6" fill="#eabb52" stroke="#a67a1c" strokeWidth="0.9" />
      {count !== undefined && (
        <text
          x="12"
          y="12"
          textAnchor="middle"
          dominantBaseline="central"
          fontSize="9"
          fontWeight="700"
          fill="#4a340a"
        >
          {count}
        </text>
      )}
    </svg>
  );
}

/** A colonist — the little brown disc that staffs a plantation or a building. */
export function Colonist({ size = 16, empty = false }: { size?: number; empty?: boolean }) {
  return (
    <svg
      className={`piece piece--colonist${empty ? " piece--empty" : ""}`}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      role="img"
      aria-label={empty ? "Empty colonist space" : "Colonist"}
      data-testid={empty ? "colonist-empty" : "colonist"}
    >
      <circle
        cx="12"
        cy="12"
        r="9.6"
        fill={empty ? "none" : "#7c4a24"}
        stroke={empty ? "#8d7a56" : "#3a2110"}
        strokeWidth="1.4"
        strokeDasharray={empty ? "2.6 2.2" : undefined}
      />
      {!empty && (
        <path
          d="M12 6.6a2.7 2.7 0 1 1 0 5.4 2.7 2.7 0 0 1 0-5.4zM6.6 18.6a5.4 5.4 0 0 1 10.8 0z"
          fill="#d8b98c"
        />
      )}
    </svg>
  );
}

/** Crop motifs, one per plantation kind, drawn over the tile's field. */
function cropMotif(type: TileType) {
  switch (type) {
    case "CORN":
      return (
        <g stroke="#3d5c22" strokeWidth="1.4" fill="#dcc056">
          <path d="M16 34v-12" />
          <ellipse cx="16" cy="18" rx="3.2" ry="6" />
          <path d="M32 34v-14" />
          <ellipse cx="32" cy="16" rx="3.2" ry="6" />
        </g>
      );
    case "INDIGO":
      return (
        <g fill="#3f4f9c" stroke="#25305f" strokeWidth="1">
          <circle cx="17" cy="20" r="4.4" />
          <circle cx="27" cy="16" r="3.6" />
          <circle cx="31" cy="24" r="4.8" />
          <path d="M17 24v10M31 29v5" stroke="#2f4a1c" strokeWidth="1.6" />
        </g>
      );
    case "SUGAR":
      return (
        <g stroke="#cfd8a0" strokeWidth="2.4" strokeLinecap="round">
          <path d="M14 34V14M20 34V17M26 34V13M32 34V18" />
        </g>
      );
    case "TOBACCO":
      return (
        <g fill="#7b9a45" stroke="#3f5720" strokeWidth="1">
          <ellipse cx="16" cy="22" rx="5.6" ry="8" />
          <ellipse cx="30" cy="24" rx="5" ry="7.2" />
          <path d="M16 30v4M30 31v3" stroke="#3f5720" strokeWidth="1.6" />
        </g>
      );
    case "COFFEE":
      return (
        <g>
          <path d="M23 34V16" stroke="#4a3a22" strokeWidth="2" />
          <ellipse cx="16" cy="18" rx="5.4" ry="4" fill="#4e7332" />
          <ellipse cx="30" cy="18" rx="5.4" ry="4" fill="#4e7332" />
          <circle cx="19" cy="25" r="2.4" fill="#8d2f22" />
          <circle cx="28" cy="26" r="2.4" fill="#8d2f22" />
        </g>
      );
    case "QUARRY":
      return (
        <g fill="#a9a49b" stroke="#5f5a52" strokeWidth="1.2">
          <path d="M12 32l7-11 6 6 5-8 8 13z" />
          <circle cx="17" cy="16" r="2.6" fill="#c4bfb5" />
        </g>
      );
  }
}

/**
 * A plantation tile: a green field with its crop, or a grey quarry. A staffed tile carries its
 * colonist; an idle one shows the empty circle it needs, which is the difference between a tile
 * that produces and one that only takes up island space.
 */
export function PlantationTile({
  type,
  occupied,
  size = 56,
}: {
  type: TileType;
  occupied?: boolean;
  size?: number;
}) {
  const isQuarry = type === "QUARRY";
  return (
    <span className="tile" data-testid={`tile-${type}`} data-occupied={occupied ?? false}>
      <svg
        width={size}
        height={size}
        viewBox="0 0 46 46"
        role="img"
        aria-label={`${TILE_NAMES[type]}${occupied === undefined ? "" : occupied ? ", staffed" : ", idle"}`}
      >
        <rect
          x="1.5"
          y="1.5"
          width="43"
          height="43"
          rx="4"
          fill={isQuarry ? "#8e8880" : "#5b8a3c"}
          stroke="#2c3a1c"
          strokeWidth="2"
        />
        <rect
          x="4.5"
          y="4.5"
          width="37"
          height="37"
          rx="2.5"
          fill={isQuarry ? "#9d968c" : "#67994a"}
        />
        <path
          d="M4.5 34h37v7.5a2.5 2.5 0 0 1-2.5 2.5H7a2.5 2.5 0 0 1-2.5-2.5z"
          fill={isQuarry ? "#877f75" : "#4c7733"}
        />
        {cropMotif(type)}
      </svg>
      {occupied !== undefined && (
        <span className="tile__colonist">
          <Colonist size={18} empty={!occupied} />
        </span>
      )}
    </span>
  );
}

/**
 * A cargo ship: one hold per unit of capacity, filled left to right. A ship only ever carries one
 * kind of good, so the whole loaded run is that colour.
 */
export function ShipArt({
  capacity,
  loaded,
  cargo,
}: {
  capacity: number;
  loaded: number;
  cargo?: Good | null;
}) {
  const holdWidth = 15;
  const width = capacity * holdWidth + 22;
  const label = cargo
    ? `Ship with ${capacity} holds, ${loaded} loaded with ${GOOD_NAMES[cargo]}`
    : `Empty ship with ${capacity} holds`;
  return (
    <svg
      className="ship"
      width={width}
      height="46"
      viewBox={`0 0 ${width} 46`}
      role="img"
      aria-label={label}
    >
      <path
        d={`M4 12h${width - 8}v14c0 8-5 13-13 13H17c-8 0-13-5-13-13z`}
        fill="#8a5a2b"
        stroke="#3f2712"
        strokeWidth="2"
      />
      {Array.from({ length: capacity }, (_, i) => (
        <rect
          key={i}
          x={11 + i * holdWidth}
          y="16"
          width={holdWidth - 4}
          height="13"
          rx="2"
          fill={i < loaded && cargo ? GOOD_COLORS[cargo] : "#6b4520"}
          stroke="#3f2712"
          strokeWidth="1.2"
        />
      ))}
      <path d="M4 12L1 6h6z" fill="#3f2712" />
    </svg>
  );
}
