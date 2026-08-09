package com.PRS.model.engine;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.PlayerState;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.goods.Good;
import java.util.ArrayList;
import java.util.List;

/** Trader phase: one sale each into a four-slot trading house that rejects duplicate kinds. */
final class TraderPhaseHandler {

  private TraderPhaseHandler() {}

  /** List price plus the trader's privilege and any market bonuses. */
  static int priceFor(GameState state, Phase.TraderPhase phase, Good good) {
    PlayerState player = state.player(phase.actorSeat());
    int privilege = phase.actorSeat() == phase.chooserSeat() ? 1 : 0;
    int markets =
        (player.hasOccupied(BuildingType.SMALL_MARKET) ? 1 : 0)
            + (player.hasOccupied(BuildingType.LARGE_MARKET) ? 2 : 0);
    return good.price() + privilege + markets;
  }

  static boolean canSell(GameState state, PlayerState player, Good good) {
    if (player.goodsCount(good) == 0 || state.tradingHouse().isFull()) {
      return false;
    }
    return !state.tradingHouse().contains(good) || player.hasOccupied(BuildingType.OFFICE);
  }

  static List<PlayerAction> legalActions(GameState state, Phase.TraderPhase phase) {
    int seat = phase.actorSeat();
    PlayerState player = state.player(seat);

    List<PlayerAction> actions = new ArrayList<>();
    for (Good good : Good.values()) {
      if (canSell(state, player, good)) {
        actions.add(new PlayerAction.SellGood(seat, good));
      }
    }
    actions.add(new PlayerAction.PassTrading(seat));
    return actions;
  }

  static ActionResult apply(GameState state, Phase.TraderPhase phase, PlayerAction action) {
    int seat = phase.actorSeat();
    PlayerState player = state.player(seat);

    return switch (action) {
      case PlayerAction.SellGood sell -> {
        Good good = sell.good();
        if (player.goodsCount(good) == 0) {
          yield ActionResult.reject(RejectionReason.GOOD_NOT_HELD, "No " + good + " to sell");
        }
        if (state.tradingHouse().isFull()) {
          yield ActionResult.reject(
              RejectionReason.TRADING_HOUSE_FULL, "The trading house has no free slot");
        }
        if (state.tradingHouse().contains(good) && !player.hasOccupied(BuildingType.OFFICE)) {
          yield ActionResult.reject(
              RejectionReason.DUPLICATE_GOOD_IN_TRADING_HOUSE,
              good + " is already at the trading house and the player has no occupied Office");
        }
        GameState next =
            state.toBuilder()
                .tradingHouse(state.tradingHouse().sell(good))
                .build()
                .withPlayer(player.plusGoods(good, -1).plusDoubloons(priceFor(state, phase, good)));
        yield ActionResult.accept(advance(next, phase));
      }
      case PlayerAction.PassTrading ignored -> ActionResult.accept(advance(state, phase));
      default ->
          ActionResult.reject(
              RejectionReason.WRONG_PHASE,
              action.getClass().getSimpleName() + " is not a trader action");
    };
  }

  /** The phase also ends early the moment the trading house fills. */
  private static GameState advance(GameState state, Phase.TraderPhase phase) {
    List<Integer> rest = PhaseFlow.advanceQueue(phase.queue());
    if (rest != null && !state.tradingHouse().isFull()) {
      return state.withPhase(new Phase.TraderPhase(phase.chooserSeat(), rest));
    }
    // The trader's last duty: clear the house, but only when all four slots are filled.
    return PhaseFlow.endRolePhase(
        state.toBuilder().tradingHouse(state.tradingHouse().clearIfFull()).build());
  }
}
