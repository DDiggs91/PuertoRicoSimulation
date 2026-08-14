package com.PRS.model.engine;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.IslandTile;
import com.PRS.model.boards.PlayerState;
import com.PRS.model.buildings.PlacedBuilding;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import java.util.ArrayList;
import java.util.List;

/**
 * Mayor phase: colonists come off the ship and every player stages their board.
 *
 * <p>Taking colonists off the ship is a fixed clockwise deal with no decisions in it, so the engine
 * resolves it on entry. Staffing itself is not played out move by move — a turn is answered with
 * one {@link PlayerAction.SetColonistPlacement} carrying the whole finished board, island and
 * buildings both, index-aligned with the player's own lists. Anything the arrangement does not
 * place stays in San Juan.
 *
 * <p>Submitting the whole arrangement in one step is what lets a player try placements and undo
 * them freely before committing: nothing about that belongs in the engine, which only ever sees the
 * arrangement a client has already settled on. It also sidesteps a real difficulty with modelling
 * rearrangement as individual place/remove moves — an actor offered both per circle has no way to
 * converge on the fully-staffed board where finishing is legal, since undoing a move is always as
 * available as making progress.
 */
final class MayorPhaseHandler {

  private MayorPhaseHandler() {}

  static GameState begin(GameState state, int chooserSeat) {
    GameState next = state;

    // Privilege: one colonist straight from the supply, never off the ship.
    if (next.colonistSupply() > 0) {
      next =
          next.toBuilder()
              .colonistSupply(next.colonistSupply() - 1)
              .build()
              .withPlayer(
                  next.player(chooserSeat).toBuilder()
                      .colonistsInSanJuan(next.player(chooserSeat).colonistsInSanJuan() + 1)
                      .build());
    }

    // Deal the colonist ship out one at a time, clockwise from the mayor.
    List<Integer> order = next.turnOrderFrom(chooserSeat);
    int onShip = next.colonistsOnShip();
    for (int i = 0; i < onShip; i++) {
      PlayerState player = next.player(order.get(i % order.size()));
      next =
          next.withPlayer(
              player.toBuilder().colonistsInSanJuan(player.colonistsInSanJuan() + 1).build());
    }
    next = next.toBuilder().colonistsOnShip(0).build();

    return next.withPhase(new Phase.MayorPhase(chooserSeat, next.turnOrderFrom(chooserSeat)));
  }

  /**
   * Exactly one option: the greedy fill of the player's current board — San Juan emptied into
   * vacant circles in index order, island first. It is always legal, so an actor that simply takes
   * what it is offered finishes its turn in a single step; a human client stages something better
   * and submits that instead.
   */
  static List<PlayerAction> legalActions(GameState state, Phase.MayorPhase phase) {
    int seat = phase.actorSeat();
    PlayerState player = state.player(seat);

    List<Boolean> island = new ArrayList<>();
    int spare = player.colonistsInSanJuan();
    for (IslandTile tile : player.island()) {
      if (tile.occupied()) {
        island.add(true);
      } else if (spare > 0) {
        island.add(true);
        spare--;
      } else {
        island.add(false);
      }
    }

    List<Integer> buildings = new ArrayList<>();
    for (PlacedBuilding building : player.buildings()) {
      int colonists = building.colonists();
      int room = building.type().colonistCapacity() - colonists;
      int add = Math.min(room, spare);
      buildings.add(colonists + add);
      spare -= add;
    }

    return List.of(new PlayerAction.SetColonistPlacement(seat, island, buildings));
  }

  static ActionResult apply(GameState state, Phase.MayorPhase phase, PlayerAction action) {
    int seat = phase.actorSeat();
    PlayerState player = state.player(seat);

    if (!(action instanceof PlayerAction.SetColonistPlacement set)) {
      return ActionResult.reject(
          RejectionReason.WRONG_PHASE,
          action.getClass().getSimpleName() + " is not a mayor action");
    }

    if (set.islandOccupied().size() != player.island().size()
        || set.buildingColonists().size() != player.buildings().size()) {
      return ActionResult.reject(
          RejectionReason.INVALID_COLONIST_MOVE,
          "Arrangement does not match the shape of the board");
    }

    for (int i = 0; i < set.buildingColonists().size(); i++) {
      int colonists = set.buildingColonists().get(i);
      int capacity = player.buildings().get(i).type().colonistCapacity();
      if (colonists < 0 || colonists > capacity) {
        return ActionResult.reject(
            RejectionReason.INVALID_COLONIST_MOVE,
            "Building %d holds %d colonists but only has %d circles"
                .formatted(i, colonists, capacity));
      }
    }

    int onBoardBefore =
        (int) player.island().stream().filter(IslandTile::occupied).count()
            + player.buildings().stream().mapToInt(PlacedBuilding::colonists).sum();
    int onBoardAfter =
        (int) set.islandOccupied().stream().filter(Boolean::booleanValue).count()
            + set.buildingColonists().stream().mapToInt(Integer::intValue).sum();
    int movedFromSanJuan = onBoardAfter - onBoardBefore;
    if (movedFromSanJuan > player.colonistsInSanJuan()) {
      return ActionResult.reject(
          RejectionReason.INVALID_COLONIST_MOVE, "Not enough colonists in San Juan for that many");
    }

    int remaining = player.colonistsInSanJuan() - movedFromSanJuan;
    boolean anyVacant =
        set.islandOccupied().contains(false) || anyBuildingHasRoom(set.buildingColonists(), player);
    if (remaining > 0 && anyVacant) {
      return ActionResult.reject(
          RejectionReason.COLONISTS_UNPLACED,
          "Colonists may not sit in San Juan while circles are empty");
    }

    List<IslandTile> island = new ArrayList<>();
    for (int i = 0; i < player.island().size(); i++) {
      island.add(player.island().get(i).withOccupied(set.islandOccupied().get(i)));
    }
    List<PlacedBuilding> buildings = new ArrayList<>();
    for (int i = 0; i < player.buildings().size(); i++) {
      buildings.add(player.buildings().get(i).withColonists(set.buildingColonists().get(i)));
    }

    PlayerState staffed =
        player.toBuilder()
            .island(island)
            .buildings(buildings)
            .colonistsInSanJuan(remaining)
            .build();

    return ActionResult.accept(advance(state.withPlayer(staffed), phase));
  }

  private static boolean anyBuildingHasRoom(List<Integer> buildingColonists, PlayerState player) {
    for (int i = 0; i < buildingColonists.size(); i++) {
      if (buildingColonists.get(i) < player.buildings().get(i).type().colonistCapacity()) {
        return true;
      }
    }
    return false;
  }

  private static GameState advance(GameState state, Phase.MayorPhase phase) {
    List<Integer> rest = PhaseFlow.advanceQueue(phase.queue());
    if (rest != null) {
      return state.withPhase(new Phase.MayorPhase(phase.chooserSeat(), rest));
    }
    return PhaseFlow.endRolePhase(refillColonistShip(state));
  }

  /**
   * The mayor's last duty: one colonist per empty building circle across every board, never fewer
   * than one per player. Falling short of that is an end-of-game trigger.
   */
  private static GameState refillColonistShip(GameState state) {
    int emptyCircles = state.players().stream().mapToInt(PlayerState::emptyBuildingCircles).sum();
    int required = Math.max(state.playerCount(), emptyCircles);
    int loaded = Math.min(required, state.colonistSupply());
    return state.toBuilder()
        .colonistsOnShip(loaded)
        .colonistSupply(state.colonistSupply() - loaded)
        .finalRound(state.finalRound() || loaded < required)
        .build();
  }
}
