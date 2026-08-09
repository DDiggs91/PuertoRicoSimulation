package com.PRS.lobby;

import com.PRS.model.actions.PlayerAction;
import com.PRS.session.actors.Actor;
import com.PRS.session.actors.Decision;
import java.util.concurrent.CompletableFuture;

/** Actor stand-ins for lobby tests: the lobby doesn't care about play, just seat plumbing. */
final class StubActors {

  private StubActors() {}

  /** Always answers instantly with the first legal option. */
  static Actor named(String name) {
    return new Actor() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public CompletableFuture<PlayerAction> decide(Decision decision) {
        return CompletableFuture.completedFuture(decision.options().getFirst());
      }
    };
  }

  /** Never answers on its own; test code drives it via {@link DeferredActor#answer}. */
  static DeferredActor deferred(String name) {
    return new DeferredActor(name);
  }

  static final class DeferredActor implements Actor {
    private final String name;
    private volatile CompletableFuture<PlayerAction> current;

    private DeferredActor(String name) {
      this.name = name;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public synchronized CompletableFuture<PlayerAction> decide(Decision decision) {
      current = new CompletableFuture<>();
      return current;
    }

    boolean hasPending() {
      CompletableFuture<PlayerAction> pending = current;
      return pending != null && !pending.isDone();
    }

    void answer(PlayerAction action) {
      current.complete(action);
    }
  }
}
