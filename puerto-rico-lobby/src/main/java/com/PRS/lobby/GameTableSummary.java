package com.PRS.lobby;

import java.util.List;

/** A point-in-time snapshot of one table, for lobby listings. */
public record GameTableSummary(GameId id, List<SeatSummary> seats, GameTableStatus status) {

  public GameTableSummary {
    seats = List.copyOf(seats);
  }
}
