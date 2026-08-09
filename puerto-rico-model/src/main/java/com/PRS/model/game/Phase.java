package com.PRS.model.game;

import com.PRS.model.goods.Good;
import java.util.List;
import java.util.Set;

/**
 * Where a game currently is. Each variant carries exactly the transient state its phase needs, so
 * the engine never has to infer whose turn it is or what has already happened.
 *
 * <p>Phases that hand every player a turn carry a {@code queue} of seats, head first.
 */
public sealed interface Phase {

  /** The seat that must act next, or -1 when the game is over. */
  int actorSeat();

  /** Somebody must choose a role card. */
  record RoleSelection(int actorSeat) implements Phase {}

  record SettlerPhase(int chooserSeat, List<Integer> queue, boolean haciendaOffered)
      implements Phase {
    public SettlerPhase {
      queue = List.copyOf(queue);
    }

    @Override
    public int actorSeat() {
      return queue.getFirst();
    }
  }

  record MayorPhase(int chooserSeat, List<Integer> queue) implements Phase {
    public MayorPhase {
      queue = List.copyOf(queue);
    }

    @Override
    public int actorSeat() {
      return queue.getFirst();
    }
  }

  record BuilderPhase(int chooserSeat, List<Integer> queue) implements Phase {
    public BuilderPhase {
      queue = List.copyOf(queue);
    }

    @Override
    public int actorSeat() {
      return queue.getFirst();
    }
  }

  /**
   * Production itself is deterministic and resolved on entry; the only craftsman decision is which
   * bonus barrel to take.
   */
  record CraftsmanBonus(int chooserSeat, Set<Good> options) implements Phase {
    public CraftsmanBonus {
      options = Set.copyOf(options);
    }

    @Override
    public int actorSeat() {
      return chooserSeat;
    }
  }

  record TraderPhase(int chooserSeat, List<Integer> queue) implements Phase {
    public TraderPhase {
      queue = List.copyOf(queue);
    }

    @Override
    public int actorSeat() {
      return queue.getFirst();
    }
  }

  /**
   * Loading cycles clockwise for as long as anyone can load, so this tracks a single actor rather
   * than a queue, plus the once-per-phase privileges.
   */
  record CaptainLoading(int chooserSeat, int actorSeat, Set<Integer> wharfUsed, boolean bonusUsed)
      implements Phase {
    public CaptainLoading {
      wharfUsed = Set.copyOf(wharfUsed);
    }
  }

  /** Deciding what survives the end of the captain phase. Only players holding goods queue up. */
  record CaptainStorage(int chooserSeat, List<Integer> queue) implements Phase {
    public CaptainStorage {
      queue = List.copyOf(queue);
    }

    @Override
    public int actorSeat() {
      return queue.getFirst();
    }
  }

  record GameOver() implements Phase {
    @Override
    public int actorSeat() {
      return -1;
    }
  }
}
