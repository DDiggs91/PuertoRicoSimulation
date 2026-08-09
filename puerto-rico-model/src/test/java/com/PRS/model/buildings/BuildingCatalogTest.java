package com.PRS.model.buildings;

import static com.PRS.model.buildings.BuildingType.CITY_HALL;
import static com.PRS.model.buildings.BuildingType.COFFEE_ROASTER;
import static com.PRS.model.buildings.BuildingType.CONSTRUCTION_HUT;
import static com.PRS.model.buildings.BuildingType.CUSTOMS_HOUSE;
import static com.PRS.model.buildings.BuildingType.FACTORY;
import static com.PRS.model.buildings.BuildingType.FORTRESS;
import static com.PRS.model.buildings.BuildingType.GUILD_HALL;
import static com.PRS.model.buildings.BuildingType.HACIENDA;
import static com.PRS.model.buildings.BuildingType.HARBOR;
import static com.PRS.model.buildings.BuildingType.HOSPICE;
import static com.PRS.model.buildings.BuildingType.INDIGO_PLANT;
import static com.PRS.model.buildings.BuildingType.LARGE_MARKET;
import static com.PRS.model.buildings.BuildingType.LARGE_WAREHOUSE;
import static com.PRS.model.buildings.BuildingType.OFFICE;
import static com.PRS.model.buildings.BuildingType.RESIDENCE;
import static com.PRS.model.buildings.BuildingType.SMALL_INDIGO_PLANT;
import static com.PRS.model.buildings.BuildingType.SMALL_MARKET;
import static com.PRS.model.buildings.BuildingType.SMALL_SUGAR_MILL;
import static com.PRS.model.buildings.BuildingType.SMALL_WAREHOUSE;
import static com.PRS.model.buildings.BuildingType.SUGAR_MILL;
import static com.PRS.model.buildings.BuildingType.TOBACCO_STORAGE;
import static com.PRS.model.buildings.BuildingType.UNIVERSITY;
import static com.PRS.model.buildings.BuildingType.WHARF;
import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.goods.Good;
import java.util.Arrays;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** The numbers printed on the building cards, and the totals in the box. */
public class BuildingCatalogTest {

  /** name, cost, victory points, colonist circles, copies. */
  @DataProvider(name = "buildings")
  public Object[][] buildings() {
    return new Object[][] {
      {SMALL_INDIGO_PLANT, 1, 1, 1, 4},
      {SMALL_SUGAR_MILL, 2, 1, 1, 4},
      {INDIGO_PLANT, 3, 2, 3, 3},
      {SUGAR_MILL, 4, 2, 3, 3},
      {TOBACCO_STORAGE, 5, 3, 3, 3},
      {COFFEE_ROASTER, 6, 3, 2, 3},
      {SMALL_MARKET, 1, 1, 1, 2},
      {HACIENDA, 2, 1, 1, 2},
      {CONSTRUCTION_HUT, 2, 1, 1, 2},
      {SMALL_WAREHOUSE, 3, 1, 1, 2},
      {HOSPICE, 4, 2, 1, 2},
      {OFFICE, 5, 2, 1, 2},
      {LARGE_MARKET, 5, 2, 1, 2},
      {LARGE_WAREHOUSE, 6, 2, 1, 2},
      {FACTORY, 7, 3, 1, 2},
      {UNIVERSITY, 8, 3, 1, 2},
      {HARBOR, 8, 3, 1, 2},
      {WHARF, 9, 3, 1, 2},
      {GUILD_HALL, 10, 4, 1, 1},
      {RESIDENCE, 10, 4, 1, 1},
      {FORTRESS, 10, 4, 1, 1},
      {CUSTOMS_HOUSE, 10, 4, 1, 1},
      {CITY_HALL, 10, 4, 1, 1},
    };
  }

  @Test(dataProvider = "buildings")
  public void buildingCardsCarryTheirPrintedNumbers(
      BuildingType type, int cost, int victoryPoints, int circles, int copies) {
    assertThat(type.cost()).as("%s cost", type).isEqualTo(cost);
    assertThat(type.victoryPoints()).as("%s VP", type).isEqualTo(victoryPoints);
    assertThat(type.colonistCapacity()).as("%s circles", type).isEqualTo(circles);
    assertThat(type.copies()).as("%s copies", type).isEqualTo(copies);
  }

  @Test
  public void theBoxHoldsFortyNineBuildings() {
    assertThat(countIn(BuildingCategory.PRODUCTION_SMALL, BuildingCategory.PRODUCTION_LARGE))
        .isEqualTo(20);
    assertThat(countIn(BuildingCategory.VIOLET_SMALL)).isEqualTo(24);
    assertThat(countIn(BuildingCategory.VIOLET_LARGE)).isEqualTo(5);
    assertThat(Arrays.stream(BuildingType.values()).mapToInt(BuildingType::copies).sum())
        .isEqualTo(49);
  }

  @Test
  public void thereAreTwelveSmallVioletTypesAndFiveLargeOnes() {
    assertThat(typesIn(BuildingCategory.VIOLET_SMALL)).isEqualTo(12);
    assertThat(typesIn(BuildingCategory.VIOLET_LARGE)).isEqualTo(5);
  }

  @Test
  public void onlyLargeVioletBuildingsTakeTwoCitySpaces() {
    for (BuildingType type : BuildingType.values()) {
      int expected = type.category() == BuildingCategory.VIOLET_LARGE ? 2 : 1;
      assertThat(type.citySpaces()).as("%s city spaces", type).isEqualTo(expected);
    }
  }

  /**
   * The building display's four columns cap the quarry discount, and the columns are the 1/2/3/4
   * victory point groups — so the cap is just the printed VP value.
   */
  @Test(dataProvider = "buildings")
  public void quarryDiscountIsCappedAtTheBuildingsVictoryPoints(
      BuildingType type, int cost, int victoryPoints, int circles, int copies) {
    assertThat(type.maxQuarryDiscount()).as("%s discount cap", type).isEqualTo(victoryPoints);
  }

  @DataProvider(name = "productionBuildings")
  public Object[][] productionBuildings() {
    return new Object[][] {
      {SMALL_INDIGO_PLANT, Good.INDIGO},
      {INDIGO_PLANT, Good.INDIGO},
      {SMALL_SUGAR_MILL, Good.SUGAR},
      {SUGAR_MILL, Good.SUGAR},
      {TOBACCO_STORAGE, Good.TOBACCO},
      {COFFEE_ROASTER, Good.COFFEE},
    };
  }

  @Test(dataProvider = "productionBuildings")
  public void productionBuildingsProcessOneGoodEach(BuildingType type, Good good) {
    assertThat(type.producedGood()).contains(good);
  }

  @Test
  public void cornHasNoProductionBuilding() {
    assertThat(Arrays.stream(BuildingType.values()))
        .noneMatch(t -> t.producedGood().orElse(null) == Good.CORN);
  }

  @Test
  public void violetBuildingsProcessNothing() {
    for (BuildingType type : BuildingType.values()) {
      if (type.isViolet()) {
        assertThat(type.producedGood()).as("%s", type).isEmpty();
      }
    }
  }

  private static long countIn(BuildingCategory... categories) {
    return Arrays.stream(BuildingType.values())
        .filter(t -> Arrays.asList(categories).contains(t.category()))
        .mapToInt(BuildingType::copies)
        .sum();
  }

  private static long typesIn(BuildingCategory category) {
    return Arrays.stream(BuildingType.values()).filter(t -> t.category() == category).count();
  }
}
