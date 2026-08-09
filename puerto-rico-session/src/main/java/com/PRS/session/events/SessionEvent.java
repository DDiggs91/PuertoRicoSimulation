package com.PRS.session.events;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.engine.RejectionReason;
import com.PRS.model.scoring.ScoreBreakdown;
import com.PRS.session.view.GameView;
import java.util.List;

/**
 * Something a session broadcasts. Every variant carries a {@link GameView} snapshot of the state
 * right after the event, so a listener never has to replay history to catch up — state is
 * immutable, so the snapshot is a reference copy, not a copy operation.
 */
public sealed interface SessionEvent {

  GameView view();

  record GameStarted(GameView view, List<String> seatNames) implements SessionEvent {}

  record DecisionRequested(GameView view, int seat, List<PlayerAction> options, long requestId)
      implements SessionEvent {
    public DecisionRequested {
      options = List.copyOf(options);
    }
  }

  record ActionApplied(GameView view, int seat, PlayerAction action) implements SessionEvent {}

  record ActionRejected(
      GameView view, int seat, PlayerAction action, RejectionReason reason, String detail)
      implements SessionEvent {}

  record GameEnded(GameView view, List<ScoreBreakdown> standings) implements SessionEvent {
    public GameEnded {
      standings = List.copyOf(standings);
    }
  }

  /** The session gave up — e.g. an actor kept failing to produce a usable action. */
  record SessionFailed(GameView view, String detail) implements SessionEvent {}
}
