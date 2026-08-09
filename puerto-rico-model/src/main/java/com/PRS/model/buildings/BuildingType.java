package com.PRS.model.buildings;

import static com.PRS.model.buildings.BuildingCategory.PRODUCTION_LARGE;
import static com.PRS.model.buildings.BuildingCategory.PRODUCTION_SMALL;
import static com.PRS.model.buildings.BuildingCategory.VIOLET_LARGE;
import static com.PRS.model.buildings.BuildingCategory.VIOLET_SMALL;

import com.PRS.model.goods.Good;
import java.util.Optional;

/**
 * The 23 building types, carrying the numbers printed on the cards: cost, victory points, colonist
 * circles, and how many copies the game supplies.
 *
 * <p>Corn has no production building — it comes straight off the plantation.
 */
public enum BuildingType {
  SMALL_INDIGO_PLANT(1, 1, 1, 4, PRODUCTION_SMALL, Good.INDIGO),
  SMALL_SUGAR_MILL(2, 1, 1, 4, PRODUCTION_SMALL, Good.SUGAR),
  INDIGO_PLANT(3, 2, 3, 3, PRODUCTION_LARGE, Good.INDIGO),
  SUGAR_MILL(4, 2, 3, 3, PRODUCTION_LARGE, Good.SUGAR),
  TOBACCO_STORAGE(5, 3, 3, 3, PRODUCTION_LARGE, Good.TOBACCO),
  COFFEE_ROASTER(6, 3, 2, 3, PRODUCTION_LARGE, Good.COFFEE),

  SMALL_MARKET(1, 1, 1, 2, VIOLET_SMALL, null),
  HACIENDA(2, 1, 1, 2, VIOLET_SMALL, null),
  CONSTRUCTION_HUT(2, 1, 1, 2, VIOLET_SMALL, null),
  SMALL_WAREHOUSE(3, 1, 1, 2, VIOLET_SMALL, null),
  HOSPICE(4, 2, 1, 2, VIOLET_SMALL, null),
  OFFICE(5, 2, 1, 2, VIOLET_SMALL, null),
  LARGE_MARKET(5, 2, 1, 2, VIOLET_SMALL, null),
  LARGE_WAREHOUSE(6, 2, 1, 2, VIOLET_SMALL, null),
  FACTORY(7, 3, 1, 2, VIOLET_SMALL, null),
  UNIVERSITY(8, 3, 1, 2, VIOLET_SMALL, null),
  HARBOR(8, 3, 1, 2, VIOLET_SMALL, null),
  WHARF(9, 3, 1, 2, VIOLET_SMALL, null),

  GUILD_HALL(10, 4, 1, 1, VIOLET_LARGE, null),
  RESIDENCE(10, 4, 1, 1, VIOLET_LARGE, null),
  FORTRESS(10, 4, 1, 1, VIOLET_LARGE, null),
  CUSTOMS_HOUSE(10, 4, 1, 1, VIOLET_LARGE, null),
  CITY_HALL(10, 4, 1, 1, VIOLET_LARGE, null);

  private final int cost;
  private final int victoryPoints;
  private final int colonistCapacity;
  private final int copies;
  private final BuildingCategory category;
  private final Good producedGood;

  BuildingType(
      int cost,
      int victoryPoints,
      int colonistCapacity,
      int copies,
      BuildingCategory category,
      Good producedGood) {
    this.cost = cost;
    this.victoryPoints = victoryPoints;
    this.colonistCapacity = colonistCapacity;
    this.copies = copies;
    this.category = category;
    this.producedGood = producedGood;
  }

  /** Undiscounted cost in doubloons. */
  public int cost() {
    return cost;
  }

  /** Scored at game end whether or not the building is occupied. */
  public int victoryPoints() {
    return victoryPoints;
  }

  /** Colonist circles — for production buildings, also the maximum barrels produced. */
  public int colonistCapacity() {
    return colonistCapacity;
  }

  /** How many copies of this building the game supplies. */
  public int copies() {
    return copies;
  }

  public BuildingCategory category() {
    return category;
  }

  /** The good this building processes, empty for violet buildings. */
  public Optional<Good> producedGood() {
    return Optional.ofNullable(producedGood);
  }

  /** Large violet buildings need two adjacent city spaces; everything else needs one. */
  public int citySpaces() {
    return category == VIOLET_LARGE ? 2 : 1;
  }

  /**
   * The building display's four columns cap how many occupied quarries may discount a purchase, and
   * a building's column is its victory point value.
   */
  public int maxQuarryDiscount() {
    return victoryPoints;
  }

  public boolean isViolet() {
    return category.isViolet();
  }
}
