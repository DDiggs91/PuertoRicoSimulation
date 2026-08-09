package com.PRS.model.phases;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.TestGames;
import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.IslandTile;
import com.PRS.model.boards.TileType;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.engine.ActionResult;
import com.PRS.model.engine.GameEngine;
import com.PRS.model.engine.RejectionReason;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.rolecards.Role;
import org.testng.annotations.Test;

/** Settler phase: plantations, the quarry privilege, and the Hacienda/Hospice/Construction Hut. */
public class SettlerPhaseTest {

  private static GameState settlerPhase(int players) {
    return TestGames.chooseRole(TestGames.newGame(players), Role.SETTLER);
  }

  @Test
  public void theSettlerActsFirstAndTheRestFollowClockwise() {
    GameState state = settlerPhase(4);
    for (int expected = 0; expected < 4; expected++) {
      assertThat(state.phase()).isInstanceOf(Phase.SettlerPhase.class);
      assertThat(state.phase().actorSeat()).isEqualTo(expected);
      state = TestGames.apply(state, new PlayerAction.PassSettling(expected));
    }
    assertThat(state.phase()).isInstanceOf(Phase.RoleSelection.class);
  }

  @Test
  public void takingAFaceUpTilePutsItOnTheIslandAndRemovesItFromTheRow() {
    GameState state = settlerPhase(3);
    TileType taken = state.tiles().faceUp().getFirst();
    int rowSize = state.tiles().faceUp().size();

    state = TestGames.apply(state, new PlayerAction.TakeFaceUpTile(0, 0));

    assertThat(state.tiles().faceUp()).hasSize(rowSize - 1);
    assertThat(state.player(0).island().getLast().type()).isEqualTo(taken);
    // Nothing staffs a fresh tile without a Hospice.
    assertThat(state.player(0).island().getLast().occupied()).isFalse();
  }

  @Test
  public void onlyTheSettlerMayTakeAQuarry() {
    GameState state = settlerPhase(3);
    assertThat(GameEngine.legalActions(state)).contains(new PlayerAction.TakeQuarry(0));

    state = TestGames.apply(state, new PlayerAction.PassSettling(0));

    assertThat(GameEngine.legalActions(state)).doesNotContain(new PlayerAction.TakeQuarry(1));
    ActionResult.Rejected rejected = TestGames.reject(state, new PlayerAction.TakeQuarry(1));
    assertThat(rejected.reason()).isEqualTo(RejectionReason.QUARRY_NOT_ALLOWED);
  }

  @Test
  public void takingAQuarryDrawsItFromTheQuarryStack() {
    GameState state = settlerPhase(3);
    int quarries = state.tiles().quarriesRemaining();

    state = TestGames.apply(state, new PlayerAction.TakeQuarry(0));

    assertThat(state.tiles().quarriesRemaining()).isEqualTo(quarries - 1);
    assertThat(state.player(0).island().getLast().type()).isEqualTo(TileType.QUARRY);
  }

