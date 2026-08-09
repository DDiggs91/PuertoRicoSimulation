package com.PRS.model.scoring;

import com.PRS.model.boards.PlayerState;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.buildings.PlacedBuilding;
import com.PRS.model.game.GameState;
import java.util.Comparator;
import java.util.List;

/**
 * Final scoring: VP chips, the printed value of every building, and the large buildings' bonuses.
 *
 * <p>Buildings score their printed points whether occupied or not; only the large buildings'
 * <em>extra</em> points require a colonist.
 */
public final class Scorer {

  private Scorer() {}

  public static ScoreBreakdown score(PlayerState player) {
    int buildingPoints = player.buildings().stream().mapToInt(b -> b.type().victoryPoints()).sum();
    return new ScoreBreakdown(
        player.seat(),
        player.name(),
        player.victoryPoints(),
        buildingPoints,
        bonusPoints(player),
        player.doubloons() + player.totalGoods());
  }

  /** Highest total first; ties broken on doubloons plus goods. */
  public static List<ScoreBreakdown> finalStandings(GameState state) {
    return state.players().stream()
        .map(Scorer::score)
        .sorted(
            Comparator.comparingInt(ScoreBreakdown::total)
                .thenComparingInt(ScoreBreakdown::tiebreak)
                .reversed())
        .toList();
  }

  static int bonusPoints(PlayerState player) {
    int bonus = 0;
    if (player.hasOccupied(BuildingType.GUILD_HALL)) {
      bonus += guildHall(player);
    }
    if (player.hasOccupied(BuildingType.RESIDENCE)) {
      bonus += residence(player.filledIslandSpaces());
    }
    if (player.hasOccupied(BuildingType.FORTRESS)) {
      bonus += player.totalColonists() / 3;
    }
    if (player.hasOccupied(BuildingType.CUSTOMS_HOUSE)) {
      bonus += player.victoryPoints() / 4;
    }
    if (player.hasOccupied(BuildingType.CITY_HALL)) {
      bonus += cityHall(player);
    }
    return bonus;
  }

  /** One point per small production building, two per large — occupied or not. */
  static int guildHall(PlayerState player) {
    int bonus = 0;
    for (PlacedBuilding building : player.buildings()) {
      bonus +=
          switch (building.type().category()) {
            case PRODUCTION_SMALL -> 1;
            case PRODUCTION_LARGE -> 2;
            default -> 0;
          };
    }
    return bonus;
  }

  static int residence(int filledIslandSpaces) {
    return switch (filledIslandSpaces) {
      case 10 -> 5;
      case 11 -> 6;
      case 12 -> 7;
      default -> 4;
    };
  }

  /** One point per violet building, the City Hall itself included. */
  static int cityHall(PlayerState player) {
    return (int) player.buildings().stream().filter(b -> b.type().category().isViolet()).count();
  }
}
