package com.PRS.model.boards;

import com.PRS.model.goods.Good;
import java.util.Optional;

/**
 * What can sit on an island space: the five plantation kinds plus quarries, which produce a
 * building discount rather than a good.
 */
public enum TileType {
  CORN(10, Good.CORN),
  INDIGO(12, Good.INDIGO),
  SUGAR(11, Good.SUGAR),
  TOBACCO(9, Good.TOBACCO),
  COFFEE(8, Good.COFFEE),
  QUARRY(8, null);

  private final int tileCount;
  private final Good good;

  TileType(int tileCount, Good good) {
    this.tileCount = tileCount;
    this.good = good;
  }

  /** How many tiles of this kind the game supplies. */
  public int tileCount() {
    return tileCount;
  }

  /** The good an occupied tile of this kind yields, empty for quarries. */
  public Optional<Good> good() {
    return Optional.ofNullable(good);
  }

  public boolean isQuarry() {
    return this == QUARRY;
  }

  public static TileType of(Good good) {
    for (TileType type : values()) {
      if (type.good == good) {
        return type;
      }
    }
    throw new IllegalArgumentException("No tile produces " + good);
  }
}
