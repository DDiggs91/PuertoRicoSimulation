package com.PRS.model.engine;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.rolecards.Role;
import java.util.ArrayList;
import java.util.List;

/** Choosing a role card, which also collects any doubloons that have piled up on it. */
final class RoleSelectionHandler {

  private RoleSelectionHandler() {}

  static List<PlayerAction> legalActions(GameState state, Phase.RoleSelection phase) {
    List<PlayerAction> actions = new ArrayList<>();
    for (Role role : state.roles().availableRoles()) {
      actions.add(new PlayerAction.SelectRole(phase.actorSeat(), role));
    }
    return actions;
  }

  static ActionResult apply(GameState state, Phase.RoleSelection phase, PlayerAction action) {
    if (!(action instanceof PlayerAction.SelectRole select)) {
      return ActionResult.reject(
          RejectionReason.WRONG_PHASE, "A role must be chosen before anything else happens");
    }
    if (!state.roles().isAvailable(select.role())) {
      return ActionResult.reject(
          RejectionReason.ROLE_UNAVAILABLE, select.role() + " has already been taken this round");
    }

    int seat = phase.actorSeat();
    int doubloons = state.roles().doubloonsOn(select.role());
    GameState next =
        state.toBuilder()
            .roles(state.roles().take(select.role(), seat))
            .build()
            .withPlayer(state.player(seat).plusDoubloons(doubloons));

    return ActionResult.accept(PhaseFlow.beginRolePhase(next, select.role(), seat));
  }
}
