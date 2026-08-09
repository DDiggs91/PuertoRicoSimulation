package com.PRS.model.buildings;

/** A building standing in a player's city, together with the colonists staffing it. */
public record PlacedBuilding(BuildingType type, int colonists) {

  public PlacedBuilding {
    if (colonists < 0 || colonists > type.colonistCapacity()) {
      throw new IllegalArgumentException(
          "%s holds 0..%d colonists, got %d".formatted(type, type.colonistCapacity(), colonists));
    }
  }

  public static PlacedBuilding unstaffed(BuildingType type) {
    return new PlacedBuilding(type, 0);
  }

  /** Only occupied buildings have any effect; victory points are the exception. */
  public boolean isOccupied() {
    return colonists > 0;
  }

  public int emptyCircles() {
    return type.colonistCapacity() - colonists;
  }

  public PlacedBuilding withColonists(int count) {
    return new PlacedBuilding(type, count);
  }
}
