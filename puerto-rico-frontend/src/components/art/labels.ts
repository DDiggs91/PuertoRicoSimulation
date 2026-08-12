import type { BuildingType, Good, Role, TileType } from "../../api/types";

/**
 * Presentation only: how a wire enum is spelled to a person, and what colour it's drawn in.
 *
 * Nothing here is a rule. Every *number* the UI shows — a building's cost, a good's price, a
 * card's colonist circles — comes off the wire, computed by the model (see `GameEngine.buildCost`
 * / `sellPrice` and `PlacedBuilding`). What this file holds is the half the server has no opinion
 * about: English names and a palette. The enums it keys on are closed — the base game has exactly
 * five goods, six tile types, seven roles and twenty-three buildings — so there is no drift to
 * track, only spelling.
 */

export const GOOD_NAMES: Record<Good, string> = {
  CORN: "Corn",
  INDIGO: "Indigo",
  SUGAR: "Sugar",
  TOBACCO: "Tobacco",
  COFFEE: "Coffee",
};

/** Barrel colours, taken from the goods' printed look: pale corn through near-black coffee. */
export const GOOD_COLORS: Record<Good, string> = {
  CORN: "#e0b84b",
  INDIGO: "#42559b",
  SUGAR: "#efe9db",
  TOBACCO: "#9a6b3c",
  COFFEE: "#43301f",
};

/** Goods in trading-house order, which is also ascending price order. */
export const GOOD_ORDER: Good[] = ["CORN", "INDIGO", "SUGAR", "TOBACCO", "COFFEE"];

export const TILE_NAMES: Record<TileType, string> = {
  CORN: "Corn field",
  INDIGO: "Indigo plantation",
  SUGAR: "Sugar plantation",
  TOBACCO: "Tobacco field",
  COFFEE: "Coffee plantation",
  QUARRY: "Quarry",
};

export const ROLE_NAMES: Record<Role, string> = {
  SETTLER: "Settler",
  MAYOR: "Mayor",
  BUILDER: "Builder",
  CRAFTSMAN: "Craftsman",
  TRADER: "Trader",
  CAPTAIN: "Captain",
  PROSPECTOR: "Prospector",
};

/** What each role does, one line — the same summary the board prints beside the role tiles. */
export const ROLE_SUMMARIES: Record<Role, string> = {
  SETTLER: "Everyone takes a plantation. You may take a quarry instead.",
  MAYOR: "Everyone takes colonists. You take one extra from the supply.",
  BUILDER: "Everyone may build. You pay one doubloon less.",
  CRAFTSMAN: "Everyone produces goods. You take one extra barrel.",
  TRADER: "Everyone may sell one good. You earn one doubloon more.",
  CAPTAIN: "Everyone ships goods. You score one extra victory point.",
  PROSPECTOR: "No action — you simply take a doubloon from the bank.",
};

/** What the board is doing right now, for the phase line and the action panel's prompt. */
export const PHASE_NAMES: Record<string, string> = {
  ROLE_SELECTION: "Choosing a role",
  SETTLER: "Settler — taking plantations",
  MAYOR: "Mayor — placing colonists",
  BUILDER: "Builder — building",
  CRAFTSMAN_BONUS: "Craftsman — the extra barrel",
  TRADER: "Trader — selling goods",
  CAPTAIN_LOADING: "Captain — loading ships",
  CAPTAIN_STORAGE: "Captain — storing goods",
  GAME_OVER: "Game over",
};

/** What a player just did, for the event log. */
export const ACTION_NAMES: Record<string, string> = {
  SELECT_ROLE: "took a role",
  TAKE_FACE_UP_TILE: "took a plantation",
  TAKE_QUARRY: "took a quarry",
  TAKE_HACIENDA_TILE: "used the Hacienda",
  SKIP_HACIENDA: "skipped the Hacienda",
  PASS_SETTLING: "took no plantation",
  PLACE_COLONIST: "placed a colonist",
  END_COLONIST_PLACEMENT: "finished placing colonists",
  BUILD_BUILDING: "built a building",
  PASS_BUILDING: "built nothing",
  TAKE_CRAFTSMAN_BONUS: "took an extra barrel",
  PASS_CRAFTSMAN_BONUS: "took no extra barrel",
  SELL_GOOD: "sold a good",
  PASS_TRADING: "sold nothing",
  LOAD_SHIP: "loaded a ship",
  LOAD_WHARF: "used the Wharf",
  DECLINE_WHARF: "declined the Wharf",
  STORE_GOODS: "stored goods",
};

export const BUILDING_NAMES: Record<BuildingType, string> = {
  SMALL_INDIGO_PLANT: "Small Indigo Plant",
  SMALL_SUGAR_MILL: "Small Sugar Mill",
  INDIGO_PLANT: "Indigo Plant",
  SUGAR_MILL: "Sugar Mill",
  TOBACCO_STORAGE: "Tobacco Storage",
  COFFEE_ROASTER: "Coffee Roaster",
  SMALL_MARKET: "Small Market",
  HACIENDA: "Hacienda",
  CONSTRUCTION_HUT: "Construction Hut",
  SMALL_WAREHOUSE: "Small Warehouse",
  HOSPICE: "Hospice",
  OFFICE: "Office",
  LARGE_MARKET: "Large Market",
  LARGE_WAREHOUSE: "Large Warehouse",
  FACTORY: "Factory",
  UNIVERSITY: "University",
  HARBOR: "Harbor",
  WHARF: "Wharf",
  GUILD_HALL: "Guild Hall",
  RESIDENCE: "Residence",
  FORTRESS: "Fortress",
  CUSTOMS_HOUSE: "Customs House",
  CITY_HALL: "City Hall",
};

/**
 * Which colour band a building card is drawn in. The physical cards separate production buildings
 * (blue-grey) from the violet ones, and give the five big violets their own darker tier — the same
 * grouping `BuildingCategory` makes server-side, restated here purely as a palette key.
 */
export type BuildingTint = "production" | "violet" | "violet-large";

const PRODUCTION: BuildingType[] = [
  "SMALL_INDIGO_PLANT",
  "SMALL_SUGAR_MILL",
  "INDIGO_PLANT",
  "SUGAR_MILL",
  "TOBACCO_STORAGE",
  "COFFEE_ROASTER",
];

const VIOLET_LARGE: BuildingType[] = [
  "GUILD_HALL",
  "RESIDENCE",
  "FORTRESS",
  "CUSTOMS_HOUSE",
  "CITY_HALL",
];

export function buildingTint(type: BuildingType): BuildingTint {
  if (PRODUCTION.includes(type)) {
    return "production";
  }
  return VIOLET_LARGE.includes(type) ? "violet-large" : "violet";
}
