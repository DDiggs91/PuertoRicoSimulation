package com.PRS.model.phases;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.TestGames;
import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.IslandTile;
import com.PRS.model.boards.PlayerState;
import com.PRS.model.boards.TileType;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.buildings.PlacedBuilding;
import com.PRS.model.engine.GameEngine;
import com.PRS.model.engine.RejectionReason;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.rolecards.Role;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

/**
 * Mayor phase: the colonist ship empties out, boards get staffed, then the ship is refilled.
 *
 * <p>Staffing is one action per turn — {@link PlayerAction.SetColonistPlacement} carries the whole
 * finished board — so there is no place/remove/end sequence to drive here; a test builds the
 * arrangement it wants and applies it directly.
 */
public class MayorPhaseTest {

  private static GameState mayorPhase(int players) {
    return TestGames.chooseRole(TestGames.newGame(players), Role.MAYOR);
  }

  /** The board's current occupancy, as a mutable list ready to be edited into a new arrangement. */
  private static List<Boolean> island(PlayerState player) {
    return new ArrayList<>(player.island().stream().map(IslandTile::occupied).toList());
  }

  /** The board's current colonist counts, as a mutable list ready to be edited. */
  private static List<Integer> buildings(PlayerState player) {
    return new ArrayList<>(player.buildings().stream().map(PlacedBuilding::colonists).toList());
  }

  /** Applies the single legal option — the greedy fill — for every seat's turn, in order. */
  private static GameState finishPlacement(GameState state) {
    while (state.phase() instanceof Phase.MayorPhase) {
      state = TestGames.apply(state, GameEngine.legalActions(state).getFirst());
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

  /** A board that was already staffed keeps its staffing; only the new arrivals need placing. */
  @Test
  public void colonistsAlreadyOnTheBoardStayWhereTheyAre() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(
            TestGames.player(0).staffed(TileType.CORN, 1).tiles(TileType.INDIGO, 1, false).build());
    state = TestGames.chooseRole(state, Role.MAYOR);

    assertThat(state.player(0).island().getFirst().occupied()).isTrue();
    assertThat(state.player(0).colonistsInSanJuan()).isEqualTo(2);
  }

  /**
   * The one legal option is a greedy fill of San Juan into vacant circles, island before buildings,
   * in index order — and applying it advances the turn, since it always leaves nothing placeable.
   */
  @Test
  public void theSingleOfferedOptionGreedilyFillsVacantCirclesInOrder() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(
            TestGames.player(0)
                .tiles(TileType.CORN, 3, false)
                .building(BuildingType.SUGAR_MILL, 0)
                .build());
    state = TestGames.chooseRole(state, Role.MAYOR); // 2 colonists arrive in San Juan

    List<PlayerAction> legal = GameEngine.legalActions(state);
    assertThat(legal).hasSize(1);
    PlayerAction.SetColonistPlacement fill = (PlayerAction.SetColonistPlacement) legal.getFirst();
    assertThat(fill.islandOccupied()).containsExactly(true, true, false);
    assertThat(fill.buildingColonists()).containsExactly(0);

    state = TestGames.apply(state, fill);
    assertThat(state.player(0).colonistsInSanJuan()).isZero();
    // Legal by construction, so this seat's turn ends in this one step and the next seat is up.
    assertThat(state.phase().actorSeat()).isEqualTo(1);
  }

  /**
   * One action can both vacate a circle and fill another — a full rearrangement, not a sequence.
   */
  @Test
  public void anArrangementCanMoveAColonistFromOneTileToAnotherInOneStep() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(
            TestGames.player(0).staffed(TileType.CORN, 1).tiles(TileType.INDIGO, 1, false).build());
    state = TestGames.chooseRole(state, Role.MAYOR);
    // Isolate the mechanic under test: pretend San Juan is already empty, so only the move itself —
    // not also placing the colonists this turn dealt — is what the assertion below is about.
    state = state.withPlayer(state.player(0).toBuilder().colonistsInSanJuan(0).build());
    int totalBefore = state.player(0).totalColonists();

    List<Boolean> island = island(state.player(0));
    island.set(0, false);
    island.set(1, true);
    state =
        TestGames.apply(
            state, new PlayerAction.SetColonistPlacement(0, island, buildings(state.player(0))));

