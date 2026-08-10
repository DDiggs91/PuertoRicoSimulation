package com.PRS.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.PRS.contract.model.CreateGameResponse;
import com.PRS.contract.model.Decision;
import com.PRS.contract.model.SeatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class GameApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private record StartedGame(String gameId, String humanAToken, String humanBToken) {}

  /** Ana seat 0, Bo seat 1 (both human), Coco seat 2 (AI) — seat 0 always governs first. */
  private StartedGame startGame() throws Exception {
    String gameId =
        objectMapper
            .readValue(
                mockMvc.perform(post("/api/games")).andReturn().getResponse().getContentAsString(),
                CreateGameResponse.class)
            .getGameId();

    String tokenA = seatHuman(gameId, "Ana");
    String tokenB = seatHuman(gameId, "Bo");
    mockMvc
        .perform(
            post("/api/games/{id}/seats", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Coco\",\"kind\":\"AI\",\"engineId\":\"random\"}"))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/games/{id}/start", gameId)).andExpect(status().isOk());
    return new StartedGame(gameId, tokenA, tokenB);
  }

  private String seatHuman(String gameId, String name) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/games/{id}/seats", gameId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"%s\",\"kind\":\"HUMAN\"}".formatted(name)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readValue(body, SeatResponse.class).getSeatToken().orElse(null);
  }

  private Decision pendingDecision(String gameId) throws Exception {
    String body =
        mockMvc
            .perform(get("/api/games/{id}/decision", gameId))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readValue(body, Decision.class);
  }

  @Test
  void gameStateReflectsThreeSeatedPlayers() throws Exception {
    StartedGame game = startGame();

    mockMvc
        .perform(get("/api/games/{id}/state", game.gameId()))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.state.players.length()")
                .value(3));
  }

  @Test
  void gameStateOnUnknownIdReturns404() throws Exception {
    mockMvc
        .perform(get("/api/games/{id}/state", "00000000-0000-0000-0000-000000000000"))
        .andExpect(status().isNotFound());
  }

  @Test
  void pendingDecisionIsSeatZeroRightAfterStart() throws Exception {
    StartedGame game = startGame();

    Decision decision = pendingDecision(game.gameId());

    assertThat(decision.getSeat()).isZero();
    assertThat(decision.getOptions()).isNotEmpty();
  }

  @Test
  void aValidMoveFromTheCorrectSeatIsAccepted() throws Exception {
    StartedGame game = startGame();
    Decision decision = pendingDecision(game.gameId());

    String moveJson =
        "{\"requestId\":%d,\"action\":%s}"
            .formatted(
                decision.getRequestId(),
                objectMapper.writeValueAsString(decision.getOptions().getFirst()));

    mockMvc
        .perform(
            post("/api/games/{id}/moves", game.gameId())
                .header("X-Seat-Token", game.humanAToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(moveJson))
        .andExpect(status().isAccepted());
  }

  @Test
  void aMoveWithoutASeatTokenIsForbidden() throws Exception {
    StartedGame game = startGame();
    Decision decision = pendingDecision(game.gameId());
    String moveJson =
        "{\"requestId\":%d,\"action\":%s}"
            .formatted(
                decision.getRequestId(),
                objectMapper.writeValueAsString(decision.getOptions().getFirst()));

    mockMvc
        .perform(
            post("/api/games/{id}/moves", game.gameId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(moveJson))
        .andExpect(status().isForbidden());
  }

  @Test
  void aMoveWithTheWrongSeatsTokenIsForbidden() throws Exception {
    StartedGame game = startGame();
    Decision decision = pendingDecision(game.gameId());
    String moveJson =
        "{\"requestId\":%d,\"action\":%s}"
            .formatted(
                decision.getRequestId(),
                objectMapper.writeValueAsString(decision.getOptions().getFirst()));

    mockMvc
        .perform(
            post("/api/games/{id}/moves", game.gameId())
                .header("X-Seat-Token", game.humanBToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(moveJson))
        .andExpect(status().isForbidden());
  }

  @Test
  void aMoveWithAStaleRequestIdIsRejected() throws Exception {
    StartedGame game = startGame();
    Decision decision = pendingDecision(game.gameId());
    String moveJson =
        "{\"requestId\":%d,\"action\":%s}"
            .formatted(
                decision.getRequestId() + 999,
                objectMapper.writeValueAsString(decision.getOptions().getFirst()));

    mockMvc
        .perform(
            post("/api/games/{id}/moves", game.gameId())
                .header("X-Seat-Token", game.humanAToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(moveJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void aMoveNotAmongTheOfferedOptionsIsRejected() throws Exception {
    StartedGame game = startGame();
    Decision decision = pendingDecision(game.gameId());
    // Seat 0's pending decision is a role choice; a PassBuilding is never a legal answer to it.
    com.PRS.contract.model.PassBuildingAction illegalAction =
        new com.PRS.contract.model.PassBuildingAction();
    illegalAction.setSeat(decision.getSeat());
    String illegal = objectMapper.writeValueAsString(illegalAction);
    String moveJson =
        "{\"requestId\":%d,\"action\":%s}".formatted(decision.getRequestId(), illegal);

    mockMvc
        .perform(
            post("/api/games/{id}/moves", game.gameId())
                .header("X-Seat-Token", game.humanAToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(moveJson))
        .andExpect(status().isBadRequest());
  }
}
