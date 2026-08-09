package com.PRS.model.phases;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.TestGames;
import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.CargoShip;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.engine.GameEngine;
import com.PRS.model.engine.RejectionReason;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.goods.Good;
import com.PRS.model.rolecards.Role;
import java.util.List;
import org.testng.annotations.Test;

/** Captain phase: compulsory shipping, ship selection, the Wharf/Harbor, and goods storage. */
public class CaptainPhaseTest {

  /** Replaces the fleet so a test can pin down exact capacities. */
  private static GameState withShips(GameState state, int... capacities) {
    List<CargoShip> ships = new java.util.ArrayList<>();
    for (int capacity : capacities) {
      ships.add(CargoShip.empty(capacity));
    }
    return state.toBuilder().ships(ships).build();
  }

  private static GameState storeEverything(GameState state) {
    while (state.phase() instanceof Phase.CaptainStorage) {
      state = TestGames.apply(state, GameEngine.legalActions(state).getFirst());
    }
    return state;
  }

  @Test
  public void loadingIsCompulsoryAndThereIsNoPassAction() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).goods(Good.CORN, 2).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    assertThat(state.phase()).isInstanceOf(Phase.CaptainLoading.class);
    assertThat(GameEngine.legalActions(state))
        .isNotEmpty()
        .allMatch(a -> a instanceof PlayerAction.LoadShip);
  }

  @Test
  public void playersWithNothingLoadableAreSkipped() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(1).goods(Good.CORN, 2).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    // Seat 0 is captain but holds nothing, so seat 1 is straight up.
    assertThat(state.phase().actorSeat()).isEqualTo(1);
  }

  @Test
  public void aPlayerLoadsEveryBarrelOfTheKindThatWillFit() {
    GameState state = withShips(TestGames.newGame(3), 4, 5, 6);
    state = state.withPlayer(TestGames.player(0).goods(Good.CORN, 3).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    state = TestGames.apply(state, new PlayerAction.LoadShip(0, 0, Good.CORN));

    assertThat(state.ships().getFirst().loaded()).isEqualTo(3);
    assertThat(state.ships().getFirst().cargoKind()).contains(Good.CORN);
    assertThat(state.player(0).goodsCount(Good.CORN)).isZero();
  }

  @Test
  public void oneVictoryPointPerBarrelWhateverTheKind() {
    GameState state = withShips(TestGames.newGame(3), 4, 5, 6);
    // Seat 1 loads, so the captain's own bonus is not in play.
    state = state.withPlayer(TestGames.player(1).goods(Good.COFFEE, 3).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    state = TestGames.apply(state, new PlayerAction.LoadShip(1, 0, Good.COFFEE));
    assertThat(state.player(1).victoryPoints()).isEqualTo(3);
  }

  @Test
  public void theCaptainEarnsOneBonusPointOnTheirFirstLoadOnly() {
    GameState state = withShips(TestGames.newGame(3), 4, 5, 6);
    state = state.withPlayer(TestGames.player(0).goods(Good.CORN, 2).goods(Good.SUGAR, 2).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    state = TestGames.apply(state, new PlayerAction.LoadShip(0, 0, Good.CORN));
    assertThat(state.player(0).victoryPoints()).isEqualTo(3); // 2 barrels + 1 bonus

    state = TestGames.apply(state, new PlayerAction.LoadShip(0, 1, Good.SUGAR));
    assertThat(state.player(0).victoryPoints()).isEqualTo(5); // 2 more barrels, no second bonus
  }

  @Test
  public void aShipCarriesOnlyOneKind() {
    GameState state = TestGames.newGame(3);
    // A corn ship with room to spare, plus an empty one so loading is still possible.
    state =
        state.toBuilder()
            .ships(List.of(CargoShip.empty(6).load(Good.CORN, 2), CargoShip.empty(4)))
            .build()
            .withPlayer(TestGames.player(0).goods(Good.SUGAR, 2).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    assertThat(TestGames.reject(state, new PlayerAction.LoadShip(0, 0, Good.SUGAR)).reason())
        .isEqualTo(RejectionReason.SHIP_UNAVAILABLE);
    assertThat(GameEngine.legalActions(state))
        .containsExactly(new PlayerAction.LoadShip(0, 1, Good.SUGAR));
  }

  @Test
  public void aNonsenseShipIndexIsRejectedRatherThanThrown() {
    GameState state = withShips(TestGames.newGame(3), 4, 5, 6);
    state = state.withPlayer(TestGames.player(0).goods(Good.CORN, 2).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    assertThat(TestGames.reject(state, new PlayerAction.LoadShip(0, 99, Good.CORN)).reason())
        .isEqualTo(RejectionReason.SHIP_UNAVAILABLE);
  }

  @Test
  public void aKindAlreadyAtSeaCannotBeStartedOnAnotherShip() {
    GameState state = TestGames.newGame(3);
    state =
        state.toBuilder()
            .ships(List.of(CargoShip.empty(4).load(Good.CORN, 4), CargoShip.empty(6)))
            .build()
            .withPlayer(TestGames.player(0).goods(Good.CORN, 3).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    // The corn ship is full and corn may not open a second one, so nobody can load.
    assertThat(state.phase()).isNotInstanceOf(Phase.CaptainLoading.class);
  }

  /** The rulebook's example: 6 sugar with a 5 and a 7 free must go on the 7. */
  @Test
  public void aPlayerMustPickTheShipThatTakesTheMostBarrels() {
    GameState state = withShips(TestGames.newGame(3), 5, 7);
    state = state.withPlayer(TestGames.player(0).goods(Good.SUGAR, 6).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    assertThat(GameEngine.legalActions(state))
        .containsExactly(new PlayerAction.LoadShip(0, 1, Good.SUGAR));
    assertThat(TestGames.reject(state, new PlayerAction.LoadShip(0, 0, Good.SUGAR)).reason())
        .isEqualTo(RejectionReason.SUBOPTIMAL_SHIP);

    state = TestGames.apply(state, new PlayerAction.LoadShip(0, 1, Good.SUGAR));
    assertThat(state.player(0).victoryPoints()).isEqualTo(7); // 6 barrels + captain bonus
  }

  @Test
  public void eitherShipIsFineWhenBothWouldTakeEverything() {
    GameState state = withShips(TestGames.newGame(3), 5, 7);
    state = state.withPlayer(TestGames.player(0).goods(Good.SUGAR, 3).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    assertThat(GameEngine.legalActions(state))
        .containsExactlyInAnyOrder(
            new PlayerAction.LoadShip(0, 0, Good.SUGAR),
            new PlayerAction.LoadShip(0, 1, Good.SUGAR));
  }

  @Test
  public void whichKindToLoadIsAFreeChoice() {
    GameState state = withShips(TestGames.newGame(3), 4, 5, 6);
    state = state.withPlayer(TestGames.player(0).goods(Good.CORN, 1).goods(Good.SUGAR, 4).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    // Loading the single corn is legal even though sugar would ship more.
    assertThat(GameEngine.legalActions(state))
        .anyMatch(a -> a instanceof PlayerAction.LoadShip load && load.good() == Good.CORN);
  }

  @Test
  public void turnsCycleUntilNobodyCanLoad() {
    GameState state = withShips(TestGames.newGame(3), 4, 5, 6);
    state = state.withPlayer(TestGames.player(0).goods(Good.CORN, 2).build());
    state = state.withPlayer(TestGames.player(1).goods(Good.SUGAR, 2).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    state = TestGames.apply(state, new PlayerAction.LoadShip(0, 0, Good.CORN));
    assertThat(state.phase().actorSeat()).isEqualTo(1);
    state = TestGames.apply(state, new PlayerAction.LoadShip(1, 1, Good.SUGAR));

    // Both are empty-handed, so loading is over.
    assertThat(state.phase()).isNotInstanceOf(Phase.CaptainLoading.class);
  }

  @Test
  public void anOccupiedHarborAddsAPointToEveryDelivery() {
    GameState state = withShips(TestGames.newGame(3), 4, 5, 6);
    state =
        state.withPlayer(
            TestGames.player(1)
                .goods(Good.CORN, 2)
                .goods(Good.SUGAR, 2)
                .building(BuildingType.HARBOR)
                .build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    state = TestGames.apply(state, new PlayerAction.LoadShip(1, 0, Good.CORN));
    assertThat(state.player(1).victoryPoints()).isEqualTo(3); // 2 barrels + 1 harbor

    state = TestGames.apply(state, new PlayerAction.LoadShip(1, 1, Good.SUGAR));
    assertThat(state.player(1).victoryPoints()).isEqualTo(6); // and again
  }

  @Test
  public void theWharfShipsAKindStraightToTheSupply() {
    GameState state = withShips(TestGames.newGame(3), 4);
    state =
        state.toBuilder()
            .ships(List.of(CargoShip.empty(4).load(Good.CORN, 4)))
            .build()
            .withPlayer(
                TestGames.player(1).goods(Good.TOBACCO, 5).building(BuildingType.WHARF).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    assertThat(GameEngine.legalActions(state))
        .contains(new PlayerAction.LoadWharf(1, Good.TOBACCO));

    int supply = state.goods().available(Good.TOBACCO);
    state = TestGames.apply(state, new PlayerAction.LoadWharf(1, Good.TOBACCO));

    assertThat(state.player(1).goodsCount(Good.TOBACCO)).isZero();
    assertThat(state.goods().available(Good.TOBACCO)).isEqualTo(supply + 5);
    assertThat(state.player(1).victoryPoints()).isEqualTo(5);
  }

  @Test
  public void theWharfIsUsableOnlyOncePerCaptainPhase() {
    GameState state = withShips(TestGames.newGame(3), 4);
    state =
        state.toBuilder()
            .ships(List.of(CargoShip.empty(4).load(Good.CORN, 4)))
            .build()
            .withPlayer(
                TestGames.player(0)
                    .goods(Good.TOBACCO, 2)
                    .goods(Good.COFFEE, 2)
                    .building(BuildingType.WHARF)
                    .build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    state = TestGames.apply(state, new PlayerAction.LoadWharf(0, Good.TOBACCO));

    // Coffee remains, but the Wharf is spent and no ship will take it.
    assertThat(state.player(0).goodsCount(Good.COFFEE)).isEqualTo(2);
    assertThat(state.phase()).isNotInstanceOf(Phase.CaptainLoading.class);
  }

  @Test
  public void anUnstaffedWharfDoesNothing() {
    GameState state = TestGames.newGame(3);
    state =
        state.toBuilder()
            .ships(List.of(CargoShip.empty(4).load(Good.CORN, 4)))
            .build()
            .withPlayer(
                TestGames.player(0).goods(Good.TOBACCO, 2).building(BuildingType.WHARF, 0).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    assertThat(state.phase()).isNotInstanceOf(Phase.CaptainLoading.class);
  }

  @Test
  public void fullShipsAreEmptiedAtTheEndOfThePhaseAndPartialOnesAreNot() {
    GameState state = withShips(TestGames.newGame(3), 2, 5);
    state = state.withPlayer(TestGames.player(0).goods(Good.CORN, 2).build());
    state = state.withPlayer(TestGames.player(1).goods(Good.SUGAR, 2).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    state = TestGames.apply(state, new PlayerAction.LoadShip(0, 0, Good.CORN));
    state = TestGames.apply(state, new PlayerAction.LoadShip(1, 1, Good.SUGAR));
    state = storeEverything(state);

    assertThat(state.ships().getFirst().isEmpty()).as("full ship emptied").isTrue();
    assertThat(state.ships().get(1).loaded()).as("partial ship kept").isEqualTo(2);
  }

  @Test
  public void aPlayerKeepsOneLooseBarrelAndLosesTheRest() {
    GameState state = TestGames.newGame(3);
    // Every ship is full of corn, so the tobacco cannot be loaded at all.
    state =
        state.toBuilder()
            .ships(List.of(CargoShip.empty(4).load(Good.CORN, 4)))
            .build()
            .withPlayer(TestGames.player(0).goods(Good.TOBACCO, 3).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    assertThat(state.phase()).isInstanceOf(Phase.CaptainStorage.class);
    state = TestGames.apply(state, new PlayerAction.StoreGoods(0, List.of(), Good.TOBACCO));

    assertThat(state.player(0).goodsCount(Good.TOBACCO)).isEqualTo(1);
  }

  @Test
  public void warehousesProtectWholeKinds() {
    GameState state = TestGames.newGame(3);
    state =
        state.toBuilder()
            .ships(List.of(CargoShip.empty(4).load(Good.CORN, 4)))
            .build()
            .withPlayer(
                TestGames.player(0)
                    .goods(Good.TOBACCO, 3)
                    .goods(Good.COFFEE, 2)
                    .goods(Good.SUGAR, 2)
                    .building(BuildingType.SMALL_WAREHOUSE)
                    .building(BuildingType.LARGE_WAREHOUSE)
                    .build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    state =
        TestGames.apply(
            state,
            new PlayerAction.StoreGoods(0, List.of(Good.TOBACCO, Good.COFFEE, Good.SUGAR), null));

    // Three kinds protected by the two warehouses together.
    assertThat(state.player(0).goodsCount(Good.TOBACCO)).isEqualTo(3);
    assertThat(state.player(0).goodsCount(Good.COFFEE)).isEqualTo(2);
    assertThat(state.player(0).goodsCount(Good.SUGAR)).isEqualTo(2);
  }

  @Test
  public void storingMoreKindsThanTheWarehousesCoverIsRefused() {
    GameState state = TestGames.newGame(3);
    state =
        state.toBuilder()
            .ships(List.of(CargoShip.empty(4).load(Good.CORN, 4)))
            .build()
            .withPlayer(TestGames.player(0).goods(Good.TOBACCO, 3).goods(Good.COFFEE, 2).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    assertThat(
            TestGames.reject(
                    state, new PlayerAction.StoreGoods(0, List.of(Good.TOBACCO, Good.COFFEE), null))
                .reason())
        .isEqualTo(RejectionReason.INVALID_STORAGE);
  }

  @Test
  public void goodsThatFallOutOfStorageGoBackToTheSupply() {
    GameState state = TestGames.newGame(3);
    state =
        state.toBuilder()
            .ships(List.of(CargoShip.empty(4).load(Good.CORN, 4)))
            .build()
            .withPlayer(TestGames.player(0).goods(Good.TOBACCO, 3).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);
    int supply = state.goods().available(Good.TOBACCO);

    state = TestGames.apply(state, new PlayerAction.StoreGoods(0, List.of(), Good.TOBACCO));

    assertThat(state.goods().available(Good.TOBACCO)).isEqualTo(supply + 2);
  }

  @Test
  public void anEmptiedVictoryPointSupplyEndsTheGameButKeepsPayingOut() {
    GameState state = withShips(TestGames.newGame(3), 6);
    state =
        state.toBuilder()
            .victoryPointSupply(2)
            .build()
            .withPlayer(TestGames.player(0).goods(Good.CORN, 5).build());
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    state = TestGames.apply(state, new PlayerAction.LoadShip(0, 0, Good.CORN));

    // Six points earned against a pool of two: the player still banks all six.
    assertThat(state.player(0).victoryPoints()).isEqualTo(6);
    assertThat(state.victoryPointSupply()).isZero();
    assertThat(state.finalRound()).isTrue();
    assertThat(state.isOver()).isFalse();
  }

  @Test
  public void nobodyHoldingGoodsMeansThePhasePassesStraightThrough() {
    GameState state = TestGames.newGame(3);
    state = TestGames.chooseRole(state, Role.CAPTAIN);

    assertThat(state.phase()).isInstanceOf(Phase.RoleSelection.class);
  }
}
