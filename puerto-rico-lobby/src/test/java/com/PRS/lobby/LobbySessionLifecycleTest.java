package com.PRS.lobby;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.scoring.ScoreBreakdown;
import com.PRS.session.GameSession;
import com.PRS.session.SessionStatus;
import com.PRS.session.actors.ActorKind;
import com.PRS.session.actors.SeatedActor;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.testng.annotations.Test;

public class LobbySessionLifecycleTest {

  @Test
  public void startedGameSessionSeatOrderMatchesJoinOrder() {
    try (Lobby lobby = new Lobby()) {
      GameId id = lobby.createGame();
      lobby.join(id, StubActors.named("Ana"), ActorKind.AI);
      lobby.join(id, StubActors.named("Bo"), ActorKind.HUMAN);
      lobby.join(id, StubActors.named("Coco"), ActorKind.AI);

      lobby.start(id, 42L);

      GameSession session = lobby.sessionFor(id).orElseThrow();
      assertThat(session.seats())
          .extracting(seated -> seated.actor().name())
          .containsExactly("Ana", "Bo", "Coco");
      assertThat(session.seats())
          .extracting(SeatedActor::kind)
          .containsExactly(ActorKind.AI, ActorKind.HUMAN, ActorKind.AI);
    }
  }

  @Test
  public void anAllStubActorGameDrivenThroughTheLobbyReachesFinished() throws Exception {
    try (Lobby lobby = new Lobby()) {
      GameId id = lobby.createGame();
      lobby.join(id, StubActors.named("Ana"), ActorKind.AI);
      lobby.join(id, StubActors.named("Bo"), ActorKind.AI);
      lobby.join(id, StubActors.named("Coco"), ActorKind.AI);

      lobby.start(id, 42L);
      GameSession session = lobby.sessionFor(id).orElseThrow();

      waitUntil(() -> session.status() == SessionStatus.FINISHED, 30);

      List<ScoreBreakdown> standings = session.standings();
      assertThat(standings).hasSize(3);
    }
  }

  @Test
  public void closeMidGameStopsTheRunnerWithoutCorruptingTheSession() throws Exception {
    StubActors.DeferredActor human = StubActors.deferred("Human");
    Lobby lobby = new Lobby();
    GameId id = lobby.createGame();
    lobby.join(id, human, ActorKind.HUMAN);
    lobby.join(id, StubActors.named("Bo"), ActorKind.AI);
    lobby.join(id, StubActors.named("Coco"), ActorKind.AI);

    lobby.start(id, 5L);
    GameSession session = lobby.sessionFor(id).orElseThrow();
    // Seat 0 (the human) is the first governor and acts first in RoleSelection.
    waitUntil(human::hasPending, 5);

    lobby.close();

    assertThat(session.status()).isEqualTo(SessionStatus.AWAITING_DECISION);
  }

  private static void waitUntil(BooleanSupplier condition, int timeoutSeconds)
      throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() > deadline) {
        throw new AssertionError("condition not met within " + timeoutSeconds + "s");
      }
      Thread.sleep(10);
    }
  }
}
