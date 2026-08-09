package com.PRS.model.engine;

import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.rolecards.Role;
import java.util.List;

/**
 * Movement between phases: entering a role's phase, finishing one, and closing out a round.
 *
 * <p>Which seat chooses next is derived from how many role cards have been taken, so no separate
 * "seats that have chosen" bookkeeping is needed.
 */
final class PhaseFlow {

  private PhaseFlow() {}

  /** A colonist pulled from the supply, falling back to the colonist ship. */
  record ColonistDraw(GameState state, boolean drawn) {}

  static ColonistDraw drawColonist(GameState state) {
    if (state.colonistSupply() > 0) {
      return new ColonistDraw(
          state.toBuilder().colonistSupply(state.colonistSupply() - 1).build(), true);
    }
    if (state.colonistsOnShip() > 0) {
      return new ColonistDraw(
          state.toBuilder().colonistsOnShip(state.colonistsOnShip() - 1).build(), true);
    }
    return new ColonistDraw(state, false);
  }

  /**
   * Awards victory points. The chip supply running dry ends the game after this round but does not
   * stop players scoring — the rulebook has them tracked on paper from that point.
   */
  static GameState awardVictoryPoints(GameState state, int seat, int points) {
    if (points <= 0) {
      return state;
    }
    GameState next = state.withPlayer(state.player(seat).plusVictoryPoints(points));
    int remaining = next.victoryPointSupply() - points;
    return next.toBuilder()
        .victoryPointSupply(Math.max(0, remaining))
        .finalRound(next.finalRound() || remaining <= 0)
        .build();
  }

  /** Sets up the phase for a role that has just been chosen, resolving it outright if automatic. */
  static GameState beginRolePhase(GameState state, Role role, int chooserSeat) {
    return switch (role) {
      case SETTLER ->
          state.withPhase(
              new Phase.SettlerPhase(chooserSeat, state.turnOrderFrom(chooserSeat), false));
      case MAYOR -> MayorPhaseHandler.begin(state, chooserSeat);
      case BUILDER ->
          state.withPhase(new Phase.BuilderPhase(chooserSeat, state.turnOrderFrom(chooserSeat)));
      case CRAFTSMAN -> CraftsmanPhaseHandler.begin(state, chooserSeat);
      case TRADER ->
          state.withPhase(new Phase.TraderPhase(chooserSeat, state.turnOrderFrom(chooserSeat)));
      case CAPTAIN -> CaptainPhaseHandler.begin(state, chooserSeat);
      case PROSPECTOR -> endRolePhase(state.withPlayer(state.player(chooserSeat).plusDoubloons(1)));
    };
  }

  /** The current role's phase is done: hand the choice to the next player, or close the round. */
  static GameState endRolePhase(GameState state) {
    int chosen = rolesChosen(state);
    if (chosen >= state.playerCount()) {
      return endRound(state);
    }
    return state.withPhase(
        new Phase.RoleSelection((state.governorSeat() + chosen) % state.playerCount()));
  }

  private static GameState endRound(GameState state) {
    GameState next =
        state.toBuilder()
            .roles(state.roles().endRound())
            .governorSeat(state.nextSeat(state.governorSeat()))
            .build();
    return next.withPhase(
        next.finalRound() ? new Phase.GameOver() : new Phase.RoleSelection(next.governorSeat()));
  }

  private static int rolesChosen(GameState state) {
    return (int) state.roles().cards().stream().filter(card -> card.isTaken()).count();
  }

  /** Advances a queue-driven phase, or returns null once every player has acted. */
  static List<Integer> advanceQueue(List<Integer> queue) {
    return queue.size() <= 1 ? null : queue.subList(1, queue.size());
  }
}
