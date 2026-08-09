package com.PRS.session;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.engine.ActionResult;
import com.PRS.model.engine.GameEngine;
import com.PRS.model.engine.RejectionReason;
import com.PRS.model.game.GameConfig;
import com.PRS.model.game.GameSetup;
import com.PRS.model.game.GameState;
import com.PRS.model.scoring.ScoreBreakdown;
import com.PRS.model.scoring.Scorer;
import com.PRS.session.actors.Decision;
import com.PRS.session.actors.SeatedActor;
import com.PRS.session.events.SessionEvent;
import com.PRS.session.events.SessionListener;
import com.PRS.session.view.GameView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One running game: seats an {@link SeatedActor} per seat, deals via {@link GameSetup}, and drives
 * the game forward through {@link GameEngine} as actions are submitted.
 *
 * <p>This is a synchronous state machine, not a thread — it holds no thread of its own and blocks
 * on nothing. Something else (typically {@link SessionRunner}) is responsible for asking actors for
 * decisions and calling {@link #submit}. That split is what lets an AI-vs-AI game and a human-vs-AI
 * game share every line of this class.
 *
 * <p>{@link #state()}, {@link #viewFor}, {@link #status()}, {@link #pendingDecision()} and {@link
 * #standings()} are lock-free reads of a single immutable snapshot. {@link #submit} is synchronized
 * so state transitions stay serialized even if callers bypass {@link SessionRunner}.
 */
public final class GameSession {

  private static final Logger log = LoggerFactory.getLogger(GameSession.class);

  private record Snapshot(
      GameState state,
      long requestId,
      List<PlayerAction> history,
      SessionStatus status,
      List<ScoreBreakdown> standings) {}

  private final List<SeatedActor> seats;
  private final CopyOnWriteArrayList<SessionListener> listeners = new CopyOnWriteArrayList<>();
  private final AtomicLong requestIdGen = new AtomicLong();
  private volatile Snapshot snapshot;

  private GameSession(List<SeatedActor> seats, GameState state) {
    this.seats = List.copyOf(seats);
    this.snapshot = new Snapshot(state, 0L, List.of(), SessionStatus.AWAITING_DECISION, List.of());
  }

  /**
   * Deals a game for the given seats. No events fire yet — register listeners, then call {@link
   * #start()} to announce {@link SessionEvent.GameStarted} and the first decision.
   */
  public static GameSession create(long seed, List<SeatedActor> seats) {
    List<String> names = seats.stream().map(seated -> seated.actor().name()).toList();
    GameState state = GameSetup.create(new GameConfig(names, seed));
    return new GameSession(seats, state);
  }

  /** Announces {@link SessionEvent.GameStarted} and requests the first decision. Call once. */
  public void start() {
    Snapshot current = snapshot;
    List<String> names = seats.stream().map(seated -> seated.actor().name()).toList();
    emit(new SessionEvent.GameStarted(broadcastView(current.state()), names));
    requestNextDecision(current.state(), current.history());
  }

  public GameState state() {
    return snapshot.state();
  }

  public GameView viewFor(Integer seat) {
    return GameView.of(snapshot.state(), seat);
  }

  public SessionStatus status() {
    return snapshot.status();
  }

  public List<SeatedActor> seats() {
    return seats;
  }

  /** Null once the game has finished or failed. */
  public Decision pendingDecision() {
    Snapshot current = snapshot;
    if (current.status() != SessionStatus.AWAITING_DECISION) {
      return null;
    }
    int seat = current.state().phase().actorSeat();
    return new Decision(
        seat,
        GameView.of(current.state(), seat),
        GameEngine.legalActions(current.state()),
        current.requestId());
  }

  public List<ScoreBreakdown> standings() {
    return snapshot.standings();
  }

  public List<PlayerAction> history() {
    return snapshot.history();
  }

  public void addListener(SessionListener listener) {
    listeners.add(listener);
  }

  /**
   * Gives up on the game — e.g. a {@link SessionRunner} whose actor kept failing to produce a
   * usable action. A no-op once the session is already finished or failed.
   */
  public synchronized void fail(String detail) {
    Snapshot current = snapshot;
    if (current.status() != SessionStatus.AWAITING_DECISION) {
      return;
    }
    snapshot =
        new Snapshot(
            current.state(),
            current.requestId(),
            current.history(),
            SessionStatus.FAILED,
            List.of());
    emit(new SessionEvent.SessionFailed(broadcastView(current.state()), detail));
  }

  /**
   * Submits {@code action} on behalf of {@code seat}, guarded by {@code requestId} echoing the id
   * on the decision it answers. Refused for the wrong seat, a stale request id, an action the model
   * engine itself rejects, or a session that isn't awaiting a decision.
   */
  public synchronized SubmitOutcome submit(int seat, long requestId, PlayerAction action) {
    Snapshot current = snapshot;
    if (current.status() != SessionStatus.AWAITING_DECISION) {
      return new SubmitOutcome.Refused(RejectionReason.GAME_OVER, "The game has finished");
    }
    if (requestId != current.requestId()) {
      return new SubmitOutcome.Stale(current.requestId());
    }
    if (seat != action.seat()) {
      return new SubmitOutcome.Refused(
          RejectionReason.NOT_YOUR_TURN,
          "Action submitted for seat %d as seat %d".formatted(action.seat(), seat));
    }

    ActionResult result = GameEngine.apply(current.state(), action);
    if (result instanceof ActionResult.Rejected rejected) {
      emit(
          new SessionEvent.ActionRejected(
              broadcastView(current.state()), seat, action, rejected.reason(), rejected.detail()));
      return new SubmitOutcome.Refused(rejected.reason(), rejected.detail());
    }

    GameState next = result.state();
    List<PlayerAction> history = new ArrayList<>(current.history());
    history.add(action);
    history = List.copyOf(history);

    emit(new SessionEvent.ActionApplied(broadcastView(next), seat, action));

    if (next.isOver()) {
      List<ScoreBreakdown> standings = Scorer.finalStandings(next);
      snapshot =
          new Snapshot(next, current.requestId(), history, SessionStatus.FINISHED, standings);
      emit(new SessionEvent.GameEnded(broadcastView(next), standings));
    } else {
      requestNextDecision(next, history);
    }
    return new SubmitOutcome.Applied(broadcastView(snapshot.state()));
  }

  private void requestNextDecision(GameState state, List<PlayerAction> history) {
    long requestId = requestIdGen.incrementAndGet();
    snapshot = new Snapshot(state, requestId, history, SessionStatus.AWAITING_DECISION, List.of());
    int seat = state.phase().actorSeat();
    List<PlayerAction> options = GameEngine.legalActions(state);
    emit(new SessionEvent.DecisionRequested(broadcastView(state), seat, options, requestId));
  }

  private GameView broadcastView(GameState state) {
    return GameView.of(state, null);
  }

  private void emit(SessionEvent event) {
    for (SessionListener listener : listeners) {
      try {
        listener.onEvent(event);
      } catch (RuntimeException e) {
        log.warn("Session listener threw handling {}", event.getClass().getSimpleName(), e);
      }
    }
  }
}
