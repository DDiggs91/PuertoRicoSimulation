package com.PRS.model.game;

import java.util.List;

/**
 * What a game needs to start: the seated players in turn order, and the seed that fixes the
 * plantation shuffle so a game is reproducible.
 */
public record GameConfig(List<String> playerNames, long seed) {

  public GameConfig {
    playerNames = List.copyOf(playerNames);
    SetupTable.requireSupportedPlayerCount(playerNames.size());
  }

  public int playerCount() {
    return playerNames.size();
  }
}
