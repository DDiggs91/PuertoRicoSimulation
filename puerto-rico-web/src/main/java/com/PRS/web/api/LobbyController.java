package com.PRS.web.api;

import com.PRS.ai.AiRegistry;
import com.PRS.contract.api.LobbyApi;
import com.PRS.contract.model.CreateGameResponse;
import com.PRS.contract.model.GameTableSummary;
import com.PRS.contract.model.SeatRequest;
import com.PRS.contract.model.SeatResponse;
import com.PRS.contract.model.StartRequest;
import com.PRS.lobby.GameId;
import com.PRS.lobby.JoinOutcome;
import com.PRS.lobby.Lobby;
import com.PRS.lobby.LobbyRejectionReason;
import com.PRS.lobby.StartOutcome;
import com.PRS.session.actors.Actor;
import com.PRS.session.actors.ActorKind;
import com.PRS.web.actors.HumanActor;
import com.PRS.web.actors.SeatTokens;
import com.PRS.web.events.GameEventStream;
import com.PRS.web.wire.LobbyMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LobbyController implements LobbyApi {

  private final Lobby lobby;
  private final AiRegistry aiRegistry;
  private final SeatTokens seatTokens;
  private final GameEventStream eventStream;

  public LobbyController(
      Lobby lobby, AiRegistry aiRegistry, SeatTokens seatTokens, GameEventStream eventStream) {
    this.lobby = lobby;
    this.aiRegistry = aiRegistry;
    this.seatTokens = seatTokens;
    this.eventStream = eventStream;
  }

  @Override
  public ResponseEntity<CreateGameResponse> createGame() {
    GameId id = lobby.createGame();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new CreateGameResponse(id.value().toString()));
  }

  @Override
  public ResponseEntity<GameTableSummary> getGame(String gameId) {
    GameId id = parseId(gameId);
    com.PRS.lobby.GameTableSummary summary = lobby.find(id).orElseThrow(() -> notFound(gameId));
    return ResponseEntity.ok(LobbyMapper.toWire(summary));
  }

  @Override
  public ResponseEntity<List<GameTableSummary>> listGames() {
    return ResponseEntity.ok(lobby.listGames().stream().map(LobbyMapper::toWire).toList());
  }

  @Override
  public ResponseEntity<SeatResponse> seatActor(String gameId, SeatRequest seatRequest) {
    GameId id = parseId(gameId);
    ActorKind kind = LobbyMapper.toModel(seatRequest.getKind());
    Actor actor =
        kind == ActorKind.AI ? buildAiActor(seatRequest) : new HumanActor(seatRequest.getName());

    JoinOutcome outcome = lobby.join(id, actor, kind);
    return switch (outcome) {
      case JoinOutcome.Seated seated -> {
        SeatResponse response = new SeatResponse(seated.seatIndex());
        if (kind == ActorKind.HUMAN) {
          response.seatToken(seatTokens.mint(id, seated.seatIndex()));
        }
        yield ResponseEntity.ok(response);
      }
      case JoinOutcome.Rejected rejected -> throw toApiException(rejected.reason());
    };
  }

  @Override
  public ResponseEntity<GameTableSummary> startGame(String gameId, StartRequest startRequest) {
    GameId id = parseId(gameId);
    StartOutcome outcome =
        lobby.start(id, seedOrRandom(startRequest), List.of(eventStream.listenerFor(id)));
    return switch (outcome) {
      case StartOutcome.Started started ->
          ResponseEntity.ok(LobbyMapper.toWire(lobby.find(id).orElseThrow()));
      case StartOutcome.Rejected rejected -> throw toApiException(rejected.reason());
    };
  }

  /**
   * The caller's seed, or a fresh random one. {@code seed} is a {@link
   * org.openapitools.jackson.nullable.JsonNullable}, so an explicit JSON {@code null} arrives
   * <em>present</em> holding null — only an omitted field is {@code undefined}. Both mean "pick one
   * for me" here, so present-and-null is treated exactly like absent rather than unboxed into an
   * NPE.
   */
  private static long seedOrRandom(StartRequest startRequest) {
    Long seed = startRequest == null ? null : startRequest.getSeed().orElse(null);
    return seed == null ? ThreadLocalRandom.current().nextLong() : seed;
  }

  private Actor buildAiActor(SeatRequest seatRequest) {
    String engineId = seatRequest.getEngineId().orElse(null);
    if (engineId == null) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "Missing engine id", "AI seats require an engineId.", null);
    }
    return aiRegistry
        .create(engineId, seatRequest.getName(), ThreadLocalRandom.current().nextLong())
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown AI engine",
                    "No AI engine registered with id '" + engineId + "'.",
                    null));
  }

  static GameId parseId(String raw) {
    try {
      return new GameId(UUID.fromString(raw));
    } catch (IllegalArgumentException e) {
      throw notFound(raw);
    }
  }

  static ApiException notFound(String gameId) {
    return new ApiException(
        HttpStatus.NOT_FOUND, "Game not found", "No such game: " + gameId, "GAME_NOT_FOUND");
  }

  static ApiException toApiException(LobbyRejectionReason reason) {
    HttpStatus status =
        switch (reason) {
          case GAME_NOT_FOUND -> HttpStatus.NOT_FOUND;
          case TABLE_FULL, ALREADY_STARTED, TOO_FEW_SEATS -> HttpStatus.CONFLICT;
        };
    return new ApiException(status, "Request rejected", reason.name(), reason.name());
  }
}
