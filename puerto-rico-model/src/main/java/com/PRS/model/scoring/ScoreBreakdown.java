package com.PRS.model.scoring;

/**
 * One player's final score.
 *
 * @param tiebreak doubloons plus goods, counting one barrel as one doubloon
 */
public record ScoreBreakdown(
    int seat, String name, int chips, int buildingPoints, int bonusPoints, int tiebreak) {

  public int total() {
    return chips + buildingPoints + bonusPoints;
  }
}
