package com.PRS.web.wire;

import com.PRS.contract.model.BuildOptionView;
import com.PRS.contract.model.BuildingCatalogEntry;
import com.PRS.contract.model.CargoShipView;
import com.PRS.contract.model.GameConfigView;
import com.PRS.contract.model.GameStateView;
import com.PRS.contract.model.GoodPriceView;
import com.PRS.contract.model.IslandTile;
import com.PRS.contract.model.PlacedBuilding;
import com.PRS.contract.model.PlayerStateView;
import com.PRS.contract.model.RoleCardView;
import com.PRS.contract.model.RoleTrackView;
import com.PRS.contract.model.TileSupplyView;
import com.PRS.contract.model.TradingHouseView;
import com.PRS.model.boards.CargoShip;
import com.PRS.model.boards.PlayerState;
import com.PRS.model.engine.GameEngine;
import com.PRS.model.game.GameConfig;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.goods.Good;
import com.PRS.model.rolecards.RoleCard;
import com.PRS.model.rolecards.RoleTrack;
import com.PRS.model.scoring.ScoreBreakdown;
import com.PRS.session.actors.Decision;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@code com.PRS.session.view.GameView} (and everything it carries: {@code GameState}, {@code
 * Decision}, {@code ScoreBreakdown}) to the generated wire DTOs. One direction only — nothing on
 * this side is ever deserialized from the wire.
 */
public final class GameMapper {

  private GameMapper() {}

  public static com.PRS.contract.model.GameView toWire(com.PRS.session.view.GameView view) {
    com.PRS.contract.model.GameView wire =
        new com.PRS.contract.model.GameView(toWire(view, view.state()));
    if (view.viewerSeat() != null) {
      wire.viewerSeat(view.viewerSeat());
    }
    return wire;
  }

  public static com.PRS.contract.model.Decision toWire(Decision decision) {
    com.PRS.contract.model.Decision wire =
        new com.PRS.contract.model.Decision(
            decision.seat(),
            toWire(decision.view()),
            decision.options().stream().map(ActionMapper::toWire).toList(),
            decision.requestId());
    return wire;
  }

  public static com.PRS.contract.model.ScoreBreakdown toWire(ScoreBreakdown score) {
    return new com.PRS.contract.model.ScoreBreakdown(
        score.seat(),
        score.name(),
        score.chips(),
        score.buildingPoints(),
        score.bonusPoints(),
        score.tiebreak(),
        score.total());
  }

  private static GameStateView toWire(com.PRS.session.view.GameView view, GameState state) {
    GameStateView wire =
        new GameStateView(
            toWire(state.config()),
            state.players().stream().map(GameMapper::toWirePlayer).toList(),
            state.governorSeat(),
            toWire(state.roles()),
            toWireTiles(view),
            toGoodsMap(state.goods().barrels()),
            toBuildingMap(state.buildings().remaining()),
            toWire(state.tradingHouse()),
            state.ships().stream().map(GameMapper::toWire).toList(),
            state.colonistSupply(),
            state.colonistsOnShip(),
            state.victoryPointSupply(),
            toWirePhase(state),
            state.finalRound());
    return wire;
  }

  private static GameConfigView toWire(GameConfig config) {
    return new GameConfigView(config.playerNames(), buildingCatalog());
  }

  /**
   * The printed building table, every type of it. {@link #buildOptions} quotes what one player
   * would pay right now and exists only during the builder phase; this is the card face, constant
   * for the whole game, so a client can draw the building display in any phase.
   */
  private static List<BuildingCatalogEntry> buildingCatalog() {
    return Arrays.stream(com.PRS.model.buildings.BuildingType.values())
        .map(
            type ->
                new BuildingCatalogEntry(
                    ActionMapper.toWire(type),
                    type.cost(),
                    type.victoryPoints(),
                    type.colonistCapacity(),
                    type.copies()))
        .toList();
  }

  private static PlayerStateView toWirePlayer(PlayerState player) {
    PlayerStateView wire =
        new PlayerStateView(
            player.seat(),
            player.name(),
            player.doubloons(),
            player.victoryPoints(),
            player.island().stream()
                .map(t -> new IslandTile(toWire(t.type()), t.occupied()))
                .toList(),
            PlayerState.ISLAND_SPACES,
            player.buildings().stream().map(GameMapper::toWire).toList(),
            PlayerState.CITY_SPACES,
            player.citySpacesUsed(),
            player.colonistsInSanJuan(),
            toGoodsMap(player.goods()));
    return wire;
  }

  /**
   * Carries the card's printed capacity and victory points alongside the occupancy count, so a
   * client can draw a building without holding its own copy of the building table.
   */
  private static PlacedBuilding toWire(com.PRS.model.buildings.PlacedBuilding building) {
    com.PRS.model.buildings.BuildingType type = building.type();
    return new PlacedBuilding(
        ActionMapper.toWire(type),
        building.colonists(),
        type.colonistCapacity(),
        type.victoryPoints());
  }

  private static RoleTrackView toWire(RoleTrack roles) {
    return new RoleTrackView(roles.cards().stream().map(GameMapper::toWire).toList());
  }

  private static RoleCardView toWire(RoleCard card) {
    RoleCardView wire = new RoleCardView(ActionMapper.toWire(card.role()), card.doubloons());
    if (card.takenBySeat() != null) {
      wire.takenBySeat(card.takenBySeat());
    }
    return wire;
  }

