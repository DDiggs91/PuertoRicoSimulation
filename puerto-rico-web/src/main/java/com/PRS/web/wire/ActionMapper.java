package com.PRS.web.wire;

import com.PRS.contract.model.BuildBuildingAction;
import com.PRS.contract.model.DeclineWharfAction;
import com.PRS.contract.model.LoadShipAction;
import com.PRS.contract.model.LoadWharfAction;
import com.PRS.contract.model.PassBuildingAction;
import com.PRS.contract.model.PassCraftsmanBonusAction;
import com.PRS.contract.model.PassSettlingAction;
import com.PRS.contract.model.PassTradingAction;
import com.PRS.contract.model.SelectRoleAction;
import com.PRS.contract.model.SellGoodAction;
import com.PRS.contract.model.SetColonistPlacementAction;
import com.PRS.contract.model.SkipHaciendaAction;
import com.PRS.contract.model.StoreGoodsAction;
import com.PRS.contract.model.TakeCraftsmanBonusAction;
import com.PRS.contract.model.TakeFaceUpTileAction;
import com.PRS.contract.model.TakeHaciendaTileAction;
import com.PRS.contract.model.TakeQuarryAction;
import com.PRS.model.actions.PlayerAction;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.goods.Good;
import com.PRS.model.rolecards.Role;

/** {@code com.PRS.model.actions.PlayerAction} &lt;-&gt; the generated wire hierarchy, both ways. */
public final class ActionMapper {

  private ActionMapper() {}

  public static com.PRS.contract.model.PlayerAction toWire(PlayerAction action) {
    return switch (action) {
      case PlayerAction.SelectRole a -> {
        SelectRoleAction wire = new SelectRoleAction();
        wire.setSeat(a.seat());
        wire.setRole(toWire(a.role()));
        yield wire;
      }
      case PlayerAction.TakeFaceUpTile a -> {
        TakeFaceUpTileAction wire = new TakeFaceUpTileAction();
        wire.setSeat(a.seat());
        wire.setFaceUpIndex(a.faceUpIndex());
        yield wire;
      }
      case PlayerAction.TakeQuarry a -> {
        TakeQuarryAction wire = new TakeQuarryAction();
        wire.setSeat(a.seat());
        yield wire;
      }
      case PlayerAction.TakeHaciendaTile a -> {
        TakeHaciendaTileAction wire = new TakeHaciendaTileAction();
        wire.setSeat(a.seat());
        yield wire;
      }
      case PlayerAction.SkipHacienda a -> {
        SkipHaciendaAction wire = new SkipHaciendaAction();
        wire.setSeat(a.seat());
        yield wire;
      }
      case PlayerAction.PassSettling a -> {
        PassSettlingAction wire = new PassSettlingAction();
        wire.setSeat(a.seat());
        yield wire;
      }
      case PlayerAction.SetColonistPlacement a -> {
        SetColonistPlacementAction wire = new SetColonistPlacementAction();
        wire.setSeat(a.seat());
        wire.setIslandOccupied(a.islandOccupied());
        wire.setBuildingColonists(a.buildingColonists());
        yield wire;
      }
      case PlayerAction.BuildBuilding a -> {
        BuildBuildingAction wire = new BuildBuildingAction();
        wire.setSeat(a.seat());
        wire.setBuildingType(toWire(a.type()));
        yield wire;
      }
      case PlayerAction.PassBuilding a -> {
        PassBuildingAction wire = new PassBuildingAction();
        wire.setSeat(a.seat());
        yield wire;
      }
      case PlayerAction.TakeCraftsmanBonus a -> {
        TakeCraftsmanBonusAction wire = new TakeCraftsmanBonusAction();
        wire.setSeat(a.seat());
        wire.setGood(toWire(a.good()));
        yield wire;
      }
      case PlayerAction.PassCraftsmanBonus a -> {
        PassCraftsmanBonusAction wire = new PassCraftsmanBonusAction();
        wire.setSeat(a.seat());
        yield wire;
      }
      case PlayerAction.SellGood a -> {
        SellGoodAction wire = new SellGoodAction();
        wire.setSeat(a.seat());
        wire.setGood(toWire(a.good()));
        yield wire;
      }
      case PlayerAction.PassTrading a -> {
        PassTradingAction wire = new PassTradingAction();
        wire.setSeat(a.seat());
        yield wire;
      }
      case PlayerAction.LoadShip a -> {
        LoadShipAction wire = new LoadShipAction();
        wire.setSeat(a.seat());
        wire.setShipIndex(a.shipIndex());
        wire.setGood(toWire(a.good()));
        yield wire;
      }
      case PlayerAction.LoadWharf a -> {
        LoadWharfAction wire = new LoadWharfAction();
        wire.setSeat(a.seat());
        wire.setGood(toWire(a.good()));
        yield wire;
      }
      case PlayerAction.DeclineWharf a -> {
        DeclineWharfAction wire = new DeclineWharfAction();
        wire.setSeat(a.seat());
        yield wire;
      }
      case PlayerAction.StoreGoods a -> {
        StoreGoodsAction wire = new StoreGoodsAction();
        wire.setSeat(a.seat());
        wire.setWarehouseKinds(a.warehouseKinds().stream().map(ActionMapper::toWire).toList());
        wire.singleBarrel(a.singleBarrel() == null ? null : toWire(a.singleBarrel()));
        yield wire;
      }
    };
  }

