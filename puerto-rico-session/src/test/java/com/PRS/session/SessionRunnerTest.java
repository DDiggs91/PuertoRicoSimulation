package com.PRS.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.PRS.model.scoring.ScoreBreakdown;
import com.PRS.session.actors.ActorKind;
import com.PRS.session.actors.SeatedActor;
import com.PRS.session.events.SessionEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import org.testng.annotations.Test;

public class SessionRunnerTest {

  private static List<SeatedActor> allAi(String... names) {
    return List.of(names).stream()
        .map(name -> new SeatedActor(FakeActors.firstLegal(name), ActorKind.AI))
        .toList();
  }

  @Test
  public void anAllAiGameRunsToCompletionWithCorrectStandings()
      throws ExecutionException, InterruptedException, TimeoutException {
    List<SeatedActor> seats = allAi("Ana", "Bo", "Coco");
    GameSession session = GameSession.create(3L, seats);
    session.start();

    try (SessionRunner runner = SessionRunner.drive(session, seats)) {
      List<ScoreBreakdown> standings = runner.completion().get(30, TimeUnit.SECONDS);

      assertThat(standings).hasSize(3);
      assertThat(standings).isEqualTo(session.standings());
      assertThat(session.status()).isEqualTo(SessionStatus.FINISHED);
    }
  }

  @Test
  public void aCompleteAllAiGameDoesNotOverflowTheStack()
      throws ExecutionException, InterruptedException, TimeoutException {
    // Every decision here resolves synchronously; if SessionRunner chained futures inline
    // instead of trampolining through the executor, a full 5-player game would blow the stack.
    List<SeatedActor> seats = allAi("Ana", "Bo", "Coco", "Deb", "Eve");
    GameSession session = GameSession.create(99L, seats);
    session.start();

    try (SessionRunner runner = SessionRunner.drive(session, seats)) {
      List<ScoreBreakdown> standings = runner.completion().get(60, TimeUnit.SECONDS);
      assertThat(standings).hasSize(5);
    }
  }

  @Test
  public void aDeferredActorsTurnBlocksProgressWithoutBlockingAThread() throws Exception {
    FakeActors.DeferredActor human = FakeActors.deferred("Human");
    List<SeatedActor> seats =
        List.of(
            new SeatedActor(human, ActorKind.HUMAN),
            new SeatedActor(FakeActors.firstLegal("Bo"), ActorKind.AI),
            new SeatedActor(FakeActors.firstLegal("Coco"), ActorKind.AI));
    GameSession session = GameSession.create(5L, seats);
    session.start();
    // Seat 0 (the human) is the first governor and acts first in RoleSelection.
    assertThat(session.pendingDecision().seat()).isZero();

    try (SessionRunner runner = SessionRunner.drive(session, seats)) {
      waitUntil(human::hasPending, 5);
      assertThat(runner.completion().isDone()).isFalse();
      assertThat(session.status()).isEqualTo(SessionStatus.AWAITING_DECISION);
      assertThat(session.history()).isEmpty();

      human.answer(session.pendingDecision().options().getFirst());

      // The human's seat is asked again on later turns and this fixture never answers those, so
      // driving the whole game to completion isn't the point here — just that answering once
      // unsticks the runner and it makes progress past the decision it was blocked on.
      waitUntil(() -> !session.history().isEmpty(), 5);
      assertThat(session.history()).hasSize(1);
    }
  }

  @Test
  public void aFailingActorIsRetriedThenAbortsWithSessionFailed() throws Exception {
    List<SeatedActor> seats =
        List.of(
            new SeatedActor(FakeActors.failing("Broken"), ActorKind.AI),
            new SeatedActor(FakeActors.firstLegal("Bo"), ActorKind.AI),
            new SeatedActor(FakeActors.firstLegal("Coco"), ActorKind.AI));
    GameSession session = GameSession.create(5L, seats);
    List<SessionEvent> events = new CopyOnWriteArrayList<>();
    session.addListener(events::add);
    session.start();

    try (SessionRunner runner = SessionRunner.drive(session, seats)) {
      assertThatThrownBy(() -> runner.completion().get(30, TimeUnit.SECONDS))
          .isInstanceOf(ExecutionException.class);
    }
    assertThat(session.status()).isEqualTo(SessionStatus.FAILED);
    assertThat(events).anyMatch(SessionEvent.SessionFailed.class::isInstance);
  }

  @Test
  public void anIllegalActorIsRetriedThenAbortsWithoutCrashingTheSession() throws Exception {
    List<SeatedActor> seats =
        List.of(
            new SeatedActor(FakeActors.illegal("Broken"), ActorKind.AI),
            new SeatedActor(FakeActors.firstLegal("Bo"), ActorKind.AI),
            new SeatedActor(FakeActors.firstLegal("Coco"), ActorKind.AI));
    GameSession session = GameSession.create(5L, seats);
    session.start();

    try (SessionRunner runner = SessionRunner.drive(session, seats)) {
      // An illegal answer is a Refused outcome, not a thrown exception — the runner must not
      // propagate anything uncaught, it just exhausts its retries and fails the session cleanly.
      assertThatThrownBy(() -> runner.completion().get(30, TimeUnit.SECONDS))
          .isInstanceOf(ExecutionException.class);
    }
    assertThat(session.status()).isEqualTo(SessionStatus.FAILED);
  }

  @Test
  public void closeMidGameStopsCleanly() throws Exception {
    FakeActors.DeferredActor human = FakeActors.deferred("Human");
    List<SeatedActor> seats =
        List.of(
            new SeatedActor(human, ActorKind.HUMAN),
            new SeatedActor(FakeActors.firstLegal("Bo"), ActorKind.AI),
            new SeatedActor(FakeActors.firstLegal("Coco"), ActorKind.AI));
    GameSession session = GameSession.create(5L, seats);
    session.start();

    SessionRunner runner = SessionRunner.drive(session, seats);
    waitUntil(human::hasPending, 5);
    runner.close();

    // CompletableFuture.get() special-cases a CancellationException cause and rethrows it
    // directly rather than wrapping it in an ExecutionException.
    assertThatThrownBy(() -> runner.completion().get(5, TimeUnit.SECONDS))
        .isInstanceOf(java.util.concurrent.CancellationException.class);
    // Closing stops the runner; it does not corrupt the session it was driving.
    assertThat(session.status()).isEqualTo(SessionStatus.AWAITING_DECISION);
  }

  private static void waitUntil(BooleanSupplier condition, int timeoutSeconds)
      throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() > deadline) {
        throw new AssertionError("condition not met within " + timeoutSeconds + "s");
      }
      Thread.sleep(10);
    }
  }
}
