package com.PRS.model.phases;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.TestGames;
import com.PRS.model.actions.ColonistSlot;
import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.PlayerState;
import com.PRS.model.boards.TileType;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.engine.GameEngine;
import com.PRS.model.engine.RejectionReason;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.rolecards.Role;
import java.util.List;
import org.testng.annotations.Test;

/** Mayor phase: the colonist ship empties out, boards get staffed, then the ship is refilled. */
public class MayorPhaseTest {

  private static GameState mayorPhase(int players) {
    return TestGames.chooseRole(TestGames.newGame(players), Role.MAYOR);
  }

  /** Places every colonist somewhere — players may not stop while San Juan still holds any. */
  private static GameState finishPlacement(GameState state) {
    while (state.phase() instanceof Phase.MayorPhase) {
      List<PlayerAction> legal = GameEngine.legalActions(state);
      PlayerAction action =
          legal.stream()
              .filter(a -> a instanceof PlayerAction.EndColonistPlacement)
              .findFirst()
              .orElseGet(legal::getFirst);
      state = TestGames.apply(state, action);
    }
    return state;
  }

  @Test
  public void theMayorTakesAnExtraColonistStraightFromTheSupply() {
    GameState before = TestGames.newGame(3);
    int supply = before.colonistSupply();
    int onShip = before.colonistsOnShip();

    GameState state = TestGames.chooseRole(before, Role.MAYOR);

    // Three off the ship shared out one each, plus one privilege colonist for the mayor.
    assertThat(state.colonistSupply()).isEqualTo(supply - 1);
    assertThat(state.colonistsOnShip()).isZero();
    assertThat(state.player(0).colonistsInSanJuan()).isEqualTo(2);
    assertThat(state.player(1).colonistsInSanJuan()).isEqualTo(1);
    assertThat(state.player(2).colonistsInSanJuan()).isEqualTo(1);
    assertThat(onShip).isEqualTo(3);
  }

  @Test
  public void shipColonistsAreDealtOneAtATimeClockwiseFromTheMayor() {
    // Seven colonists over four players: 2, 2, 2, 1 starting from the mayor.
    GameState state = TestGames.newGame(4).toBuilder().colonistsOnShip(7).build();
    state = TestGames.chooseRole(state, Role.MAYOR);

    assertThat(state.player(0).colonistsInSanJuan()).isEqualTo(3); // 2 dealt + 1 privilege
    assertThat(state.player(1).colonistsInSanJuan()).isEqualTo(2);
    assertThat(state.player(2).colonistsInSanJuan()).isEqualTo(2);
    assertThat(state.player(3).colonistsInSanJuan()).isEqualTo(1);
  }

  @Test
  public void colonistsMoveFromSanJuanOntoEmptyCircles() {
    GameState state = mayorPhase(3);
    assertThat(state.player(0).colonistsInSanJuan()).isEqualTo(2);

    state = TestGames.apply(state, new PlayerAction.PlaceColonist(0, new ColonistSlot.Island(0)));

    assertThat(state.player(0).island().getFirst().occupied()).isTrue();
    assertThat(state.player(0).colonistsInSanJuan()).isEqualTo(1);
  }

  /**
   * Colonists already on the board are lifted back to San Juan when a player's turn starts, so a
   * rearrangement is just a fresh placement.
   */
  @Test
  public void placedColonistsAreLiftedSoTheyCanBeRearranged() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(
            TestGames.player(0).staffed(TileType.CORN, 1).tiles(TileType.INDIGO, 1, false).build());
    state = TestGames.chooseRole(state, Role.MAYOR);

    // The corn colonist is back in the pool alongside the two new arrivals.
    assertThat(state.player(0).colonistsInSanJuan()).isEqualTo(3);
    assertThat(state.player(0).island()).noneMatch(t -> t.occupied());

