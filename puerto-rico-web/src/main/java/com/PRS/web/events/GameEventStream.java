package com.PRS.web.events;

import com.PRS.lobby.GameId;
import com.PRS.session.events.SessionListener;
import com.PRS.web.wire.SessionEventMapper;
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
 */
public final class GameEventStream {

  private final ConcurrentHashMap<GameId, CopyOnWriteArrayList<SseEmitter>> subscribers =
      new ConcurrentHashMap<>();

  public SseEmitter subscribe(GameId gameId) {
    SseEmitter emitter = new SseEmitter(0L);
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
      for (SseEmitter emitter : emittersFor(gameId)) {
        try {
          emitter.send(wire);
        } catch (RuntimeException | java.io.IOException e) {
          emittersFor(gameId).remove(emitter);
        }
      }
    };
  }

  private CopyOnWriteArrayList<SseEmitter> emittersFor(GameId gameId) {
    return subscribers.computeIfAbsent(gameId, id -> new CopyOnWriteArrayList<>());
  }
}
