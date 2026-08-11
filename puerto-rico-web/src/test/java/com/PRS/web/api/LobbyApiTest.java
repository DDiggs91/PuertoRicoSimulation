package com.PRS.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.PRS.contract.model.CreateGameResponse;
import com.PRS.contract.model.GameTableSummary;
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
class LobbyApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private String createGame() throws Exception {
    String body =
        mockMvc
            .perform(post("/api/games"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readValue(body, CreateGameResponse.class).getGameId();
  }

  private void seat(String gameId, String name, String kind, String engineId) throws Exception {
    String json =
        engineId == null
            ? "{\"name\":\"%s\",\"kind\":\"%s\"}".formatted(name, kind)
            : "{\"name\":\"%s\",\"kind\":\"%s\",\"engineId\":\"%s\"}"
                .formatted(name, kind, engineId);
    mockMvc
        .perform(
            post("/api/games/{id}/seats", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isOk());
  }

  @Test
  void createGameReturnsANonBlankId() throws Exception {
    assertThat(createGame()).isNotBlank();
  }

  @Test
  void getGameOnAFreshlyCreatedGameReturnsAnOpenEmptyTable() throws Exception {
    String gameId = createGame();

    mockMvc
        .perform(get("/api/games/{id}", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.seats").isEmpty());
  }

  @Test
  void getGameOnAnUnknownIdReturns404WithReason() throws Exception {
    mockMvc
        .perform(get("/api/games/{id}", "00000000-0000-0000-0000-000000000000"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.reason").value("GAME_NOT_FOUND"));
  }

  @Test
  void listGamesIncludesAFreshlyCreatedGame() throws Exception {
    String gameId = createGame();

    String body =
        mockMvc
            .perform(get("/api/games"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    GameTableSummary[] games = objectMapper.readValue(body, GameTableSummary[].class);

    assertThat(games).extracting(GameTableSummary::getId).contains(gameId);
  }

  @Test
  void seatingAiReturnsASeatIndexAndNoToken() throws Exception {
    String gameId = createGame();

    String body =
        mockMvc
            .perform(
                post("/api/games/{id}/seats", gameId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Bot\",\"kind\":\"AI\",\"engineId\":\"random\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    SeatResponse response = objectMapper.readValue(body, SeatResponse.class);

    assertThat(response.getSeatIndex()).isZero();
    assertThat(response.getSeatToken().isPresent()).isFalse();
  }

  @Test
  void seatingAHumanReturnsASeatToken() throws Exception {
    String gameId = createGame();

    String body =
        mockMvc
            .perform(
                post("/api/games/{id}/seats", gameId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Ana\",\"kind\":\"HUMAN\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    SeatResponse response = objectMapper.readValue(body, SeatResponse.class);

    assertThat(response.getSeatToken().orElse(null)).isNotBlank();
  }

  @Test
  void seatingAnUnknownAiEngineReturns400() throws Exception {
    String gameId = createGame();

    mockMvc
        .perform(
            post("/api/games/{id}/seats", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Bot\",\"kind\":\"AI\",\"engineId\":\"nonexistent\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void aSixthSeatIsRejectedAsTableFull() throws Exception {
    String gameId = createGame();
    for (int i = 0; i < 5; i++) {
      seat(gameId, "Bot" + i, "AI", "random");
    }

    mockMvc
        .perform(
            post("/api/games/{id}/seats", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Overflow\",\"kind\":\"AI\",\"engineId\":\"random\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.reason").value("TABLE_FULL"));
  }

  @Test
  void startingWithFewerThanThreeSeatsReturns409() throws Exception {
    String gameId = createGame();
    seat(gameId, "Bot0", "AI", "random");

    mockMvc
        .perform(post("/api/games/{id}/start", gameId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.reason").value("TOO_FEW_SEATS"));
  }

  @Test
  void startingAnUnknownGameReturns404() throws Exception {
    mockMvc
        .perform(post("/api/games/{id}/start", "00000000-0000-0000-0000-000000000000"))
        .andExpect(status().isNotFound());
  }

  @Test
  void startingASufficientlySeatedGameFlipsItToStarted() throws Exception {
    String gameId = createGame();
    for (int i = 0; i < 3; i++) {
      seat(gameId, "Bot" + i, "AI", "random");
    }

    mockMvc
        .perform(post("/api/games/{id}/start", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("STARTED"));
  }

  @Test
  void startingAnAlreadyStartedGameReturns409() throws Exception {
    String gameId = seatedGame();
    mockMvc.perform(post("/api/games/{id}/start", gameId)).andExpect(status().isOk());

    mockMvc
        .perform(post("/api/games/{id}/start", gameId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.reason").value("ALREADY_STARTED"));
  }

  @Test
  void seatingAfterTheGameHasStartedReturns409() throws Exception {
    String gameId = seatedGame();
    mockMvc.perform(post("/api/games/{id}/start", gameId)).andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/games/{id}/seats", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Latecomer\",\"kind\":\"AI\",\"engineId\":\"random\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.reason").value("ALREADY_STARTED"));
  }

  /**
   * {@code seed} is a JsonNullable, so an explicit null arrives present-holding-null rather than
   * undefined — the case that used to unbox into an NPE and 500.
   */
  @Test
  void startingWithAnExplicitNullSeedPicksARandomOne() throws Exception {
    String gameId = seatedGame();

    mockMvc
        .perform(
            post("/api/games/{id}/start", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seed\":null}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("STARTED"));
  }

  @Test
  void twoGamesStartedWithTheSameSeedDealTheSameBoard() throws Exception {
    String first = startWithSeed(seatedGame(), 42L);
    String second = startWithSeed(seatedGame(), 42L);

    assertThat(faceUpTiles(first)).isEqualTo(faceUpTiles(second)).isNotEmpty();
  }

  /** Three human seats, so nothing plays on its own and the dealt state stays put to be read. */
  private String seatedGame() throws Exception {
    String gameId = createGame();
    for (int i = 0; i < 3; i++) {
      seat(gameId, "Human" + i, "HUMAN", null);
    }
    return gameId;
  }

  private String startWithSeed(String gameId, long seed) throws Exception {
    mockMvc
        .perform(
            post("/api/games/{id}/start", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seed\":%d}".formatted(seed)))
        .andExpect(status().isOk());
    return gameId;
  }

  private String faceUpTiles(String gameId) throws Exception {
    return objectMapper
        .readTree(
            mockMvc
                .perform(get("/api/games/{id}/state", gameId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .at("/state/tiles/faceUp")
        .toString();
  }

  @Test
  void listAiEnginesIncludesRandom() throws Exception {
    mockMvc
        .perform(get("/api/ai/engines"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id=='random')]").exists());
  }
}
