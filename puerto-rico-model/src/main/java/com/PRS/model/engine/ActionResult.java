package com.PRS.model.engine;

import com.PRS.model.game.GameState;

/** The outcome of submitting an action: either a new state, or a reason the move was illegal. */
public sealed interface ActionResult {

  record Accepted(GameState state) implements ActionResult {}

  record Rejected(RejectionReason reason, String detail) implements ActionResult {}

  static ActionResult accept(GameState state) {
    return new Accepted(state);
  }

  static ActionResult reject(RejectionReason reason, String detail) {
    return new Rejected(reason, detail);
  }

  /** The resulting state, or throws when the action was rejected. */
  default GameState state() {
    if (this instanceof Accepted accepted) {
      return accepted.state();
    }
    Rejected rejected = (Rejected) this;
    throw new IllegalStateException(
        "Action was rejected: %s (%s)".formatted(rejected.reason(), rejected.detail()));
  }
}
