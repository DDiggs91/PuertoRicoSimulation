package com.PRS.web.actors;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.game.GameConfig;
import com.PRS.model.game.GameSetup;
import com.PRS.model.game.GameState;
import com.PRS.model.rolecards.Role;
import com.PRS.session.actors.Decision;
import com.PRS.session.view.GameView;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class HumanActorTest {

  private static Decision decisionFor(int requestId) {
    GameState state = GameSetup.create(new GameConfig(List.of("Ana", "Bo", "Coco"), 1L));
    int seat = state.governorSeat();
    return new Decision(
        seat,
        GameView.of(state, seat),
        List.of(
            new PlayerAction.SelectRole(seat, Role.SETTLER),
            new PlayerAction.SelectRole(seat, Role.MAYOR)),
        requestId);
  }

  @Test
  void decideReturnsAnIncompleteFuture() {
    HumanActor actor = new HumanActor("Ana");

    CompletableFuture<PlayerAction> future = actor.decide(decisionFor(1));

    assertThat(future).isNotDone();
  }

  @Test
  void aValidOfferCompletesTheFuture() throws Exception {
    HumanActor actor = new HumanActor("Ana");
    Decision decision = decisionFor(1);
    CompletableFuture<PlayerAction> future = actor.decide(decision);

    OfferResult result = actor.offer(1, decision.options().getFirst());

    assertThat(result).isInstanceOf(OfferResult.Accepted.class);
    assertThat(future.get()).isEqualTo(decision.options().getFirst());
  }

  @Test
  void anOfferWithAStaleRequestIdIsRejectedAndTheFutureStaysPending() {
    HumanActor actor = new HumanActor("Ana");
    Decision decision = decisionFor(5);
    CompletableFuture<PlayerAction> future = actor.decide(decision);

    OfferResult result = actor.offer(4, decision.options().getFirst());

    assertThat(result).isInstanceOf(OfferResult.Rejected.class);
    assertThat(future).isNotDone();
  }

  @Test
  void anOfferNotAmongTheOptionsIsRejectedAndTheFutureStaysPending() {
    HumanActor actor = new HumanActor("Ana");
    Decision decision = decisionFor(1);
    CompletableFuture<PlayerAction> future = actor.decide(decision);

    OfferResult result = actor.offer(1, new PlayerAction.PassBuilding(decision.seat()));

    assertThat(result).isInstanceOf(OfferResult.Rejected.class);
    assertThat(future).isNotDone();
  }

  @Test
  void anOfferWithNothingPendingIsRejected() {
    HumanActor actor = new HumanActor("Ana");

    OfferResult result = actor.offer(1, new PlayerAction.PassBuilding(0));

    assertThat(result).isInstanceOf(OfferResult.Rejected.class);
  }

  @Test
  void aSecondOfferAfterAnAcceptedOneIsRejected() {
    HumanActor actor = new HumanActor("Ana");
    Decision decision = decisionFor(1);
    actor.decide(decision);
    actor.offer(1, decision.options().getFirst());

    OfferResult second = actor.offer(1, decision.options().get(1));

    assertThat(second).isInstanceOf(OfferResult.Rejected.class);
  }

  @Test
  void nameReturnsTheConstructedName() {
    assertThat(new HumanActor("Ana").name()).isEqualTo("Ana");
  }
}
