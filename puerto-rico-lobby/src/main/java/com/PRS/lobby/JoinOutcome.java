package com.PRS.lobby;

/** The result of {@link Lobby#join}. Rejection is a value here too, never an exception. */
public sealed interface JoinOutcome {

  record Seated(GameId id, int seatIndex) implements JoinOutcome {}

  record Rejected(LobbyRejectionReason reason) implements JoinOutcome {}
}
