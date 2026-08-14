package com.PRS.model.actions;

import com.PRS.model.buildings.BuildingType;
import com.PRS.model.goods.Good;
import com.PRS.model.rolecards.Role;
import java.util.List;

/**
 * Every decision a player can be asked to make. One record per genuine choice — steps the rules
 * leave no discretion over (goods production, colonist distribution off the ship, the trader's and
 * captain's end-of-phase duties) are resolved by the engine rather than surfaced as actions.
 */
public sealed interface PlayerAction {

  /** The seat this action is submitted for. */
  int seat();

  /** Choose one of the round's remaining role cards. */
  record SelectRole(int seat, Role role) implements PlayerAction {}

  // --- settler phase ---

  /** Take one of the face-up plantation tiles. */
  record TakeFaceUpTile(int seat, int faceUpIndex) implements PlayerAction {}

  /** Take a quarry — the settler's privilege, or the Construction Hut's function. */
  record TakeQuarry(int seat) implements PlayerAction {}

  /** Take the top face-down plantation via an occupied Hacienda, before the normal pick. */
  record TakeHaciendaTile(int seat) implements PlayerAction {}

  /** Decline the Hacienda draw and go straight to the normal pick. */
  record SkipHacienda(int seat) implements PlayerAction {}

  /** Take no tile this settler phase. */
  record PassSettling(int seat) implements PlayerAction {}

  // --- mayor phase ---

  /**
   * The whole of a player's colonist arrangement for their mayor turn, submitted as one action
   * rather than played out move by move. Both lists are index-aligned with the player's own board —
   * {@code islandOccupied.get(i)} answers for {@code island.get(i)}, {@code
   * buildingColonists.get(i)} for {@code buildings.get(i)} — and whatever colonists the arrangement
   * does not place stay in San Juan.
   */
  record SetColonistPlacement(
      int seat, List<Boolean> islandOccupied, List<Integer> buildingColonists)
      implements PlayerAction {}

  // --- builder phase ---

  record BuildBuilding(int seat, BuildingType type) implements PlayerAction {}

  record PassBuilding(int seat) implements PlayerAction {}

  // --- craftsman phase ---

  /** The craftsman's bonus barrel, which must be a kind they just produced. */
  record TakeCraftsmanBonus(int seat, Good good) implements PlayerAction {}

  record PassCraftsmanBonus(int seat) implements PlayerAction {}

  // --- trader phase ---

  record SellGood(int seat, Good good) implements PlayerAction {}

  record PassTrading(int seat) implements PlayerAction {}

  // --- captain phase ---

  /** Load every barrel of one kind that will fit onto the given ship. */
  record LoadShip(int seat, int shipIndex, Good good) implements PlayerAction {}

  /** Ship every barrel of one kind via an occupied Wharf. Once per captain phase. */
  record LoadWharf(int seat, Good good) implements PlayerAction {}

  /**
   * Decline to use the Wharf and end this turn. Using the Wharf is optional even though loading is
   * otherwise compulsory, so this is offered only when the Wharf is the owner's sole remaining
   * option — a player who could still load a cargo ship must.
   */
  record DeclineWharf(int seat) implements PlayerAction {}

  /**
   * Choose what survives the end of the captain phase: whole kinds covered by warehouses, plus a
   * single loose barrel. Everything else goes back to the supply.
   */
  record StoreGoods(int seat, List<Good> warehouseKinds, Good singleBarrel)
      implements PlayerAction {}
}
