package com.PRS.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.PRS.session.actors.Actor;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.testng.annotations.Test;

public class AiRegistryTest {

  @Test
  public void availableListsTheRandomEngine() {
    AiRegistry registry = new AiRegistry(Duration.ZERO);

    List<AiEngineInfo> engines = registry.available();

    assertThat(engines).extracting(AiEngineInfo::id).contains("random");
  }

  @Test
  public void createByKnownIdReturnsAnActorWithTheGivenDisplayName() {
    AiRegistry registry = new AiRegistry(Duration.ZERO);

    Optional<Actor> actor = registry.create("random", "Ana", 5L);

    assertThat(actor).isPresent();
    assertThat(actor.get().name()).isEqualTo("Ana");
  }

  @Test
  public void createByUnknownIdReturnsEmptyRatherThanThrowing() {
    AiRegistry registry = new AiRegistry(Duration.ZERO);

    Optional<Actor> actor = registry.create("nonexistent", "Ana", 5L);

    assertThat(actor).isEmpty();
  }
}
