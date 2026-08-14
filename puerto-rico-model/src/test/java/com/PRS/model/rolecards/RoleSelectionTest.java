package com.PRS.model.rolecards;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.TestGames;
import com.PRS.model.actions.PlayerAction;
import com.PRS.model.engine.ActionResult;
import com.PRS.model.engine.GameEngine;
import com.PRS.model.engine.RejectionReason;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import org.testng.annotations.Test;

/** Choosing roles, the doubloons that pile up on unchosen cards, and the round boundary. */
public class RoleSelectionTest {

  /** Plays a whole round, each player taking the first role still on the table. */
  private static GameState playRound(GameState state) {
    for (int i = 0; i < state.playerCount(); i++) {
      state = playOutRole(state, state.roles().availableRoles().getFirst());
    }
    return state;
  }

  @Test
  public void everyRoleIsOfferedAtTheStartOfARound() {
    GameState state = TestGames.newGame(5);
    assertThat(state.roles().availableRoles()).containsExactlyInAnyOrder(Role.values());
    assertThat(GameEngine.legalActions(state)).hasSize(Role.values().length);
  }

  @Test
  public void threePlayerGamesHaveNoProspector() {
    GameState state = TestGames.newGame(3);
    assertThat(state.roles().availableRoles()).doesNotContain(Role.PROSPECTOR);
  }

  @Test
  public void aChosenRoleIsUnavailableForTheRestOfTheRound() {
    GameState state = TestGames.chooseRole(TestGames.newGame(4), Role.PROSPECTOR);
    // One Prospector card at four players, so the role itself is now gone.
    assertThat(state.roles().availableRoles()).doesNotContain(Role.PROSPECTOR);

    ActionResult result = GameEngine.apply(state, new PlayerAction.SelectRole(1, Role.PROSPECTOR));
    assertThat(result).isInstanceOf(ActionResult.Rejected.class);
    assertThat(((ActionResult.Rejected) result).reason())
        .isEqualTo(RejectionReason.ROLE_UNAVAILABLE);
  }

  @Test
  public void fivePlayerGamesHaveTwoProspectorsSoTheRoleSurvivesOnePick() {
    GameState state = TestGames.chooseRole(TestGames.newGame(5), Role.PROSPECTOR);
    assertThat(state.roles().availableRoles()).contains(Role.PROSPECTOR);
  }

  @Test
  public void choosingAProspectorPaysADoubloonAndPassesStraightOn() {
    GameState state = TestGames.newGame(4);
    int before = state.player(0).doubloons();

    state = TestGames.chooseRole(state, Role.PROSPECTOR);

    assertThat(state.player(0).doubloons()).isEqualTo(before + 1);
    // No action for anybody else: the next player is already choosing.
    assertThat(state.phase()).isInstanceOf(Phase.RoleSelection.class);
    assertThat(state.phase().actorSeat()).isEqualTo(1);
  }

  @Test
  public void playersChooseClockwiseFromTheGovernor() {
    GameState state = TestGames.newGame(4);
    for (int expectedSeat = 0; expectedSeat < 4; expectedSeat++) {
      assertThat(state.phase().actorSeat()).isEqualTo(expectedSeat);
      state = playOutRole(state, state.roles().availableRoles().getFirst());
      if (expectedSeat < 3) {
        assertThat(state.phase()).isInstanceOf(Phase.RoleSelection.class);
      }
    }
  }

  @Test
  public void exactlyThreeUnchosenCardsCollectADoubloonEachRound() {
    GameState state = playRound(TestGames.newGame(4));

    assertThat(state.roles().cards().stream().filter(c -> c.doubloons() == 1)).hasSize(3);
    assertThat(state.roles().cards().stream().filter(c -> c.doubloons() == 0)).hasSize(4);
    assertThat(state.roles().cards()).noneMatch(RoleCard::isTaken);
  }

  @Test
  public void theChooserCollectsTheDoubloonsSittingOnTheCard() {
    GameState state = TestGames.newGame(4);
    // Round one: take everything except the Captain, which then gains a doubloon.
    for (int i = 0; i < 4; i++) {
      Role role =
          state.roles().availableRoles().stream()
              .filter(r -> r != Role.CAPTAIN)
              .findFirst()
              .orElseThrow();
      state = playOutRole(state, role);
    }
    assertThat(state.roles().doubloonsOn(Role.CAPTAIN)).isEqualTo(1);

    int seat = state.phase().actorSeat();
    int before = state.player(seat).doubloons();
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    assertThat(state.player(seat).doubloons()).isEqualTo(before + 1);
    assertThat(state.roles().doubloonsOn(Role.CAPTAIN)).isZero();
  }

  @Test
  public void theGovernorMovesOnClockwiseAndAllCardsComeBack() {
    GameState state = TestGames.newGame(4);
    assertThat(state.governorSeat()).isZero();

    state = playRound(state);

    assertThat(state.governorSeat()).isEqualTo(1);
    assertThat(state.phase().actorSeat()).isEqualTo(1);
    assertThat(state.roles().availableRoles()).containsExactlyInAnyOrder(Role.values());
  }

  @Test
  public void aPlayerOutOfTurnIsRefused() {
    GameState state = TestGames.newGame(4);
    ActionResult result = GameEngine.apply(state, new PlayerAction.SelectRole(2, Role.PROSPECTOR));
    assertThat(((ActionResult.Rejected) result).reason()).isEqualTo(RejectionReason.NOT_YOUR_TURN);
  }

  /** Chooses a role and passes through whatever phase it opens, so the round can move along. */
  static GameState playOutRole(GameState state, Role role) {
    GameState next = TestGames.chooseRole(state, role);
    while (!(next.phase() instanceof Phase.RoleSelection)
        && !(next.phase() instanceof Phase.GameOver)) {
      next = TestGames.apply(next, passiveAction(next));
    }
    return next;
  }

  /** The most do-nothing action available, so fixtures can skip through a phase. */
  private static PlayerAction passiveAction(GameState state) {
    return GameEngine.legalActions(state).stream()
        .filter(
            a ->
                a instanceof PlayerAction.PassSettling
                    || a instanceof PlayerAction.PassBuilding
                    || a instanceof PlayerAction.PassCraftsmanBonus
                    || a instanceof PlayerAction.PassTrading
                    || a instanceof PlayerAction.StoreGoods)
        .findFirst()
        .orElseGet(() -> GameEngine.legalActions(state).getFirst());
  }
}
