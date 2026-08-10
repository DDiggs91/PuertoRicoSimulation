package com.PRS.web.wire;

import com.PRS.contract.model.CargoShipView;
import com.PRS.contract.model.GameConfigView;
import com.PRS.contract.model.GameStateView;
import com.PRS.contract.model.IslandTile;
import com.PRS.contract.model.PlacedBuilding;
import com.PRS.contract.model.PlayerStateView;
import com.PRS.contract.model.RoleCardView;
import com.PRS.contract.model.RoleTrackView;
import com.PRS.contract.model.TileSupplyView;
import com.PRS.contract.model.TradingHouseView;
import com.PRS.model.boards.CargoShip;
import com.PRS.model.game.GameConfig;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.goods.Good;
import com.PRS.model.rolecards.RoleCard;
import com.PRS.model.rolecards.RoleTrack;
import com.PRS.model.scoring.ScoreBreakdown;
import com.PRS.session.actors.Decision;
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
            toWire(state.phase()),
            state.finalRound());
    return wire;
  }

  private static GameConfigView toWire(GameConfig config) {
    return new GameConfigView(config.playerNames());
  }

  private static PlayerStateView toWirePlayer(com.PRS.model.boards.PlayerState player) {
    PlayerStateView wire =
        new PlayerStateView(
            player.seat(),
            player.name(),
            player.doubloons(),
            player.victoryPoints(),
            player.island().stream()
                .map(t -> new IslandTile(toWire(t.type()), t.occupied()))
                .toList(),
            player.buildings().stream()
                .map(b -> new PlacedBuilding(ActionMapper.toWire(b.type()), b.colonists()))
                .toList(),
            player.colonistsInSanJuan(),
            toGoodsMap(player.goods()));
    return wire;
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

  private static com.PRS.contract.model.Phase toWire(Phase phase) {
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
