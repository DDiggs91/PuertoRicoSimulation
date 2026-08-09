package com.PRS.model.actions;

/** A circle on a player board that a colonist can be placed on. */
public sealed interface ColonistSlot {

  /** An island space, indexed into the player's island list. */
  record Island(int index) implements ColonistSlot {}

  /** A city building, indexed into the player's building list. */
  record Building(int index) implements ColonistSlot {}
}
