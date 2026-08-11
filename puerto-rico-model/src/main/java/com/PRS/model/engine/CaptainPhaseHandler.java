package com.PRS.model.engine;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.CargoShip;
import com.PRS.model.boards.PlayerState;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.goods.Good;
import com.PRS.model.goods.GoodsSupply;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Captain phase: shipping, which is the one compulsory action in the game.
 *
 * <p>Turns cycle clockwise for as long as anybody can still load, so players get several turns. A
 * player with nothing loadable is skipped rather than asked to pass — loading a cargo ship is
 * compulsory, so there is no general pass action. The one exception is the Wharf, which the
 * rulebook makes optional: a player whose only remaining option is the Wharf may decline it with
 * {@link PlayerAction.DeclineWharf}.
 */
final class CaptainPhaseHandler {

  /**
   * docs/game-rules.md §6: "Capacity 11 barrels." No kind has more than 11 barrels in the game, so
   * this never binds today — it is here so the limit lives in the rules layer rather than being an
   * accident of {@code Good}'s supply table.
   */
  static final int WHARF_CAPACITY = 11;

  private CaptainPhaseHandler() {}

  static GameState begin(GameState state, int chooserSeat) {
    Phase.CaptainLoading loading =
        new Phase.CaptainLoading(chooserSeat, chooserSeat, Set.of(), false);
    return nextLoader(state, loading, chooserSeat);
  }

  // --- loading ---

  /**
   * Ships this player may put a kind onto. A kind already at sea may only go on the ship carrying
   * it; otherwise any empty ship will do, but it must be one that takes the most barrels.
   */
  static List<Integer> candidateShips(GameState state, PlayerState player, Good good) {
    int held = player.goodsCount(good);
    if (held == 0) {
      return List.of();
    }

    for (int i = 0; i < state.ships().size(); i++) {
      CargoShip ship = state.ships().get(i);
      if (ship.cargoKind().orElse(null) == good) {
        return ship.isFull() ? List.of() : List.of(i);
      }
    }

    int best = 0;
    List<Integer> candidates = new ArrayList<>();
    for (int i = 0; i < state.ships().size(); i++) {
      CargoShip ship = state.ships().get(i);
      if (!ship.isEmpty()) {
        continue;
      }
      int loadable = Math.min(held, ship.freeSpace());
      if (loadable > best) {
        best = loadable;
        candidates.clear();
      }
      if (loadable == best && loadable > 0) {
        candidates.add(i);
      }
    }
    return candidates;
  }

  /**
   * Whether one specific ship could take a kind at all, ignoring whether another ship would take
   * more. Also guards the index, so a nonsense one is a rejection rather than an exception.
   */
  static boolean accepts(GameState state, int shipIndex, Good good) {
    if (shipIndex < 0 || shipIndex >= state.ships().size()) {
      return false;
    }
    for (int i = 0; i < state.ships().size(); i++) {
      if (i != shipIndex && state.ships().get(i).cargoKind().orElse(null) == good) {
        return false;
      }
    }
    return state.ships().get(shipIndex).accepts(good);
  }

  static boolean mayUseWharf(PlayerState player, Phase.CaptainLoading phase) {
    return player.hasOccupied(BuildingType.WHARF)
        && !phase.wharfUsed().contains(player.seat())
        && player.totalGoods() > 0;
  }

