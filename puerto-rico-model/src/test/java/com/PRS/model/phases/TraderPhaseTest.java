package com.PRS.model.phases;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.TestGames;
import com.PRS.model.actions.PlayerAction;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.engine.GameEngine;
import com.PRS.model.engine.RejectionReason;
import com.PRS.model.game.GameState;
import com.PRS.model.game.Phase;
import com.PRS.model.goods.Good;
import com.PRS.model.goods.TradingHouse;
import com.PRS.model.rolecards.Role;
import java.util.List;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Trader phase: prices, market bonuses, the four-slot house and its duplicate rule. */
public class TraderPhaseTest {

  @DataProvider(name = "prices")
  public Object[][] prices() {
    return new Object[][] {
      {Good.CORN, 0}, {Good.INDIGO, 1}, {Good.SUGAR, 2}, {Good.TOBACCO, 3}, {Good.COFFEE, 4}
    };
  }

  @Test(dataProvider = "prices")
  public void goodsSellForTheirListedPrice(Good good, int price) {
    GameState state = TestGames.newGame(3);
    // Seat 1 sells, so the trader's own privilege is not in play.
    state = state.withPlayer(TestGames.player(1).doubloons(0).goods(good, 1).build());
    state = TestGames.chooseRole(state, Role.TRADER);
    state = TestGames.apply(state, new PlayerAction.PassTrading(0));

    state = TestGames.apply(state, new PlayerAction.SellGood(1, good));

    assertThat(state.player(1).doubloons()).isEqualTo(price);
    assertThat(state.player(1).goodsCount(good)).isZero();
    assertThat(state.tradingHouse().goods()).containsExactly(good);
  }

