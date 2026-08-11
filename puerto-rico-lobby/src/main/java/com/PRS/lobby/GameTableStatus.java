package com.PRS.lobby;

/**
 * Where a table is in its life: accepting seats, running a {@code GameSession}, or done with one.
 * {@code OPEN} and {@code STARTED} are stored on the table; the two terminal values are derived
 * from the live session, so a finished game stops advertising itself as in progress.
 */
public enum GameTableStatus {
  OPEN,
  STARTED,
  /** The game ran to a scored ending. */
  FINISHED,
  /** The session gave up — e.g. an actor kept failing to produce a usable action. */
  FAILED;

  public boolean isOver() {
    return this == FINISHED || this == FAILED;
  }
}
