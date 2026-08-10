package com.PRS.web.api;

import com.PRS.ai.AiRegistry;
import com.PRS.contract.api.AiApi;
import com.PRS.contract.model.AiEngineInfo;
import com.PRS.web.wire.LobbyMapper;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AiController implements AiApi {

  private final AiRegistry aiRegistry;

  public AiController(AiRegistry aiRegistry) {
    this.aiRegistry = aiRegistry;
  }

  @Override
  public ResponseEntity<List<AiEngineInfo>> listAiEngines() {
    return ResponseEntity.ok(aiRegistry.available().stream().map(LobbyMapper::toWire).toList());
  }
}
