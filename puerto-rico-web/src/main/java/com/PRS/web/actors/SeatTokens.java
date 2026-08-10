package com.PRS.web.actors;

import com.PRS.lobby.GameId;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mints and validates the per-seat tokens that make holding a human seat's token the thing that
 * authorizes submitting moves for it. The smallest thing that answers the player/spectator
 * permission boundary: everyone can read every endpoint, only a token holder can post a move for
 * that seat.
 */
public final class SeatTokens {

  private record SeatRef(GameId gameId, int seat) {}

  private final ConcurrentHashMap<String, SeatRef> tokens = new ConcurrentHashMap<>();

  public String mint(GameId gameId, int seat) {
    String token = UUID.randomUUID().toString();
    tokens.put(token, new SeatRef(gameId, seat));
    return token;
  }

  public boolean isValid(GameId gameId, int seat, String token) {
    if (token == null) {
      return false;
    }
    SeatRef ref = tokens.get(token);
    return ref != null && ref.equals(new SeatRef(gameId, seat));
  }
}
