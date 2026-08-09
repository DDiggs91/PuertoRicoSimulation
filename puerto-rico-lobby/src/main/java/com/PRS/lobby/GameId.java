package com.PRS.lobby;

import java.util.UUID;

/** Opaque identifier for a table tracked by the {@link Lobby}. */
public record GameId(UUID value) {

  public static GameId newId() {
    return new GameId(UUID.randomUUID());
  }
}
