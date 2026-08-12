package com.PRS.model.engine;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.goods.Good;
import java.util.List;

/**
 * The Command/Query contract for a game of Puerto Rico.
 *
 * <p>Both operations are pure functions of a {@link GameState}: nothing is mutated, so a caller can
 * hold snapshots, replay a game, or search ahead without copying anything. An illegal action comes
 * back as {@link ActionResult.Rejected} rather than an exception.
 */
public final class GameEngine {

  private GameEngine() {}

  /** Every action the player to move may legally take. Empty only once the game is over. */
  public static List<PlayerAction> legalActions(GameState state) {
    return switch (state.phase()) {
      case Phase.RoleSelection phase -> RoleSelectionHandler.legalActions(state, phase);
      case Phase.SettlerPhase phase -> SettlerPhaseHandler.legalActions(state, phase);
      case Phase.MayorPhase phase -> MayorPhaseHandler.legalActions(state, phase);
      case Phase.BuilderPhase phase -> BuilderPhaseHandler.legalActions(state, phase);
      case Phase.CraftsmanBonus phase -> CraftsmanPhaseHandler.legalActions(state, phase);
      case Phase.TraderPhase phase -> TraderPhaseHandler.legalActions(state, phase);
      case Phase.CaptainLoading phase -> CaptainPhaseHandler.legalActions(state, phase);
      case Phase.CaptainStorage phase -> CaptainPhaseHandler.legalActions(state, phase);
      case Phase.GameOver ignored -> List.of();
    };
  }

  /** Applies an action, returning the resulting state or the reason it was refused. */
  public static ActionResult apply(GameState state, PlayerAction action) {
    if (state.isOver()) {
      return ActionResult.reject(RejectionReason.GAME_OVER, "The game has finished");
    }
    if (action.seat() != state.phase().actorSeat()) {
      return ActionResult.reject(
          RejectionReason.NOT_YOUR_TURN,
          "Seat %d is to act, not %d".formatted(state.phase().actorSeat(), action.seat()));
    }

    return switch (state.phase()) {
      case Phase.RoleSelection phase -> RoleSelectionHandler.apply(state, phase, action);
      case Phase.SettlerPhase phase -> SettlerPhaseHandler.apply(state, phase, action);
      case Phase.MayorPhase phase -> MayorPhaseHandler.apply(state, phase, action);
      case Phase.BuilderPhase phase -> BuilderPhaseHandler.apply(state, phase, action);
      case Phase.CraftsmanBonus phase -> CraftsmanPhaseHandler.apply(state, phase, action);
      case Phase.TraderPhase phase -> TraderPhaseHandler.apply(state, phase, action);
      case Phase.CaptainLoading phase -> CaptainPhaseHandler.apply(state, phase, action);
      case Phase.CaptainStorage phase -> CaptainPhaseHandler.apply(state, phase, action);
      case Phase.GameOver ignored ->
          ActionResult.reject(RejectionReason.GAME_OVER, "The game has finished");
    };
  }

  /**
   * What the player to move would pay for {@code type} right now: the printed cost less the
   * builder's privilege and the quarry discount. Priced, not decided — a caller still has to check
   * {@link #legalActions} to know whether the purchase is actually available.
   *
   * <p>Exposed so a client can display the number without restating the rule; the arithmetic lives
   * in one place and this is the read-only window onto it.
   *
   * @throws IllegalStateException if the game is not in the builder phase, where no such price
   *     exists
   */
  public static int buildCost(GameState state, BuildingType type) {
    if (!(state.phase() instanceof Phase.BuilderPhase phase)) {
      throw new IllegalStateException("Build costs only exist during the builder phase");
    }
    return BuilderPhaseHandler.costFor(state, phase, type);
  }

  /**
   * What the player to move would be paid for selling one barrel of {@code good} right now: the
   * list price plus the trader's privilege and any market bonuses. As with {@link #buildCost}, this
   * prices the sale without asserting it is legal.
   *
   * @throws IllegalStateException if the game is not in the trader phase, where no such price
   *     exists
   */
  public static int sellPrice(GameState state, Good good) {
    if (!(state.phase() instanceof Phase.TraderPhase phase)) {
      throw new IllegalStateException("Sale prices only exist during the trader phase");
    }
    return TraderPhaseHandler.priceFor(state, phase, good);
  }
}