  @Test
  public void anOccupiedConstructionHutLetsANonSettlerTakeAQuarry() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(1).building(BuildingType.CONSTRUCTION_HUT).build());
    state = TestGames.chooseRole(state, Role.SETTLER);
    state = TestGames.apply(state, new PlayerAction.PassSettling(0));

    assertThat(GameEngine.legalActions(state)).contains(new PlayerAction.TakeQuarry(1));
    state = TestGames.apply(state, new PlayerAction.TakeQuarry(1));
    assertThat(state.player(1).island().getLast().type()).isEqualTo(TileType.QUARRY);
  }

  @Test
  public void anUnstaffedConstructionHutDoesNothing() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(TestGames.player(1).building(BuildingType.CONSTRUCTION_HUT, 0).build());
    state = TestGames.chooseRole(state, Role.SETTLER);
    state = TestGames.apply(state, new PlayerAction.PassSettling(0));

    assertThat(GameEngine.legalActions(state)).doesNotContain(new PlayerAction.TakeQuarry(1));
  }

  @Test
  public void withNoQuarriesLeftEvenTheSettlerGoesWithout() {
    GameState state = TestGames.newGame(3);
    state = state.withTiles(state.tiles().toBuilder().quarriesRemaining(0).build());
    state = TestGames.chooseRole(state, Role.SETTLER);

    assertThat(GameEngine.legalActions(state)).doesNotContain(new PlayerAction.TakeQuarry(0));
  }

  @Test
  public void aFullIslandTakesNoMoreTiles() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).tiles(TileType.CORN, 12, false).build());
    state = TestGames.chooseRole(state, Role.SETTLER);

    assertThat(GameEngine.legalActions(state)).containsExactly(new PlayerAction.PassSettling(0));
    assertThat(TestGames.reject(state, new PlayerAction.TakeFaceUpTile(0, 0)).reason())
        .isEqualTo(RejectionReason.NO_ISLAND_SPACE);
  }

  @Test
  public void theRowIsDealtBackToOneMoreThanPlayersAtTheEndOfThePhase() {
    GameState state = settlerPhase(3);
    state = TestGames.apply(state, new PlayerAction.TakeFaceUpTile(0, 0));
    state = TestGames.apply(state, new PlayerAction.TakeFaceUpTile(1, 0));
    assertThat(state.tiles().faceUp()).hasSize(2);

    state = TestGames.apply(state, new PlayerAction.PassSettling(2));

    assertThat(state.tiles().faceUp()).hasSize(4);
  }

  @Test
  public void untakenTilesAreDiscardedRatherThanKept() {
    GameState state = settlerPhase(3);
    for (int seat = 0; seat < 3; seat++) {
      state = TestGames.apply(state, new PlayerAction.PassSettling(seat));
    }
    // The four tiles nobody took are now on the discard pile.
    assertThat(state.tiles().discardPile()).hasSize(4);
  }

  @Test
  public void anOccupiedHospiceStaffsEachNewTile() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).building(BuildingType.HOSPICE).build());
    state = TestGames.chooseRole(state, Role.SETTLER);
    int supply = state.colonistSupply();

    state = TestGames.apply(state, new PlayerAction.TakeFaceUpTile(0, 0));

    assertThat(state.player(0).island().getLast().occupied()).isTrue();
    assertThat(state.colonistSupply()).isEqualTo(supply - 1);
  }

  @Test
  public void anOccupiedHaciendaOffersAFaceDownTileBeforeTheNormalPick() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).building(BuildingType.HACIENDA).build());
    state = TestGames.chooseRole(state, Role.SETTLER);

    assertThat(GameEngine.legalActions(state))
        .containsExactlyInAnyOrder(
            new PlayerAction.TakeHaciendaTile(0), new PlayerAction.SkipHacienda(0));

    state = TestGames.apply(state, new PlayerAction.TakeHaciendaTile(0));

    assertThat(state.player(0).island()).hasSize(1);
    // Still seat 0's turn — the extra tile does not replace the normal pick.
    assertThat(state.phase().actorSeat()).isZero();
    assertThat(GameEngine.legalActions(state)).contains(new PlayerAction.TakeFaceUpTile(0, 0));

    state = TestGames.apply(state, new PlayerAction.TakeFaceUpTile(0, 0));
    assertThat(state.player(0).island()).hasSize(2);
  }

  @Test
  public void theHaciendaTileNeverGetsAHospiceColonist() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(
            TestGames.player(0)
                .building(BuildingType.HACIENDA)
                .building(BuildingType.HOSPICE)
                .build());
    state = TestGames.chooseRole(state, Role.SETTLER);

    state = TestGames.apply(state, new PlayerAction.TakeHaciendaTile(0));
    assertThat(state.player(0).island().getLast().occupied()).isFalse();

    // The normal pick still earns one.
    state = TestGames.apply(state, new PlayerAction.TakeFaceUpTile(0, 0));
    assertThat(state.player(0).island().getLast().occupied()).isTrue();
  }

  @Test
  public void skippingTheHaciendaGoesStraightToTheNormalPick() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).building(BuildingType.HACIENDA).build());
    state = TestGames.chooseRole(state, Role.SETTLER);

    state = TestGames.apply(state, new PlayerAction.SkipHacienda(0));

    assertThat(state.player(0).island()).isEmpty();
    assertThat(GameEngine.legalActions(state)).contains(new PlayerAction.TakeQuarry(0));
  }

  @Test
  public void aPlayerWithoutAHaciendaIsNeverOfferedTheDraw() {
    GameState state = settlerPhase(3);
    assertThat(GameEngine.legalActions(state))
        .noneMatch(a -> a instanceof PlayerAction.TakeHaciendaTile);
    assertThat(TestGames.reject(state, new PlayerAction.TakeHaciendaTile(0)).reason())
        .isEqualTo(RejectionReason.WRONG_PHASE);
  }

  @Test
  public void tilesAlreadyOnAnIslandStayPutAcrossPhases() {
    GameState state = TestGames.newGame(3);
    IslandTile existing = state.player(0).island().getFirst();
    state = TestGames.chooseRole(state, Role.SETTLER);
    state = TestGames.apply(state, new PlayerAction.TakeFaceUpTile(0, 0));

    assertThat(state.player(0).island().getFirst()).isEqualTo(existing);
  }
}
