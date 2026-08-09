package com.PRS.session.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.boards.TileSupply;
import com.PRS.model.game.GameConfig;
import com.PRS.model.game.GameSetup;
import com.PRS.model.game.GameState;
import java.util.List;
import org.testng.annotations.Test;

public class GameViewTest {

  private static GameState newGame() {
    return GameSetup.create(new GameConfig(List.of("Ana", "Bo", "Coco"), 42L));
  }

  @Test
  public void scrubsDrawAndDiscardPilesAndTheSeed() {
    GameState state = newGame();
    GameView view = GameView.of(state, null);

    assertThat(view.state().tiles().drawPile()).isEmpty();
    assertThat(view.state().tiles().discardPile()).isEmpty();
    assertThat(view.state().tiles().seed()).isZero();
  }

  @Test
  public void reportsCountsForTheScrubbedPiles() {
    GameState state = newGame();
    TileSupply tiles = state.tiles();
    GameView view = GameView.of(state, null);

    assertThat(view.faceDownTiles()).isEqualTo(tiles.drawPile().size()).isPositive();
    assertThat(view.discardedTiles()).isEqualTo(tiles.discardPile().size());
  }

  @Test
  public void preservesPublicInformation() {
    GameState state = newGame();
    GameView view = GameView.of(state, null);

    assertThat(view.state().tiles().faceUp()).isEqualTo(state.tiles().faceUp());
    assertThat(view.state().tiles().quarriesRemaining())
        .isEqualTo(state.tiles().quarriesRemaining());
    assertThat(view.state().players()).isEqualTo(state.players());
    assertThat(view.state().ships()).isEqualTo(state.ships());
    assertThat(view.state().tradingHouse()).isEqualTo(state.tradingHouse());
    assertThat(view.state().roles()).isEqualTo(state.roles());
    assertThat(view.state().buildings()).isEqualTo(state.buildings());
  }

  @Test
  public void spectatorAndSeatViewsDifferOnlyInViewerSeat() {
    GameState state = newGame();
    GameView spectator = GameView.of(state, null);
    GameView player = GameView.of(state, 1);

    assertThat(spectator.viewerSeat()).isNull();
    assertThat(player.viewerSeat()).isEqualTo(1);
    assertThat(spectator.state()).isEqualTo(player.state());
  }
}
