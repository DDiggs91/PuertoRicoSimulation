package com.PRS.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.engine.RejectionReason;
import com.PRS.model.game.GameState;
import com.PRS.model.scoring.ScoreBreakdown;
import com.PRS.session.actors.ActorKind;
import com.PRS.session.actors.Decision;
import com.PRS.session.actors.SeatedActor;
import com.PRS.session.events.SessionEvent;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.Test;

public class GameSessionTest {

  private static GameSession newSession(String... names) {
    List<SeatedActor> seats =
        List.of(names).stream()
            .map(name -> new SeatedActor(FakeActors.firstLegal(name), ActorKind.AI))
            .toList();
    GameSession session = GameSession.create(7L, seats);
    session.start();
    return session;
  }

  private static SubmitOutcome act(GameSession session) {
    Decision decision = session.pendingDecision();
    PlayerAction action = decision.options().getFirst();
    return session.submit(decision.seat(), decision.requestId(), action);
  }

  private static List<ScoreBreakdown> playToCompletion(GameSession session) {
    try (SessionRunner runner = SessionRunner.drive(session, session.seats())) {
      return runner.completion().get(30, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void startSeatsActorsAndDealsViaGameSetup() {
    GameSession session = newSession("Ana", "Bo", "Coco");

    assertThat(session.state().playerCount()).isEqualTo(3);
    assertThat(session.state().players().stream().map(p -> p.name()).toList())
        .containsExactly("Ana", "Bo", "Coco");
    assertThat(session.status()).isEqualTo(SessionStatus.AWAITING_DECISION);
    assertThat(session.pendingDecision().seat()).isEqualTo(session.state().phase().actorSeat());
  }

  @Test
  public void pendingDecisionTracksThePhasesActorSeat() {
    GameSession session = newSession("Ana", "Bo", "Coco");

    for (int i = 0; i < 5; i++) {
      assertThat(session.pendingDecision().seat()).isEqualTo(session.state().phase().actorSeat());
      act(session);
    }
  }

  @Test
  public void submittingForTheWrongSeatIsRefused() {
    GameSession session = newSession("Ana", "Bo", "Coco");
    Decision decision = session.pendingDecision();
    var stateBefore = session.state();
    int wrongSeat = (decision.seat() + 1) % session.state().playerCount();

    SubmitOutcome outcome =
        session.submit(wrongSeat, decision.requestId(), decision.options().getFirst());

    assertThat(outcome).isInstanceOf(SubmitOutcome.Refused.class);
    assertThat(((SubmitOutcome.Refused) outcome).reason()).isEqualTo(RejectionReason.NOT_YOUR_TURN);
    assertThat(session.pendingDecision().requestId()).isEqualTo(decision.requestId());
    assertThat(session.state()).isEqualTo(stateBefore);
  }

  @Test
  public void anActionTheEngineRejectsLeavesStateUnchanged() {
    GameSession session = newSession("Ana", "Bo", "Coco");
    Decision decision = session.pendingDecision();
    var stateBefore = session.state();

    // A well-formed action, but the wrong shape for the RoleSelection phase.
    SubmitOutcome outcome =
        session.submit(
            decision.seat(), decision.requestId(), new PlayerAction.PassBuilding(decision.seat()));

    assertThat(outcome).isInstanceOf(SubmitOutcome.Refused.class);
    assertThat(((SubmitOutcome.Refused) outcome).reason()).isEqualTo(RejectionReason.WRONG_PHASE);
    assertThat(session.state()).isEqualTo(stateBefore);
    assertThat(session.pendingDecision().requestId()).isEqualTo(decision.requestId());
  }

  @Test
  public void aStaleRequestIdIsRefusedWithoutChangingState() {
    GameSession session = newSession("Ana", "Bo", "Coco");
    Decision stale = session.pendingDecision();
    act(session); // advances to a new decision with a new requestId
    var stateAfterFirstMove = session.state();

    SubmitOutcome outcome =
        session.submit(stale.seat(), stale.requestId(), stale.options().getFirst());

    assertThat(outcome).isInstanceOf(SubmitOutcome.Stale.class);
    assertThat(((SubmitOutcome.Stale) outcome).currentRequestId())
        .isEqualTo(session.pendingDecision().requestId());
    assertThat(session.state()).isEqualTo(stateAfterFirstMove);
  }

  @Test
  public void requestIdStrictlyIncreases() {
    GameSession session = newSession("Ana", "Bo", "Coco");
    long previous = session.pendingDecision().requestId();

    for (int i = 0; i < 5; i++) {
      act(session);
      long current = session.pendingDecision().requestId();
      assertThat(current).isGreaterThan(previous);
      previous = current;
    }
  }

  @Test
  public void standingsAreEmptyUntilTheGameFinishes() {
    GameSession session = newSession("Ana", "Bo", "Coco");
    assertThat(session.standings()).isEmpty();

    List<ScoreBreakdown> standings = playToCompletion(session);

    assertThat(standings).isNotEmpty();
    assertThat(session.standings()).isEqualTo(standings);
    assertThat(session.status()).isEqualTo(SessionStatus.FINISHED);
  }

  @Test
  public void aFinishedGameRefusesEverythingWithGameOver() {
    GameSession session = newSession("Ana", "Bo", "Coco");
    playToCompletion(session);

    SubmitOutcome outcome = session.submit(0, 1L, new PlayerAction.PassBuilding(0));

    assertThat(outcome).isInstanceOf(SubmitOutcome.Refused.class);
    assertThat(((SubmitOutcome.Refused) outcome).reason()).isEqualTo(RejectionReason.GAME_OVER);
    assertThat(session.pendingDecision()).isNull();
  }

  /**
   * A repeat {@code start()} used to re-announce the game and bump the request id, invalidating the
   * outstanding decision and stranding whoever was answering it.
   */
  @Test
  public void aSecondStartIsANoOpAndLeavesThePendingDecisionAnswerable() {
    GameSession session = newSession("Ana", "Bo", "Coco");
    Decision before = session.pendingDecision();
    List<SessionEvent> seen = new java.util.ArrayList<>();
    session.addListener(seen::add);

    session.start();

    assertThat(seen).isEmpty();
    assertThat(session.pendingDecision().requestId()).isEqualTo(before.requestId());
    assertThat(session.submit(before.seat(), before.requestId(), before.options().getFirst()))
        .isInstanceOf(SubmitOutcome.Applied.class);
  }

  /**
   * Listeners run inline on this thread, so a client that reacts to ACTION_APPLIED by re-reading
   * {@code state()} must not be handed the board from before the action it was just told about.
   */
  @Test
  public void stateAlreadyReflectsTheActionWhenActionAppliedIsBroadcast() {
    GameSession session = newSession("Ana", "Bo", "Coco");
    List<GameState> observed = new java.util.ArrayList<>();
    session.addListener(
        event -> {
          if (event instanceof SessionEvent.ActionApplied) {
            observed.add(session.state());
          }
        });

    Decision decision = session.pendingDecision();
    session.submit(decision.seat(), decision.requestId(), decision.options().getFirst());

    assertThat(observed).hasSize(1);
    assertThat(observed.getFirst().phase()).isEqualTo(session.state().phase());
  }
}
