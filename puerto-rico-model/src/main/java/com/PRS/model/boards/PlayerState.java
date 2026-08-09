package com.PRS.model.boards;

import com.PRS.model.buildings.BuildingType;
import com.PRS.model.buildings.PlacedBuilding;
import com.PRS.model.goods.Good;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * One player's board: island tiles, city buildings, colonists, doubloons, goods and VP chips.
 *
 * <p>Update it with {@code player.toBuilder().doubloons(n).build()}.
 */
@Builder(toBuilder = true)
public record PlayerState(
    int seat,
    String name,
    int doubloons,
    int victoryPoints,
    List<IslandTile> island,
    List<PlacedBuilding> buildings,
    int colonistsInSanJuan,
    Map<Good, Integer> goods) {

  public static final int ISLAND_SPACES = 12;
  public static final int CITY_SPACES = 12;

  public PlayerState {
    island = List.copyOf(island);
    buildings = List.copyOf(buildings);
    // Built key-first rather than via new EnumMap<>(map), which rejects an empty non-EnumMap.
    EnumMap<Good, Integer> copy = new EnumMap<>(Good.class);
    copy.putAll(goods);
    goods = Collections.unmodifiableMap(copy);
  }

  public static PlayerState newPlayer(int seat, String name, int doubloons, TileType plantation) {
    return PlayerState.builder()
        .seat(seat)
        .name(name)
        .doubloons(doubloons)
        .victoryPoints(0)
        .island(List.of(IslandTile.unstaffed(plantation)))
        .buildings(List.of())
        .colonistsInSanJuan(0)
        .goods(new EnumMap<>(Good.class))
        .build();
  }

  // --- island ---

  public int freeIslandSpaces() {
    return ISLAND_SPACES - island.size();
  }

  public int filledIslandSpaces() {
    return island.size();
  }

  public int occupiedQuarries() {
    return (int) island.stream().filter(t -> t.type().isQuarry() && t.occupied()).count();
  }

  public int occupiedPlantations(Good good) {
    return (int)
        island.stream().filter(t -> t.occupied() && t.type().good().orElse(null) == good).count();
  }

  public PlayerState plusTile(IslandTile tile) {
    List<IslandTile> next = new ArrayList<>(island);
    next.add(tile);
    return toBuilder().island(next).build();
  }

  // --- city ---

  public int citySpacesUsed() {
    return buildings.stream().mapToInt(b -> b.type().citySpaces()).sum();
  }

  public int freeCitySpaces() {
    return CITY_SPACES - citySpacesUsed();
  }

  public boolean owns(BuildingType type) {
    return buildings.stream().anyMatch(b -> b.type() == type);
  }

  public boolean hasOccupied(BuildingType type) {
    return buildings.stream().anyMatch(b -> b.type() == type && b.isOccupied());
  }

  public PlayerState plusBuilding(PlacedBuilding building) {
    List<PlacedBuilding> next = new ArrayList<>(buildings);
    next.add(building);
    return toBuilder().buildings(next).build();
  }

  // --- colonists ---

  /** Empty circles on buildings only — plantation and quarry circles never refill the ship. */
  public int emptyBuildingCircles() {
    return buildings.stream().mapToInt(PlacedBuilding::emptyCircles).sum();
  }

  public int emptyCircles() {
    return emptyBuildingCircles() + (int) island.stream().filter(t -> !t.occupied()).count();
  }

  /** Every colonist the player controls, San Juan included — what the Fortress scores on. */
  public int totalColonists() {
    return colonistsInSanJuan
        + (int) island.stream().filter(IslandTile::occupied).count()
        + buildings.stream().mapToInt(PlacedBuilding::colonists).sum();
  }

  // --- production ---

  /**
   * How many barrels of a good this player would produce, ignoring supply limits: occupied
   * plantations capped by staffed production-building circles. Corn needs no building.
   */
  public int productionCapacity(Good good) {
    int plantations = occupiedPlantations(good);
    if (good == Good.CORN) {
      return plantations;
    }
    int buildingCapacity =
        buildings.stream()
            .filter(b -> b.type().producedGood().orElse(null) == good)
            .mapToInt(PlacedBuilding::colonists)
            .sum();
    return Math.min(plantations, buildingCapacity);
  }

  // --- goods, money, points ---

  public int goodsCount(Good good) {
    return goods.getOrDefault(good, 0);
  }

  public int totalGoods() {
    return goods.values().stream().mapToInt(Integer::intValue).sum();
  }

  public PlayerState plusGoods(Good good, int count) {
    EnumMap<Good, Integer> next = new EnumMap<>(Good.class);
    next.putAll(goods);
    next.merge(good, count, Integer::sum);
    if (next.getOrDefault(good, 0) <= 0) {
      next.remove(good);
    }
    return toBuilder().goods(next).build();
  }

  public PlayerState plusDoubloons(int amount) {
    return toBuilder().doubloons(doubloons + amount).build();
  }

  public PlayerState plusVictoryPoints(int amount) {
    return toBuilder().victoryPoints(victoryPoints + amount).build();
  }
}
