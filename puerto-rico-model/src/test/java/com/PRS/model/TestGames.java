package com.PRS.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.boards.IslandTile;
import com.PRS.model.boards.PlayerState;
import com.PRS.model.boards.TileType;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.buildings.PlacedBuilding;
import com.PRS.model.engine.ActionResult;
import com.PRS.model.engine.GameEngine;
import com.PRS.model.game.GameConfig;
import com.PRS.model.game.GameSetup;
import com.PRS.model.game.GameState;
import com.PRS.model.goods.Good;
import com.PRS.model.rolecards.Role;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.IntStream;

/** Fixtures for building games in a known state without playing them out move by move. */
public final class TestGames {

  public static final long SEED = 42L;

  private TestGames() {}

  public static GameState newGame(int players) {
    return newGame(players, SEED);
  }

  public static GameState newGame(int players, long seed) {
    List<String> names = IntStream.range(0, players).mapToObj(i -> "P" + i).toList();
    return GameSetup.create(new GameConfig(names, seed));
  }

  /** A player board built from scratch, with no starting plantation unless one is added. */
  public static PlayerFixture player(int seat) {
    return new PlayerFixture(seat);
  }

  public static final class PlayerFixture {
    private PlayerState state;

    private PlayerFixture(int seat) {
      state =
          PlayerState.builder()
              .seat(seat)
              .name("P" + seat)
              .doubloons(0)
              .victoryPoints(0)
              .island(List.of())
              .buildings(List.of())
              .colonistsInSanJuan(0)
              .goods(new EnumMap<>(Good.class))
              .build();
    }

    public PlayerFixture doubloons(int amount) {
      state = state.toBuilder().doubloons(amount).build();
      return this;
    }

    public PlayerFixture victoryPoints(int amount) {
      state = state.toBuilder().victoryPoints(amount).build();
      return this;
    }

    public PlayerFixture sanJuan(int colonists) {
      state = state.toBuilder().colonistsInSanJuan(colonists).build();
      return this;
    }

    /** Adds {@code count} tiles of a kind, all staffed. */
    public PlayerFixture staffed(TileType type, int count) {
      return tiles(type, count, true);
    }

    public PlayerFixture tiles(TileType type, int count, boolean occupied) {
      List<IslandTile> island = new ArrayList<>(state.island());
      for (int i = 0; i < count; i++) {
        island.add(new IslandTile(type, occupied));
      }
      state = state.toBuilder().island(island).build();
      return this;
    }

    /** Adds a building staffed to capacity. */
    public PlayerFixture building(BuildingType type) {
      return building(type, type.colonistCapacity());
    }

    public PlayerFixture building(BuildingType type, int colonists) {
      List<PlacedBuilding> buildings = new ArrayList<>(state.buildings());
      buildings.add(new PlacedBuilding(type, colonists));
      state = state.toBuilder().buildings(buildings).build();
      return this;
    }

    public PlayerFixture goods(Good good, int count) {
      state = state.plusGoods(good, count);
      return this;
    }

    public PlayerState build() {
      return state;
    }
  }

  // --- driving the engine ---

  /** Applies an action, failing the test if the engine rejects it. */
  public static GameState apply(GameState state, PlayerAction action) {
    ActionResult result = GameEngine.apply(state, action);
    assertThat(result)
        .as("action %s should be legal in phase %s", action, state.phase())
        .isInstanceOf(ActionResult.Accepted.class);
    return result.state();
  }

  /** Applies an action expecting it to be refused. */
  public static ActionResult.Rejected reject(GameState state, PlayerAction action) {
    ActionResult result = GameEngine.apply(state, action);
    assertThat(result)
        .as("action %s should be rejected in phase %s", action, state.phase())
        .isInstanceOf(ActionResult.Rejected.class);
    return (ActionResult.Rejected) result;
  }

  /** The seat currently to act chooses a role. */
  public static GameState chooseRole(GameState state, Role role) {
    return apply(state, new PlayerAction.SelectRole(state.phase().actorSeat(), role));
  }
}
