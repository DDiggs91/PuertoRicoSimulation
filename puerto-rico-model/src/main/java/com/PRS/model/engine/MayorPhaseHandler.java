package com.PRS.model.engine;

import com.PRS.model.actions.ColonistSlot;
import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.IslandTile;
import com.PRS.model.boards.PlayerState;
import com.PRS.model.buildings.PlacedBuilding;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import java.util.ArrayList;
import java.util.List;

/**
 * Mayor phase: colonists come off the ship and every player staffs their board.
 *
 * <p>Taking colonists off the ship is a fixed clockwise deal with no decisions in it, so the engine
 * resolves it on entry and only the placement is played out as actions.
 *
 * <p>The rules let a player rearrange colonists already on their board. Rather than model that as
 * free colonist-to-colonist moves — which lets a player shuffle back and forth forever, so the
 * phase need never end — a player's colonists are all recalled to San Juan when their turn starts
 * and then placed from scratch. The reachable end positions are exactly the same, since keeping a
 * colonist where it was just means putting it back, but every action now strictly empties San Juan
 * and the phase is guaranteed to finish.
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

    return enterSeat(next, chooserSeat, next.turnOrderFrom(chooserSeat));
  }

  static List<PlayerAction> legalActions(GameState state, Phase.MayorPhase phase) {
    int seat = phase.actorSeat();
    PlayerState player = state.player(seat);

    List<PlayerAction> actions = new ArrayList<>();
    if (player.colonistsInSanJuan() > 0) {
      for (ColonistSlot slot : vacantSlots(player)) {
        actions.add(new PlayerAction.PlaceColonist(seat, slot));
      }
    }
    if (mayFinish(player)) {
      actions.add(new PlayerAction.EndColonistPlacement(seat));
    }
    return actions;
  }

  static ActionResult apply(GameState state, Phase.MayorPhase phase, PlayerAction action) {
    int seat = phase.actorSeat();
    PlayerState player = state.player(seat);

    return switch (action) {
      case PlayerAction.PlaceColonist place -> {
        if (player.colonistsInSanJuan() == 0) {
          yield ActionResult.reject(
              RejectionReason.INVALID_COLONIST_MOVE, "No colonists left in San Juan");
        }
        if (!hasRoom(player, place.slot())) {
          yield ActionResult.reject(
              RejectionReason.INVALID_COLONIST_MOVE, "No empty circle at " + place.slot());
        }
        PlayerState placed =
            occupy(
                player.toBuilder().colonistsInSanJuan(player.colonistsInSanJuan() - 1).build(),
                place.slot());
        yield ActionResult.accept(state.withPlayer(placed));
      }
      case PlayerAction.EndColonistPlacement ignored -> {
        if (!mayFinish(player)) {
          yield ActionResult.reject(
              RejectionReason.COLONISTS_UNPLACED,
              "Colonists may not sit in San Juan while circles are empty");
        }
        yield ActionResult.accept(advance(state, phase));
      }
      default ->
          ActionResult.reject(
              RejectionReason.WRONG_PHASE,
              action.getClass().getSimpleName() + " is not a mayor action");
    };
  }

  /** A player may stop once San Juan is empty, or once there is nowhere left to put anyone. */
  private static boolean mayFinish(PlayerState player) {
    return player.colonistsInSanJuan() == 0 || vacantSlots(player).isEmpty();
  }

  /** Hands the turn to a seat, lifting their colonists so they can be placed afresh. */
  private static GameState enterSeat(GameState state, int chooserSeat, List<Integer> queue) {
    int seat = queue.getFirst();
    return state
        .withPlayer(recallColonists(state.player(seat)))
        .withPhase(new Phase.MayorPhase(chooserSeat, queue));
  }

  private static PlayerState recallColonists(PlayerState player) {
    List<IslandTile> island =
        player.island().stream().map(tile -> tile.withOccupied(false)).toList();
    List<PlacedBuilding> buildings =
        player.buildings().stream().map(b -> b.withColonists(0)).toList();
    return player.toBuilder()
        .colonistsInSanJuan(player.totalColonists())
        .island(island)
        .buildings(buildings)
        .build();
  }

  private static GameState advance(GameState state, Phase.MayorPhase phase) {
    List<Integer> rest = PhaseFlow.advanceQueue(phase.queue());
    if (rest != null) {
      return enterSeat(state, phase.chooserSeat(), rest);
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

  // --- slot plumbing ---

  private static List<ColonistSlot> vacantSlots(PlayerState player) {
    List<ColonistSlot> slots = new ArrayList<>();
    for (int i = 0; i < player.island().size(); i++) {
      if (!player.island().get(i).occupied()) {
        slots.add(new ColonistSlot.Island(i));
      }
    }
    for (int i = 0; i < player.buildings().size(); i++) {
      if (player.buildings().get(i).emptyCircles() > 0) {
        slots.add(new ColonistSlot.Building(i));
      }
    }
    return slots;
  }

  private static boolean hasRoom(PlayerState player, ColonistSlot slot) {
    return switch (slot) {
      case ColonistSlot.Island island ->
          inRange(island.index(), player.island().size())
              && !player.island().get(island.index()).occupied();
      case ColonistSlot.Building building ->
          inRange(building.index(), player.buildings().size())
              && player.buildings().get(building.index()).emptyCircles() > 0;
    };
  }

  private static PlayerState occupy(PlayerState player, ColonistSlot slot) {
    return switch (slot) {
      case ColonistSlot.Island island -> {
        List<IslandTile> tiles = new ArrayList<>(player.island());
        tiles.set(island.index(), tiles.get(island.index()).withOccupied(true));
        yield player.toBuilder().island(tiles).build();
      }
      case ColonistSlot.Building building -> {
        List<PlacedBuilding> buildings = new ArrayList<>(player.buildings());
        PlacedBuilding target = buildings.get(building.index());
        buildings.set(building.index(), target.withColonists(target.colonists() + 1));
        yield player.toBuilder().buildings(buildings).build();
      }
    };
  }

  private static boolean inRange(int index, int size) {
    return index >= 0 && index < size;
  }
}
