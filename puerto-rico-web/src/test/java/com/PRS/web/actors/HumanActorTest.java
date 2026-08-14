package com.PRS.web.actors;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.IslandTile;
import com.PRS.model.boards.TileType;
import com.PRS.model.engine.ActionResult;
import com.PRS.model.engine.GameEngine;
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

  /**
   * A mayor decision with room to spare: three empty island tiles for two colonists, so more than
   * one arrangement is legal and the single enumerated option (the greedy fill) is not the only
   * answer — exactly the situation {@code offer}'s engine-validated arm exists for.
   */
  private static Decision mayorDecisionFor(int requestId) {
    GameState state = GameSetup.create(new GameConfig(List.of("Ana", "Bo", "Coco"), 1L));
    int seat = state.governorSeat();
    state =
        state.withPlayer(
            state.player(seat).toBuilder()
                .island(
                    List.of(
                        IslandTile.unstaffed(TileType.CORN),
                        IslandTile.unstaffed(TileType.CORN),
                        IslandTile.unstaffed(TileType.CORN)))
                .build());
    GameState mayorState =
        ((ActionResult.Accepted)
                GameEngine.apply(state, new PlayerAction.SelectRole(seat, Role.MAYOR)))
            .state();
    int actor = mayorState.phase().actorSeat();
    return new Decision(
        actor, GameView.of(mayorState, actor), GameEngine.legalActions(mayorState), requestId);
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

  /**
   * A staged colonist arrangement doesn't have to equal the one option the engine happened to
   * enumerate — it only has to be legal, checked directly against the pending decision's board.
   */
  @Test
  void aStagedArrangementThatDiffersFromTheOfferedOneIsAcceptedWhenTheEngineAllowsIt()
      throws Exception {
    HumanActor actor = new HumanActor("Ana");
    Decision decision = mayorDecisionFor(1);
    CompletableFuture<PlayerAction> future = actor.decide(decision);

    PlayerAction offered = decision.options().getFirst();
    PlayerAction different =
        new PlayerAction.SetColonistPlacement(
            decision.seat(), List.of(false, true, true), List.of());
    assertThat(different).isNotEqualTo(offered);

    OfferResult result = actor.offer(1, different);

    assertThat(result).isInstanceOf(OfferResult.Accepted.class);
    assertThat(future.get()).isEqualTo(different);
  }

  /**
   * An arrangement the engine itself would refuse is refused at {@code offer}, before it can count
   * as one of the runner's three consecutive unusable answers — and the future stays pending so the
   * player can fix and resubmit.
   */
  @Test
  void anArrangementTheEngineRejectsIsRefusedWithTheEnginesOwnReason() {
    HumanActor actor = new HumanActor("Ana");
    Decision decision = mayorDecisionFor(1);
    CompletableFuture<PlayerAction> future = actor.decide(decision);

    // One of two colonists placed, one tile still empty: colonists may not sit in San Juan while a
    // circle is empty.
    PlayerAction invalid =
        new PlayerAction.SetColonistPlacement(
            decision.seat(), List.of(true, false, false), List.of());

    OfferResult result = actor.offer(1, invalid);

    assertThat(result).isInstanceOf(OfferResult.Rejected.class);
    assertThat(((OfferResult.Rejected) result).detail())
        .isEqualTo("Colonists may not sit in San Juan while circles are empty");
    assertThat(future).isNotDone();
  }

  /**
   * The engine-validated arm only applies to a decision actually offering {@code
   * SetColonistPlacement} — anywhere else, one is just an unrecognized action.
   */
  @Test
  void aColonistArrangementOfferedOutsideTheMayorPhaseIsRejected() {
    HumanActor actor = new HumanActor("Ana");
    Decision decision = decisionFor(1);
    actor.decide(decision);

    OfferResult result =
        actor.offer(
            1, new PlayerAction.SetColonistPlacement(decision.seat(), List.of(), List.of()));

    assertThat(result).isInstanceOf(OfferResult.Rejected.class);
  }
}
