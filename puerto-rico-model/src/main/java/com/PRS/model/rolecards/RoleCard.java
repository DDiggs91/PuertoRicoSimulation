package com.PRS.model.rolecards;

/**
 * One role card on the table, with any doubloons that have accumulated on it from rounds it went
 * unchosen, and the seat that has claimed it this round.
 */
public record RoleCard(Role role, int doubloons, Integer takenBySeat) {

  public static RoleCard available(Role role) {
    return new RoleCard(role, 0, null);
  }

  public boolean isTaken() {
    return takenBySeat != null;
  }

  public RoleCard takenBy(int seat) {
    return new RoleCard(role, 0, seat);
  }

  public RoleCard plusDoubloon() {
    return new RoleCard(role, doubloons + 1, takenBySeat);
  }

  public RoleCard released() {
    return new RoleCard(role, doubloons, null);
  }
}
