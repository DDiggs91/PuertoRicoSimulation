package com.PRS.model.rolecards;

/** The seven roles. Two Prospector cards exist, so role cards outnumber roles. */
public enum Role {
  SETTLER,
  MAYOR,
  BUILDER,
  CRAFTSMAN,
  TRADER,
  CAPTAIN,

  /** Alone among the roles, triggers no action for anybody — not even the player who took it. */
  PROSPECTOR
}
