package com.PRS.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.PRS.model.scoring.ScoreBreakdown;
import com.PRS.session.actors.ActorKind;
import com.PRS.session.actors.SeatedActor;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.Test;

public class SeatingTest {

  private static List<SeatedActor> allAi(String... names) {
    return List.of(names).stream()
        .map(name -> new SeatedActor(FakeActors.firstLegal(name), ActorKind.AI))
        .toList();
  }

  @Test
  public void rejectsUnsupportedPlayerCounts() {
    assertThatThrownBy(() -> GameSession.create(1L, allAi("Solo")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> GameSession.create(1L, allAi("A", "B", "C", "D", "E", "F")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void acceptsThreeToFivePlayers() {
    for (int n = 3; n <= 5; n++) {
      String[] names = new String[n];
      for (int i = 0; i < n; i++) {
        names[i] = "P" + i;
      }
      GameSession session = GameSession.create(1L, allAi(names));
      session.start();
      assertThat(session.state().playerCount()).isEqualTo(n);
    }
  }

  @Test
  public void seatIndicesMatchConfiguredOrder() {
    GameSession session = GameSession.create(1L, allAi("Ana", "Bo", "Coco"));
    session.start();

    assertThat(session.state().players().get(0).name()).isEqualTo("Ana");
    assertThat(session.state().players().get(1).name()).isEqualTo("Bo");
    assertThat(session.state().players().get(2).name()).isEqualTo("Coco");
  }

  @Test
  public void actorKindNeverAffectsRouting() throws Exception {
    List<SeatedActor> humanFirst =
        List.of(
            new SeatedActor(FakeActors.firstLegal("Ana"), ActorKind.HUMAN),
            new SeatedActor(FakeActors.firstLegal("Bo"), ActorKind.AI),
            new SeatedActor(FakeActors.firstLegal("Coco"), ActorKind.AI));
    List<SeatedActor> allAiSeats =
        List.of(
            new SeatedActor(FakeActors.firstLegal("Ana"), ActorKind.AI),
            new SeatedActor(FakeActors.firstLegal("Bo"), ActorKind.AI),
            new SeatedActor(FakeActors.firstLegal("Coco"), ActorKind.AI));

    GameSession sessionA = GameSession.create(123L, humanFirst);
    sessionA.start();
    GameSession sessionB = GameSession.create(123L, allAiSeats);
    sessionB.start();

    try (SessionRunner runnerA = SessionRunner.drive(sessionA, humanFirst);
        SessionRunner runnerB = SessionRunner.drive(sessionB, allAiSeats)) {
      List<ScoreBreakdown> standingsA = runnerA.completion().get(30, TimeUnit.SECONDS);
      List<ScoreBreakdown> standingsB = runnerB.completion().get(30, TimeUnit.SECONDS);

      assertThat(sessionA.state()).isEqualTo(sessionB.state());
      assertThat(standingsA).isEqualTo(standingsB);
    }
  }
}
