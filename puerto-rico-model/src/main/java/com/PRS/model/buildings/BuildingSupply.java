package com.PRS.model.buildings;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** The building display on the game board, tracking how many copies of each type are left. */
public record BuildingSupply(Map<BuildingType, Integer> remaining) {

  public BuildingSupply {
    // Built key-first rather than via new EnumMap<>(map), which rejects an empty non-EnumMap.
    EnumMap<BuildingType, Integer> copy = new EnumMap<>(BuildingType.class);
    copy.putAll(remaining);
    remaining = Collections.unmodifiableMap(copy);
  }

  /** Every building in the game, unclaimed. */
  public static BuildingSupply full() {
    EnumMap<BuildingType, Integer> all = new EnumMap<>(BuildingType.class);
    for (BuildingType type : BuildingType.values()) {
      all.put(type, type.copies());
    }
    return new BuildingSupply(all);
  }

  public int remaining(BuildingType type) {
    return remaining.getOrDefault(type, 0);
  }

  public boolean isAvailable(BuildingType type) {
    return remaining(type) > 0;
  }

  public BuildingSupply take(BuildingType type) {
    if (!isAvailable(type)) {
      throw new IllegalStateException("No copies of " + type + " remain");
    }
    EnumMap<BuildingType, Integer> next = new EnumMap<>(BuildingType.class);
    next.putAll(remaining);
    next.merge(type, -1, Integer::sum);
    return new BuildingSupply(next);
  }
}
