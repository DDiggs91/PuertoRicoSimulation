package com.PRS.model.boards;

/** A plantation or quarry on a player's island, and whether a colonist is working it. */
public record IslandTile(TileType type, boolean occupied) {

  public static IslandTile unstaffed(TileType type) {
    return new IslandTile(type, false);
  }

  public IslandTile withOccupied(boolean nowOccupied) {
    return new IslandTile(type, nowOccupied);
  }
}
