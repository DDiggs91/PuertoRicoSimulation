package com.PRS.session.actors;

import com.PRS.model.actions.PlayerAction;
import com.PRS.session.view.GameView;
import java.util.List;

/**
 * What a session asks an actor: given this (redacted) view and these legal options, what do you
 * choose? {@code options} is {@code GameEngine.legalActions} handed over pre-computed, so an actor
 * never needs to call the engine itself.
 *
 * <p>{@code requestId} is a monotonic token minted per decision. An actor must echo it back when
 * submitting; a reply carrying a stale id (a double-click, a duplicate socket frame, an answer that
 * arrives after the session moved on) is refused rather than misapplied.
 */
public record Decision(int seat, GameView view, List<PlayerAction> options, long requestId) {

  public Decision {
    options = List.copyOf(options);
  }
}
