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
import lombok.Builder;

/**
 * The complete state of one game. Immutable: the engine returns a new instance per action, so
 * callers can keep snapshots, replay, or search ahead without defensive copying.
 *
 * <p>Update it with {@code state.toBuilder().phase(next).build()}.
 */
@Builder(toBuilder = true)
public record GameState(
    GameConfig config,
    List<PlayerState> players,
    int governorSeat,
    RoleTrack roles,
    TileSupply tiles,
    GoodsSupply goods,
    BuildingSupply buildings,
    TradingHouse tradingHouse,
    List<CargoShip> ships,
    int colonistSupply,
    int colonistsOnShip,
    int victoryPointSupply,
    Phase phase,
    /** Set once an end condition fires; the game stops when the round finishes. */
    boolean finalRound) {

  public GameState {
    players = List.copyOf(players);
    ships = List.copyOf(ships);
  }

  public int playerCount() {
    return players.size();
  }

  public PlayerState player(int seat) {
    return players.get(seat);
  }

  public boolean isOver() {
    return phase instanceof Phase.GameOver;
  }

  /** Seat order for a phase: the player who chose the role, then clockwise. */
  public List<Integer> turnOrderFrom(int seat) {
    List<Integer> order = new ArrayList<>(playerCount());
    for (int i = 0; i < playerCount(); i++) {
      order.add((seat + i) % playerCount());
    }
    return order;
  }

  public int nextSeat(int seat) {
    return (seat + 1) % playerCount();
  }

  /** Replaces one player, keyed on their seat. */
  public GameState withPlayer(PlayerState player) {
    List<PlayerState> next = new ArrayList<>(players);
    next.set(player.seat(), player);
    return toBuilder().players(next).build();
  }

  public GameState withPhase(Phase next) {
    return toBuilder().phase(next).build();
  }

  public GameState withTiles(TileSupply next) {
    return toBuilder().tiles(next).build();
  }
}
