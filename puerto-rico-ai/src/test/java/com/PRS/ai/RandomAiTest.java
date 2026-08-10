package com.PRS.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.engine.GameEngine;
import com.PRS.model.game.GameConfig;
import com.PRS.model.game.GameSetup;
import com.PRS.model.game.GameState;
import com.PRS.session.actors.Decision;
import com.PRS.session.view.GameView;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.Test;

public class RandomAiTest {

  private static Decision decisionFor(GameState state) {
    int seat = state.phase().actorSeat();
    return new Decision(seat, GameView.of(state, seat), GameEngine.legalActions(state), 1L);
  }

  @Test
  public void decideAlwaysReturnsAnOfferedOption() throws Exception {
    RandomAi ai = new RandomAi("Ana", 7L, Duration.ZERO);
    GameState state = GameSetup.create(new GameConfig(List.of("Ana", "Bo", "Coco"), 7L));

    for (int i = 0; i < 50 && !state.isOver(); i++) {
      Decision decision = decisionFor(state);
      PlayerAction chosen = ai.decide(decision).get(5, TimeUnit.SECONDS);

      assertThat(decision.options()).contains(chosen);
      state = GameEngine.apply(state, chosen).state();
    }
  }

  @Test
  public void theSameSeedReplaysIdentically() throws Exception {
    List<PlayerAction> first = playWithAi(new RandomAi("Ana", 11L, Duration.ZERO), 11L);
    List<PlayerAction> second = playWithAi(new RandomAi("Ana", 11L, Duration.ZERO), 11L);

    assertThat(first).isEqualTo(second);
  }

  @Test
  public void aDifferentSeedDiverges() throws Exception {
    List<PlayerAction> first = playWithAi(new RandomAi("Ana", 11L, Duration.ZERO), 11L);
    List<PlayerAction> second = playWithAi(new RandomAi("Ana", 12L, Duration.ZERO), 12L);

    assertThat(first).isNotEqualTo(second);
  }

  @Test
  public void nameReturnsTheConstructedName() {
    assertThat(new RandomAi("Ana", 1L, Duration.ZERO).name()).isEqualTo("Ana");
  }

  @Test
  public void thinkTimeDelaysCompletionWithoutBlockingTheCaller() throws Exception {
    RandomAi ai = new RandomAi("Ana", 1L, Duration.ofMillis(200));
    GameState state = GameSetup.create(new GameConfig(List.of("Ana", "Bo", "Coco"), 1L));

    long before = System.nanoTime();
    CompletableFuture<PlayerAction> future = ai.decide(decisionFor(state));
    long callReturnedAfterMillis = (System.nanoTime() - before) / 1_000_000;

    assertThat(callReturnedAfterMillis).as("decide() must not block the caller").isLessThan(50);
    assertThat(future.isDone()).isFalse();

    future.get(2, TimeUnit.SECONDS);
    long totalMillis = (System.nanoTime() - before) / 1_000_000;
    assertThat(totalMillis).isGreaterThanOrEqualTo(180);
  }

  /** Every seat plays via the same seeded AI, driven single-threaded — deterministic by seed. */
  private static List<PlayerAction> playWithAi(RandomAi seatZeroAi, long seed) throws Exception {
    GameState state = GameSetup.create(new GameConfig(List.of("Ana", "Bo", "Coco"), seed));
    RandomAi[] seats = {
      seatZeroAi,
      new RandomAi("Bo", seed + 1, Duration.ZERO),
      new RandomAi("Coco", seed + 2, Duration.ZERO)
    };
    List<PlayerAction> history = new java.util.ArrayList<>();
    for (int i = 0; i < 100 && !state.isOver(); i++) {
      Decision decision = decisionFor(state);
      PlayerAction chosen = seats[decision.seat()].decide(decision).get(5, TimeUnit.SECONDS);
      history.add(chosen);
      state = GameEngine.apply(state, chosen).state();
    }
    return history;
  }
}