    // It can now go on the indigo tile instead.
    state = TestGames.apply(state, new PlayerAction.PlaceColonist(0, new ColonistSlot.Island(1)));
    assertThat(state.player(0).island().get(1).occupied()).isTrue();
    assertThat(state.player(0).island().getFirst().occupied()).isFalse();
  }

  @Test
  public void liftingOnlyTouchesThePlayerWhoseTurnItIs() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(1).staffed(TileType.CORN, 1).build());
    state = TestGames.chooseRole(state, Role.MAYOR);

    // Seat 0 is placing; seat 1's colonist stays put until their own turn comes round.
    assertThat(state.phase().actorSeat()).isZero();
    assertThat(state.player(1).island().getFirst().occupied()).isTrue();
  }

  @Test
  public void placementAlwaysTerminates() {
    GameState state = mayorPhase(3);
    // Every legal move empties San Juan by one, so taking the first one always runs down.
    int steps = 0;
    while (state.phase() instanceof Phase.MayorPhase && steps++ < 200) {
      state = TestGames.apply(state, GameEngine.legalActions(state).getFirst());
    }
    assertThat(state.phase()).isNotInstanceOf(Phase.MayorPhase.class);
  }

  @Test
  public void aPlayerMayNotStopWhileColonistsSitInSanJuanAndCirclesAreEmpty() {
    GameState state = mayorPhase(3);
    assertThat(state.player(0).colonistsInSanJuan()).isEqualTo(2);
    assertThat(state.player(0).emptyCircles()).isPositive();

    assertThat(GameEngine.legalActions(state))
        .noneMatch(a -> a instanceof PlayerAction.EndColonistPlacement);
    assertThat(TestGames.reject(state, new PlayerAction.EndColonistPlacement(0)).reason())
        .isEqualTo(RejectionReason.COLONISTS_UNPLACED);
  }

  @Test
  public void aPlayerWithNowhereLeftToPutAnyoneMayStop() {
    GameState state = TestGames.newGame(3);
    // A single tile for three colonists, so two of them have nowhere to go.
    state = state.withPlayer(TestGames.player(0).staffed(TileType.CORN, 1).build());
    state = TestGames.chooseRole(state, Role.MAYOR);
    state = TestGames.apply(state, new PlayerAction.PlaceColonist(0, new ColonistSlot.Island(0)));

    assertThat(state.player(0).emptyCircles()).isZero();
    assertThat(state.player(0).colonistsInSanJuan()).isEqualTo(2);
    assertThat(GameEngine.legalActions(state)).contains(new PlayerAction.EndColonistPlacement(0));
  }

  @Test
  public void placingIntoAFullBuildingIsRefused() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).building(BuildingType.SMALL_INDIGO_PLANT).build());
    state = TestGames.chooseRole(state, Role.MAYOR);
    // Fill the plant's single circle, then try to squeeze in another.
    state = TestGames.apply(state, new PlayerAction.PlaceColonist(0, new ColonistSlot.Building(0)));

    assertThat(
            TestGames.reject(state, new PlayerAction.PlaceColonist(0, new ColonistSlot.Building(0)))
                .reason())
        .isEqualTo(RejectionReason.INVALID_COLONIST_MOVE);
  }

  @Test
  public void anOutOfRangeSlotIsRejectedRatherThanThrown() {
    GameState state = mayorPhase(3);

    assertThat(
            TestGames.reject(state, new PlayerAction.PlaceColonist(0, new ColonistSlot.Island(99)))
                .reason())
        .isEqualTo(RejectionReason.INVALID_COLONIST_MOVE);
  }

  @Test
  public void theShipIsRefilledOnePerEmptyBuildingCircleWithAFloorOfOnePerPlayer() {
    GameState state = TestGames.newGame(3);
    // Eight circles on the mayor's board, only two colonists to fill them with.
    state =
        state.withPlayer(
            TestGames.player(0)
                .building(BuildingType.SUGAR_MILL, 0)
                .building(BuildingType.TOBACCO_STORAGE, 0)
                .building(BuildingType.COFFEE_ROASTER, 0)
                .build());
    state = TestGames.chooseRole(state, Role.MAYOR);
    state = finishPlacement(state);

    // Six circles still empty, comfortably above the three-player floor.
    assertThat(state.colonistsOnShip()).isEqualTo(6);
  }

  @Test
  public void plantationCirclesNeverCountTowardsTheRefill() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).tiles(TileType.CORN, 8, false).build());
    state = TestGames.chooseRole(state, Role.MAYOR);
    state = finishPlacement(state);

    // Eight bare plantations, no buildings anywhere: only the per-player floor applies.
    assertThat(state.colonistsOnShip()).isEqualTo(3);
  }

  @Test
  public void aShortSupplyLoadsWhatIsLeftAndEndsTheGameAfterThisRound() {
    GameState state = TestGames.newGame(3).toBuilder().colonistSupply(2).build();
    state = TestGames.chooseRole(state, Role.MAYOR);
    state = finishPlacement(state);

    // Two left, minus the mayor's privilege colonist, leaves one for a ship that wanted three.
    assertThat(state.colonistsOnShip()).isEqualTo(1);
    assertThat(state.colonistSupply()).isZero();
    assertThat(state.finalRound()).isTrue();
  }

  @Test
  public void anExhaustedSupplyDeniesTheMayorTheirPrivilege() {
    GameState state = TestGames.newGame(3).toBuilder().colonistSupply(0).build();
    state = TestGames.chooseRole(state, Role.MAYOR);

    // Only the three off the ship are shared out; the mayor gets no extra.
    assertThat(state.players()).extracting(PlayerState::colonistsInSanJuan).containsOnly(1);
  }
}
