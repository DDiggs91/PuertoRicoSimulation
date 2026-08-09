package com.PRS.model.engine;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.PlayerState;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.buildings.PlacedBuilding;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import java.util.ArrayList;
import java.util.List;

/** Builder phase: one building each, discounted by the builder's privilege and by quarries. */
final class BuilderPhaseHandler {

  private BuilderPhaseHandler() {}

  /**
   * Cost after the builder's privilege and the quarry discount, which is capped by the building's
   * display column — and a column is simply the building's victory point value.
   */
  static int costFor(GameState state, Phase.BuilderPhase phase, BuildingType type) {
    PlayerState player = state.player(phase.actorSeat());
    int privilege = phase.actorSeat() == phase.chooserSeat() ? 1 : 0;
    int quarries = Math.min(player.occupiedQuarries(), type.maxQuarryDiscount());
    return Math.max(0, type.cost() - privilege - quarries);
  }

  static List<PlayerAction> legalActions(GameState state, Phase.BuilderPhase phase) {
    int seat = phase.actorSeat();
    PlayerState player = state.player(seat);

    List<PlayerAction> actions = new ArrayList<>();
    for (BuildingType type : BuildingType.values()) {
      if (state.buildings().isAvailable(type)
          && !player.owns(type)
          && player.freeCitySpaces() >= type.citySpaces()
          && costFor(state, phase, type) <= player.doubloons()) {
        actions.add(new PlayerAction.BuildBuilding(seat, type));
      }
    }
    actions.add(new PlayerAction.PassBuilding(seat));
    return actions;
  }

  static ActionResult apply(GameState state, Phase.BuilderPhase phase, PlayerAction action) {
    int seat = phase.actorSeat();
    PlayerState player = state.player(seat);

    return switch (action) {
      case PlayerAction.BuildBuilding build -> {
        BuildingType type = build.type();
        if (!state.buildings().isAvailable(type)) {
          yield ActionResult.reject(
              RejectionReason.BUILDING_UNAVAILABLE, "No copies of " + type + " remain");
        }
        if (player.owns(type)) {
          yield ActionResult.reject(RejectionReason.DUPLICATE_BUILDING, "Already owns a " + type);
        }
        if (player.freeCitySpaces() < type.citySpaces()) {
          yield ActionResult.reject(
              RejectionReason.NO_CITY_SPACE,
              "%s needs %d city space(s)".formatted(type, type.citySpaces()));
        }
        int cost = costFor(state, phase, type);
        if (cost > player.doubloons()) {
          yield ActionResult.reject(
              RejectionReason.INSUFFICIENT_DOUBLOONS,
              "%s costs %d, player has %d".formatted(type, cost, player.doubloons()));
        }

        GameState next =
            state.toBuilder()
                .buildings(state.buildings().take(type))
                .build()
                .withPlayer(
                    player.plusDoubloons(-cost).plusBuilding(PlacedBuilding.unstaffed(type)));
        next = staffFromUniversity(next, seat);

        // Filling the twelfth city space ends the game once this round finishes.
        if (next.player(seat).citySpacesUsed() >= PlayerState.CITY_SPACES) {
          next = next.toBuilder().finalRound(true).build();
        }
        yield ActionResult.accept(advance(next, phase));
      }
      case PlayerAction.PassBuilding ignored -> ActionResult.accept(advance(state, phase));
      default ->
          ActionResult.reject(
              RejectionReason.WRONG_PHASE,
              action.getClass().getSimpleName() + " is not a builder action");
    };
  }

  /** An occupied University staffs each new building with one colonist, whatever its size. */
  private static GameState staffFromUniversity(GameState state, int seat) {
    PlayerState player = state.player(seat);
    if (!player.hasOccupied(BuildingType.UNIVERSITY)) {
      return state;
    }
    PhaseFlow.ColonistDraw draw = PhaseFlow.drawColonist(state);
    if (!draw.drawn()) {
      return state;
    }
    PlayerState owner = draw.state().player(seat);
    List<PlacedBuilding> buildings = new ArrayList<>(owner.buildings());
    int last = buildings.size() - 1;
    buildings.set(last, buildings.get(last).withColonists(1));
    return draw.state().withPlayer(owner.toBuilder().buildings(buildings).build());
  }

  private static GameState advance(GameState state, Phase.BuilderPhase phase) {
    List<Integer> rest = PhaseFlow.advanceQueue(phase.queue());
    if (rest != null) {
      return state.withPhase(new Phase.BuilderPhase(phase.chooserSeat(), rest));
    }
    return PhaseFlow.endRolePhase(state);
  }
}
