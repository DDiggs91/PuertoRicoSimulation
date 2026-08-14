package com.PRS.web.wire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.PRS.contract.model.ActionAppliedEvent;
import com.PRS.contract.model.ActionRejectedEvent;
import com.PRS.contract.model.AiEngineInfo;
import com.PRS.contract.model.BuildOptionView;
import com.PRS.contract.model.DecisionRequestedEvent;
import com.PRS.contract.model.GameEndedEvent;
import com.PRS.contract.model.GameStartedEvent;
import com.PRS.contract.model.GameTableStatus;
import com.PRS.contract.model.GoodPriceView;
import com.PRS.contract.model.Phase;
import com.PRS.contract.model.SessionFailedEvent;
import com.PRS.model.actions.PlayerAction;
import com.PRS.model.buildings.BuildingType;
import com.PRS.model.engine.GameEngine;
import com.PRS.model.engine.RejectionReason;
import com.PRS.model.game.GameConfig;
import com.PRS.model.game.GameSetup;
import com.PRS.model.game.GameState;
import com.PRS.model.goods.Good;
import com.PRS.model.rolecards.Role;
import com.PRS.model.scoring.ScoreBreakdown;
import com.PRS.session.actors.ActorKind;
import com.PRS.session.actors.Decision;
import com.PRS.session.events.SessionEvent;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WireMappingTest {

  private static List<String> names() {
    return List.of("Ana", "Bo", "Coco");
  }

  private static GameState newGame() {
    return GameSetup.create(new GameConfig(names(), 42L));
  }

  private static com.PRS.session.view.GameView newView() {
    return com.PRS.session.view.GameView.of(newGame(), null);
  }

  static Stream<PlayerAction> allActionVariants() {
    return Stream.of(
        new PlayerAction.SelectRole(0, Role.SETTLER),
        new PlayerAction.TakeFaceUpTile(1, 2),
        new PlayerAction.TakeQuarry(2),
        new PlayerAction.TakeHaciendaTile(0),
        new PlayerAction.SkipHacienda(1),
        new PlayerAction.PassSettling(2),
        new PlayerAction.SetColonistPlacement(0, List.of(true, false), List.of(1, 0)),
        new PlayerAction.SetColonistPlacement(1, List.of(), List.of(3)),
        new PlayerAction.BuildBuilding(0, BuildingType.SMALL_MARKET),
        new PlayerAction.PassBuilding(1),
        new PlayerAction.TakeCraftsmanBonus(2, Good.CORN),
        new PlayerAction.PassCraftsmanBonus(0),
        new PlayerAction.SellGood(1, Good.SUGAR),
        new PlayerAction.PassTrading(2),
        new PlayerAction.LoadShip(0, 1, Good.INDIGO),
        new PlayerAction.LoadWharf(1, Good.TOBACCO),
        new PlayerAction.DeclineWharf(2),
        new PlayerAction.StoreGoods(2, List.of(Good.CORN, Good.COFFEE), Good.SUGAR),
        new PlayerAction.StoreGoods(0, List.of(), null));
  }

  @ParameterizedTest
  @MethodSource("allActionVariants")
  void everyActionVariantRoundTripsThroughTheWireModel(PlayerAction action) {
    com.PRS.contract.model.PlayerAction wire = ActionMapper.toWire(action);
    PlayerAction roundTripped = ActionMapper.toModel(wire);

    assertThat(roundTripped).isEqualTo(action);
  }

  @Test
  void initialGameViewCarriesEveryTopLevelField() {
    GameState state = newGame();
    com.PRS.session.view.GameView view = com.PRS.session.view.GameView.of(state, 1);

    com.PRS.contract.model.GameView wire = GameMapper.toWire(view);

    assertThat(wire.getViewerSeat().orElse(null)).isEqualTo(1);
    assertThat(wire.getState().getConfig().getPlayerNames()).isEqualTo(names());
    assertThat(wire.getState().getPlayers()).hasSize(3);
    assertThat(wire.getState().getGovernorSeat()).isEqualTo(state.governorSeat());
    assertThat(wire.getState().getColonistSupply()).isEqualTo(state.colonistSupply());
    assertThat(wire.getState().getColonistsOnShip()).isEqualTo(state.colonistsOnShip());
    assertThat(wire.getState().getVictoryPointSupply()).isEqualTo(state.victoryPointSupply());
    assertThat(wire.getState().getFinalRound()).isFalse();
    assertThat(wire.getState().getShips()).hasSize(3);
    assertThat(wire.getState().getRoles().getCards()).hasSize(state.roles().cards().size());
  }

  @Test
  void theConfigCarriesEveryBuildingsPrintedCardFace() {
    com.PRS.contract.model.GameView wire = GameMapper.toWire(newView());

    var catalog = wire.getState().getConfig().getBuildingCatalog();

    assertThat(catalog).hasSize(BuildingType.values().length);
    assertThat(catalog)
        .filteredOn(entry -> entry.getType() == com.PRS.contract.model.BuildingType.COFFEE_ROASTER)
        .singleElement()
        .satisfies(
            entry -> {
              // The printed cost, not a discounted quote — that is BuildOptionView's job.
              assertThat(entry.getCost()).isEqualTo(BuildingType.COFFEE_ROASTER.cost());
              assertThat(entry.getVictoryPoints())
                  .isEqualTo(BuildingType.COFFEE_ROASTER.victoryPoints());
              assertThat(entry.getColonistCapacity())
                  .isEqualTo(BuildingType.COFFEE_ROASTER.colonistCapacity());
              assertThat(entry.getCopies()).isEqualTo(BuildingType.COFFEE_ROASTER.copies());
            });
  }

  @Test
  void aPlayerCarriesTheirBoardsCapacitiesAlongsideWhatIsOnIt() {
    GameState state =
        newGame()
            .withPlayer(
                GameSetup.create(new GameConfig(names(), 42L)).player(0).toBuilder()
                    .buildings(
                        List.of(
                            new com.PRS.model.buildings.PlacedBuilding(BuildingType.CITY_HALL, 0),
                            new com.PRS.model.buildings.PlacedBuilding(
                                BuildingType.SMALL_MARKET, 0)))
                    .build());

    var player =
        GameMapper.toWire(com.PRS.session.view.GameView.of(state, 0))
            .getState()
            .getPlayers()
            .getFirst();

    assertThat(player.getIslandSpaces()).isEqualTo(com.PRS.model.boards.PlayerState.ISLAND_SPACES);
    assertThat(player.getCitySpaces()).isEqualTo(com.PRS.model.boards.PlayerState.CITY_SPACES);
    // Two buildings, three spaces — the large violet one occupies two.
    assertThat(player.getBuildings()).hasSize(2);
    assertThat(player.getCitySpacesUsed()).isEqualTo(3);
  }

  @Test
  void tileSupplyViewTakesFaceDownAndDiscardedCountsFromTheOuterViewNotTheState() {
    com.PRS.session.view.GameView view = newView();

    com.PRS.contract.model.GameView wire = GameMapper.toWire(view);

    assertThat(wire.getState().getTiles().getFaceDownCount()).isEqualTo(view.faceDownTiles());
    assertThat(wire.getState().getTiles().getDiscardedCount()).isEqualTo(view.discardedTiles());
    assertThat(wire.getState().getTiles().getFaceUp())
        .hasSize(view.state().tiles().faceUp().size());
    assertThat(wire.getState().getTiles().getQuarriesRemaining())
        .isEqualTo(view.state().tiles().quarriesRemaining());
  }

  @Test
  void spectatorViewHasNullViewerSeat() {
    com.PRS.contract.model.GameView wire = GameMapper.toWire(newView());

    assertThat(wire.getViewerSeat().isPresent()).isFalse();
  }

  @Test
  void roleSelectionPhaseFlattensToTypeAndActorSeatOnly() {
    GameState state = newGame();

    Phase wire =
        GameMapper.toWire(com.PRS.session.view.GameView.of(state, null)).getState().getPhase();

    assertThat(wire.getType()).isEqualTo(Phase.TypeEnum.ROLE_SELECTION);
    assertThat(wire.getActorSeat()).isEqualTo(state.governorSeat());
  }

  @Test
  void settlerPhaseCarriesQueueAndHaciendaFlag() {
    GameState state = newGame();
    int governor = state.governorSeat();
    state = GameEngine.apply(state, new PlayerAction.SelectRole(governor, Role.SETTLER)).state();

    Phase wire =
        GameMapper.toWire(com.PRS.session.view.GameView.of(state, null)).getState().getPhase();

    assertThat(wire.getType()).isEqualTo(Phase.TypeEnum.SETTLER);
    assertThat(wire.getChooserSeat().orElse(null)).isEqualTo(governor);
    assertThat(wire.getQueue().orElse(List.of())).isNotEmpty();
    assertThat(wire.getHaciendaOffered().isPresent()).isTrue();
  }

  /**
   * Every {@code Phase} variant, not just the two a fresh game passes through. The mapper is a
   * switch over a sealed interface, so an unmapped variant is a compile error — what this covers is
   * the per-variant field sets, which nothing else touches.
   */
  static Stream<Arguments> allPhaseVariants() {
    return Stream.of(
        arguments(new com.PRS.model.game.Phase.RoleSelection(0), Phase.TypeEnum.ROLE_SELECTION, 0),
        arguments(
            new com.PRS.model.game.Phase.SettlerPhase(1, List.of(2, 0), true),
            Phase.TypeEnum.SETTLER,
            2),
        arguments(
            new com.PRS.model.game.Phase.MayorPhase(0, List.of(1, 2)), Phase.TypeEnum.MAYOR, 1),
        arguments(
            new com.PRS.model.game.Phase.BuilderPhase(2, List.of(0, 1)), Phase.TypeEnum.BUILDER, 0),
        arguments(
            new com.PRS.model.game.Phase.CraftsmanBonus(1, Set.of(Good.CORN, Good.INDIGO)),
            Phase.TypeEnum.CRAFTSMAN_BONUS,
            1),
        arguments(
            new com.PRS.model.game.Phase.TraderPhase(2, List.of(2, 0, 1)),
            Phase.TypeEnum.TRADER,
            2),
        arguments(
            new com.PRS.model.game.Phase.CaptainLoading(0, 1, Set.of(2), true),
            Phase.TypeEnum.CAPTAIN_LOADING,
            1),
        arguments(
            new com.PRS.model.game.Phase.CaptainStorage(1, List.of(0, 2)),
            Phase.TypeEnum.CAPTAIN_STORAGE,
            0),
        arguments(new com.PRS.model.game.Phase.GameOver(), Phase.TypeEnum.GAME_OVER, -1));
  }

  @ParameterizedTest
  @MethodSource("allPhaseVariants")
  void everyPhaseVariantMapsItsTypeAndActorSeat(
      com.PRS.model.game.Phase phase, Phase.TypeEnum expectedType, int expectedActorSeat) {
    Phase wire = phaseOnTheWire(phase);

    assertThat(wire.getType()).isEqualTo(expectedType);
    assertThat(wire.getActorSeat()).isEqualTo(expectedActorSeat);
  }

  @Test
  void craftsmanBonusCarriesItsOptionsAsActions() {
    Phase wire =
        phaseOnTheWire(
            new com.PRS.model.game.Phase.CraftsmanBonus(1, Set.of(Good.CORN, Good.COFFEE)));

    assertThat(wire.getChooserSeat().orElse(null)).isEqualTo(1);
    assertThat(wire.getCraftsmanOptions().orElse(List.of())).hasSize(2);
    assertThat(wire.getQueue().orElse(List.of())).isEmpty();
  }

  @Test
  void captainLoadingCarriesTheOncePerPhasePrivileges() {
    Phase wire =
        phaseOnTheWire(new com.PRS.model.game.Phase.CaptainLoading(0, 2, Set.of(1, 2), true));

    assertThat(wire.getChooserSeat().orElse(null)).isEqualTo(0);
    assertThat(wire.getWharfUsed().orElse(List.of())).containsExactlyInAnyOrder(1, 2);
    assertThat(wire.getBonusUsed().orElse(null)).isTrue();
  }

  @Test
  void gameOverCarriesNoPhaseSpecificFields() {
    Phase wire = phaseOnTheWire(new com.PRS.model.game.Phase.GameOver());

    assertThat(wire.getChooserSeat().isPresent()).isFalse();
    assertThat(wire.getQueue().orElse(List.of())).isEmpty();
    assertThat(wire.getCraftsmanOptions().orElse(List.of())).isEmpty();
  }

  /**
   * The builder phase quotes a price for every building, discounts applied, so a client never
   * re-derives the rule. Seat 0 is the chooser here, so the privilege is in play.
   */
  @Test
  void builderPhaseQuotesADiscountedCostForEveryBuilding() {
    Phase wire = phaseOnTheWire(new com.PRS.model.game.Phase.BuilderPhase(0, List.of(0, 1, 2)));

    List<BuildOptionView> options = wire.getBuildOptions().orElse(List.of());
    assertThat(options).hasSize(BuildingType.values().length);

    BuildOptionView harbor =
        options.stream()
            .filter(o -> o.getBuildingType() == com.PRS.contract.model.BuildingType.HARBOR)
            .findFirst()
            .orElseThrow();
    // Printed 8, less the builder's own privilege; no quarries on a fresh board.
    assertThat(harbor.getCost()).isEqualTo(7);
    assertThat(harbor.getVictoryPoints()).isEqualTo(BuildingType.HARBOR.victoryPoints());
    assertThat(harbor.getColonistCapacity()).isEqualTo(BuildingType.HARBOR.colonistCapacity());
  }

  /** Priced, not filtered: a building the player cannot afford still appears, with its price. */
  @Test
  void builderPhaseQuotesBuildingsThePlayerCannotAfford() {
    Phase wire = phaseOnTheWire(new com.PRS.model.game.Phase.BuilderPhase(1, List.of(0)));

    assertThat(wire.getBuildOptions().orElse(List.of()))
        .anySatisfy(
            option -> {
              assertThat(option.getBuildingType())
                  .isEqualTo(com.PRS.contract.model.BuildingType.CITY_HALL);
              assertThat(option.getCost()).isEqualTo(10);
            });
  }

  @Test
  void traderPhaseQuotesAPriceForEveryGood() {
    Phase wire = phaseOnTheWire(new com.PRS.model.game.Phase.TraderPhase(1, List.of(1, 2, 0)));

    List<GoodPriceView> prices = wire.getGoodPrices().orElse(List.of());
    assertThat(prices).hasSize(Good.values().length);
    // Seat 1 is both chooser and actor, so every list price carries the trader's privilege.
    assertThat(prices)
        .anySatisfy(
            price -> {
              assertThat(price.getGood()).isEqualTo(com.PRS.contract.model.Good.COFFEE);
              assertThat(price.getPrice()).isEqualTo(Good.COFFEE.price() + 1);
            });
  }

  /** Only the phase that has them: nothing else pays to compute a price list it cannot use. */
  @Test
  void pricesAreAbsentOutsideTheirOwnPhase() {
    Phase roleSelection = phaseOnTheWire(new com.PRS.model.game.Phase.RoleSelection(0));

    assertThat(roleSelection.getBuildOptions().orElse(List.of())).isEmpty();
    assertThat(roleSelection.getGoodPrices().orElse(List.of())).isEmpty();

    Phase builder = phaseOnTheWire(new com.PRS.model.game.Phase.BuilderPhase(0, List.of(0)));
    assertThat(builder.getGoodPrices().orElse(List.of())).isEmpty();
  }

  /** A card's printed numbers ride along with the occupancy count, so a client can draw it. */
  @Test
  void placedBuildingsCarryTheirPrintedCapacityAndVictoryPoints() {
    GameState state =
        newGame()
            .withPlayer(
                newGame().player(0).toBuilder()
                    .buildings(
                        List.of(new com.PRS.model.buildings.PlacedBuilding(BuildingType.WHARF, 1)))
                    .build());

    var wire = GameMapper.toWire(com.PRS.session.view.GameView.of(state, null));

    assertThat(wire.getState().getPlayers().getFirst().getBuildings())
        .singleElement()
        .satisfies(
            building -> {
              assertThat(building.getColonists()).isEqualTo(1);
              assertThat(building.getCapacity()).isEqualTo(BuildingType.WHARF.colonistCapacity());
              assertThat(building.getVictoryPoints()).isEqualTo(BuildingType.WHARF.victoryPoints());
            });
  }

  private static Phase phaseOnTheWire(com.PRS.model.game.Phase phase) {
    GameState state = newGame().withPhase(phase);
    return GameMapper.toWire(com.PRS.session.view.GameView.of(state, null)).getState().getPhase();
  }

  @Test
  void decisionMapsSeatViewOptionsAndRequestId() {
    GameState state = newGame();
    Decision decision =
        new Decision(
            state.governorSeat(),
            com.PRS.session.view.GameView.of(state, state.governorSeat()),
            List.of(new PlayerAction.SelectRole(state.governorSeat(), Role.SETTLER)),
            7L);

    com.PRS.contract.model.Decision wire = GameMapper.toWire(decision);

    assertThat(wire.getSeat()).isEqualTo(state.governorSeat());
    assertThat(wire.getRequestId()).isEqualTo(7L);
    assertThat(wire.getOptions()).hasSize(1);
  }

  @Test
  void scoreBreakdownIncludesTheComputedTotal() {
    ScoreBreakdown score = new ScoreBreakdown(0, "Ana", 5, 3, 2, 8);

    com.PRS.contract.model.ScoreBreakdown wire = GameMapper.toWire(score);

    assertThat(wire.getTotal()).isEqualTo(score.total());
    assertThat(wire.getTiebreak()).isEqualTo(8);
  }

  @Test
  void gameStartedEventCarriesSeatNames() {
    SessionEvent event = new SessionEvent.GameStarted(newView(), names());

    com.PRS.contract.model.SessionEvent wire = SessionEventMapper.toWire(event);

    assertThat(wire)
        .isInstanceOfSatisfying(
            GameStartedEvent.class, e -> assertThat(e.getSeatNames()).isEqualTo(names()));
  }

  @Test
  void decisionRequestedEventCarriesSeatOptionsAndRequestId() {
    SessionEvent event =
        new SessionEvent.DecisionRequested(
            newView(), 0, List.of(new PlayerAction.SelectRole(0, Role.SETTLER)), 3L);

    com.PRS.contract.model.SessionEvent wire = SessionEventMapper.toWire(event);

    assertThat(wire)
        .isInstanceOfSatisfying(
            DecisionRequestedEvent.class,
            e -> {
              assertThat(e.getSeat()).isEqualTo(0);
              assertThat(e.getRequestId()).isEqualTo(3L);
              assertThat(e.getOptions()).hasSize(1);
            });
  }

  @Test
  void actionAppliedEventCarriesSeatAndAction() {
    PlayerAction action = new PlayerAction.PassSettling(1);
    SessionEvent event = new SessionEvent.ActionApplied(newView(), 1, action);

    com.PRS.contract.model.SessionEvent wire = SessionEventMapper.toWire(event);

    assertThat(wire)
        .isInstanceOfSatisfying(
            ActionAppliedEvent.class, e -> assertThat(e.getSeat()).isEqualTo(1));
  }

  @Test
  void actionRejectedEventCarriesReasonAndDetail() {
    PlayerAction action = new PlayerAction.PassBuilding(2);
    SessionEvent event =
        new SessionEvent.ActionRejected(newView(), 2, action, RejectionReason.WRONG_PHASE, "nope");

    com.PRS.contract.model.SessionEvent wire = SessionEventMapper.toWire(event);

    assertThat(wire)
        .isInstanceOfSatisfying(
            ActionRejectedEvent.class,
            e -> {
              assertThat(e.getReason().name()).isEqualTo("WRONG_PHASE");
              assertThat(e.getDetail()).isEqualTo("nope");
            });
  }

  @Test
  void gameEndedEventCarriesStandings() {
    SessionEvent event =
        new SessionEvent.GameEnded(newView(), List.of(new ScoreBreakdown(0, "Ana", 1, 2, 3, 4)));

    com.PRS.contract.model.SessionEvent wire = SessionEventMapper.toWire(event);

    assertThat(wire)
        .isInstanceOfSatisfying(GameEndedEvent.class, e -> assertThat(e.getStandings()).hasSize(1));
  }

  @Test
  void sessionFailedEventCarriesDetail() {
    SessionEvent event = new SessionEvent.SessionFailed(newView(), "boom");

    com.PRS.contract.model.SessionEvent wire = SessionEventMapper.toWire(event);

    assertThat(wire)
        .isInstanceOfSatisfying(
            SessionFailedEvent.class, e -> assertThat(e.getDetail()).isEqualTo("boom"));
  }

  @Test
  void actorKindRoundTrips() {
    assertThat(LobbyMapper.toModel(LobbyMapper.toWire(ActorKind.HUMAN))).isEqualTo(ActorKind.HUMAN);
    assertThat(LobbyMapper.toModel(LobbyMapper.toWire(ActorKind.AI))).isEqualTo(ActorKind.AI);
  }

  @Test
  void gameTableSummaryMapsSeatsAndStatus() {
    com.PRS.lobby.GameTableSummary summary =
        new com.PRS.lobby.GameTableSummary(
            com.PRS.lobby.GameId.newId(),
            List.of(new com.PRS.lobby.SeatSummary("Ana", ActorKind.HUMAN)),
            com.PRS.lobby.GameTableStatus.OPEN);

    com.PRS.contract.model.GameTableSummary wire = LobbyMapper.toWire(summary);

    assertThat(wire.getId()).isEqualTo(summary.id().value().toString());
    assertThat(wire.getStatus()).isEqualTo(GameTableStatus.OPEN);
    assertThat(wire.getSeats()).hasSize(1);
    assertThat(wire.getSeats().getFirst().getName()).isEqualTo("Ana");
  }

  @Test
  void aiEngineInfoMapsAllThreeFields() {
    com.PRS.ai.AiEngineInfo info =
        new com.PRS.ai.AiEngineInfo("random", "Random", "Picks randomly.");

    AiEngineInfo wire = LobbyMapper.toWire(info);

    assertThat(wire.getId()).isEqualTo("random");
    assertThat(wire.getDisplayName()).isEqualTo("Random");
    assertThat(wire.getDescription()).isEqualTo("Picks randomly.");
  }
}
