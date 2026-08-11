package com.PRS.lobby;

import com.PRS.model.game.SetupTable;
import com.PRS.session.GameSession;
import com.PRS.session.SessionRunner;
import com.PRS.session.actors.Actor;
import com.PRS.session.actors.ActorKind;
import com.PRS.session.actors.SeatedActor;
import com.PRS.session.events.SessionListener;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One table: an implementation detail of {@link Lobby}, the way {@code GameSession}'s private
 * snapshot record is an implementation detail of that class. Accepts seats while {@link
 * GameTableStatus#OPEN}; once {@link #start} succeeds it owns a live {@code GameSession} and the
 * {@code SessionRunner} driving it.
 */
final class GameTable {

  private record Snapshot(List<SeatedActor> seats, GameTableStatus status) {}

  private final GameId id;
  private final Instant createdAt;
  private volatile Snapshot snapshot = new Snapshot(List.of(), GameTableStatus.OPEN);
  private volatile GameSession session;
  private volatile SessionRunner runner;
  private volatile Instant terminalSince;

  GameTable(GameId id, Instant createdAt) {
    this.id = id;
    this.createdAt = createdAt;
  }

  synchronized JoinOutcome join(Actor actor, ActorKind kind) {
    Snapshot current = snapshot;
    if (current.status() != GameTableStatus.OPEN) {
      return new JoinOutcome.Rejected(LobbyRejectionReason.ALREADY_STARTED);
    }
    if (current.seats().size() >= SetupTable.MAX_PLAYERS) {
      return new JoinOutcome.Rejected(LobbyRejectionReason.TABLE_FULL);
    }

    List<SeatedActor> seats = new ArrayList<>(current.seats());
    seats.add(new SeatedActor(actor, kind));
    int seatIndex = seats.size() - 1;
    snapshot = new Snapshot(List.copyOf(seats), GameTableStatus.OPEN);
    return new JoinOutcome.Seated(id, seatIndex);
  }

  synchronized StartOutcome start(long seed, List<SessionListener> listeners) {
    Snapshot current = snapshot;
    if (current.status() != GameTableStatus.OPEN) {
      return new StartOutcome.Rejected(LobbyRejectionReason.ALREADY_STARTED);
    }
    if (current.seats().size() < SetupTable.MIN_PLAYERS) {
      return new StartOutcome.Rejected(LobbyRejectionReason.TOO_FEW_SEATS);
    }

    session = GameSession.create(seed, current.seats());
    for (SessionListener listener : listeners) {
      session.addListener(listener);
    }
    session.start();
    runner = SessionRunner.drive(session, current.seats());
    snapshot = new Snapshot(current.seats(), GameTableStatus.STARTED);
    return new StartOutcome.Started(id);
  }

  GameTableSummary summary() {
    Snapshot current = snapshot;
    List<SeatSummary> seats =
        current.seats().stream()
            .map(seated -> new SeatSummary(seated.actor().name(), seated.kind()))
            .toList();
    return new GameTableSummary(id, seats, status());
  }

  /**
   * The stored status up to {@code STARTED}, then whatever the live session says. Nothing writes
   * the snapshot again after {@link #start}, so asking the session is the only way a finished game
   * stops reporting itself as in progress.
   */
  GameTableStatus status() {
    Snapshot current = snapshot;
    if (current.status() != GameTableStatus.STARTED || session == null) {
      return current.status();
    }
    return switch (session.status()) {
      case AWAITING_DECISION -> GameTableStatus.STARTED;
      case FINISHED -> GameTableStatus.FINISHED;
      case FAILED -> GameTableStatus.FAILED;
    };
  }

  /** {@code null} until {@link #start} has succeeded. */
  GameSession session() {
    return session;
  }

  /**
   * Whether {@link Lobby}'s sweep should drop this table. Two things qualify: a game that has been
   * over for longer than {@code retention} — the delay is what lets spectators still read the final
   * board — and a table created but never seated in that same window.
   *
   * <p>Stamps the moment the game was first observed to be over, so this is a sweep step rather
   * than a query; call it only from the sweep.
   */
  boolean isEvictable(Instant now, Duration retention) {
    GameTableStatus status = status();
    if (status.isOver()) {
      if (terminalSince == null) {
        terminalSince = now;
        return false;
      }
      return !now.isBefore(terminalSince.plus(retention));
    }
    terminalSince = null;
    return status == GameTableStatus.OPEN
        && snapshot.seats().isEmpty()
        && !now.isBefore(createdAt.plus(retention));
  }

  /** No-op if the table never started. */
  void closeRunner() {
    if (runner != null) {
      runner.close();
    }
  }
}
