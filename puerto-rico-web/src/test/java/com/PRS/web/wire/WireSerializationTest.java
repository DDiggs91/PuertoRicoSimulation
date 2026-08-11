package com.PRS.web.wire;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.model.actions.PlayerAction;
import com.PRS.model.game.GameConfig;
import com.PRS.model.game.GameSetup;
import com.PRS.model.goods.Good;
import com.PRS.model.rolecards.Role;
import com.PRS.session.events.SessionEvent;
import com.PRS.session.view.GameView;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

/**
 * The discriminator is the one part of the wire format no mapper writes: {@code ActionMapper} and
 * {@code SessionEventMapper} never set {@code type}, so correct JSON depends entirely on the
 * {@code @JsonTypeInfo}/{@code @JsonSubTypes} annotations the generator puts on the base classes.
 * The object-to-object round-trip in {@link WireMappingTest} cannot see that, so a generator-config
 * change that dropped the annotations would break every client with the suite still green. These
 * tests serialize for real, against the application's own Jackson 3 mapper.
 */
@SpringBootTest
class WireSerializationTest {

  @Autowired private ObjectMapper objectMapper;

  private static GameView sampleView() {
    return GameView.of(GameSetup.create(new GameConfig(List.of("Ana", "Bo", "Coco"), 1L)), null);
  }

  @Test
  void anActionSerializesWithItsDiscriminator() {
    com.PRS.contract.model.PlayerAction wire =
        ActionMapper.toWire(new PlayerAction.SelectRole(0, Role.SETTLER));

    String json = objectMapper.writeValueAsString(wire);

    assertThat(json).contains("\"type\":\"SELECT_ROLE\"", "\"seat\":0", "\"role\":\"SETTLER\"");
  }

  @Test
  void eachActionVariantSerializesUnderItsOwnDiscriminator() {
    assertThat(objectMapper.writeValueAsString(ActionMapper.toWire(new PlayerAction.TakeQuarry(2))))
        .contains("\"type\":\"TAKE_QUARRY\"");
    assertThat(
            objectMapper.writeValueAsString(
                ActionMapper.toWire(new PlayerAction.LoadShip(1, 0, Good.INDIGO))))
        .contains("\"type\":\"LOAD_SHIP\"", "\"shipIndex\":0", "\"good\":\"INDIGO\"");
    assertThat(
            objectMapper.writeValueAsString(ActionMapper.toWire(new PlayerAction.DeclineWharf(2))))
        .contains("\"type\":\"DECLINE_WHARF\"", "\"seat\":2");
  }

  @Test
  void anEventSerializesWithItsDiscriminator() {
    com.PRS.contract.model.SessionEvent wire =
        SessionEventMapper.toWire(new SessionEvent.GameStarted(sampleView(), List.of("Ana", "Bo")));

    String json = objectMapper.writeValueAsString(wire);

    assertThat(json).contains("\"type\":\"GAME_STARTED\"", "\"seatNames\":[\"Ana\",\"Bo\"]");
  }

  @Test
  void aSerializedActionDeserializesBackToItsConcreteSubtype() {
    com.PRS.contract.model.PlayerAction wire =
        ActionMapper.toWire(new PlayerAction.SellGood(1, Good.SUGAR));

    String json = objectMapper.writeValueAsString(wire);
    com.PRS.contract.model.PlayerAction back =
        objectMapper.readValue(json, com.PRS.contract.model.PlayerAction.class);

    assertThat(ActionMapper.toModel(back)).isEqualTo(new PlayerAction.SellGood(1, Good.SUGAR));
  }

  /** The view is what every event carries, and the seed must not survive into the JSON. */
  @Test
  void aSerializedViewCarriesNoSeed() {
    String json = objectMapper.writeValueAsString(GameMapper.toWire(sampleView()));

    assertThat(json).doesNotContain("seed");
  }
}
