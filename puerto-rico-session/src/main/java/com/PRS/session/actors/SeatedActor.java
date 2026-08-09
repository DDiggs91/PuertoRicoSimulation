package com.PRS.session.actors;

/** An actor bound to a seat before a game starts. Seat order becomes turn order. */
public record SeatedActor(Actor actor, ActorKind kind) {}
