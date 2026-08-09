package com.PRS.lobby;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.session.actors.ActorKind;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class LobbyTest {

  @Test
  public void createGameReturnsUniqueIds() {
    try (Lobby lobby = new Lobby()) {
      GameId a = lobby.createGame();
      GameId b = lobby.createGame();
      assertThat(a).isNotEqualTo(b);
    }
  }

  @Test
  public void joinFillsSeatsInOrder() {
    try (Lobby lobby = new Lobby()) {
      GameId id = lobby.createGame();

      JoinOutcome first = lobby.join(id, StubActors.named("Ana"), ActorKind.AI);
      JoinOutcome second = lobby.join(id, StubActors.named("Bo"), ActorKind.HUMAN);

      assertThat(first).isEqualTo(new JoinOutcome.Seated(id, 0));
      assertThat(second).isEqualTo(new JoinOutcome.Seated(id, 1));
    }
  }

  @Test
  public void sixthJoinIsRejectedTableFull() {
    try (Lobby lobby = new Lobby()) {
      GameId id = lobby.createGame();
      seatPlayers(lobby, id, 5);

      JoinOutcome sixth = lobby.join(id, StubActors.named("Overflow"), ActorKind.AI);

      assertThat(sixth).isEqualTo(new JoinOutcome.Rejected(LobbyRejectionReason.TABLE_FULL));
    }
  }

  @Test
  public void joinOnUnknownGameIsRejectedGameNotFound() {
    try (Lobby lobby = new Lobby()) {
      JoinOutcome outcome = lobby.join(GameId.newId(), StubActors.named("Ana"), ActorKind.AI);

      assertThat(outcome).isEqualTo(new JoinOutcome.Rejected(LobbyRejectionReason.GAME_NOT_FOUND));
    }
  }

  @Test
  public void startOnUnknownGameIsRejectedGameNotFound() {
    try (Lobby lobby = new Lobby()) {
      StartOutcome outcome = lobby.start(GameId.newId(), 1L);

      assertThat(outcome).isEqualTo(new StartOutcome.Rejected(LobbyRejectionReason.GAME_NOT_FOUND));
    }
  }

  @Test(dataProvider = "tooFewSeats")
  public void startWithFewerThanThreeSeatsIsRejected(int seatCount) {
    try (Lobby lobby = new Lobby()) {
      GameId id = lobby.createGame();
      seatPlayers(lobby, id, seatCount);

      StartOutcome outcome = lobby.start(id, 1L);

      assertThat(outcome).isEqualTo(new StartOutcome.Rejected(LobbyRejectionReason.TOO_FEW_SEATS));
      assertThat(lobby.find(id).orElseThrow().status()).isEqualTo(GameTableStatus.OPEN);
    }
  }

  @DataProvider
  public Object[][] tooFewSeats() {
    return new Object[][] {{0}, {1}, {2}};
  }

  @Test(dataProvider = "supportedSeatCounts")
  public void startWithSupportedSeatCountsSucceeds(int seatCount) {
    try (Lobby lobby = new Lobby()) {
      GameId id = lobby.createGame();
      seatPlayers(lobby, id, seatCount);

      StartOutcome outcome = lobby.start(id, 1L);

      assertThat(outcome).isEqualTo(new StartOutcome.Started(id));
      assertThat(lobby.find(id).orElseThrow().status()).isEqualTo(GameTableStatus.STARTED);
    }
  }

  @DataProvider
  public Object[][] supportedSeatCounts() {
    return new Object[][] {{3}, {4}, {5}};
  }

  @Test
  public void joinAfterStartIsRejectedAlreadyStarted() {
    try (Lobby lobby = new Lobby()) {
      GameId id = lobby.createGame();
      seatPlayers(lobby, id, 3);
      lobby.start(id, 1L);

      JoinOutcome outcome = lobby.join(id, StubActors.named("Late"), ActorKind.AI);

      assertThat(outcome).isEqualTo(new JoinOutcome.Rejected(LobbyRejectionReason.ALREADY_STARTED));
    }
  }

  @Test
  public void startAfterStartIsRejectedAlreadyStarted() {
    try (Lobby lobby = new Lobby()) {
      GameId id = lobby.createGame();
      seatPlayers(lobby, id, 3);
      lobby.start(id, 1L);

      StartOutcome outcome = lobby.start(id, 2L);

      assertThat(outcome)
          .isEqualTo(new StartOutcome.Rejected(LobbyRejectionReason.ALREADY_STARTED));
    }
  }

  @Test
  public void listGamesAndFindReflectLiveState() {
    try (Lobby lobby = new Lobby()) {
      GameId id = lobby.createGame();
      assertThat(lobby.listGames()).extracting(GameTableSummary::id).contains(id);
      assertThat(lobby.find(id).orElseThrow().seats()).isEmpty();

      lobby.join(id, StubActors.named("Ana"), ActorKind.HUMAN);

      GameTableSummary afterJoin = lobby.find(id).orElseThrow();
      assertThat(afterJoin.seats()).containsExactly(new SeatSummary("Ana", ActorKind.HUMAN));
      assertThat(afterJoin.status()).isEqualTo(GameTableStatus.OPEN);

      seatPlayers(lobby, id, 2);
      lobby.start(id, 1L);

      assertThat(lobby.find(id).orElseThrow().status()).isEqualTo(GameTableStatus.STARTED);
    }
  }

  private static void seatPlayers(Lobby lobby, GameId id, int count) {
    for (int i = 0; i < count; i++) {
      lobby.join(id, StubActors.named("Filler" + i), ActorKind.AI);
    }
  }
}
