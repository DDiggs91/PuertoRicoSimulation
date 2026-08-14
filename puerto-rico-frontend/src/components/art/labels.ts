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
  SET_COLONIST_PLACEMENT: "placed their colonists",
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
 * What each building does, one line — the text printed on the card itself, minus the numbers the
 * card already shows. Production buildings say what they turn a plantation into; violet ones say
 * their function and the phase it fires in, since that is what a player is scanning the display
 * for. Large violet bonuses are scored at game end and only while occupied, which the wording says
 * out loud because it is the edge people forget.
 */
export const BUILDING_SUMMARIES: Record<BuildingType, string> = {
  SMALL_INDIGO_PLANT: "Turns occupied indigo plantations into indigo.",
  SMALL_SUGAR_MILL: "Turns occupied sugar plantations into sugar.",
  INDIGO_PLANT: "Turns occupied indigo plantations into indigo.",
  SUGAR_MILL: "Turns occupied sugar plantations into sugar.",
  TOBACCO_STORAGE: "Turns occupied tobacco fields into tobacco.",
  COFFEE_ROASTER: "Turns occupied coffee plantations into coffee.",
  SMALL_MARKET: "Trader: one extra doubloon on every good you sell.",
  HACIENDA: "Settler: also take the top face-down plantation, and place it.",
  CONSTRUCTION_HUT: "Settler: take a quarry instead of a plantation.",
  SMALL_WAREHOUSE: "Captain: keep every barrel of one kind you choose.",
  HOSPICE: "Settler: a colonist arrives on each plantation you settle.",
  OFFICE: "Trader: sell a kind the trading house already holds.",
  LARGE_MARKET: "Trader: two extra doubloons on every good you sell.",
  LARGE_WAREHOUSE: "Captain: keep every barrel of two kinds you choose.",
  FACTORY: "Craftsman: doubloons for the variety of goods you produced.",
  UNIVERSITY: "Builder: a colonist arrives on each building you build.",
  HARBOR: "Captain: one extra victory point every time you load.",
  WHARF: "Captain: once a phase, ship every barrel of one kind at will.",
  GUILD_HALL: "1 VP per small production building, 2 per large. Occupied only.",
  RESIDENCE: "4 VP, rising to 7 as your island fills past 9 tiles. Occupied only.",
  FORTRESS: "1 VP per three colonists anywhere on your board. Occupied only.",
  CUSTOMS_HOUSE: "1 VP per four victory point chips you shipped for. Occupied only.",
  CITY_HALL: "1 VP per violet building in your city, itself included. Occupied only.",
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
