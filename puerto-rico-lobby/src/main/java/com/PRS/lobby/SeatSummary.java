package com.PRS.lobby;

import com.PRS.session.actors.ActorKind;

/** A seated actor's display name and kind, without exposing the {@code Actor} itself. */
public record SeatSummary(String name, ActorKind kind) {}
