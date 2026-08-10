package com.PRS.ai;

import com.PRS.session.actors.Actor;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Every AI engine seatable in a game, discoverable by id so {@code puerto-rico-lobby} (via {@code
 * puerto-rico-web}) can offer a specific one when seating.
 *
 * <p>{@code thinkTime} is applied to every engine this registry creates — tests construct one with
 * {@link Duration#ZERO} so games resolve instantly; a real deployment uses a few hundred
 * milliseconds so an AI-vs-AI game is actually watchable.
 */
public final class AiRegistry {

  private static final AiEngineInfo RANDOM =
      new AiEngineInfo("random", "Random", "Picks uniformly among the legal moves.");

  private final Duration thinkTime;

  public AiRegistry(Duration thinkTime) {
    this.thinkTime = thinkTime;
  }

  public List<AiEngineInfo> available() {
    return List.of(RANDOM);
  }

  /** Empty for an unrecognized {@code engineId}, never throws. */
  public Optional<Actor> create(String engineId, String displayName, long seed) {
    if (engineId.equals(RANDOM.id())) {
      return Optional.of(new RandomAi(displayName, seed, thinkTime));
    }
    return Optional.empty();
  }
}
