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
  | components["schemas"]["PlaceColonistAction"]
  | components["schemas"]["EndColonistPlacementAction"]
  | components["schemas"]["BuildBuildingAction"]
  | components["schemas"]["PassBuildingAction"]
  | components["schemas"]["TakeCraftsmanBonusAction"]
  | components["schemas"]["PassCraftsmanBonusAction"]
  | components["schemas"]["SellGoodAction"]
  | components["schemas"]["PassTradingAction"]
  | components["schemas"]["LoadShipAction"]
  | components["schemas"]["LoadWharfAction"]
  | components["schemas"]["StoreGoodsAction"];

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
