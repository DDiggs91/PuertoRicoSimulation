package com.PRS.session.view;

import com.PRS.model.boards.TileSupply;
import com.PRS.model.game.GameState;
import java.util.List;

/**
 * A game state with the face-down plantation pile scrubbed. The base game's only hidden information
 * is which tile is where in the draw and discard piles, and it is hidden from every viewer equally,
 * so one view serves both players and spectators.
 */
public record GameView(GameState state, int faceDownTiles, int discardedTiles, Integer viewerSeat) {

  /** {@code viewerSeat} is null for a spectator. */
  public static GameView of(GameState full, Integer viewerSeat) {
    TileSupply tiles = full.tiles();
    TileSupply scrubbed =
        tiles.toBuilder().drawPile(List.of()).discardPile(List.of()).seed(0L).build();
    return new GameView(
        full.withTiles(scrubbed), tiles.drawPile().size(), tiles.discardPile().size(), viewerSeat);
  }
}
