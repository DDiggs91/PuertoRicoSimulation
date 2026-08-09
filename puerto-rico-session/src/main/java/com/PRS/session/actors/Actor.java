package com.PRS.session.actors;

import com.PRS.model.actions.PlayerAction;
import java.util.concurrent.CompletableFuture;

/**
 * A decision-maker: human or AI, indistinguishable to the session. An AI answers immediately with
 * {@code CompletableFuture.completedFuture(...)}; a human adapter returns an incomplete future and
 * completes it once a move arrives over the network. Either way the session drives the game the
 * same way.
 */
public interface Actor {

  String name();

  CompletableFuture<PlayerAction> decide(Decision decision);
}
