import type { components } from "./schema";

/**
 * openapi-typescript synthesizes a real discriminated union from a `oneOf` list, but
 * PlayerAction/SessionEvent use `allOf` + `discriminator` in the spec instead — the pattern
 * openapi-generator's Java side handles most reliably (see puerto-rico.yaml). Each concrete
 * subtype still generates correctly, literal `type` field included; only the "group these into
 * one union" step doesn't happen automatically, so it's done by hand here. Nothing about a
 * variant's shape is hand-maintained — only this grouping.
 */
export type PlayerAction =
  | components["schemas"]["SelectRoleAction"]
  | components["schemas"]["TakeFaceUpTileAction"]
  | components["schemas"]["TakeQuarryAction"]
  | components["schemas"]["TakeHaciendaTileAction"]
  | components["schemas"]["SkipHaciendaAction"]
  | components["schemas"]["PassSettlingAction"]
  | components["schemas"]["SetColonistPlacementAction"]
  | components["schemas"]["BuildBuildingAction"]
  | components["schemas"]["PassBuildingAction"]
  | components["schemas"]["TakeCraftsmanBonusAction"]
  | components["schemas"]["PassCraftsmanBonusAction"]
  | components["schemas"]["SellGoodAction"]
  | components["schemas"]["PassTradingAction"]
  | components["schemas"]["LoadShipAction"]
  | components["schemas"]["LoadWharfAction"]
  | components["schemas"]["DeclineWharfAction"]
  | components["schemas"]["StoreGoodsAction"];

/**
 * The wire's *base* `PlayerAction` — literally `{ type: string; seat: number }`, since that is all
 * the shared parent schema declares. Anywhere the spec references `PlayerAction` (a decision's
 * `options`, an applied event's `action`) generates as this rather than as the union above.
 */
export type WirePlayerAction = components["schemas"]["PlayerAction"];

/**
 * Narrows wire actions to the union above. The spec's `discriminator` already guarantees every
 * value is one of the variants; only the grouping step is missing from generation, so this
 * restates what the contract promises rather than asserting anything new about the data. One named
 * place to do it, so the cast doesn't get scattered across call sites.
 */
export function asPlayerActions(options: readonly WirePlayerAction[]): PlayerAction[] {
  return options as PlayerAction[];
}

export type SessionEvent =
  | components["schemas"]["GameStartedEvent"]
  | components["schemas"]["DecisionRequestedEvent"]
  | components["schemas"]["ActionAppliedEvent"]
  | components["schemas"]["ActionRejectedEvent"]
  | components["schemas"]["GameEndedEvent"]
  | components["schemas"]["SessionFailedEvent"];

export type GameView = components["schemas"]["GameView"];
export type GameStateView = components["schemas"]["GameStateView"];
export type PlayerStateView = components["schemas"]["PlayerStateView"];
export type Phase = components["schemas"]["Phase"];
export type Decision = components["schemas"]["Decision"];
export type ScoreBreakdown = components["schemas"]["ScoreBreakdown"];
export type GameTableSummary = components["schemas"]["GameTableSummary"];
export type SeatSummary = components["schemas"]["SeatSummary"];
export type Problem = components["schemas"]["Problem"];
export type AiEngineInfo = components["schemas"]["AiEngineInfo"];
export type ActorKind = components["schemas"]["ActorKind"];
export type GameTableStatus = components["schemas"]["GameTableStatus"];
export type Good = components["schemas"]["Good"];
export type TileType = components["schemas"]["TileType"];
export type Role = components["schemas"]["Role"];
export type BuildingType = components["schemas"]["BuildingType"];
export type BuildOptionView = components["schemas"]["BuildOptionView"];
export type GoodPriceView = components["schemas"]["GoodPriceView"];
export type PlacedBuilding = components["schemas"]["PlacedBuilding"];
export type IslandTile = components["schemas"]["IslandTile"];
export type GoodsMap = components["schemas"]["GoodsMap"];
export type BuildingSupplyMap = components["schemas"]["BuildingSupplyMap"];
export type BuildingCatalogEntry = components["schemas"]["BuildingCatalogEntry"];
export type CargoShipView = components["schemas"]["CargoShipView"];
export type TradingHouseView = components["schemas"]["TradingHouseView"];
export type RoleTrackView = components["schemas"]["RoleTrackView"];
export type RoleCardView = components["schemas"]["RoleCardView"];
export type TileSupplyView = components["schemas"]["TileSupplyView"];