  @Test
  public void theTraderEarnsAnExtraDoubloonOnTheirOwnSale() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).doubloons(0).goods(Good.COFFEE, 1).build());
    state = TestGames.chooseRole(state, Role.TRADER);

    state = TestGames.apply(state, new PlayerAction.SellGood(0, Good.COFFEE));
    assertThat(state.player(0).doubloons()).isEqualTo(5);
  }

  @Test
  public void aTraderWhoDoesNotSellEarnsNothing() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).doubloons(0).build());
    state = TestGames.chooseRole(state, Role.TRADER);

    state = TestGames.apply(state, new PlayerAction.PassTrading(0));
    assertThat(state.player(0).doubloons()).isZero();
  }

  @DataProvider(name = "markets")
  public Object[][] markets() {
    return new Object[][] {
      {List.of(BuildingType.SMALL_MARKET), 1},
      {List.of(BuildingType.LARGE_MARKET), 2},
      {List.of(BuildingType.SMALL_MARKET, BuildingType.LARGE_MARKET), 3},
    };
  }

  @Test(dataProvider = "markets")
  public void marketsAddToEverySaleAndStackTogether(List<BuildingType> owned, int bonus) {
    GameState state = TestGames.newGame(3);
    var fixture = TestGames.player(1).doubloons(0).goods(Good.SUGAR, 1);
    for (BuildingType market : owned) {
      fixture = fixture.building(market);
    }
    state = state.withPlayer(fixture.build());
    state = TestGames.chooseRole(state, Role.TRADER);
    state = TestGames.apply(state, new PlayerAction.PassTrading(0));

    state = TestGames.apply(state, new PlayerAction.SellGood(1, Good.SUGAR));
    assertThat(state.player(1).doubloons()).isEqualTo(2 + bonus);
  }

  @Test
  public void unstaffedMarketsPayNothing() {
    GameState state = TestGames.newGame(3);
    state =
        state.withPlayer(
            TestGames.player(1)
                .doubloons(0)
                .goods(Good.SUGAR, 1)
                .building(BuildingType.LARGE_MARKET, 0)
                .build());
    state = TestGames.chooseRole(state, Role.TRADER);
    state = TestGames.apply(state, new PlayerAction.PassTrading(0));

    state = TestGames.apply(state, new PlayerAction.SellGood(1, Good.SUGAR));
    assertThat(state.player(1).doubloons()).isEqualTo(2);
  }

  @Test
  public void cornSellsForNothingButTheSaleIsStillLegal() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(1).doubloons(0).goods(Good.CORN, 1).build());
    state = TestGames.chooseRole(state, Role.TRADER);
    state = TestGames.apply(state, new PlayerAction.PassTrading(0));

    assertThat(GameEngine.legalActions(state)).contains(new PlayerAction.SellGood(1, Good.CORN));
    state = TestGames.apply(state, new PlayerAction.SellGood(1, Good.CORN));
    assertThat(state.player(1).doubloons()).isZero();
  }

  @Test
  public void theHouseRefusesAKindItAlreadyHolds() {
    GameState state = TestGames.newGame(3);
    state =
        state.toBuilder()
            .tradingHouse(new TradingHouse(List.of(Good.SUGAR)))
            .build()
            .withPlayer(TestGames.player(0).goods(Good.SUGAR, 1).build());
    state = TestGames.chooseRole(state, Role.TRADER);

    assertThat(GameEngine.legalActions(state))
        .doesNotContain(new PlayerAction.SellGood(0, Good.SUGAR));
    assertThat(TestGames.reject(state, new PlayerAction.SellGood(0, Good.SUGAR)).reason())
        .isEqualTo(RejectionReason.DUPLICATE_GOOD_IN_TRADING_HOUSE);
  }

  @Test
  public void anOccupiedOfficeSellsIntoAKindAlreadyThere() {
    GameState state = TestGames.newGame(3);
    state =
        state.toBuilder()
            .tradingHouse(new TradingHouse(List.of(Good.SUGAR)))
            .build()
            .withPlayer(
                TestGames.player(0).goods(Good.SUGAR, 1).building(BuildingType.OFFICE).build());
    state = TestGames.chooseRole(state, Role.TRADER);

    state = TestGames.apply(state, new PlayerAction.SellGood(0, Good.SUGAR));
    assertThat(state.tradingHouse().goods()).containsExactly(Good.SUGAR, Good.SUGAR);
  }

  @Test
  public void theOfficeStillCannotSellIntoAFullHouse() {
    GameState state = TestGames.newGame(3);
    state =
        state.toBuilder()
            .tradingHouse(
                new TradingHouse(List.of(Good.CORN, Good.INDIGO, Good.SUGAR, Good.TOBACCO)))
            .build()
            .withPlayer(
                TestGames.player(0).goods(Good.SUGAR, 1).building(BuildingType.OFFICE).build());
    state = TestGames.chooseRole(state, Role.TRADER);

    assertThat(TestGames.reject(state, new PlayerAction.SellGood(0, Good.SUGAR)).reason())
        .isEqualTo(RejectionReason.TRADING_HOUSE_FULL);
  }

  @Test
  public void thePhaseEndsTheMomentTheHouseFills() {
    GameState state = TestGames.newGame(4);
    state =
        state.toBuilder()
            .tradingHouse(new TradingHouse(List.of(Good.CORN, Good.INDIGO, Good.SUGAR)))
            .build()
            .withPlayer(TestGames.player(0).goods(Good.TOBACCO, 1).build())
            .withPlayer(TestGames.player(1).goods(Good.COFFEE, 1).build());
    state = TestGames.chooseRole(state, Role.TRADER);

    state = TestGames.apply(state, new PlayerAction.SellGood(0, Good.TOBACCO));

    // Seats 1-3 never get a turn, and the trader's last duty has already emptied the house.
    assertThat(state.phase()).isInstanceOf(Phase.RoleSelection.class);
    assertThat(state.tradingHouse().goods()).isEmpty();
    assertThat(state.player(1).goodsCount(Good.COFFEE)).isEqualTo(1);
  }

  @Test
  public void aPartlyFilledHouseCarriesOverToTheNextRound() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).goods(Good.COFFEE, 1).build());
    state = TestGames.chooseRole(state, Role.TRADER);
    state = TestGames.apply(state, new PlayerAction.SellGood(0, Good.COFFEE));
    state = TestGames.apply(state, new PlayerAction.PassTrading(1));
    state = TestGames.apply(state, new PlayerAction.PassTrading(2));

    assertThat(state.tradingHouse().goods()).containsExactly(Good.COFFEE);
  }

  @Test
  public void aPlayerWithNoGoodsCanOnlyPass() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).build());
    state = TestGames.chooseRole(state, Role.TRADER);

    assertThat(GameEngine.legalActions(state)).containsExactly(new PlayerAction.PassTrading(0));
    assertThat(TestGames.reject(state, new PlayerAction.SellGood(0, Good.COFFEE)).reason())
        .isEqualTo(RejectionReason.GOOD_NOT_HELD);
  }

  @Test
  public void eachPlayerSellsAtMostOneGood() {
    GameState state = TestGames.newGame(3);
    state = state.withPlayer(TestGames.player(0).goods(Good.COFFEE, 3).build());
    state = TestGames.chooseRole(state, Role.TRADER);

    state = TestGames.apply(state, new PlayerAction.SellGood(0, Good.COFFEE));

    assertThat(state.phase().actorSeat()).isEqualTo(1);
    assertThat(state.player(0).goodsCount(Good.COFFEE)).isEqualTo(2);
  }
}