  public static PlayerAction toModel(com.PRS.contract.model.PlayerAction action) {
    return switch (action) {
      case SelectRoleAction a -> new PlayerAction.SelectRole(a.getSeat(), toModel(a.getRole()));
      case TakeFaceUpTileAction a ->
          new PlayerAction.TakeFaceUpTile(a.getSeat(), a.getFaceUpIndex());
      case TakeQuarryAction a -> new PlayerAction.TakeQuarry(a.getSeat());
      case TakeHaciendaTileAction a -> new PlayerAction.TakeHaciendaTile(a.getSeat());
      case SkipHaciendaAction a -> new PlayerAction.SkipHacienda(a.getSeat());
      case PassSettlingAction a -> new PlayerAction.PassSettling(a.getSeat());
      case SetColonistPlacementAction a ->
          new PlayerAction.SetColonistPlacement(
              a.getSeat(), a.getIslandOccupied(), a.getBuildingColonists());
      case BuildBuildingAction a ->
          new PlayerAction.BuildBuilding(a.getSeat(), toModel(a.getBuildingType()));
      case PassBuildingAction a -> new PlayerAction.PassBuilding(a.getSeat());
      case TakeCraftsmanBonusAction a ->
          new PlayerAction.TakeCraftsmanBonus(a.getSeat(), toModel(a.getGood()));
      case PassCraftsmanBonusAction a -> new PlayerAction.PassCraftsmanBonus(a.getSeat());
      case SellGoodAction a -> new PlayerAction.SellGood(a.getSeat(), toModel(a.getGood()));
      case PassTradingAction a -> new PlayerAction.PassTrading(a.getSeat());
      case LoadShipAction a ->
          new PlayerAction.LoadShip(a.getSeat(), a.getShipIndex(), toModel(a.getGood()));
      case LoadWharfAction a -> new PlayerAction.LoadWharf(a.getSeat(), toModel(a.getGood()));
      case DeclineWharfAction a -> new PlayerAction.DeclineWharf(a.getSeat());
      case StoreGoodsAction a -> {
        com.PRS.contract.model.Good singleBarrel = a.getSingleBarrel().orElse(null);
        yield new PlayerAction.StoreGoods(
            a.getSeat(),
            a.getWarehouseKinds().stream().map(ActionMapper::toModel).toList(),
            singleBarrel == null ? null : toModel(singleBarrel));
      }
      default ->
          throw new IllegalArgumentException(
              "Unrecognized PlayerAction wire type: " + action.getClass());
    };
  }

  static com.PRS.contract.model.Role toWire(Role role) {
    return com.PRS.contract.model.Role.valueOf(role.name());
  }

  static Role toModel(com.PRS.contract.model.Role role) {
    return Role.valueOf(role.name());
  }

  static com.PRS.contract.model.Good toWire(Good good) {
    return com.PRS.contract.model.Good.valueOf(good.name());
  }

  static Good toModel(com.PRS.contract.model.Good good) {
    return Good.valueOf(good.name());
  }

  static com.PRS.contract.model.BuildingType toWire(BuildingType type) {
    return com.PRS.contract.model.BuildingType.valueOf(type.name());
  }

  static BuildingType toModel(com.PRS.contract.model.BuildingType type) {
    return BuildingType.valueOf(type.name());
  }
}
