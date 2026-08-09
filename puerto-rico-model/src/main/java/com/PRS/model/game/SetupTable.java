package com.PRS.model.game;

import com.PRS.model.boards.TileType;
import java.util.List;

/** The rulebook's setup values, which all scale with the number of players. */
public final class SetupTable {

  public static final int MIN_PLAYERS = 3;
  public static final int MAX_PLAYERS = 5;

  private SetupTable() {}

  public static void requireSupportedPlayerCount(int playerCount) {
    if (playerCount < MIN_PLAYERS || playerCount > MAX_PLAYERS) {
      throw new IllegalArgumentException(
          "Puerto Rico is a %d-%d player game, got %d"
              .formatted(MIN_PLAYERS, MAX_PLAYERS, playerCount));
    }
  }

  public static int startingDoubloons(int playerCount) {
    requireSupportedPlayerCount(playerCount);
    return playerCount - 1;
  }

  public static int colonistSupply(int playerCount) {
    return switch (playerCount) {
      case 3 -> 55;
      case 4 -> 75;
      case 5 -> 95;
      default -> throw unsupported(playerCount);
    };
  }

  public static int victoryPointSupply(int playerCount) {
    return switch (playerCount) {
      case 3 -> 75;
      case 4 -> 100;
      case 5 -> 122;
      default -> throw unsupported(playerCount);
    };
  }

  /** Three ships, each one bigger than the last, starting one above the player count. */
  public static List<Integer> cargoShipCapacities(int playerCount) {
    requireSupportedPlayerCount(playerCount);
    return List.of(playerCount + 1, playerCount + 2, playerCount + 3);
  }

  /** Always three more cards than players, so exactly three go unchosen each round. */
  public static int roleCardCount(int playerCount) {
    requireSupportedPlayerCount(playerCount);
    return playerCount + 3;
  }

  public static int faceUpPlantations(int playerCount) {
    requireSupportedPlayerCount(playerCount);
    return playerCount + 1;
  }

  /** The colonist ship starts holding one colonist per player. */
  public static int startingColonistsOnShip(int playerCount) {
    requireSupportedPlayerCount(playerCount);
    return playerCount;
  }

  /**
   * Earlier seats start with indigo and later ones with corn: two corn at four and five players,
   * one at three.
   */
  public static TileType startingPlantation(int playerCount, int seat) {
    requireSupportedPlayerCount(playerCount);
    int cornSeats = playerCount == 3 ? 1 : 2;
    return seat >= playerCount - cornSeats ? TileType.CORN : TileType.INDIGO;
  }

  private static IllegalArgumentException unsupported(int playerCount) {
    return new IllegalArgumentException("Unsupported player count: " + playerCount);
  }
}
