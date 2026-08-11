package com.PRS.model.game;

import com.PRS.model.boards.CargoShip;
import com.PRS.model.boards.PlayerState;
import com.PRS.model.boards.TileSupply;
import com.PRS.model.buildings.BuildingSupply;
import com.PRS.model.goods.GoodsSupply;
import com.PRS.model.goods.TradingHouse;
import com.PRS.model.rolecards.RoleTrack;
import java.util.ArrayList;
import java.util.List;

/** Deals a new game. Seat 0 is the first governor. */
public final class GameSetup {

  private GameSetup() {}

  public static GameState create(GameConfig config) {
    int playerCount = config.playerCount();

    List<PlayerState> players = new ArrayList<>(playerCount);
    for (int seat = 0; seat < playerCount; seat++) {
      players.add(
          PlayerState.newPlayer(
              seat,
              config.playerNames().get(seat),
              SetupTable.startingDoubloons(playerCount),
              SetupTable.startingPlantation(playerCount, seat)));
    }

    List<CargoShip> ships =
        SetupTable.cargoShipCapacities(playerCount).stream().map(CargoShip::empty).toList();

    return GameState.builder()
        .config(config)
        .players(players)
        .governorSeat(0)
        .roles(RoleTrack.forPlayerCount(playerCount))
        .tiles(
            TileSupply.create(config.seed())
                .refillFaceUp(SetupTable.faceUpPlantations(playerCount)))
        .goods(GoodsSupply.full())
        .buildings(BuildingSupply.full())
        .tradingHouse(TradingHouse.empty())
        .ships(ships)
        .colonistSupply(
            SetupTable.colonistSupply(playerCount)
                - SetupTable.startingColonistsOnShip(playerCount))
        .colonistsOnShip(SetupTable.startingColonistsOnShip(playerCount))
        .victoryPointSupply(SetupTable.victoryPointSupply(playerCount))
        .phase(new Phase.RoleSelection(0))
        .finalRound(false)
        .build();
  }
}
