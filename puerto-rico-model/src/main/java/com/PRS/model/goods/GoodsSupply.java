package com.PRS.model.goods;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** The shared pool of goods barrels players draw from when producing. */
public record GoodsSupply(Map<Good, Integer> barrels) {

  public GoodsSupply {
    // Built key-first rather than via new EnumMap<>(map), which rejects an empty non-EnumMap.
    EnumMap<Good, Integer> copy = new EnumMap<>(Good.class);
    copy.putAll(barrels);
    barrels = Collections.unmodifiableMap(copy);
  }

  /** A supply holding every barrel in the game. */
  public static GoodsSupply full() {
    EnumMap<Good, Integer> all = new EnumMap<>(Good.class);
    for (Good good : Good.values()) {
      all.put(good, good.barrelSupply());
    }
    return new GoodsSupply(all);
  }

  public int available(Good good) {
    return barrels.getOrDefault(good, 0);
  }

  /**
   * Takes up to {@code count} barrels, returning fewer than asked when the supply is short — a
   * player producing more than the supply holds simply goes without the remainder.
   */
  public GoodsSupply take(Good good, int count) {
    return adjust(good, -Math.min(count, available(good)));
  }

  public GoodsSupply put(Good good, int count) {
    return adjust(good, count);
  }

  private GoodsSupply adjust(Good good, int delta) {
    EnumMap<Good, Integer> next = new EnumMap<>(Good.class);
    next.putAll(barrels);
    next.merge(good, delta, Integer::sum);
    return new GoodsSupply(next);
  }
}
