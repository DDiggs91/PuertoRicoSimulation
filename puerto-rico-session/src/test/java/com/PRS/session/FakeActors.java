package com.PRS.session;

import com.PRS.model.actions.PlayerAction;
import com.PRS.session.actors.Actor;
import com.PRS.session.actors.Decision;
import java.util.concurrent.CompletableFuture;

/** Actor stand-ins for session tests: no real strategy, just controllable answers. */
final class FakeActors {

  private FakeActors() {}

  /** Always answers instantly with the first legal option — the simplest possible AI. */
  static Actor firstLegal(String name) {
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

  /** Always fails to answer — simulates a broken AI plugin. */
  static Actor failing(String name) {
    return new Actor() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public CompletableFuture<PlayerAction> decide(Decision decision) {
        return CompletableFuture.failedFuture(new RuntimeException(name + " always fails"));
      }
    };
  }

  /**
   * Answers with the first legal option, but stamped for a different seat — always refused with
   * {@code NOT_YOUR_TURN}, regardless of phase.
   */
  static Actor illegal(String name) {
    return new Actor() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public CompletableFuture<PlayerAction> decide(Decision decision) {
        return CompletableFuture.completedFuture(withWrongSeat(decision.options().getFirst()));
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

  private static PlayerAction withWrongSeat(PlayerAction action) {
    int wrongSeat = (action.seat() + 1) % 5;
    return switch (action) {
      case PlayerAction.SelectRole a -> new PlayerAction.SelectRole(wrongSeat, a.role());
      case PlayerAction.TakeFaceUpTile a ->
          new PlayerAction.TakeFaceUpTile(wrongSeat, a.faceUpIndex());
      case PlayerAction.TakeQuarry a -> new PlayerAction.TakeQuarry(wrongSeat);
      case PlayerAction.TakeHaciendaTile a -> new PlayerAction.TakeHaciendaTile(wrongSeat);
      case PlayerAction.SkipHacienda a -> new PlayerAction.SkipHacienda(wrongSeat);
      case PlayerAction.PassSettling a -> new PlayerAction.PassSettling(wrongSeat);
      case PlayerAction.PlaceColonist a -> new PlayerAction.PlaceColonist(wrongSeat, a.slot());
      case PlayerAction.EndColonistPlacement a -> new PlayerAction.EndColonistPlacement(wrongSeat);
      case PlayerAction.BuildBuilding a -> new PlayerAction.BuildBuilding(wrongSeat, a.type());
      case PlayerAction.PassBuilding a -> new PlayerAction.PassBuilding(wrongSeat);
      case PlayerAction.TakeCraftsmanBonus a ->
          new PlayerAction.TakeCraftsmanBonus(wrongSeat, a.good());
      case PlayerAction.PassCraftsmanBonus a -> new PlayerAction.PassCraftsmanBonus(wrongSeat);
      case PlayerAction.SellGood a -> new PlayerAction.SellGood(wrongSeat, a.good());
      case PlayerAction.PassTrading a -> new PlayerAction.PassTrading(wrongSeat);
      case PlayerAction.LoadShip a -> new PlayerAction.LoadShip(wrongSeat, a.shipIndex(), a.good());
      case PlayerAction.LoadWharf a -> new PlayerAction.LoadWharf(wrongSeat, a.good());
      case PlayerAction.DeclineWharf a -> new PlayerAction.DeclineWharf(wrongSeat);
      case PlayerAction.StoreGoods a ->
          new PlayerAction.StoreGoods(wrongSeat, a.warehouseKinds(), a.singleBarrel());
    };
  }
}
