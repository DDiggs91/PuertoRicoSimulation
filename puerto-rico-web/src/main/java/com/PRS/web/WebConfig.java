package com.PRS.web;

import com.PRS.ai.AiRegistry;
import com.PRS.lobby.Lobby;
import com.PRS.web.actors.SeatTokens;
import com.PRS.web.events.GameEventStream;
import java.time.Duration;
import org.openapitools.jackson.nullable.JsonNullableJackson3Module;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

  @Bean
  public Lobby lobby() {
    return new Lobby();
  }

  /**
   * {@code thinkTimeMs} paces an AI-vs-AI game so a spectator can actually watch it move by move —
   * 300ms by default. Configurable (`app.ai.think-time-ms`, e.g. via a `-D` system property)
   * specifically so Playwright's e2e suite can drive a full game through in seconds rather than the
   * minutes real pacing would take, without changing the production default.
   */
  @Bean
  public AiRegistry aiRegistry(@Value("${app.ai.think-time-ms:300}") long thinkTimeMs) {
    return new AiRegistry(Duration.ofMillis(thinkTimeMs));
  }

  @Bean
  public SeatTokens seatTokens() {
    return new SeatTokens();
  }

  @Bean
  public GameEventStream gameEventStream() {
    return new GameEventStream();
  }

  /**
   * Required by openapi-generator's "spring" output: every optional (as opposed to nullable) field
   * is generated as {@code JsonNullable<T>}, which Jackson cannot (de)serialize without this module
   * registered. This project runs on Jackson 3 ({@code tools.jackson.databind}, per Spring Boot
   * 4.1's default), so it's specifically the Jackson-3-flavored module — the plain {@code
   * JsonNullableModule} in the same library targets Jackson 2 and won't register against a Jackson
   * 3 {@code ObjectMapper}.
   */
  @Bean
  public JsonNullableJackson3Module jsonNullableModule() {
    return new JsonNullableJackson3Module();
  }
}
