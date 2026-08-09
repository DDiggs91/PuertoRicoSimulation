package com.PRS.model.phases;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.TestGames;
import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.TileType;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.engine.GameEngine;
import com.PRS.model.engine.RejectionReason;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.goods.Good;
import com.PRS.model.goods.GoodsSupply;
import com.PRS.model.rolecards.Role;
import java.util.EnumMap;
import java.util.Map;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Craftsman phase: production, the bonus barrel, and the Factory. */
public class CraftsmanPhaseTest {

  @Test
  public void staffedCornPlantationsProduceWithoutAnyBuilding() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).staffed(TileType.CORN, 3).build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    assertThat(state.player(0).goodsCount(Good.CORN)).isEqualTo(3);
  }

  @Test
  public void unstaffedPlantationsProduceNothing() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).tiles(TileType.CORN, 3, false).build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    assertThat(state.player(0).totalGoods()).isZero();
  }

  @Test
  public void processedGoodsNeedAStaffedProductionBuildingAsWellAsPlantations() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).staffed(TileType.INDIGO, 2).build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    assertThat(state.player(0).goodsCount(Good.INDIGO)).isZero();
  }

  @Test
  public void yieldIsTheSmallerOfPlantationsAndBuildingCircles() {
    GameState state = TestGames.newGame(3);
    // Three staffed sugar plantations but only two staffed circles in the mill.
    state =
        state.withPlayer(
            TestGames.player(0)
                .staffed(TileType.SUGAR, 3)
                .building(BuildingType.SUGAR_MILL, 2)
                .build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    assertThat(state.player(0).goodsCount(Good.SUGAR)).isEqualTo(2);
  }

  @Test
  public void sparePlantationCapacityIsWastedWhenTheBuildingIsBigger() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(
            TestGames.player(0)
                .staffed(TileType.TOBACCO, 1)
                .building(BuildingType.TOBACCO_STORAGE, 3)
                .build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    assertThat(state.player(0).goodsCount(Good.TOBACCO)).isEqualTo(1);
  }

  @Test
  public void unstaffedProductionBuildingsProcessNothing() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(
            TestGames.player(0)
                .staffed(TileType.COFFEE, 2)
                .building(BuildingType.COFFEE_ROASTER, 0)
                .build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    assertThat(state.player(0).goodsCount(Good.COFFEE)).isZero();
  }

  @Test
  public void productionBuildingsOfTheSameGoodStack() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(
            TestGames.player(0)
                .staffed(TileType.INDIGO, 4)
                .building(BuildingType.SMALL_INDIGO_PLANT, 1)
                .building(BuildingType.INDIGO_PLANT, 3)
                .build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    assertThat(state.player(0).goodsCount(Good.INDIGO)).isEqualTo(4);
  }

  @Test
  public void anExhaustedSupplyLeavesTheProducerWithNothing() {
    GameState state = TestGames.newGame(3);
    Map<Good, Integer> empty = new EnumMap<>(Good.class);
    state =
        state.toBuilder()
            .goods(new GoodsSupply(empty))
            .build()
            .withPlayer(TestGames.player(0).staffed(TileType.CORN, 3).build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    assertThat(state.player(0).totalGoods()).isZero();
  }

  @Test
  public void aShortSupplyIsHandedOutInTurnOrderFromTheCraftsman() {
    GameState state = TestGames.newGame(3);
    Map<Good, Integer> two = new EnumMap<>(Good.class);
    two.put(Good.CORN, 2);
    state =
        state.toBuilder()
            .goods(new GoodsSupply(two))
            .build()
            .withPlayer(TestGames.player(0).staffed(TileType.CORN, 2).build())
            .withPlayer(TestGames.player(1).staffed(TileType.CORN, 2).build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    // The craftsman takes both barrels; the next player goes without.
    assertThat(state.player(0).goodsCount(Good.CORN)).isEqualTo(2);
    assertThat(state.player(1).goodsCount(Good.CORN)).isZero();
  }

  @Test
  public void theCraftsmanTakesABonusBarrelOfAKindTheyProduced() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(
            TestGames.player(0).staffed(TileType.CORN, 2).staffed(TileType.SUGAR, 1).build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    // Sugar needs a mill, so only corn was actually produced.
    assertThat(state.phase()).isInstanceOf(Phase.CraftsmanBonus.class);
    assertThat(GameEngine.legalActions(state))
        .containsExactlyInAnyOrder(
            new PlayerAction.TakeCraftsmanBonus(0, Good.CORN),
            new PlayerAction.PassCraftsmanBonus(0));

    state = TestGames.apply(state, new PlayerAction.TakeCraftsmanBonus(0, Good.CORN));
    assertThat(state.player(0).goodsCount(Good.CORN)).isEqualTo(3);
  }

  @Test
  public void theBonusBarrelMustBeAKindActuallyProduced() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).staffed(TileType.CORN, 1).build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    assertThat(
            TestGames.reject(state, new PlayerAction.TakeCraftsmanBonus(0, Good.COFFEE)).reason())
        .isEqualTo(RejectionReason.GOOD_NOT_PRODUCED);
  }

  @Test
  public void aCraftsmanWhoProducesNothingIsNeverOfferedABonus() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    // Straight past the bonus and on to the next player's role choice.
    assertThat(state.phase()).isInstanceOf(Phase.RoleSelection.class);
  }

  @Test
  public void theBonusMayBeDeclined() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).staffed(TileType.CORN, 1).build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    state = TestGames.apply(state, new PlayerAction.PassCraftsmanBonus(0));
    assertThat(state.player(0).goodsCount(Good.CORN)).isEqualTo(1);
    assertThat(state.phase()).isInstanceOf(Phase.RoleSelection.class);
  }

  /** distinct kinds produced, doubloons paid. */
  @DataProvider(name = "factoryPayouts")
  public Object[][] factoryPayouts() {
    return new Object[][] {{1, 0}, {2, 1}, {3, 2}, {4, 3}, {5, 5}};
  }

  @Test(dataProvider = "factoryPayouts")
  public void theFactoryPaysForVarietyNotVolume(int kinds, int doubloons) {
    GameState state = TestGames.newGame(3);
    var fixture = TestGames.player(0).doubloons(0).building(BuildingType.FACTORY);

    Good[] order = {Good.CORN, Good.INDIGO, Good.SUGAR, Good.TOBACCO, Good.COFFEE};
    for (int i = 0; i < kinds; i++) {
      Good good = order[i];
      fixture = fixture.staffed(TileType.of(good), 2);
      if (good != Good.CORN) {
        fixture = fixture.building(productionBuildingFor(good));
      }
    }
    state = state.withPlayer(fixture.build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    assertThat(state.player(0).doubloons()).isEqualTo(doubloons);
  }

  @Test
  public void anUnstaffedFactoryPaysNothing() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(
            TestGames.player(0)
                .doubloons(0)
                .building(BuildingType.FACTORY, 0)
                .staffed(TileType.CORN, 1)
                .staffed(TileType.INDIGO, 1)
                .building(BuildingType.SMALL_INDIGO_PLANT)
                .build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    assertThat(state.player(0).doubloons()).isZero();
  }

  @Test
  public void everyPlayerProducesNotJustTheCraftsman() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).staffed(TileType.CORN, 1).build());
    state = state.withPlayer(TestGames.player(2).staffed(TileType.CORN, 2).build());
    state = TestGames.chooseRole(state, Role.CRAFTSMAN);

    assertThat(state.player(2).goodsCount(Good.CORN)).isEqualTo(2);
  }

  private static BuildingType productionBuildingFor(Good good) {
    return switch (good) {
      case INDIGO -> BuildingType.INDIGO_PLANT;
      case SUGAR -> BuildingType.SUGAR_MILL;
      case TOBACCO -> BuildingType.TOBACCO_STORAGE;
      case COFFEE -> BuildingType.COFFEE_ROASTER;
      case CORN -> throw new IllegalArgumentException("Corn needs no production building");
    };
  }
}
