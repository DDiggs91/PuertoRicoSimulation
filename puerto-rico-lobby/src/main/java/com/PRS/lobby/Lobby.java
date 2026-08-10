package com.PRS.lobby;

import com.PRS.session.GameSession;
import com.PRS.session.actors.Actor;
import com.PRS.session.actors.ActorKind;
import com.PRS.session.events.SessionListener;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks, in memory, the set of joinable and in-progress games. This is the entry point before a
 * {@code GameSession} exists — nothing in {@code puerto-rico-session} gets created until a table
 * here is seated and started.
 *
 * <p>The lobby seats whatever {@link Actor} it's handed and never builds one itself — an AI plugin
 * from {@code puerto-rico-ai} or a human adapter from {@code puerto-rico-web} are both just actors
 * to this class. {@link #start} both creates the {@code GameSession} and starts driving it via a
 * {@code SessionRunner}, which this class then owns; {@link #close} shuts every such runner down.
 */
public final class Lobby implements AutoCloseable {

  private final ConcurrentHashMap<GameId, GameTable> tables = new ConcurrentHashMap<>();

  public GameId createGame() {
    GameId id = GameId.newId();
    tables.put(id, new GameTable(id));
    return id;
  }

  public JoinOutcome join(GameId id, Actor actor, ActorKind kind) {
    GameTable table = tables.get(id);
    if (table == null) {
      return new JoinOutcome.Rejected(LobbyRejectionReason.GAME_NOT_FOUND);
    }
    return table.join(actor, kind);
  }

  public StartOutcome start(GameId id, long seed) {
    return start(id, seed, List.of());
  }

  /**
   * Like {@link #start(GameId, long)}, but attaches {@code listeners} to the {@code GameSession}
   * before it starts — the only window in which that's possible. An all-AI game can run to
   * completion before this call even returns, so a listener attached afterwards may see nothing.
   */
  public StartOutcome start(GameId id, long seed, List<SessionListener> listeners) {
    GameTable table = tables.get(id);
    if (table == null) {
      return new StartOutcome.Rejected(LobbyRejectionReason.GAME_NOT_FOUND);
    }
    return table.start(seed, listeners);
  }

  public List<GameTableSummary> listGames() {
    return tables.values().stream().map(GameTable::summary).toList();
  }

  public Optional<GameTableSummary> find(GameId id) {
    return Optional.ofNullable(tables.get(id)).map(GameTable::summary);
  }

  /** Present only once the table has started. */
  public Optional<GameSession> sessionFor(GameId id) {
    return Optional.ofNullable(tables.get(id)).map(GameTable::session);
  }

  @Override
  public void close() {
    tables.values().forEach(GameTable::closeRunner);
  }
}
