package com.PRS.lobby;

import com.PRS.model.game.SetupTable;
import com.PRS.session.GameSession;
import com.PRS.session.SessionRunner;
import com.PRS.session.actors.Actor;
import com.PRS.session.actors.ActorKind;
import com.PRS.session.actors.SeatedActor;
import com.PRS.session.events.SessionListener;
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
  private volatile Snapshot snapshot = new Snapshot(List.of(), GameTableStatus.OPEN);
  private volatile GameSession session;
  private volatile SessionRunner runner;

  GameTable(GameId id) {
    this.id = id;
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
    return new GameTableSummary(id, seats, current.status());
  }

  /** {@code null} until {@link #start} has succeeded. */
  GameSession session() {
    return session;
  }

  /** No-op if the table never started. */
  void closeRunner() {
    if (runner != null) {
      runner.close();
    }
  }
}
