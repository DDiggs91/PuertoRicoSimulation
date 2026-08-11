package com.PRS.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.lobby.GameId;
import com.PRS.lobby.Lobby;
import com.PRS.session.actors.Actor;
import com.PRS.session.actors.ActorKind;
import com.PRS.session.actors.Decision;
import com.PRS.web.actors.SeatTokens;
import com.PRS.web.events.GameEventStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/**
 * The sweeper is the one place lobby tables, seat tokens, and SSE subscriptions are reclaimed
 * together — they are keyed on the same {@link GameId} and nothing else drops any of them.
 */
class GameTableSweeperTest {

  private static final class TestClock extends Clock {
    private Instant now = Instant.parse("2026-01-01T00:00:00Z");

    void advance(Duration by) {
      now = now.plus(by);
    }

    @Override
    public Instant instant() {
      return now;
    }

    @Override
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }
  }

  private static Actor stubActor(String name) {
    return new Actor() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public CompletableFuture<com.PRS.model.actions.PlayerAction> decide(Decision decision) {
        return new CompletableFuture<>();
      }
    };
  }

  @Test
  void sweepingAnAbandonedTableAlsoDropsItsTokensAndStreams() {
    TestClock clock = new TestClock();
    try (Lobby lobby = new Lobby(clock)) {
      SeatTokens seatTokens = new SeatTokens();
      GameEventStream eventStream = new GameEventStream();
      GameTableSweeper sweeper = new GameTableSweeper(lobby, seatTokens, eventStream, 30);

      GameId id = lobby.createGame();
      String token = seatTokens.mint(id, 0);
      eventStream.subscribe(id);
      assertThat(seatTokens.isValid(id, 0, token)).isTrue();

      // Inside the retention window nothing is touched.
      sweeper.sweep();
      assertThat(lobby.find(id)).isPresent();
      assertThat(seatTokens.isValid(id, 0, token)).isTrue();

      clock.advance(Duration.ofMinutes(31));
      sweeper.sweep();

      assertThat(lobby.find(id)).isEmpty();
      assertThat(seatTokens.isValid(id, 0, token)).isFalse();
      assertThat(eventStream.subscriberCount(id)).isZero();
    }
  }

  @Test
  void aTokenForAnotherGameSurvivesTheSweep() {
    TestClock clock = new TestClock();
    try (Lobby lobby = new Lobby(clock)) {
      SeatTokens seatTokens = new SeatTokens();
      GameTableSweeper sweeper = new GameTableSweeper(lobby, seatTokens, new GameEventStream(), 30);

      GameId stale = lobby.createGame();
      String staleToken = seatTokens.mint(stale, 0);
      clock.advance(Duration.ofMinutes(31));
      GameId fresh = lobby.createGame();
      String freshToken = seatTokens.mint(fresh, 0);

      sweeper.sweep();

      assertThat(seatTokens.isValid(stale, 0, staleToken)).isFalse();
      assertThat(seatTokens.isValid(fresh, 0, freshToken)).isTrue();
      assertThat(lobby.find(fresh)).isPresent();
    }
  }

  @Test
  void sweepingWithNothingStaleIsHarmless() {
    try (Lobby lobby = new Lobby()) {
      SeatTokens seatTokens = new SeatTokens();
      GameTableSweeper sweeper = new GameTableSweeper(lobby, seatTokens, new GameEventStream(), 30);
      GameId id = lobby.createGame();
      lobby.join(id, stubActor("Ana"), ActorKind.HUMAN);

      sweeper.sweep();

      assertThat(lobby.find(id)).isPresent();
    }
  }
}
