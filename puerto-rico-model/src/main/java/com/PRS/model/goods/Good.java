package com.PRS.model.goods;

/**
 * The five kinds of goods, with their trading-house price and the size of their barrel supply.
 *
 * <p>Declared in ascending price order, which is also the order they appear on the trading house.
 */
public enum Good {
  CORN(0, 10),
  INDIGO(1, 11),
  SUGAR(2, 11),
  TOBACCO(3, 9),
  COFFEE(4, 9);

  private final int price;
  private final int barrelSupply;

  Good(int price, int barrelSupply) {
    this.price = price;
    this.barrelSupply = barrelSupply;
  }

  /** Doubloons paid by the trading house for one barrel, before any market bonuses. */
  public int price() {
    return price;
  }

  /** Number of barrels of this good in the game. */
  public int barrelSupply() {
    return barrelSupply;
  }
}
