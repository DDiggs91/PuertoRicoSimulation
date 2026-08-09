package com.PRS.lobby;

/** The result of {@link Lobby#start}. Rejection is a value here too, never an exception. */
public sealed interface StartOutcome {

  record Started(GameId id) implements StartOutcome {}

  record Rejected(LobbyRejectionReason reason) implements StartOutcome {}
}
