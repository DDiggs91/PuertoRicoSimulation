package com.PRS.ai;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.engine.RandomPlay;
import com.PRS.session.actors.Actor;
import com.PRS.session.actors.Decision;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Picks uniformly at random among the legal options, via {@code RandomPlay.choose} — the same
 * selector {@code puerto-rico-model}'s own fuzz tests use, so both provably share one
 * implementation.
 *
 * <p>{@code thinkTime} paces an AI-vs-AI game so a spectator can actually watch it, without
 * blocking a thread while waiting: the delay is scheduled on {@link
 * CompletableFuture#delayedExecutor}, not slept through.
 */
public final class RandomAi implements Actor {

  private final String name;
  private final Random rng;
  private final Executor executor;

  public RandomAi(String name, long seed, Duration thinkTime) {
    this.name = name;
    this.rng = new Random(seed);
    this.executor =
        thinkTime.isZero()
            ? Runnable::run
            : CompletableFuture.delayedExecutor(thinkTime.toMillis(), TimeUnit.MILLISECONDS);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public CompletableFuture<PlayerAction> decide(Decision decision) {
    return CompletableFuture.supplyAsync(
        () -> RandomPlay.choose(decision.options(), rng), executor);
  }
}
