package com.PRS.lobby;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.session.GameSession;
import com.PRS.session.SessionStatus;
import com.PRS.session.actors.ActorKind;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.testng.annotations.Test;

/**
 * Eviction: without it the lobby retains every table it has ever created, along with the {@code
 * GameSession}, {@code GameState} history, and {@code SessionRunner} thread each one owns.
 */
public class LobbyEvictionTest {

  /** A clock the test advances by hand, so retention is exercised without sleeping. */
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
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }
  }

  private static final Duration RETENTION = Duration.ofMinutes(30);

  @Test
  public void aFinishedGameIsReportedFinishedRatherThanStarted() throws Exception {
    try (Lobby lobby = new Lobby()) {
      GameId id = finishedGame(lobby);

      assertThat(lobby.find(id).orElseThrow().status()).isEqualTo(GameTableStatus.FINISHED);
    }
  }

  @Test
  public void aRunningGameStillReportsStarted() {
    try (Lobby lobby = new Lobby()) {
      GameId id = lobby.createGame();
      StubActors.DeferredActor human = StubActors.deferred("Human");
      lobby.join(id, human, ActorKind.HUMAN);
      lobby.join(id, StubActors.named("Bo"), ActorKind.AI);
      lobby.join(id, StubActors.named("Coco"), ActorKind.AI);
      lobby.start(id, 42L);

      assertThat(lobby.find(id).orElseThrow().status()).isEqualTo(GameTableStatus.STARTED);
    }
  }

  @Test
  public void aFinishedTableSurvivesTheRetentionWindowAndIsSweptAfterIt() throws Exception {
    TestClock clock = new TestClock();
    try (Lobby lobby = new Lobby(clock)) {
      GameId id = finishedGame(lobby);

      // First sweep only stamps the moment it noticed; the board stays readable meanwhile.
      assertThat(lobby.evictStaleTables(RETENTION)).isEmpty();
      clock.advance(Duration.ofMinutes(29));
      assertThat(lobby.evictStaleTables(RETENTION)).isEmpty();
      assertThat(lobby.find(id)).isPresent();

      clock.advance(Duration.ofMinutes(2));

      assertThat(lobby.evictStaleTables(RETENTION)).containsExactly(id);
      assertThat(lobby.find(id)).isEmpty();
      assertThat(lobby.listGames()).isEmpty();
    }
  }

  @Test
  public void aTableCreatedAndNeverSeatedIsSweptOnTheSameSchedule() {
    TestClock clock = new TestClock();
    try (Lobby lobby = new Lobby(clock)) {
      GameId abandoned = lobby.createGame();
      clock.advance(Duration.ofMinutes(31));

      assertThat(lobby.evictStaleTables(RETENTION)).containsExactly(abandoned);
    }
  }

  @Test
  public void aSeatedButUnstartedTableIsNotSwept() {
    TestClock clock = new TestClock();
    try (Lobby lobby = new Lobby(clock)) {
      GameId id = lobby.createGame();
      lobby.join(id, StubActors.named("Ana"), ActorKind.AI);
      clock.advance(Duration.ofHours(2));

      assertThat(lobby.evictStaleTables(RETENTION)).isEmpty();
      assertThat(lobby.find(id)).isPresent();
    }
  }

  @Test
  public void removeDropsATableAndReportsWhetherItWasThere() {
    try (Lobby lobby = new Lobby()) {
      GameId id = lobby.createGame();

      assertThat(lobby.remove(id)).isTrue();
      assertThat(lobby.find(id)).isEmpty();
      assertThat(lobby.remove(id)).isFalse();
      assertThat(lobby.remove(GameId.newId())).isFalse();
    }
  }

  private static GameId finishedGame(Lobby lobby) throws Exception {
    GameId id = lobby.createGame();
    for (String name : List.of("Ana", "Bo", "Coco")) {
      lobby.join(id, StubActors.named(name), ActorKind.AI);
    }
    lobby.start(id, 42L);

    GameSession session = lobby.sessionFor(id).orElseThrow();
    waitUntil(() -> session.status() == SessionStatus.FINISHED, 30);
    return id;
  }

  private static void waitUntil(BooleanSupplier condition, int seconds) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
    while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
      TimeUnit.MILLISECONDS.sleep(10);
    }
    assertThat(condition.getAsBoolean()).as("condition met within %ds", seconds).isTrue();
  }
}
