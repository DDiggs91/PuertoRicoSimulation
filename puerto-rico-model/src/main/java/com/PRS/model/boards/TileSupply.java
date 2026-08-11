package com.PRS.model.boards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * The plantation tiles: a face-down draw pile, the face-up row players choose from, a discard pile,
 * and the separate face-up stack of quarries.
 *
 * <p>The seed travels with the supply so reshuffling the discard stays deterministic without the
 * state holding a mutable random source.
 */
@lombok.Builder(toBuilder = true)
public record TileSupply(
    List<TileType> drawPile,
    List<TileType> discardPile,
    List<TileType> faceUp,
    int quarriesRemaining,
    long seed) {

  /** A drawn tile alongside the supply it came out of. */
  public record Draw(Optional<TileType> tile, TileSupply supply) {}

  public TileSupply {
    drawPile = List.copyOf(drawPile);
    discardPile = List.copyOf(discardPile);
    faceUp = List.copyOf(faceUp);
  }

  /**
   * All 50 plantation tiles shuffled, with all 8 quarries available and no face-up row dealt yet.
   * How many tiles go face up scales with the player count, a setup rule that lives in {@code
   * SetupTable} — which this package deliberately does not depend on — so the caller deals the row
   * with {@link #refillFaceUp}.
   */
  public static TileSupply create(long seed) {
    List<TileType> tiles = new ArrayList<>();
    for (TileType type : TileType.values()) {
      if (type.isQuarry()) {
        continue;
      }
      for (int i = 0; i < type.tileCount(); i++) {
        tiles.add(type);
      }
    }
    Random random = new Random(seed);
    Collections.shuffle(tiles, random);
    return new TileSupply(
        tiles, List.of(), List.of(), TileType.QUARRY.tileCount(), random.nextLong());
  }

  public TileSupply takeFaceUp(int index) {
    List<TileType> next = new ArrayList<>(faceUp);
    next.remove(index);
    return new TileSupply(drawPile, discardPile, next, quarriesRemaining, seed);
  }

  public boolean hasQuarry() {
    return quarriesRemaining > 0;
  }

  public TileSupply takeQuarry() {
    if (!hasQuarry()) {
      throw new IllegalStateException("No quarry tiles remain");
    }
    return new TileSupply(drawPile, discardPile, faceUp, quarriesRemaining - 1, seed);
  }

  /** Draws the top face-down tile, reshuffling the discard pile first if the draw pile is out. */
  public Draw drawFaceDown() {
    TileSupply source = drawPile.isEmpty() ? reshuffle() : this;
    if (source.drawPile.isEmpty()) {
      return new Draw(Optional.empty(), source);
    }
    List<TileType> remaining = new ArrayList<>(source.drawPile);
    TileType drawn = remaining.removeLast();
    return new Draw(
        Optional.of(drawn),
        new TileSupply(
            remaining, source.discardPile, source.faceUp, source.quarriesRemaining, source.seed));
  }

  /**
   * The settler's last duty: discard whatever nobody took and deal a fresh row. A row can come up
   * short when the tiles genuinely run out.
   */
  public TileSupply refillFaceUp(int count) {
    List<TileType> discards = new ArrayList<>(discardPile);
    discards.addAll(faceUp);
    TileSupply cleared = new TileSupply(drawPile, discards, List.of(), quarriesRemaining, seed);

    List<TileType> row = new ArrayList<>();
    TileSupply current = cleared;
    for (int i = 0; i < count; i++) {
      Draw draw = current.drawFaceDown();
      current = draw.supply();
      if (draw.tile().isEmpty()) {
        break;
      }
      row.add(draw.tile().get());
    }
    return new TileSupply(
        current.drawPile, current.discardPile, row, current.quarriesRemaining, current.seed);
  }

  private TileSupply reshuffle() {
    if (discardPile.isEmpty()) {
      return this;
    }
    List<TileType> reshuffled = new ArrayList<>(discardPile);
    Random random = new Random(seed);
    Collections.shuffle(reshuffled, random);
    return new TileSupply(reshuffled, List.of(), faceUp, quarriesRemaining, random.nextLong());
  }
}
