package com.PRS.model.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.TestGames;
import com.PRS.model.boards.PlayerState;
import com.PRS.model.boards.TileType;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.game.GameState;
import com.PRS.model.goods.Good;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Final scoring: chips, printed building points, and the large buildings' bonuses. */
public class ScoringTest {

  @Test
  public void chipsAndPrintedBuildingPointsAreSummed() {
    PlayerState player =
        TestGames.player(0)
            .victoryPoints(17)
            .building(BuildingType.TOBACCO_STORAGE, 0)
            .building(BuildingType.SMALL_MARKET, 0)
            .build();

    ScoreBreakdown score = Scorer.score(player);

    assertThat(score.chips()).isEqualTo(17);
    assertThat(score.buildingPoints()).isEqualTo(4); // 3 + 1
    assertThat(score.bonusPoints()).isZero();
    assertThat(score.total()).isEqualTo(21);
  }

  @Test
  public void buildingsScoreTheirPointsEvenUnoccupied() {
    PlayerState player = TestGames.player(0).building(BuildingType.GUILD_HALL, 0).build();

    ScoreBreakdown score = Scorer.score(player);

    assertThat(score.buildingPoints()).isEqualTo(4);
    // The extra points, unlike the printed ones, need a colonist.
    assertThat(score.bonusPoints()).isZero();
  }

  @Test
  public void theGuildHallScoresOneForSmallProductionBuildingsAndTwoForLarge() {
    PlayerState player =
        TestGames.player(0)
            .building(BuildingType.GUILD_HALL)
            .building(BuildingType.SMALL_INDIGO_PLANT, 0)
            .building(BuildingType.SMALL_SUGAR_MILL, 0)
            .building(BuildingType.SUGAR_MILL, 0)
            .building(BuildingType.COFFEE_ROASTER, 0)
            .build();

    // 1 + 1 for the small plants, 2 + 2 for the large ones.
    assertThat(Scorer.score(player).bonusPoints()).isEqualTo(6);
  }

  @DataProvider(name = "residence")
  public Object[][] residence() {
    return new Object[][] {{1, 4}, {8, 4}, {9, 4}, {10, 5}, {11, 6}, {12, 7}};
  }

  @Test(dataProvider = "residence")
  public void theResidenceScoresByFilledIslandSpaces(int filled, int expected) {
    PlayerState player =
        TestGames.player(0)
            .building(BuildingType.RESIDENCE)
            .tiles(TileType.CORN, filled, false)
            .build();

    assertThat(Scorer.score(player).bonusPoints()).isEqualTo(expected);
  }

  @Test
  public void theFortressScoresOnePerThreeColonistsAnywhereOnTheBoard() {
    PlayerState player =
        TestGames.player(0)
            .building(BuildingType.FORTRESS) // 1 colonist
            .staffed(TileType.CORN, 4) // 4 colonists
            .sanJuan(15) // San Juan counts too
            .build();

    assertThat(player.totalColonists()).isEqualTo(20);
    assertThat(Scorer.score(player).bonusPoints()).isEqualTo(6);
  }

  @Test
  public void theCustomsHouseScoresOnePerFourChipsAndIgnoresBuildingPoints() {
    PlayerState player =
        TestGames.player(0)
            .victoryPoints(23)
            .building(BuildingType.CUSTOMS_HOUSE)
            .building(BuildingType.COFFEE_ROASTER, 0)
            .build();

    assertThat(Scorer.score(player).bonusPoints()).isEqualTo(5);
  }

  @Test
  public void theCityHallScoresOnePerVioletBuildingCountingItself() {
    PlayerState player =
        TestGames.player(0)
            .building(BuildingType.CITY_HALL)
            .building(BuildingType.HACIENDA, 0)
            .building(BuildingType.HARBOR, 0)
            .building(BuildingType.OFFICE, 0)
            .building(BuildingType.CONSTRUCTION_HUT, 0)
            .building(BuildingType.LARGE_WAREHOUSE, 0)
            .building(BuildingType.RESIDENCE, 0)
            .build();

    // Six violet buildings plus the City Hall itself.
    assertThat(Scorer.score(player).bonusPoints()).isEqualTo(7);
  }

  @Test
  public void productionBuildingsDoNotCountForTheCityHall() {
    PlayerState player =
        TestGames.player(0)
            .building(BuildingType.CITY_HALL)
            .building(BuildingType.SUGAR_MILL, 0)
            .building(BuildingType.TOBACCO_STORAGE, 0)
            .build();

    assertThat(Scorer.score(player).bonusPoints()).isEqualTo(1);
  }

  @Test
  public void everyLargeBuildingBonusNeedsAColonist() {
    PlayerState occupied =
        TestGames.player(0).victoryPoints(20).building(BuildingType.CUSTOMS_HOUSE).build();
    PlayerState idle =
        TestGames.player(0).victoryPoints(20).building(BuildingType.CUSTOMS_HOUSE, 0).build();

    assertThat(Scorer.score(occupied).bonusPoints()).isEqualTo(5);
    assertThat(Scorer.score(idle).bonusPoints()).isZero();
  }

  @Test
  public void doubloonsAndGoodsScoreNothingButBreakTies() {
    PlayerState player =
        TestGames.player(0).victoryPoints(10).doubloons(7).goods(Good.COFFEE, 3).build();

    ScoreBreakdown score = Scorer.score(player);

    assertThat(score.total()).isEqualTo(10);
    assertThat(score.tiebreak()).isEqualTo(10); // 7 doubloons + 3 barrels
  }

  @Test
  public void standingsRankOnTotalThenOnDoubloonsPlusGoods() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).victoryPoints(20).doubloons(1).build());
    state = state.withPlayer(TestGames.player(1).victoryPoints(20).doubloons(9).build());
    state = state.withPlayer(TestGames.player(2).victoryPoints(25).doubloons(0).build());

    assertThat(Scorer.finalStandings(state))
        .extracting(ScoreBreakdown::seat)
        .containsExactly(2, 1, 0);
  }

  @Test
  public void bonusesFromSeveralLargeBuildingsAddUp() {
    PlayerState player =
        TestGames.player(0)
            .victoryPoints(8)
            .building(BuildingType.FORTRESS) // 1 colonist
            .building(BuildingType.CUSTOMS_HOUSE) // 1 colonist
            .staffed(TileType.CORN, 4)
            .build();

    // Fortress: 6 colonists / 3 = 2. Customs house: 8 chips / 4 = 2.
    assertThat(Scorer.score(player).bonusPoints()).isEqualTo(4);
    assertThat(Scorer.score(player).buildingPoints()).isEqualTo(8);
    assertThat(Scorer.score(player).total()).isEqualTo(20);
  }
}
