package com.PRS.model.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.TestGames;
import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.TileType;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.goods.Good;
import com.PRS.model.rolecards.Role;
import com.PRS.model.scoring.ScoreBreakdown;
import com.PRS.model.scoring.Scorer;
import java.util.List;
import java.util.Random;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** The Command/Query contract itself, plus the three ways a game can end. */
public class GameEngineContractTest {

  @DataProvider(name = "playerCounts")
  public Object[][] playerCounts() {
    return new Object[][] {{3}, {4}, {5}};
  }

  /** Plays a whole game by picking uniformly at random from the legal actions. */
  private static GameState playRandomGame(int players, long seed) {
    GameState state = TestGames.newGame(players, seed);
    Random random = new Random(seed);
    for (int step = 0; step < 20_000 && !state.isOver(); step++) {
      List<PlayerAction> legal = GameEngine.legalActions(state);
      assertThat(legal)
          .as("a live game always offers a move, phase %s", state.phase())
          .isNotEmpty();
      state = TestGames.apply(state, RandomPlay.choose(legal, random));
    }
    return state;
  }

  @Test(dataProvider = "playerCounts")
  public void aRandomGameAlwaysReachesAScoredEnding(int players) {
    GameState state = playRandomGame(players, 20260808L + players);

    assertThat(state.isOver()).isTrue();
    assertThat(GameEngine.legalActions(state)).isEmpty();

    List<ScoreBreakdown> standings = Scorer.finalStandings(state);
    assertThat(standings).hasSize(players);
    assertThat(standings.getFirst().total()).isGreaterThanOrEqualTo(standings.getLast().total());
  }

  @Test
  public void severalSeedsAllTerminate() {
    for (long seed = 1; seed <= 8; seed++) {
      assertThat(playRandomGame(4, seed).isOver()).as("seed %d", seed).isTrue();
    }
  }

  @Test
  public void applyNeverMutatesTheStateHandedToIt() {
    GameState before = TestGames.newGame(4);
    GameState snapshot = before;

    GameState after = TestGames.chooseRole(before, Role.SETTLER);

    assertThat(before).isEqualTo(snapshot);
    assertThat(before.phase()).isInstanceOf(Phase.RoleSelection.class);
    assertThat(after).isNotEqualTo(before);
  }

  @Test
  public void aRejectedActionLeavesTheStateExactlyAsItWas() {
    GameState state = TestGames.chooseRole(TestGames.newGame(4), Role.SETTLER);

    ActionResult result = GameEngine.apply(state, new PlayerAction.SellGood(0, Good.COFFEE));

    assertThat(result).isInstanceOf(ActionResult.Rejected.class);
    assertThat(state.phase()).isInstanceOf(Phase.SettlerPhase.class);
  }

  @Test
  public void everyActionOfferedByLegalActionsIsAccepted() {
    GameState state = TestGames.newGame(4, 99L);
    Random random = new Random(99L);

    for (int step = 0; step < 400 && !state.isOver(); step++) {
      List<PlayerAction> legal = GameEngine.legalActions(state);
      for (PlayerAction action : legal) {
        assertThat(GameEngine.apply(state, action))
            .as("offered action %s in %s", action, state.phase())
            .isInstanceOf(ActionResult.Accepted.class);
      }
      state = TestGames.apply(state, RandomPlay.choose(legal, random));
    }
  }

  @Test
  public void actionsSubmittedForTheWrongSeatAreRefused() {
    GameState state = TestGames.chooseRole(TestGames.newGame(4), Role.SETTLER);

    ActionResult result = GameEngine.apply(state, new PlayerAction.PassSettling(2));

    assertThat(((ActionResult.Rejected) result).reason()).isEqualTo(RejectionReason.NOT_YOUR_TURN);
  }

