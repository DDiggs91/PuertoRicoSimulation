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

  /** Cleared by the trader at the end of the phase, but only when all four slots are filled. */
  public TradingHouse clearIfFull() {
    return isFull() ? empty() : this;
  }
}