    assertThat(state.player(0).island().get(0).occupied()).isFalse();
    assertThat(state.player(0).island().get(1).occupied()).isTrue();
    // Total colonists unchanged: one moved, none created or destroyed.
    assertThat(state.player(0).totalColonists()).isEqualTo(totalBefore);
  }

  @Test
  public void anArrangementOfTheWrongLengthIsRejected() {
    GameState state = mayorPhase(3);
    PlayerState player = state.player(0);

    List<Boolean> tooShortIsland = island(player);
    tooShortIsland.removeFirst();
    assertThat(
            TestGames.reject(
                    state,
                    new PlayerAction.SetColonistPlacement(0, tooShortIsland, buildings(player)))
                .reason())
        .isEqualTo(RejectionReason.INVALID_COLONIST_MOVE);

    List<Integer> tooLongBuildings = buildings(player);
    tooLongBuildings.add(0);
    assertThat(
            TestGames.reject(
                    state,
                    new PlayerAction.SetColonistPlacement(0, island(player), tooLongBuildings))
                .reason())
        .isEqualTo(RejectionReason.INVALID_COLONIST_MOVE);
  }

  @Test
  public void stationingMoreColonistsThanABuildingHasCirclesIsRejected() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(TestGames.player(0).building(BuildingType.SMALL_INDIGO_PLANT, 0).build());
    state = TestGames.chooseRole(state, Role.MAYOR);

    assertThat(
            TestGames.reject(
                    state,
                    new PlayerAction.SetColonistPlacement(
                        0, island(state.player(0)), List.of(2))) // capacity 1
                .reason())
        .isEqualTo(RejectionReason.INVALID_COLONIST_MOVE);
  }

  @Test
  public void anArrangementNeedingMoreColonistsThanAreAvailableIsRejected() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).tiles(TileType.CORN, 2, false).build());
    state = TestGames.chooseRole(state, Role.MAYOR); // 2 in San Juan

    List<Boolean> island = island(state.player(0));
    island.set(0, true);
    island.set(1, true);
    // Both circles filled costs 2 colonists, which is all San Juan holds — legal — but this player
    // also owns nothing else to place a third colonist onto, so demanding one more must fail rather
    // than materialize one.
    GameState overStaffed =
        state.withPlayer(state.player(0).toBuilder().colonistsInSanJuan(1).build());
    assertThat(
            TestGames.reject(
                    overStaffed, new PlayerAction.SetColonistPlacement(0, island, List.of()))
                .reason())
        .isEqualTo(RejectionReason.INVALID_COLONIST_MOVE);
  }

  @Test
  public void colonistsLeftInSanJuanWhileACircleIsStillEmptyIsRejected() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).tiles(TileType.CORN, 2, false).build());
    state = TestGames.chooseRole(state, Role.MAYOR); // 2 in San Juan, 2 empty tiles

    // Only one of the two placed: the other could have been, so this must be refused.
    List<Boolean> island = island(state.player(0));
    island.set(0, true);
    assertThat(
            TestGames.reject(state, new PlayerAction.SetColonistPlacement(0, island, List.of()))
                .reason())
        .isEqualTo(RejectionReason.COLONISTS_UNPLACED);
  }

  @Test
  public void colonistsMayStayInSanJuanOnceTheBoardHasNoRoomLeft() {
    GameState state = TestGames.newGame(3);
    // One tile for two colonists: the second has nowhere to go.
    state = state.withPlayer(TestGames.player(0).tiles(TileType.CORN, 1, false).build());
    state = TestGames.chooseRole(state, Role.MAYOR);

    List<Boolean> island = island(state.player(0));
    island.set(0, true);
    state = TestGames.apply(state, new PlayerAction.SetColonistPlacement(0, island, List.of()));

    assertThat(state.player(0).colonistsInSanJuan()).isEqualTo(1);
    // No room left, so this seat may stop even with a colonist still in San Juan, and the turn
    // passes on.
    assertThat(state.phase().actorSeat()).isEqualTo(1);
  }

  @Test
  public void aNegativeBuildingColonistCountIsRejected() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).building(BuildingType.SUGAR_MILL, 1).build());
    state = TestGames.chooseRole(state, Role.MAYOR);

    assertThat(
            TestGames.reject(
                    state,
                    new PlayerAction.SetColonistPlacement(0, island(state.player(0)), List.of(-1)))
                .reason())
        .isEqualTo(RejectionReason.INVALID_COLONIST_MOVE);
  }

  @Test
  public void submittingAnyOtherActionInTheMayorPhaseIsRejected() {
    GameState state = mayorPhase(3);

    assertThat(TestGames.reject(state, new PlayerAction.PassBuilding(0)).reason())
        .isEqualTo(RejectionReason.WRONG_PHASE);
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
