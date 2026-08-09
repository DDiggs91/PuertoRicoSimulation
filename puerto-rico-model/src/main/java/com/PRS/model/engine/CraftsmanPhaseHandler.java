package com.PRS.model.engine;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.PlayerState;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.goods.Good;
import com.PRS.model.goods.GoodsSupply;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Craftsman phase: everybody produces, then the craftsman takes a bonus barrel.
 *
 * <p>Production involves no choices — a player takes everything their staffed plantations and
 * production buildings allow — so the engine resolves it on entry and only the bonus is an action.
 */
final class CraftsmanPhaseHandler {

  private CraftsmanPhaseHandler() {}

  /** Doubloons an occupied Factory pays, indexed by how many distinct kinds were produced. */
  static int factoryPayout(int distinctKinds) {
    return switch (distinctKinds) {
      case 0, 1 -> 0;
      case 2 -> 1;
      case 3 -> 2;
      case 4 -> 3;
      default -> 5;
    };
  }

  static GameState begin(GameState state, int chooserSeat) {
    GameState next = state;
    Set<Good> chooserProduced = EnumSet.noneOf(Good.class);

    for (int seat : next.turnOrderFrom(chooserSeat)) {
      Set<Good> produced = EnumSet.noneOf(Good.class);
      PlayerState player = next.player(seat);
      GoodsSupply supply = next.goods();

      for (Good good : Good.values()) {
        int wanted = player.productionCapacity(good);
        int taken = Math.min(wanted, supply.available(good));
        if (taken > 0) {
          player = player.plusGoods(good, taken);
          supply = supply.take(good, taken);
          produced.add(good);
        }
      }

      if (player.hasOccupied(BuildingType.FACTORY)) {
        player = player.plusDoubloons(factoryPayout(produced.size()));
      }

      next = next.toBuilder().goods(supply).build().withPlayer(player);
      if (seat == chooserSeat) {
        chooserProduced = produced;
      }
    }

    // The bonus barrel must be a kind the craftsman actually produced, and still in supply.
    Set<Good> options = EnumSet.noneOf(Good.class);
    for (Good good : chooserProduced) {
      if (next.goods().available(good) > 0) {
        options.add(good);
      }
    }
    if (options.isEmpty()) {
      return PhaseFlow.endRolePhase(next);
    }
    return next.withPhase(new Phase.CraftsmanBonus(chooserSeat, options));
  }

  static List<PlayerAction> legalActions(GameState state, Phase.CraftsmanBonus phase) {
    List<PlayerAction> actions = new ArrayList<>();
    for (Good good : phase.options()) {
      actions.add(new PlayerAction.TakeCraftsmanBonus(phase.chooserSeat(), good));
    }
    actions.add(new PlayerAction.PassCraftsmanBonus(phase.chooserSeat()));
    return actions;
  }

  static ActionResult apply(GameState state, Phase.CraftsmanBonus phase, PlayerAction action) {
    return switch (action) {
      case PlayerAction.TakeCraftsmanBonus bonus -> {
        if (!phase.options().contains(bonus.good())) {
          yield ActionResult.reject(
              RejectionReason.GOOD_NOT_PRODUCED,
              "The craftsman produced no " + bonus.good() + " this phase");
        }
        GameState next =
            state.toBuilder()
                .goods(state.goods().take(bonus.good(), 1))
                .build()
                .withPlayer(state.player(phase.chooserSeat()).plusGoods(bonus.good(), 1));
        yield ActionResult.accept(PhaseFlow.endRolePhase(next));
      }
      case PlayerAction.PassCraftsmanBonus ignored ->
          ActionResult.accept(PhaseFlow.endRolePhase(state));
      default ->
          ActionResult.reject(
              RejectionReason.WRONG_PHASE,
              action.getClass().getSimpleName() + " is not a craftsman action");
    };
  }
}
