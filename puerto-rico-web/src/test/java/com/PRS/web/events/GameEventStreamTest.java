package com.PRS.web.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.PRS.contract.model.GameStartedEvent;
import com.PRS.lobby.GameId;
import com.PRS.model.game.GameConfig;
import com.PRS.model.game.GameSetup;
import com.PRS.model.game.GameState;
import com.PRS.session.events.SessionEvent;
import com.PRS.session.events.SessionListener;
import com.PRS.session.view.GameView;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class GameEventStreamTest {

  private static GameView sampleView() {
    GameState state = GameSetup.create(new GameConfig(List.of("Ana", "Bo", "Coco"), 1L));
    return GameView.of(state, null);
  }

  private static SessionEvent sampleEvent() {
    return new SessionEvent.GameStarted(sampleView(), List.of("Ana", "Bo", "Coco"));
  }

  @Test
  void listenerWithNoSubscribersDoesNothing() {
    GameEventStream stream = new GameEventStream();
    SessionListener listener = stream.listenerFor(GameId.newId());

    assertThatCode(() -> listener.onEvent(sampleEvent())).doesNotThrowAnyException();
  }

  @Test
  void aSubscriberReceivesTheMappedWireEvent() throws IOException {
    GameEventStream stream = new GameEventStream();
    GameId gameId = GameId.newId();
    SseEmitter emitter = stream.register(gameId, mock(SseEmitter.class));

    stream.listenerFor(gameId).onEvent(sampleEvent());

    ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
    verify(emitter).send(sent.capture());
    assertThat(sent.getValue())
        .isInstanceOfSatisfying(
            GameStartedEvent.class,
            wire -> {
              assertThat(wire.getSeatNames()).containsExactly("Ana", "Bo", "Coco");
              assertThat(wire.getView().getState().getPlayers()).hasSize(3);
            });
  }

  /**
   * The behaviour the fan-out exists for: one dead client must neither wedge nor rob the others,
   * and must not be tried again on the next event.
   */
  @Test
  void aFailingEmitterIsDroppedAndTheLiveOneKeepsReceiving() throws IOException {
    GameEventStream stream = new GameEventStream();
    GameId gameId = GameId.newId();
    SseEmitter dead = mock(SseEmitter.class);
    doThrow(new IOException("connection closed")).when(dead).send(any(Object.class));
    stream.register(gameId, dead);
    SseEmitter live = stream.register(gameId, mock(SseEmitter.class));

    SessionListener listener = stream.listenerFor(gameId);
    listener.onEvent(sampleEvent());

    assertThat(stream.subscriberCount(gameId)).isEqualTo(1);
    verify(dead).send(any(Object.class));
    verify(live).send(any(Object.class));

    // The second event must reach only the survivor.
    listener.onEvent(sampleEvent());

    verify(dead).send(any(Object.class));
    verify(live, times(2)).send(any(Object.class));
  }

  @Test
  void aTerminalEventCompletesEveryEmitterAndForgetsTheGame() throws IOException {
    GameEventStream stream = new GameEventStream();
    GameId gameId = GameId.newId();
    SseEmitter emitter = stream.register(gameId, mock(SseEmitter.class));

    stream.listenerFor(gameId).onEvent(new SessionEvent.GameEnded(sampleView(), List.of()));

    verify(emitter).send(any(Object.class));
    verify(emitter).complete();
    assertThat(stream.subscriberCount(gameId)).isZero();
  }

  @Test
  void aFailedSessionAlsoClosesTheStream() {
    GameEventStream stream = new GameEventStream();
    GameId gameId = GameId.newId();
    SseEmitter emitter = stream.register(gameId, mock(SseEmitter.class));

    stream.listenerFor(gameId).onEvent(new SessionEvent.SessionFailed(sampleView(), "gave up"));

    verify(emitter).complete();
    assertThat(stream.subscriberCount(gameId)).isZero();
  }

  @Test
  void aNonTerminalEventLeavesTheStreamOpen() {
    GameEventStream stream = new GameEventStream();
    GameId gameId = GameId.newId();
    SseEmitter emitter = stream.register(gameId, mock(SseEmitter.class));

    stream.listenerFor(gameId).onEvent(sampleEvent());

    verify(emitter, never()).complete();
    assertThat(stream.subscriberCount(gameId)).isEqualTo(1);
  }
}
