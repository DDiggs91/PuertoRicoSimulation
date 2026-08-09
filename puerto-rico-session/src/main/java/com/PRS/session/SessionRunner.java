package com.PRS.session;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.scoring.ScoreBreakdown;
import com.PRS.session.actors.Actor;
import com.PRS.session.actors.Decision;
import com.PRS.session.actors.SeatedActor;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pumps a {@link GameSession} to completion: fetch the pending decision, ask that seat's {@link
 * Actor}, submit whatever it answers, repeat.
 *
 * <p>Every step — including the response to an already-completed future, which is what an
 * instant-answering AI produces — is re-posted to a single-threaded executor rather than chained
 * inline. Without that, an all-AI game would resolve every decision synchronously inside one
 * recursive call stack and blow it partway through a game; the executor turns the recursion into a
 * loop of independent tasks instead.
 */
public final class SessionRunner implements AutoCloseable {

  private static final int MAX_CONSECUTIVE_FAILURES = 3;

  private final GameSession session;
  private final List<SeatedActor> seats;
  private final ExecutorService executor;
  private final CompletableFuture<List<ScoreBreakdown>> completion = new CompletableFuture<>();
  private volatile boolean closed;
  private int consecutiveFailures;

  private SessionRunner(GameSession session, List<SeatedActor> seats) {
    this.session = session;
    this.seats = List.copyOf(seats);
    this.executor = Executors.newSingleThreadExecutor(SessionRunner::newDaemonThread);
  }

  /** Starts driving {@code session} immediately on a dedicated single-threaded executor. */
  public static SessionRunner drive(GameSession session, List<SeatedActor> seats) {
    SessionRunner runner = new SessionRunner(session, seats);
    runner.executor.submit(runner::step);
    return runner;
  }

  /** Completes with final standings, or exceptionally if the game had to be aborted. */
  public CompletableFuture<List<ScoreBreakdown>> completion() {
    return completion;
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    completion.completeExceptionally(new CancellationException("SessionRunner closed"));
    executor.shutdownNow();
  }

  private void step() {
    if (closed) {
      return;
    }
    if (session.status() == SessionStatus.FINISHED) {
      completion.complete(session.standings());
      close();
      return;
    }
    if (session.status() == SessionStatus.FAILED) {
      completion.completeExceptionally(new IllegalStateException("Session failed"));
      close();
      return;
    }

    Decision decision = session.pendingDecision();
    Actor actor = seats.get(decision.seat()).actor();

    CompletableFuture<PlayerAction> answer;
    try {
      answer = actor.decide(decision);
      if (answer == null) {
        answer =
            CompletableFuture.failedFuture(
                new NullPointerException(actor.name() + " returned a null decision future"));
      }
    } catch (RuntimeException e) {
      answer = CompletableFuture.failedFuture(e);
    }
    answer.whenCompleteAsync((action, error) -> onAnswer(decision, action, error), executor);
  }

  private void onAnswer(Decision decision, PlayerAction action, Throwable error) {
    if (closed) {
      return;
    }
    if (error != null || action == null) {
      handleFailure(decision);
      return;
    }

    SubmitOutcome outcome = session.submit(decision.seat(), decision.requestId(), action);
    switch (outcome) {
      case SubmitOutcome.Applied ignored -> {
        consecutiveFailures = 0;
        step();
      }
      case SubmitOutcome.Refused ignored -> handleFailure(decision);
      case SubmitOutcome.Stale ignored -> step();
    }
  }

  private void handleFailure(Decision decision) {
    consecutiveFailures++;
    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
      session.fail(
          "Seat %d's actor failed to produce a usable action after %d attempts"
              .formatted(decision.seat(), consecutiveFailures));
      step();
      return;
    }
    step();
  }

  private static Thread newDaemonThread(Runnable task) {
    Thread thread = new Thread(task, "session-runner");
    thread.setDaemon(true);
    return thread;
  }
}
