package com.PRS.web.events;

import com.PRS.lobby.GameId;
import com.PRS.session.events.SessionEvent;
import com.PRS.session.events.SessionListener;
import com.PRS.web.wire.SessionEventMapper;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Bridges {@link SessionListener} to {@link SseEmitter}: one fan-out list per game. {@link
 * #listenerFor} is what {@code Lobby.start}'s listener overload registers before a game begins;
 * {@link #subscribe} is what a client connecting to the SSE endpoint calls to join that fan-out.
 *
 * <p>{@code listenerFor}'s listener runs on the session's own thread — a listener that blocks
 * stalls the game — so a client whose emitter throws (closed connection, write failure) is dropped
 * from the list rather than allowed to wedge the others. {@code send} is still a synchronous call
 * into the servlet container's async I/O, so this keeps a dead client from crashing the fan-out,
 * not from ever taking any time at all.
 *
 * <p>Emitters never time out ({@code new SseEmitter(0L)}), so the stream itself has to say when a
 * game is done: a terminal event is delivered and then every emitter for that game is completed and
 * the game's entry dropped. Without that, each spectator holds an open connection forever after the
 * final score.
 */
public final class GameEventStream {

  private final ConcurrentHashMap<GameId, CopyOnWriteArrayList<SseEmitter>> subscribers =
      new ConcurrentHashMap<>();

  public SseEmitter subscribe(GameId gameId) {
    return register(gameId, new SseEmitter(0L));
  }

  /** The seam {@link #subscribe} is built on, so tests can register an emitter they can observe. */
  SseEmitter register(GameId gameId, SseEmitter emitter) {
    List<SseEmitter> emitters = emittersFor(gameId);
    emitters.add(emitter);
    Runnable unsubscribe = () -> emitters.remove(emitter);
    emitter.onCompletion(unsubscribe);
    emitter.onTimeout(unsubscribe);
    emitter.onError(e -> unsubscribe.run());
    return emitter;
  }

  public SessionListener listenerFor(GameId gameId) {
    return event -> {
      com.PRS.contract.model.SessionEvent wire = SessionEventMapper.toWire(event);
      CopyOnWriteArrayList<SseEmitter> emitters = emittersFor(gameId);
      for (SseEmitter emitter : emitters) {
        try {
          emitter.send(wire);
        } catch (RuntimeException | IOException e) {
          emitters.remove(emitter);
        }
      }
      if (isTerminal(event)) {
        closeStreamsFor(gameId);
      }
    };
  }

  /** After these nothing more is ever broadcast, so holding the connection open buys nothing. */
  private static boolean isTerminal(SessionEvent event) {
    return event instanceof SessionEvent.GameEnded || event instanceof SessionEvent.SessionFailed;
  }

  /**
   * Completes every emitter for a game and forgets it. Called on a terminal event, and by {@code
   * Lobby} eviction for a game that never reached one.
   */
  public void closeStreamsFor(GameId gameId) {
    CopyOnWriteArrayList<SseEmitter> emitters = subscribers.remove(gameId);
    if (emitters == null) {
      return;
    }
    for (SseEmitter emitter : emitters) {
      try {
        emitter.complete();
      } catch (RuntimeException e) {
        // Already dead: the point of completing was to release it, which it is.
      }
    }
  }

  /** How many live subscribers a game has — zero for a game this stream has never seen. */
  public int subscriberCount(GameId gameId) {
    CopyOnWriteArrayList<SseEmitter> emitters = subscribers.get(gameId);
    return emitters == null ? 0 : emitters.size();
  }

  private CopyOnWriteArrayList<SseEmitter> emittersFor(GameId gameId) {
    return subscribers.computeIfAbsent(gameId, id -> new CopyOnWriteArrayList<>());
  }
}
