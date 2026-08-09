package com.PRS.model.engine;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
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
}