  private static TileSupplyView toWireTiles(com.PRS.session.view.GameView view) {
    var tiles = view.state().tiles();
    return new TileSupplyView(
        tiles.faceUp().stream().map(GameMapper::toWire).toList(),
        tiles.quarriesRemaining(),
        view.faceDownTiles(),
        view.discardedTiles());
  }

  private static com.PRS.contract.model.TileType toWire(com.PRS.model.boards.TileType type) {
    return com.PRS.contract.model.TileType.valueOf(type.name());
  }

  private static TradingHouseView toWire(com.PRS.model.goods.TradingHouse house) {
    return new TradingHouseView(house.goods().stream().map(ActionMapper::toWire).toList());
  }

  private static CargoShipView toWire(CargoShip ship) {
    CargoShipView wire = new CargoShipView(ship.capacity(), ship.loaded());
    ship.cargoKind().ifPresent(good -> wire.cargo(ActionMapper.toWire(good)));
    return wire;
  }

  /**
   * Takes the whole state, not just the phase: the builder and trader phases quote prices, and
   * those are functions of the acting player's board as well as the phase.
   */
  private static com.PRS.contract.model.Phase toWirePhase(GameState state) {
    Phase phase = state.phase();
    com.PRS.contract.model.Phase wire;
    switch (phase) {
      case Phase.RoleSelection p ->
          wire = phaseOf(com.PRS.contract.model.Phase.TypeEnum.ROLE_SELECTION, p.actorSeat());
      case Phase.SettlerPhase p -> {
        wire = phaseOf(com.PRS.contract.model.Phase.TypeEnum.SETTLER, p.actorSeat());
        wire.chooserSeat(p.chooserSeat());
        wire.queue(p.queue());
        wire.haciendaOffered(p.haciendaOffered());
      }
      case Phase.MayorPhase p -> {
        wire = phaseOf(com.PRS.contract.model.Phase.TypeEnum.MAYOR, p.actorSeat());
        wire.chooserSeat(p.chooserSeat());
        wire.queue(p.queue());
      }
      case Phase.BuilderPhase p -> {
        wire = phaseOf(com.PRS.contract.model.Phase.TypeEnum.BUILDER, p.actorSeat());
        wire.chooserSeat(p.chooserSeat());
        wire.queue(p.queue());
        wire.buildOptions(buildOptions(state));
      }
      case Phase.CraftsmanBonus p -> {
        wire = phaseOf(com.PRS.contract.model.Phase.TypeEnum.CRAFTSMAN_BONUS, p.actorSeat());
        wire.chooserSeat(p.chooserSeat());
        wire.craftsmanOptions(p.options().stream().map(ActionMapper::toWire).toList());
      }
      case Phase.TraderPhase p -> {
        wire = phaseOf(com.PRS.contract.model.Phase.TypeEnum.TRADER, p.actorSeat());
        wire.chooserSeat(p.chooserSeat());
        wire.queue(p.queue());
        wire.goodPrices(goodPrices(state));
      }
      case Phase.CaptainLoading p -> {
        wire = phaseOf(com.PRS.contract.model.Phase.TypeEnum.CAPTAIN_LOADING, p.actorSeat());
        wire.chooserSeat(p.chooserSeat());
        wire.wharfUsed(p.wharfUsed().stream().toList());
        wire.bonusUsed(p.bonusUsed());
      }
      case Phase.CaptainStorage p -> {
        wire = phaseOf(com.PRS.contract.model.Phase.TypeEnum.CAPTAIN_STORAGE, p.actorSeat());
        wire.chooserSeat(p.chooserSeat());
        wire.queue(p.queue());
      }
      case Phase.GameOver p ->
          wire = phaseOf(com.PRS.contract.model.Phase.TypeEnum.GAME_OVER, p.actorSeat());
    }
    return wire;
  }

  /**
   * Every building priced for the acting player — not only the affordable ones. Which of them may
   * actually be bought is {@code Decision.options}' job; this is the price list beside it.
   */
  private static List<BuildOptionView> buildOptions(GameState state) {
    return Arrays.stream(com.PRS.model.buildings.BuildingType.values())
        .map(
            type ->
                new BuildOptionView(
                    ActionMapper.toWire(type),
                    GameEngine.buildCost(state, type),
                    type.victoryPoints(),
                    type.colonistCapacity()))
        .toList();
  }

  /** Every good priced for the acting player, for the same reason as {@link #buildOptions}. */
  private static List<GoodPriceView> goodPrices(GameState state) {
    return Arrays.stream(Good.values())
        .map(
            good -> new GoodPriceView(ActionMapper.toWire(good), GameEngine.sellPrice(state, good)))
        .toList();
  }

  private static com.PRS.contract.model.Phase phaseOf(
      com.PRS.contract.model.Phase.TypeEnum type, int actorSeat) {
    return new com.PRS.contract.model.Phase(type, actorSeat);
  }

  private static Map<String, Integer> toGoodsMap(Map<Good, Integer> goods) {
    return goods.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
  }

  private static Map<String, Integer> toBuildingMap(
      Map<com.PRS.model.buildings.BuildingType, Integer> buildings) {
    return buildings.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
  }
}
