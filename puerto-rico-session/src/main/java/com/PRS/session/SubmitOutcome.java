package com.PRS.session;

import com.PRS.model.engine.RejectionReason;
import com.PRS.session.view.GameView;

/** The result of {@link GameSession#submit}. */
public sealed interface SubmitOutcome {

  record Applied(GameView view) implements SubmitOutcome {}

  /**
   * The model engine refused the action — reuses its {@link RejectionReason} rather than a parallel
   * enum.
   */
  record Refused(RejectionReason reason, String detail) implements SubmitOutcome {}

  /** The submitted {@code requestId} no longer matches the pending decision. */
  record Stale(long currentRequestId) implements SubmitOutcome {}
}
