package com.PRS.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.actions.PlayerAction;
import com.PRS.session.actors.ActorKind;
import com.PRS.session.actors.Decision;
import com.PRS.session.actors.SeatedActor;
import com.PRS.session.events.SessionEvent;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

public class SessionEventTest {

  private static GameSession newSession(List<SessionEvent> recorded, String... names) {
    List<SeatedActor> seats =
        List.of(names).stream()
            .map(name -> new SeatedActor(FakeActors.firstLegal(name), ActorKind.AI))
            .toList();
    GameSession session = GameSession.create(11L, seats);
    session.addListener(recorded::add);
    session.start();
    return session;
  }

  @Test
  public void gameStartedFiresBeforeAnyDecision() {
    List<SessionEvent> events = new ArrayList<>();
    newSession(events, "Ana", "Bo", "Coco");

    assertThat(events.get(0)).isInstanceOf(SessionEvent.GameStarted.class);
    assertThat(events.get(1)).isInstanceOf(SessionEvent.DecisionRequested.class);
  }

  @Test
  public void everyAcceptedActionEmitsDecisionThenApplied() {
    List<SessionEvent> events = new ArrayList<>();
    GameSession session = newSession(events, "Ana", "Bo", "Coco");

    for (int i = 0; i < 4; i++) {
      Decision decision = session.pendingDecision();
      session.submit(decision.seat(), decision.requestId(), decision.options().getFirst());
    }

    // events[0] is GameStarted; from index 1 on it alternates DecisionRequested, ActionApplied.
    for (int i = 1; i + 1 < events.size(); i += 2) {
      assertThat(events.get(i)).isInstanceOf(SessionEvent.DecisionRequested.class);
      assertThat(events.get(i + 1)).isInstanceOf(SessionEvent.ActionApplied.class);
    }
  }

  @Test
  public void aRejectedSubmitEmitsActionRejectedNotActionApplied() {
    List<SessionEvent> events = new ArrayList<>();
    GameSession session = newSession(events, "Ana", "Bo", "Coco");
    Decision decision = session.pendingDecision();
    events.clear();

    session.submit(
        decision.seat(), decision.requestId(), new PlayerAction.PassBuilding(decision.seat()));

    assertThat(events).hasSize(1);
    assertThat(events.get(0)).isInstanceOf(SessionEvent.ActionRejected.class);
  }

  @Test
  public void everyEventCarriesThePostActionState() {
    List<SessionEvent> events = new ArrayList<>();
    GameSession session = newSession(events, "Ana", "Bo", "Coco");
    Decision decision = session.pendingDecision();

    session.submit(decision.seat(), decision.requestId(), decision.options().getFirst());

    SessionEvent applied =
        events.stream()
            .filter(SessionEvent.ActionApplied.class::isInstance)
            .findFirst()
            .orElseThrow();
    assertThat(applied.view().state()).isEqualTo(session.viewFor(null).state());
  }

  @Test
  public void aThrowingListenerIsLoggedAndTheGameContinues() {
    List<SessionEvent> events = new ArrayList<>();
    GameSession session = newSession(events, "Ana", "Bo", "Coco");
    session.addListener(
        event -> {
          throw new RuntimeException("boom");
        });

    Decision decision = session.pendingDecision();
    SubmitOutcome outcome =
        session.submit(decision.seat(), decision.requestId(), decision.options().getFirst());

    assertThat(outcome).isInstanceOf(SubmitOutcome.Applied.class);
    assertThat(events).isNotEmpty();
  }

  @Test
  public void lateListenersOnlySeeSubsequentEvents() {
    List<SessionEvent> events = new ArrayList<>();
    GameSession session = newSession(events, "Ana", "Bo", "Coco");
    Decision decision = session.pendingDecision();
    session.submit(decision.seat(), decision.requestId(), decision.options().getFirst());
    List<SessionEvent> eventsSoFar = List.copyOf(events);

    List<SessionEvent> lateEvents = new ArrayList<>();
    session.addListener(lateEvents::add);
    Decision next = session.pendingDecision();
    session.submit(next.seat(), next.requestId(), next.options().getFirst());

    assertThat(lateEvents).isNotEmpty();
    assertThat(lateEvents).doesNotContainAnyElementsOf(eventsSoFar);
  }
}
