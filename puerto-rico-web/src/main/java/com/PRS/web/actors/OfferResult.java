package com.PRS.web.actors;

/** The result of {@link HumanActor#offer}. Rejection is a value here too, never an exception. */
public sealed interface OfferResult {

  record Accepted() implements OfferResult {}

  record Rejected(String detail) implements OfferResult {}
}
