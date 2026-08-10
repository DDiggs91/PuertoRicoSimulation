package com.PRS.web.wire;

import com.PRS.contract.model.ActionAppliedEvent;
import com.PRS.contract.model.ActionRejectedEvent;
import com.PRS.contract.model.GameEndedEvent;
import com.PRS.contract.model.GameStartedEvent;
import com.PRS.contract.model.RejectionReason;
import com.PRS.contract.model.SessionFailedEvent;
import com.PRS.session.events.SessionEvent;

/** {@code com.PRS.session.events.SessionEvent} to its generated wire counterpart. */
public final class SessionEventMapper {

  private SessionEventMapper() {}

  public static com.PRS.contract.model.SessionEvent toWire(SessionEvent event) {
    return switch (event) {
      case SessionEvent.GameStarted e -> {
        GameStartedEvent wire = new GameStartedEvent();
        wire.setView(GameMapper.toWire(e.view()));
        wire.setSeatNames(e.seatNames());
        yield wire;
      }
      case SessionEvent.DecisionRequested e -> {
        com.PRS.contract.model.DecisionRequestedEvent wire =
            new com.PRS.contract.model.DecisionRequestedEvent();
        wire.setView(GameMapper.toWire(e.view()));
        wire.setSeat(e.seat());
        wire.setOptions(e.options().stream().map(ActionMapper::toWire).toList());
        wire.setRequestId(e.requestId());
        yield wire;
      }
      case SessionEvent.ActionApplied e -> {
        ActionAppliedEvent wire = new ActionAppliedEvent();
        wire.setView(GameMapper.toWire(e.view()));
        wire.setSeat(e.seat());
        wire.setAction(ActionMapper.toWire(e.action()));
        yield wire;
      }
      case SessionEvent.ActionRejected e -> {
        ActionRejectedEvent wire = new ActionRejectedEvent();
        wire.setView(GameMapper.toWire(e.view()));
        wire.setSeat(e.seat());
        wire.setAction(ActionMapper.toWire(e.action()));
        wire.setReason(RejectionReason.valueOf(e.reason().name()));
        wire.setDetail(e.detail());
        yield wire;
      }
      case SessionEvent.GameEnded e -> {
        GameEndedEvent wire = new GameEndedEvent();
        wire.setView(GameMapper.toWire(e.view()));
        wire.setStandings(e.standings().stream().map(GameMapper::toWire).toList());
        yield wire;
      }
      case SessionEvent.SessionFailed e -> {
        SessionFailedEvent wire = new SessionFailedEvent();
        wire.setView(GameMapper.toWire(e.view()));
        wire.setDetail(e.detail());
        yield wire;
      }
    };
  }
}
