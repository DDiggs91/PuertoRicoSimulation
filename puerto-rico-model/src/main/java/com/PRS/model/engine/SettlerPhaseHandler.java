package com.PRS.model.engine;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.IslandTile;
import com.PRS.model.boards.PlayerState;
import com.PRS.model.boards.TileSupply;
import com.PRS.model.boards.TileType;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.game.SetupTable;
import java.util.ArrayList;
import java.util.List;

/** Settler phase: each player takes a plantation, and the settler may take a quarry instead. */
final class SettlerPhaseHandler {

  private SettlerPhaseHandler() {}

  static List<PlayerAction> legalActions(GameState state, Phase.SettlerPhase phase) {
    int seat = phase.actorSeat();
    PlayerState player = state.player(seat);

    if (haciendaPending(state, phase, player)) {
      return List.of(new PlayerAction.TakeHaciendaTile(seat), new PlayerAction.SkipHacienda(seat));
    }

    List<PlayerAction> actions = new ArrayList<>();
    if (player.freeIslandSpaces() > 0) {
      for (int i = 0; i < state.tiles().faceUp().size(); i++) {
        actions.add(new PlayerAction.TakeFaceUpTile(seat, i));
      }
      if (mayTakeQuarry(state, phase, player)) {
        actions.add(new PlayerAction.TakeQuarry(seat));
      }
    }
    actions.add(new PlayerAction.PassSettling(seat));
    return actions;
  }

  static ActionResult apply(GameState state, Phase.SettlerPhase phase, PlayerAction action) {
    int seat = phase.actorSeat();
    PlayerState player = state.player(seat);
    boolean pending = haciendaPending(state, phase, player);

    return switch (action) {
      case PlayerAction.TakeHaciendaTile ignored -> {
        if (!pending) {
          yield ActionResult.reject(
              RejectionReason.WRONG_PHASE, "No Hacienda draw is on offer right now");
        }
        TileSupply.Draw draw = state.tiles().drawFaceDown();
        GameState next =
            state.toBuilder()
                .tiles(draw.supply())
                .build()
                .withPlayer(player.plusTile(IslandTile.unstaffed(draw.tile().orElseThrow())));
        yield ActionResult.accept(
            next.withPhase(new Phase.SettlerPhase(phase.chooserSeat(), phase.queue(), true)));
      }
      case PlayerAction.SkipHacienda ignored -> {
        if (!pending) {
          yield ActionResult.reject(
              RejectionReason.WRONG_PHASE, "No Hacienda draw is on offer right now");
        }
        yield ActionResult.accept(
            state.withPhase(new Phase.SettlerPhase(phase.chooserSeat(), phase.queue(), true)));
      }
      case PlayerAction.TakeFaceUpTile take -> {
        if (pending) {
          yield ActionResult.reject(RejectionReason.WRONG_PHASE, "Resolve the Hacienda draw first");
        }
        if (player.freeIslandSpaces() == 0) {
          yield ActionResult.reject(RejectionReason.NO_ISLAND_SPACE, "All 12 island spaces filled");
        }
        if (take.faceUpIndex() < 0 || take.faceUpIndex() >= state.tiles().faceUp().size()) {
          yield ActionResult.reject(
              RejectionReason.TILE_UNAVAILABLE, "No face-up tile at index " + take.faceUpIndex());
        }
        TileType type = state.tiles().faceUp().get(take.faceUpIndex());
        yield ActionResult.accept(
            advance(
                placeTile(
                    state.withTiles(state.tiles().takeFaceUp(take.faceUpIndex())), seat, type),
                phase));
      }
      case PlayerAction.TakeQuarry ignored -> {
        if (pending) {
          yield ActionResult.reject(RejectionReason.WRONG_PHASE, "Resolve the Hacienda draw first");
        }
        if (player.freeIslandSpaces() == 0) {
          yield ActionResult.reject(RejectionReason.NO_ISLAND_SPACE, "All 12 island spaces filled");
        }
        if (!mayTakeQuarry(state, phase, player)) {
          yield ActionResult.reject(
              RejectionReason.QUARRY_NOT_ALLOWED,
              "Only the settler, or an occupied Construction Hut owner, may take a quarry");
        }
        yield ActionResult.accept(
            advance(
                placeTile(state.withTiles(state.tiles().takeQuarry()), seat, TileType.QUARRY),
                phase));
      }
      case PlayerAction.PassSettling ignored -> ActionResult.accept(advance(state, phase));
      default ->
          ActionResult.reject(
              RejectionReason.WRONG_PHASE,
              action.getClass().getSimpleName() + " is not a settler action");
    };
  }

  /** Places a tile, staffing it straight away when the player has an occupied Hospice. */
  private static GameState placeTile(GameState state, int seat, TileType type) {
    PhaseFlow.ColonistDraw draw =
        state.player(seat).hasOccupied(BuildingType.HOSPICE)
            ? PhaseFlow.drawColonist(state)
            : new PhaseFlow.ColonistDraw(state, false);
    return draw.state()
        .withPlayer(draw.state().player(seat).plusTile(new IslandTile(type, draw.drawn())));
  }

  private static GameState advance(GameState state, Phase.SettlerPhase phase) {
    List<Integer> rest = PhaseFlow.advanceQueue(phase.queue());
    if (rest != null) {
      return state.withPhase(new Phase.SettlerPhase(phase.chooserSeat(), rest, false));
    }
    // The settler's last duty: clear the row and deal a fresh one.
    return PhaseFlow.endRolePhase(
        state.withTiles(
            state.tiles().refillFaceUp(SetupTable.faceUpPlantations(state.playerCount()))));
  }

  private static boolean haciendaPending(
      GameState state, Phase.SettlerPhase phase, PlayerState player) {
    return !phase.haciendaOffered()
        && player.hasOccupied(BuildingType.HACIENDA)
        && player.freeIslandSpaces() > 0
        && !(state.tiles().drawPile().isEmpty() && state.tiles().discardPile().isEmpty());
  }

  private static boolean mayTakeQuarry(
      GameState state, Phase.SettlerPhase phase, PlayerState player) {
    boolean entitled =
        phase.actorSeat() == phase.chooserSeat()
            || player.hasOccupied(BuildingType.CONSTRUCTION_HUT);
    return entitled && state.tiles().hasQuarry();
  }
}
