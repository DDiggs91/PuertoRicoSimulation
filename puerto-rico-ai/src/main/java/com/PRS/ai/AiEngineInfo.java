package com.PRS.ai;

/** One engine an {@link AiRegistry} can create, listed for a lobby to offer when seating a game. */
public record AiEngineInfo(String id, String displayName, String description) {}
