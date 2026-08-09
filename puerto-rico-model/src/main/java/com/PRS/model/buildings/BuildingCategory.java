package com.PRS.model.buildings;

/**
 * Building groupings that the rules themselves distinguish.
 *
 * <p>The small/large production split exists because the Guild Hall scores them differently; the
 * violet split because large buildings cost two city spaces and carry end-game bonuses.
 */
public enum BuildingCategory {
  PRODUCTION_SMALL,
  PRODUCTION_LARGE,
  VIOLET_SMALL,
  VIOLET_LARGE;

  public boolean isViolet() {
    return this == VIOLET_SMALL || this == VIOLET_LARGE;
  }
}
