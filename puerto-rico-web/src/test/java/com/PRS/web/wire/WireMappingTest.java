package com.PRS.web.wire;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.contract.model.ActionAppliedEvent;
import com.PRS.contract.model.ActionRejectedEvent;
import com.PRS.contract.model.AiEngineInfo;
import com.PRS.contract.model.DecisionRequestedEvent;
import com.PRS.contract.model.GameEndedEvent;
import com.PRS.contract.model.GameStartedEvent;
import com.PRS.contract.model.GameTableStatus;
import com.PRS.contract.model.Phase;
import com.PRS.contract.model.SessionFailedEvent;
import com.PRS.model.actions.ColonistSlot;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
        new PlayerAction.PlaceColonist(0, new ColonistSlot.Island(3)),
        new PlayerAction.PlaceColonist(1, new ColonistSlot.Building(2)),
        new PlayerAction.EndColonistPlacement(2),
        new PlayerAction.BuildBuilding(0, BuildingType.SMALL_MARKET),
        new PlayerAction.PassBuilding(1),
        new PlayerAction.TakeCraftsmanBonus(2, Good.CORN),
        new PlayerAction.PassCraftsmanBonus(0),
        new PlayerAction.SellGood(1, Good.SUGAR),
        new PlayerAction.PassTrading(2),
        new PlayerAction.LoadShip(0, 1, Good.INDIGO),
        new PlayerAction.LoadWharf(1, Good.TOBACCO),
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
