package com.PRS.model.goods;

import java.util.ArrayList;
import java.util.List;

/**
 * The trading house: four slots that normally accept only kinds not already present. The Office
 * lifts the duplicate restriction, but never the capacity one.
 */
public record TradingHouse(List<Good> goods) {

  public static final int CAPACITY = 4;

  public TradingHouse {
    goods = List.copyOf(goods);
  }

  public static TradingHouse empty() {
    return new TradingHouse(List.of());
  }

  public boolean isFull() {
    return goods.size() >= CAPACITY;
  }

  public boolean contains(Good good) {
    return goods.contains(good);
  }

  public TradingHouse sell(Good good) {
    List<Good> next = new ArrayList<>(goods);
    next.add(good);
    return new TradingHouse(next);
  }

  /**
   * The house after a clear, plus the barrels that came off it. Sold barrels belong to the game,
   * not to the buyer, so they go back to the {@link GoodsSupply} — returning them here rather than
   * emptying in place makes the caller account for them explicitly.
   */
  public record Clearing(TradingHouse house, List<Good> returned) {}

  /** Cleared by the trader at the end of the phase, but only when all four slots are filled. */
  public Clearing clearIfFull() {
    return isFull() ? new Clearing(empty(), goods) : new Clearing(this, List.of());
  }
}
