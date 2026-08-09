package com.PRS.lobby;

/**
 * Why the lobby refused a {@link Lobby#join} or {@link Lobby#start}. A separate enum from the
 * model's {@code RejectionReason} — this rejects table admission, not an in-game action.
 */
public enum LobbyRejectionReason {
  GAME_NOT_FOUND,
  TABLE_FULL,
  ALREADY_STARTED,
  TOO_FEW_SEATS
}
