package com.PRS.web.events;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.PRS.lobby.GameId;
import com.PRS.model.game.GameConfig;
import com.PRS.model.game.GameSetup;
import com.PRS.model.game.GameState;
import com.PRS.session.events.SessionEvent;
import com.PRS.session.events.SessionListener;
import com.PRS.session.view.GameView;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class GameEventStreamTest {

  private static SessionEvent sampleEvent() {
    GameState state = GameSetup.create(new GameConfig(List.of("Ana", "Bo", "Coco"), 1L));
    return new SessionEvent.GameStarted(GameView.of(state, null), List.of("Ana", "Bo", "Coco"));
  }

  @Test
  void listenerWithNoSubscribersDoesNothing() {
    GameEventStream stream = new GameEventStream();
    SessionListener listener = stream.listenerFor(GameId.newId());

    assertThatCode(() -> listener.onEvent(sampleEvent())).doesNotThrowAnyException();
  }

  @Test
  void aCompletedEmitterIsDroppedWithoutPropagating() {
    GameEventStream stream = new GameEventStream();
    GameId gameId = GameId.newId();
    SseEmitter emitter = stream.subscribe(gameId);
    emitter.complete();

    SessionListener listener = stream.listenerFor(gameId);

    assertThatCode(() -> listener.onEvent(sampleEvent())).doesNotThrowAnyException();
  }

  @Test
  void subscribeReturnsAUsableEmitter() {
    GameEventStream stream = new GameEventStream();

    SseEmitter emitter = stream.subscribe(GameId.newId());

    assertThatCode(emitter::complete).doesNotThrowAnyException();
  }
}
