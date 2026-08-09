package com.PRS.model.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.PRS.model.TestGames;
import com.PRS.model.boards.CargoShip;
import com.PRS.model.boards.IslandTile;
import com.PRS.model.boards.PlayerState;
import com.PRS.model.boards.TileType;
import com.PRS.model.rolecards.Role;
import java.util.List;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** The rulebook's setup table, asserted row by row. */
public class GameSetupTest {

  @DataProvider(name = "playerCounts")
  public Object[][] playerCounts() {
    return new Object[][] {{3}, {4}, {5}};
  }

  @DataProvider(name = "startingDoubloons")
  public Object[][] startingDoubloons() {
    return new Object[][] {{3, 2}, {4, 3}, {5, 4}};
  }

  @Test(dataProvider = "startingDoubloons")
  public void everyPlayerStartsWithOneDoubloonPerOpponent(int players, int expected) {
    GameState state = TestGames.newGame(players);
    assertThat(state.players()).extracting(PlayerState::doubloons).containsOnly(expected);
  }

  @DataProvider(name = "colonistSupply")
  public Object[][] colonistSupply() {
    return new Object[][] {{3, 55}, {4, 75}, {5, 95}};
  }

  @Test(dataProvider = "colonistSupply")
  public void colonistSupplyScalesWithPlayers(int players, int total) {
    GameState state = TestGames.newGame(players);
    // The ship is loaded out of the same pool, so the two must add back up to the table value.
    assertThat(state.colonistSupply() + state.colonistsOnShip()).isEqualTo(total);
    assertThat(state.colonistsOnShip()).isEqualTo(players);
  }

  @DataProvider(name = "victoryPointSupply")
  public Object[][] victoryPointSupply() {
    return new Object[][] {{3, 75}, {4, 100}, {5, 122}};
  }

  @Test(dataProvider = "victoryPointSupply")
  public void victoryPointPoolScalesWithPlayers(int players, int points) {
    assertThat(TestGames.newGame(players).victoryPointSupply()).isEqualTo(points);
  }

  @DataProvider(name = "cargoShips")
  public Object[][] cargoShips() {
    return new Object[][] {{3, List.of(4, 5, 6)}, {4, List.of(5, 6, 7)}, {5, List.of(6, 7, 8)}};
  }

  @Test(dataProvider = "cargoShips")
  public void threeCargoShipsSizedByPlayerCount(int players, List<Integer> capacities) {
    GameState state = TestGames.newGame(players);
    assertThat(state.ships()).extracting(CargoShip::capacity).containsExactlyElementsOf(capacities);
    assertThat(state.ships()).allMatch(CargoShip::isEmpty);
  }

  @DataProvider(name = "roleCards")
  public Object[][] roleCards() {
    return new Object[][] {{3, 6, 0}, {4, 7, 1}, {5, 8, 2}};
  }

  @Test(dataProvider = "roleCards")
  public void roleCardsAreThreeMoreThanPlayers(int players, int cards, int prospectors) {
    GameState state = TestGames.newGame(players);
    assertThat(state.roles().cards()).hasSize(cards);
    assertThat(state.roles().cards().stream().filter(c -> c.role() == Role.PROSPECTOR))
        .hasSize(prospectors);
  }

  @Test(dataProvider = "playerCounts")
  public void faceUpRowIsOneMoreThanPlayers(int players) {
    assertThat(TestGames.newGame(players).tiles().faceUp()).hasSize(players + 1);
  }

  @Test(dataProvider = "playerCounts")
  public void allEightQuarriesAreAvailable(int players) {
    assertThat(TestGames.newGame(players).tiles().quarriesRemaining()).isEqualTo(8);
  }

  @DataProvider(name = "startingPlantations")
  public Object[][] startingPlantations() {
    return new Object[][] {
      {3, List.of(TileType.INDIGO, TileType.INDIGO, TileType.CORN)},
      {4, List.of(TileType.INDIGO, TileType.INDIGO, TileType.CORN, TileType.CORN)},
      {5, List.of(TileType.INDIGO, TileType.INDIGO, TileType.INDIGO, TileType.CORN, TileType.CORN)}
    };
  }

  @Test(dataProvider = "startingPlantations")
  public void earlySeatsGetIndigoAndLateSeatsGetCorn(int players, List<TileType> expected) {
    GameState state = TestGames.newGame(players);
    assertThat(state.players())
        .extracting(p -> p.island().getFirst().type())
        .containsExactlyElementsOf(expected);
  }

  @Test(dataProvider = "playerCounts")
  public void startingPlantationsAreUnstaffed(int players) {
    GameState state = TestGames.newGame(players);
    assertThat(state.players()).allMatch(p -> p.island().stream().noneMatch(IslandTile::occupied));
    assertThat(state.players()).allMatch(p -> p.totalColonists() == 0);
  }

  @Test(dataProvider = "playerCounts")
  public void boardsStartWithElevenFreeIslandSpacesAndTwelveCitySpaces(int players) {
    GameState state = TestGames.newGame(players);
    assertThat(state.players()).allMatch(p -> p.freeIslandSpaces() == 11);
    assertThat(state.players()).allMatch(p -> p.freeCitySpaces() == 12);
    assertThat(state.players()).allMatch(p -> p.buildings().isEmpty());
  }

  @Test
  public void seatZeroIsTheFirstGovernorAndChoosesFirst() {
    GameState state = TestGames.newGame(4);
    assertThat(state.governorSeat()).isZero();
    assertThat(state.phase()).isInstanceOf(Phase.RoleSelection.class);
    assertThat(state.phase().actorSeat()).isZero();
  }

  @Test(dataProvider = "playerCounts")
  public void gameStartsWithEmptyTradingHouseAndFullSupplies(int players) {
    GameState state = TestGames.newGame(players);
    assertThat(state.tradingHouse().goods()).isEmpty();
    assertThat(state.finalRound()).isFalse();
    assertThat(state.isOver()).isFalse();
  }

  @Test
  public void twoAndSixPlayerGamesAreRejected() {
    assertThatThrownBy(() -> TestGames.newGame(2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("3-5");
    assertThatThrownBy(() -> TestGames.newGame(6)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void sameSeedDealsTheSameGame() {
    assertThat(TestGames.newGame(4, 7L).tiles().faceUp())
        .isEqualTo(TestGames.newGame(4, 7L).tiles().faceUp());
  }
}
