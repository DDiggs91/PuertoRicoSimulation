package com.PRS.model.phases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.PRS.model.TestGames;
import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.TileType;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.engine.GameEngine;
import com.PRS.model.engine.RejectionReason;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.rolecards.Role;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Builder phase: the one-doubloon privilege, quarry discounts, and city space. */
public class BuilderPhaseTest {

  private static GameState builderPhase(GameState state) {
    return TestGames.chooseRole(state, Role.BUILDER);
  }

  @Test
  public void theBuilderPaysOneDoubloonLessThanEveryoneElse() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).doubloons(10).build());
    state = state.withPlayer(TestGames.player(1).doubloons(10).build());
    state = builderPhase(state);

    state = TestGames.apply(state, new PlayerAction.BuildBuilding(0, BuildingType.HARBOR));
    assertThat(state.player(0).doubloons()).isEqualTo(3); // 8 - 1 privilege

    state = TestGames.apply(state, new PlayerAction.BuildBuilding(1, BuildingType.HARBOR));
    assertThat(state.player(1).doubloons()).isEqualTo(2); // full price
  }

  /**
   * The rulebook's worked example: with three occupied quarries the discount is capped by the
   * building's display column, which is its victory point value.
   */
  @DataProvider(name = "threeQuarryCosts")
  public Object[][] threeQuarryCosts() {
    return new Object[][] {
      {BuildingType.CONSTRUCTION_HUT, 1},
      {BuildingType.OFFICE, 3},
      {BuildingType.HARBOR, 5},
      {BuildingType.CITY_HALL, 7},
    };
  }

  @Test(dataProvider = "threeQuarryCosts")
  public void quarryDiscountsAreCappedByTheBuildingsColumn(BuildingType type, int expected) {
    GameState state = TestGames.newGame(3);
    // Seat 1 so the builder's privilege does not muddy the arithmetic.
    state = state.withPlayer(TestGames.player(1).doubloons(20).staffed(TileType.QUARRY, 3).build());
    state = builderPhase(state);
    state = TestGames.apply(state, new PlayerAction.PassBuilding(0));

    state = TestGames.apply(state, new PlayerAction.BuildBuilding(1, type));
    assertThat(state.player(1).doubloons()).isEqualTo(20 - expected);
  }

  @Test
  public void unstaffedQuarriesGiveNoDiscount() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(
            TestGames.player(1).doubloons(20).tiles(TileType.QUARRY, 3, false).build());
    state = builderPhase(state);
    state = TestGames.apply(state, new PlayerAction.PassBuilding(0));

    state = TestGames.apply(state, new PlayerAction.BuildBuilding(1, BuildingType.HARBOR));
    assertThat(state.player(1).doubloons()).isEqualTo(12);
  }

  @Test
  public void discountsNeverPushACostBelowZero() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).doubloons(0).staffed(TileType.QUARRY, 4).build());
    state = builderPhase(state);

    // Small Market costs 1, less the builder's privilege and a quarry: floored at 0.
    state = TestGames.apply(state, new PlayerAction.BuildBuilding(0, BuildingType.SMALL_MARKET));
    assertThat(state.player(0).doubloons()).isZero();
  }

  /**
   * {@code GameEngine.buildCost} is the read-only window onto the same arithmetic a purchase
   * charges, so a client can print the price without restating the rule. Asserted against the
   * rulebook's three-quarry example above and then against what the build actually deducts.
   */
  @Test(dataProvider = "threeQuarryCosts")
  public void buildCostQuotesWhatThePurchaseWillCharge(BuildingType type, int expected) {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(1).doubloons(20).staffed(TileType.QUARRY, 3).build());
    state = builderPhase(state);
    state = TestGames.apply(state, new PlayerAction.PassBuilding(0));

    assertThat(GameEngine.buildCost(state, type)).isEqualTo(expected);

    GameState built = TestGames.apply(state, new PlayerAction.BuildBuilding(1, type));
    assertThat(built.player(1).doubloons()).isEqualTo(20 - expected);
  }

  @Test
  public void buildCostIncludesTheBuildersOwnPrivilege() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).doubloons(10).build());
    state = builderPhase(state);

    // Seat 0 chose the role, so it pays one less than the printed 8.
    assertThat(GameEngine.buildCost(state, BuildingType.HARBOR)).isEqualTo(7);
  }

  @Test
  public void thereIsNoBuildCostOutsideTheBuilderPhase() {
    GameState state = TestGames.newGame(3);

    assertThatIllegalStateException()
        .isThrownBy(() -> GameEngine.buildCost(state, BuildingType.HARBOR))
        .withMessageContaining("builder phase");
  }

  @Test
  public void aPlayerCannotAffordWhatTheyCannotPayFor() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).doubloons(2).build());
    state = builderPhase(state);

    assertThat(GameEngine.legalActions(state))
        .doesNotContain(new PlayerAction.BuildBuilding(0, BuildingType.WHARF));
    assertThat(
            TestGames.reject(state, new PlayerAction.BuildBuilding(0, BuildingType.WHARF)).reason())
        .isEqualTo(RejectionReason.INSUFFICIENT_DOUBLOONS);
  }

  @Test
  public void noPlayerMayOwnTwoOfTheSameBuilding() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(
            TestGames.player(0).doubloons(20).building(BuildingType.SMALL_MARKET, 0).build());
    state = builderPhase(state);

    assertThat(GameEngine.legalActions(state))
        .doesNotContain(new PlayerAction.BuildBuilding(0, BuildingType.SMALL_MARKET));
    assertThat(
            TestGames.reject(state, new PlayerAction.BuildBuilding(0, BuildingType.SMALL_MARKET))
                .reason())
        .isEqualTo(RejectionReason.DUPLICATE_BUILDING);
  }

  @Test
  public void aLargeBuildingNeedsTwoFreeCitySpaces() {
    GameState state = TestGames.newGame(3);
    // Eleven spaces used, so only one is free.
    var fixture = TestGames.player(0).doubloons(20);
    BuildingType[] fillers = {
      BuildingType.SMALL_MARKET,
      BuildingType.HACIENDA,
      BuildingType.CONSTRUCTION_HUT,
      BuildingType.SMALL_WAREHOUSE,
      BuildingType.HOSPICE,
      BuildingType.OFFICE,
      BuildingType.LARGE_MARKET,
      BuildingType.LARGE_WAREHOUSE,
      BuildingType.FACTORY,
      BuildingType.UNIVERSITY,
      BuildingType.HARBOR
    };
    for (BuildingType filler : fillers) {
      fixture = fixture.building(filler, 0);
    }
    state = state.withPlayer(fixture.build());
    state = builderPhase(state);

    assertThat(state.player(0).freeCitySpaces()).isEqualTo(1);
    assertThat(
            TestGames.reject(state, new PlayerAction.BuildBuilding(0, BuildingType.GUILD_HALL))
                .reason())
        .isEqualTo(RejectionReason.NO_CITY_SPACE);
    // A one-space building still fits.
    assertThat(GameEngine.legalActions(state))
        .contains(new PlayerAction.BuildBuilding(0, BuildingType.WHARF));
  }

  @Test
  public void largeBuildingsConsumeTwoCitySpaces() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).doubloons(20).build());
    state = builderPhase(state);

    state = TestGames.apply(state, new PlayerAction.BuildBuilding(0, BuildingType.GUILD_HALL));
    assertThat(state.player(0).citySpacesUsed()).isEqualTo(2);
    assertThat(state.player(0).freeCitySpaces()).isEqualTo(10);
  }

  @Test
  public void everyCopyOfABuildingCanRunOut() {
    GameState state = TestGames.newGame(3);
    state =
        state.toBuilder()
            .buildings(state.buildings().take(BuildingType.GUILD_HALL))
            .build()
            .withPlayer(TestGames.player(0).doubloons(20).build());
    state = builderPhase(state);

    assertThat(
            TestGames.reject(state, new PlayerAction.BuildBuilding(0, BuildingType.GUILD_HALL))
                .reason())
        .isEqualTo(RejectionReason.BUILDING_UNAVAILABLE);
  }

  @Test
  public void anOccupiedUniversityStaffsEachNewBuilding() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(
            TestGames.player(0).doubloons(20).building(BuildingType.UNIVERSITY).build());
    state = builderPhase(state);
    int supply = state.colonistSupply();

    state = TestGames.apply(state, new PlayerAction.BuildBuilding(0, BuildingType.SUGAR_MILL));

    // One colonist only, however many circles the new building has.
    assertThat(state.player(0).buildings().getLast().colonists()).isEqualTo(1);
    assertThat(state.colonistSupply()).isEqualTo(supply - 1);
  }

  @Test
  public void buildingTheUniversityDoesNotStaffItself() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).doubloons(20).build());
    state = builderPhase(state);

    state = TestGames.apply(state, new PlayerAction.BuildBuilding(0, BuildingType.UNIVERSITY));
    assertThat(state.player(0).buildings().getLast().colonists()).isZero();
  }

  @Test
  public void newBuildingsArriveUnstaffed() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).doubloons(20).build());
    state = builderPhase(state);

    state = TestGames.apply(state, new PlayerAction.BuildBuilding(0, BuildingType.COFFEE_ROASTER));
    assertThat(state.player(0).buildings().getLast().colonists()).isZero();
    assertThat(state.player(0).hasOccupied(BuildingType.COFFEE_ROASTER)).isFalse();
  }

  @Test
  public void everyPlayerGetsOneTurnAndOnlyOne() {
    GameState state = TestGames.newGame(3);
    state = builderPhase(state);
    for (int seat = 0; seat < 3; seat++) {
      assertThat(state.phase().actorSeat()).isEqualTo(seat);
      state = TestGames.apply(state, new PlayerAction.PassBuilding(seat));
    }
    assertThat(state.phase()).isInstanceOf(Phase.RoleSelection.class);
  }

  @Test
  public void fillingTheTwelfthCitySpaceEndsTheGameAfterTheRound() {
    GameState state = TestGames.newGame(3);
    var fixture = TestGames.player(0).doubloons(30);
    BuildingType[] fillers = {
      BuildingType.SMALL_MARKET,
      BuildingType.HACIENDA,
      BuildingType.CONSTRUCTION_HUT,
      BuildingType.SMALL_WAREHOUSE,
      BuildingType.HOSPICE,
      BuildingType.OFFICE,
      BuildingType.LARGE_MARKET,
      BuildingType.LARGE_WAREHOUSE,
      BuildingType.FACTORY,
      BuildingType.UNIVERSITY,
      BuildingType.HARBOR
    };
    for (BuildingType filler : fillers) {
      fixture = fixture.building(filler, 0);
    }
    state = state.withPlayer(fixture.build());
    state = builderPhase(state);
    assertThat(state.finalRound()).isFalse();

    state = TestGames.apply(state, new PlayerAction.BuildBuilding(0, BuildingType.WHARF));

    assertThat(state.player(0).freeCitySpaces()).isZero();
    assertThat(state.finalRound()).isTrue();
    // The round still plays out — the game is not over yet.
    assertThat(state.isOver()).isFalse();
  }
}
