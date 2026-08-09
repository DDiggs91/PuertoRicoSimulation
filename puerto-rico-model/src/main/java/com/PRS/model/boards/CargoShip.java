package com.PRS.model.boards;

import com.PRS.model.goods.Good;
import java.util.Optional;

/** One of the shared cargo ships. A ship carries a single kind of goods at a time. */
public record CargoShip(int capacity, Good cargo, int loaded) {

  public CargoShip {
    if (loaded < 0 || loaded > capacity) {
      throw new IllegalArgumentException("Ship of %d cannot hold %d".formatted(capacity, loaded));
    }
    if (loaded == 0) {
      cargo = null;
    }
  }

  public static CargoShip empty(int capacity) {
    return new CargoShip(capacity, null, 0);
  }

  public Optional<Good> cargoKind() {
    return Optional.ofNullable(cargo);
  }

  public boolean isEmpty() {
    return loaded == 0;
  }

  public boolean isFull() {
    return loaded == capacity;
  }

  public int freeSpace() {
    return capacity - loaded;
  }

  /** True when this ship would accept the given kind — either it is empty or already carries it. */
  public boolean accepts(Good good) {
    return !isFull() && (isEmpty() || cargo == good);
  }

  public CargoShip load(Good good, int count) {
    if (!accepts(good)) {
      throw new IllegalStateException("Ship carrying %s cannot take %s".formatted(cargo, good));
    }
    return new CargoShip(capacity, good, loaded + count);
  }

  /** The captain empties every completely full ship at the end of the phase. */
  public CargoShip unloadIfFull() {
    return isFull() ? empty(capacity) : this;
  }
}
