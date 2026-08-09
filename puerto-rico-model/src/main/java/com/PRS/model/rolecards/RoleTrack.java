package com.PRS.model.rolecards;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The role cards in play for a game, which is always three more than the player count — so exactly
 * three cards go unchosen and collect a doubloon each round.
 */
public record RoleTrack(List<RoleCard> cards) {

  /** Role cards beyond one per player, i.e. how many collect a doubloon at the end of a round. */
  public static final int SPARE_CARDS = 3;

  public RoleTrack {
    cards = List.copyOf(cards);
  }

  /**
   * The cards dealt for a given player count: every role once, plus a second Prospector at five
   * players. Three players use neither Prospector, four use one.
   */
  public static RoleTrack forPlayerCount(int playerCount) {
    List<RoleCard> dealt = new ArrayList<>();
    for (Role role : Role.values()) {
      if (role == Role.PROSPECTOR) {
        continue;
      }
      dealt.add(RoleCard.available(role));
    }
    int prospectors = playerCount + SPARE_CARDS - dealt.size();
    for (int i = 0; i < prospectors; i++) {
      dealt.add(RoleCard.available(Role.PROSPECTOR));
    }
    return new RoleTrack(dealt);
  }

  /** Distinct roles still choosable this round, in card order. */
  public List<Role> availableRoles() {
    Set<Role> available = new LinkedHashSet<>();
    for (RoleCard card : cards) {
      if (!card.isTaken()) {
        available.add(card.role());
      }
    }
    return List.copyOf(available);
  }

  public boolean isAvailable(Role role) {
    return indexToTake(role).isPresent();
  }

  /** Doubloons the chooser would collect along with the role. */
  public int doubloonsOn(Role role) {
    return indexToTake(role).map(i -> cards.get(i).doubloons()).orElse(0);
  }

  public RoleTrack take(Role role, int seat) {
    int index =
        indexToTake(role).orElseThrow(() -> new IllegalStateException(role + " is not available"));
    List<RoleCard> next = new ArrayList<>(cards);
    next.set(index, next.get(index).takenBy(seat));
    return new RoleTrack(next);
  }

  /** Adds a doubloon to every unchosen card and returns all cards to the supply. */
  public RoleTrack endRound() {
    List<RoleCard> next = new ArrayList<>();
    for (RoleCard card : cards) {
      next.add(card.isTaken() ? card.released() : card.plusDoubloon());
    }
    return new RoleTrack(next);
  }

  /**
   * Which physical card a chooser gets for a role. The two Prospectors differ only in the doubloons
   * on them, so taking the richer one is strictly better and the choice needs no player input.
   */
  private Optional<Integer> indexToTake(Role role) {
    int best = -1;
    for (int i = 0; i < cards.size(); i++) {
      RoleCard card = cards.get(i);
      if (card.role() == role && !card.isTaken()) {
        if (best < 0 || card.doubloons() > cards.get(best).doubloons()) {
          best = i;
        }
      }
    }
    return best < 0 ? Optional.empty() : Optional.of(best);
  }
}
