package com.PRS.web.actors;

import com.PRS.model.actions.PlayerAction;
import com.PRS.session.actors.Actor;
import com.PRS.session.actors.Decision;
import java.util.concurrent.CompletableFuture;

/**
 * Turns "wait for a move to arrive over HTTP" into an answer to the Decision contract, so a human
 * player is just another {@link Actor} from the session's point of view. {@code decide} stores the
 * pending {@link Decision} and returns an incomplete future; {@code offer} completes it once a move
 * arrives.
 *
 * <p>{@code offer} validates before completing the future, and is the only path a human move may
 * take to reach the game — never straight to {@code GameSession.submit}. Two reasons: {@code
 * SessionRunner} aborts a game after three consecutive unusable answers, so an unvalidated illegal
 * move would burn that budget on a stale click rather than refusing it outright; and submitting
 * directly would advance the game while the runner still holds this actor's unresolved future,
 * leaving it waiting forever on a decision that was already answered a different way.
 */
public final class HumanActor implements Actor {

  private final String name;
  private volatile Decision pending;
  private volatile CompletableFuture<PlayerAction> future;

  public HumanActor(String name) {
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public synchronized CompletableFuture<PlayerAction> decide(Decision decision) {
    pending = decision;
    future = new CompletableFuture<>();
    return future;
  }

  public synchronized OfferResult offer(long requestId, PlayerAction action) {
    if (pending == null || future.isDone()) {
      return new OfferResult.Rejected("No decision is currently pending for this seat.");
    }
    if (requestId != pending.requestId()) {
      return new OfferResult.Rejected("This decision has already moved on.");
    }
    if (!pending.options().contains(action)) {
      return new OfferResult.Rejected("That action is not currently legal.");
    }
    future.complete(action);
    return new OfferResult.Accepted();
  }
}
