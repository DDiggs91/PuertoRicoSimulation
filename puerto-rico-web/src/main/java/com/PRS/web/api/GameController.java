package com.PRS.web.api;

import com.PRS.contract.api.GameApi;
import com.PRS.contract.model.Decision;
import com.PRS.contract.model.GameView;
import com.PRS.contract.model.MoveRequest;
import com.PRS.lobby.GameId;
import com.PRS.lobby.Lobby;
import com.PRS.model.actions.PlayerAction;
import com.PRS.session.GameSession;
import com.PRS.session.actors.Actor;
import com.PRS.web.actors.HumanActor;
import com.PRS.web.actors.OfferResult;
import com.PRS.web.actors.SeatTokens;
import com.PRS.web.events.GameEventStream;
import com.PRS.web.wire.ActionMapper;
import com.PRS.web.wire.GameMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class GameController implements GameApi {

  private final Lobby lobby;
  private final SeatTokens seatTokens;
  private final GameEventStream eventStream;

  public GameController(Lobby lobby, SeatTokens seatTokens, GameEventStream eventStream) {
    this.lobby = lobby;
    this.seatTokens = seatTokens;
    this.eventStream = eventStream;
  }

  @Override
  public ResponseEntity<GameView> getGameState(String gameId) {
    GameSession session = sessionFor(gameId);
    return ResponseEntity.ok(GameMapper.toWire(session.viewFor(null)));
  }

  @Override
  public ResponseEntity<Decision> getPendingDecision(String gameId) {
    GameSession session = sessionFor(gameId);
    com.PRS.session.actors.Decision decision = session.pendingDecision();
    if (decision == null) {
      throw new ApiException(
          HttpStatus.NOT_FOUND,
          "No pending decision",
          "The game has no decision currently pending.",
          null);
    }
    return ResponseEntity.ok(GameMapper.toWire(decision));
  }

  @Override
  public ResponseEntity<Void> submitMove(
      String gameId, MoveRequest moveRequest, String xSeatToken) {
    GameId id = LobbyController.parseId(gameId);
    GameSession session = sessionFor(gameId);
    PlayerAction action = ActionMapper.toModel(moveRequest.getAction());

    if (xSeatToken == null || !seatTokens.isValid(id, action.seat(), xSeatToken)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "Invalid seat token",
          "This token does not authorize seat " + action.seat() + ".",
          null);
    }

    Actor actor = session.seats().get(action.seat()).actor();
    if (!(actor instanceof HumanActor human)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "Not a human seat",
          "Seat " + action.seat() + " is not human-controlled.",
          null);
    }

    OfferResult result = human.offer(moveRequest.getRequestId(), action);
    return switch (result) {
      case OfferResult.Accepted ignored -> ResponseEntity.status(HttpStatus.ACCEPTED).build();
      case OfferResult.Rejected rejected ->
          throw new ApiException(HttpStatus.BAD_REQUEST, "Move rejected", rejected.detail(), null);
    };
  }

  /**
   * Deliberately outside the generated {@code GameApi}/{@code GameStreamApi} interfaces — {@link
   * SseEmitter} has no natural OpenAPI response shape. See puerto-rico.yaml's streamGameEvents
   * operation for why this route still exists in the spec.
   */
  @GetMapping(value = "/games/{gameId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamEvents(@PathVariable("gameId") String gameId) {
    return eventStream.subscribe(LobbyController.parseId(gameId));
  }

  private GameSession sessionFor(String gameId) {
    GameId id = LobbyController.parseId(gameId);
    return lobby
        .sessionFor(id)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Game not found",
                    "No started game: " + gameId,
                    "GAME_NOT_FOUND"));
  }
}
