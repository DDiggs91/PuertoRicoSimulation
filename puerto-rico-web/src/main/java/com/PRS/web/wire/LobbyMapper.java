package com.PRS.web.wire;

import com.PRS.contract.model.AiEngineInfo;
import com.PRS.lobby.GameTableSummary;
import com.PRS.session.actors.ActorKind;

/** {@code com.PRS.lobby} and {@code com.PRS.ai} types to/from their generated wire counterparts. */
public final class LobbyMapper {

  private LobbyMapper() {}

  public static com.PRS.contract.model.ActorKind toWire(ActorKind kind) {
    return com.PRS.contract.model.ActorKind.valueOf(kind.name());
  }

  public static ActorKind toModel(com.PRS.contract.model.ActorKind kind) {
    return ActorKind.valueOf(kind.name());
  }

  public static com.PRS.contract.model.GameTableSummary toWire(GameTableSummary summary) {
    return new com.PRS.contract.model.GameTableSummary(
        summary.id().value().toString(),
        summary.seats().stream()
            .map(seat -> new com.PRS.contract.model.SeatSummary(seat.name(), toWire(seat.kind())))
            .toList(),
        com.PRS.contract.model.GameTableStatus.valueOf(summary.status().name()));
  }

  public static AiEngineInfo toWire(com.PRS.ai.AiEngineInfo info) {
    return new AiEngineInfo(info.id(), info.displayName(), info.description());
  }
}
