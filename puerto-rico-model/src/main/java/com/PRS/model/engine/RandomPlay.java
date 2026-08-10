package com.PRS.model.engine;

import com.PRS.model.actions.PlayerAction;
import java.util.List;
import java.util.Random;

/** A uniform sampler over {@link GameEngine#legalActions}, not a strategy. */
public final class RandomPlay {

  private RandomPlay() {}

  public static PlayerAction choose(List<PlayerAction> legal, Random rng) {
    return legal.get(rng.nextInt(legal.size()));
  }
}