  /** Whether any cargo ship would take something this player holds — the compulsory part. */
  static boolean canLoadAShip(GameState state, PlayerState player) {
    for (Good good : Good.values()) {
      if (!candidateShips(state, player, good).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  static boolean canLoad(GameState state, Phase.CaptainLoading phase, int seat) {
    PlayerState player = state.player(seat);
    return mayUseWharf(player, phase) || canLoadAShip(state, player);
  }

  static List<PlayerAction> legalActions(GameState state, Phase.CaptainLoading phase) {
    int seat = phase.actorSeat();
    PlayerState player = state.player(seat);

    List<PlayerAction> actions = new ArrayList<>();
    for (Good good : Good.values()) {
      for (int shipIndex : candidateShips(state, player, good)) {
        actions.add(new PlayerAction.LoadShip(seat, shipIndex, good));
      }
    }
    if (mayUseWharf(player, phase)) {
      for (Good good : Good.values()) {
        if (player.goodsCount(good) > 0) {
          actions.add(new PlayerAction.LoadWharf(seat, good));
        }
      }
      // The Wharf is optional. When it is the only thing keeping this player at the table, the
      // turn would otherwise be a forced Wharf use, so declining has to be on the menu.
      if (actions.stream().allMatch(PlayerAction.LoadWharf.class::isInstance)) {
        actions.add(new PlayerAction.DeclineWharf(seat));
      }
    }
    return actions;
  }

  static ActionResult apply(GameState state, Phase.CaptainLoading phase, PlayerAction action) {
    int seat = phase.actorSeat();
    PlayerState player = state.player(seat);

    return switch (action) {
      case PlayerAction.LoadShip load -> {
        List<Integer> candidates = candidateShips(state, player, load.good());
        if (player.goodsCount(load.good()) == 0) {
          yield ActionResult.reject(
              RejectionReason.GOOD_NOT_HELD, "No " + load.good() + " to ship");
        }
        if (candidates.isEmpty()) {
          yield ActionResult.reject(
              RejectionReason.SHIP_UNAVAILABLE, "No ship can take " + load.good());
        }
        if (!candidates.contains(load.shipIndex())) {
          yield accepts(state, load.shipIndex(), load.good())
              ? ActionResult.reject(
                  RejectionReason.SUBOPTIMAL_SHIP,
                  "A player must load onto the ship that takes the most barrels")
              : ActionResult.reject(
                  RejectionReason.SHIP_UNAVAILABLE,
                  "Ship %d cannot take %s".formatted(load.shipIndex(), load.good()));
        }

        CargoShip ship = state.ships().get(load.shipIndex());
        int barrels = Math.min(player.goodsCount(load.good()), ship.freeSpace());
        List<CargoShip> ships = new ArrayList<>(state.ships());
        ships.set(load.shipIndex(), ship.load(load.good(), barrels));

        GameState next =
            state.toBuilder()
                .ships(ships)
                .build()
                .withPlayer(player.plusGoods(load.good(), -barrels));
        yield ActionResult.accept(scoreAndAdvance(next, phase, seat, barrels));
      }
      case PlayerAction.LoadWharf wharf -> {
        if (!mayUseWharf(player, phase)) {
          yield ActionResult.reject(
              RejectionReason.WHARF_UNAVAILABLE,
              "No occupied Wharf, or it has already been used this phase");
        }
        if (player.goodsCount(wharf.good()) == 0) {
          yield ActionResult.reject(
              RejectionReason.GOOD_NOT_HELD, "No " + wharf.good() + " to ship");
        }
        int barrels = Math.min(player.goodsCount(wharf.good()), WHARF_CAPACITY);
        // The Wharf sends goods straight back to the supply, scoring as if shipped.
        GameState next =
            state.toBuilder()
                .goods(state.goods().put(wharf.good(), barrels))
                .build()
                .withPlayer(player.plusGoods(wharf.good(), -barrels));
        Set<Integer> wharfUsed = new HashSet<>(phase.wharfUsed());
        wharfUsed.add(seat);
        Phase.CaptainLoading used =
            new Phase.CaptainLoading(
                phase.chooserSeat(), phase.actorSeat(), wharfUsed, phase.bonusUsed());
        yield ActionResult.accept(scoreAndAdvance(next, used, seat, barrels));
      }
      case PlayerAction.DeclineWharf ignored -> {
        if (canLoadAShip(state, player)) {
          yield ActionResult.reject(
              RejectionReason.LOADING_IS_MANDATORY,
              "A player who can still load a cargo ship must do so");
        }
        if (!mayUseWharf(player, phase)) {
          yield ActionResult.reject(
              RejectionReason.WHARF_UNAVAILABLE, "There is no Wharf use on offer to decline");
        }
        // Declining spends the Wharf for the phase. Ships only unload once the phase is over, so
        // nothing can make it worth using later, and marking it is what stops the turn coming
        // straight back round to the same forced choice.
        Set<Integer> resolved = new HashSet<>(phase.wharfUsed());
        resolved.add(seat);
        Phase.CaptainLoading declined =
            new Phase.CaptainLoading(
                phase.chooserSeat(), phase.actorSeat(), resolved, phase.bonusUsed());
        yield ActionResult.accept(nextLoader(state, declined, state.nextSeat(seat)));
      }
      default ->
          ActionResult.reject(
              RejectionReason.WRONG_PHASE,
              action.getClass().getSimpleName() + " is not a captain loading action");
    };
  }

  /** One VP per barrel, plus the Harbor's per-delivery bonus and the captain's one-off bonus. */
  private static GameState scoreAndAdvance(
      GameState state, Phase.CaptainLoading phase, int seat, int barrels) {
    PlayerState player = state.player(seat);
    boolean captainBonus = seat == phase.chooserSeat() && !phase.bonusUsed();
    int points =
        barrels + (player.hasOccupied(BuildingType.HARBOR) ? 1 : 0) + (captainBonus ? 1 : 0);

    GameState scored = PhaseFlow.awardVictoryPoints(state, seat, points);
    Phase.CaptainLoading updated =
        new Phase.CaptainLoading(
            phase.chooserSeat(),
            phase.actorSeat(),
            phase.wharfUsed(),
            phase.bonusUsed() || captainBonus);
    return nextLoader(scored, updated, scored.nextSeat(seat));
  }

  /** Hands the turn to the next seat that can load, or moves on to storage when nobody can. */
  private static GameState nextLoader(GameState state, Phase.CaptainLoading phase, int fromSeat) {
    for (int i = 0; i < state.playerCount(); i++) {
      int seat = (fromSeat + i) % state.playerCount();
      if (canLoad(state, phase, seat)) {
        return state.withPhase(
            new Phase.CaptainLoading(
                phase.chooserSeat(), seat, phase.wharfUsed(), phase.bonusUsed()));
      }
    }
    return beginStorage(state, phase.chooserSeat());
  }

  // --- storage ---

  private static GameState beginStorage(GameState state, int chooserSeat) {
    List<Integer> queue =
        state.turnOrderFrom(chooserSeat).stream()
            .filter(seat -> state.player(seat).totalGoods() > 0)
            .toList();
    if (queue.isEmpty()) {
      return PhaseFlow.endRolePhase(unloadFullShips(state));
    }
    return state.withPhase(new Phase.CaptainStorage(chooserSeat, queue));
  }

  /** How many whole kinds a player's warehouses protect. */
  static int warehouseSlots(PlayerState player) {
    return (player.hasOccupied(BuildingType.SMALL_WAREHOUSE) ? 1 : 0)
        + (player.hasOccupied(BuildingType.LARGE_WAREHOUSE) ? 2 : 0);
  }

  static List<PlayerAction> legalActions(GameState state, Phase.CaptainStorage phase) {
    int seat = phase.actorSeat();
    PlayerState player = state.player(seat);
    List<Good> held =
        List.copyOf(
            java.util.Arrays.stream(Good.values())
                .filter(good -> player.goodsCount(good) > 0)
                .toList());

    List<PlayerAction> actions = new ArrayList<>();
    for (List<Good> kinds : subsetsUpTo(held, warehouseSlots(player))) {
      actions.add(new PlayerAction.StoreGoods(seat, kinds, null));
      for (Good single : held) {
        actions.add(new PlayerAction.StoreGoods(seat, kinds, single));
      }
    }
    return actions;
  }

  static ActionResult apply(GameState state, Phase.CaptainStorage phase, PlayerAction action) {
    int seat = phase.actorSeat();
    PlayerState player = state.player(seat);

    if (!(action instanceof PlayerAction.StoreGoods store)) {
      return ActionResult.reject(
          RejectionReason.WRONG_PHASE,
          action.getClass().getSimpleName() + " is not a storage action");
    }

    List<Good> kinds = store.warehouseKinds() == null ? List.of() : store.warehouseKinds();
    if (Set.copyOf(kinds).size() != kinds.size()) {
      return ActionResult.reject(RejectionReason.INVALID_STORAGE, "Repeated warehouse kind");
    }
    if (kinds.size() > warehouseSlots(player)) {
      return ActionResult.reject(
          RejectionReason.INVALID_STORAGE,
          "Warehouses protect %d kind(s), got %d".formatted(warehouseSlots(player), kinds.size()));
    }
    for (Good good : kinds) {
      if (player.goodsCount(good) == 0) {
        return ActionResult.reject(RejectionReason.GOOD_NOT_HELD, "No " + good + " to store");
      }
    }
    if (store.singleBarrel() != null && player.goodsCount(store.singleBarrel()) == 0) {
      return ActionResult.reject(
          RejectionReason.GOOD_NOT_HELD, "No " + store.singleBarrel() + " to store");
    }

    Map<Good, Integer> kept = new EnumMap<>(Good.class);
    for (Good good : kinds) {
      kept.put(good, player.goodsCount(good));
    }
    if (store.singleBarrel() != null && !kept.containsKey(store.singleBarrel())) {
      kept.put(store.singleBarrel(), 1);
    }

    GoodsSupply supply = state.goods();
    for (Good good : Good.values()) {
      int returned = player.goodsCount(good) - kept.getOrDefault(good, 0);
      if (returned > 0) {
        supply = supply.put(good, returned);
      }
    }

    GameState next =
        state.toBuilder().goods(supply).build().withPlayer(player.toBuilder().goods(kept).build());

    List<Integer> rest = PhaseFlow.advanceQueue(phase.queue());
    if (rest != null) {
      return ActionResult.accept(
          next.withPhase(new Phase.CaptainStorage(phase.chooserSeat(), rest)));
    }
    // The captain's last duty: empty every ship that filled up.
    return ActionResult.accept(PhaseFlow.endRolePhase(unloadFullShips(next)));
  }

  private static GameState unloadFullShips(GameState state) {
    GoodsSupply supply = state.goods();
    List<CargoShip> ships = new ArrayList<>();
    for (CargoShip ship : state.ships()) {
      if (ship.isFull()) {
        supply = supply.put(ship.cargo(), ship.loaded());
      }
      ships.add(ship.unloadIfFull());
    }
    return state.toBuilder().ships(ships).goods(supply).build();
  }

  private static List<List<Good>> subsetsUpTo(List<Good> items, int maxSize) {
    List<List<Good>> subsets = new ArrayList<>();
    subsets.add(List.of());
    for (int size = 1; size <= Math.min(maxSize, items.size()); size++) {
      collect(items, size, 0, new ArrayList<>(), subsets);
    }
    return subsets;
  }

  private static void collect(
      List<Good> items, int size, int start, List<Good> current, List<List<Good>> out) {
    if (current.size() == size) {
      out.add(List.copyOf(current));
      return;
    }
    for (int i = start; i < items.size(); i++) {
      current.add(items.get(i));
      collect(items, size, i + 1, current, out);
      current.removeLast();
    }
  }
}