  @Test
  public void anActionForTheWrongPhaseIsRefusedRatherThanThrown() {
    GameState state = TestGames.chooseRole(TestGames.newGame(4), Role.SETTLER);

    assertThat(
            TestGames.reject(state, new PlayerAction.BuildBuilding(0, BuildingType.WHARF)).reason())
        .isEqualTo(RejectionReason.WRONG_PHASE);
  }

  @Test
  public void aFinishedGameAcceptsNothing() {
    GameState state = playRandomGame(3, 5L);

    ActionResult result = GameEngine.apply(state, new PlayerAction.SelectRole(0, Role.SETTLER));

    assertThat(((ActionResult.Rejected) result).reason()).isEqualTo(RejectionReason.GAME_OVER);
  }

  // --- end conditions ---

  @Test
  public void theRoundIsPlayedOutBeforeTheGameEnds() {
    GameState state = TestGames.newGame(3).toBuilder().finalRound(true).build();
    assertThat(state.isOver()).isFalse();

    // All three players still choose a role.
    for (int seat = 0; seat < 3; seat++) {
      assertThat(state.phase()).isInstanceOf(Phase.RoleSelection.class);
      assertThat(state.phase().actorSeat()).isEqualTo(seat);
      state = playOut(state, state.roles().availableRoles().getFirst());
    }

    assertThat(state.isOver()).isTrue();
  }

  @Test
  public void anExhaustedColonistSupplyEndsTheGame() {
    GameState state = TestGames.newGame(3).toBuilder().colonistSupply(0).build();
    state = playOut(state, Role.MAYOR);

    assertThat(state.finalRound()).isTrue();
    state = finishRound(state);
    assertThat(state.isOver()).isTrue();
  }

  @Test
  public void aTwelfthCitySpaceEndsTheGame() {
    GameState state = TestGames.newGame(3);
    var fixture = TestGames.player(0).doubloons(30);
    for (BuildingType filler :
        List.of(
            BuildingType.SMALL_MARKET,
            BuildingType.HACIENDA,
            BuildingType.CONSTRUCTION_HUT,
            BuildingType.SMALL_WAREHOUSE,
            BuildingType.HOSPICE,
            BuildingType.OFFICE,
            BuildingType.LARGE_MARKET,
            BuildingType.LARGE_WAREHOUSE,
            BuildingType.FACTORY,
            BuildingType.UNIVERSITY,
            BuildingType.HARBOR)) {
      fixture = fixture.building(filler, 0);
    }
    state = state.withPlayer(fixture.build());
    state = TestGames.chooseRole(state, Role.BUILDER);
    state = TestGames.apply(state, new PlayerAction.BuildBuilding(0, BuildingType.WHARF));
    state = TestGames.apply(state, new PlayerAction.PassBuilding(1));
    state = TestGames.apply(state, new PlayerAction.PassBuilding(2));

    assertThat(state.finalRound()).isTrue();
    state = finishRound(state);
    assertThat(state.isOver()).isTrue();
  }

  @Test
  public void anExhaustedVictoryPointSupplyEndsTheGame() {
    GameState state = TestGames.newGame(3);
    state =
        state.toBuilder()
            .victoryPointSupply(1)
            .build()
            .withPlayer(TestGames.player(0).staffed(TileType.CORN, 2).build());
    state = playOut(state, Role.CRAFTSMAN);
    state = playOut(state, Role.CAPTAIN);

    assertThat(state.victoryPointSupply()).isZero();
    assertThat(state.finalRound()).isTrue();
    state = finishRound(state);
    assertThat(state.isOver()).isTrue();
  }

  /** Chooses a role and plays its phase out with the first legal action each time. */
  private static GameState playOut(GameState state, Role role) {
    GameState next = TestGames.chooseRole(state, role);
    while (!(next.phase() instanceof Phase.RoleSelection) && !next.isOver()) {
      next = TestGames.apply(next, GameEngine.legalActions(next).getFirst());
    }
    return next;
  }

  private static GameState finishRound(GameState state) {
    while (!state.isOver()) {
      state = playOut(state, state.roles().availableRoles().getFirst());
    }
    return state;
  }
}
