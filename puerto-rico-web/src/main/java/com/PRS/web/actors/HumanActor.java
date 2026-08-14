package com.PRS.web.actors;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.engine.ActionResult;
import com.PRS.model.engine.GameEngine;
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
 *
 * <p>Every action but one is validated by list membership: {@code pending.options()} is the
 * engine's own enumeration, and a client only ever hands back an object it was given, so equality
 * is exact. {@link PlayerAction.SetColonistPlacement} is the exception — a client stages its own
 * arrangement locally so a colonist can be placed and taken back with no round trip, and the result
 * essentially never equals the one arrangement the engine happened to enumerate. It is instead
 * checked the way the engine itself would check it: {@link GameEngine#apply} against the pending
 * decision's own board, which is exactly what {@code GameSession.submit} would do next. Asking
 * first rather than submitting and finding out is what keeps a bad arrangement from counting as one
 * of the runner's three consecutive unusable answers.
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
    if (pending.options().contains(action)) {
      future.complete(action);
      return new OfferResult.Accepted();
    }
    if (isArrangementInThisPhase(action)) {
      // Checked the way the engine itself would check it, against the pending decision's own
      // board — the authoritative answer, and the same call `GameSession.submit` would make next.
      // Propagating its own rejection detail (rather than a generic message) is what keeps
      // "colonists left in San Juan" as legible here as it always was for the other phases.
      return switch (GameEngine.apply(pending.view().state(), action)) {
        case ActionResult.Accepted ignored -> {
          future.complete(action);
          yield new OfferResult.Accepted();
        }
        case ActionResult.Rejected rejected -> new OfferResult.Rejected(rejected.detail());
      };
    }
    return new OfferResult.Rejected("That action is not currently legal.");
  }

  /**
   * Whether {@code action} is the one action family the engine does not enumerate exhaustively —
   * {@link PlayerAction.SetColonistPlacement}, offered as a single greedy-fill option because a
   * colonist arrangement is a configuration, not a choice from a list — and this decision is
   * actually in that phase, so the check below has something of that shape to compare against.
   */
  private boolean isArrangementInThisPhase(PlayerAction action) {
    return action instanceof PlayerAction.SetColonistPlacement
        && pending.options().stream().anyMatch(o -> o instanceof PlayerAction.SetColonistPlacement);
  }
}
