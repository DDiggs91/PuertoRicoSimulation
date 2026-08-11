package com.PRS.web;

import com.PRS.lobby.GameId;
import com.PRS.lobby.Lobby;
import com.PRS.web.actors.SeatTokens;
import com.PRS.web.events.GameEventStream;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reclaims finished and abandoned games. The lobby holds a {@code GameSession}, a full {@code
 * GameState}, and a {@code SessionRunner} thread per table, and the web layer keys seat tokens and
 * SSE subscriptions on the same {@link GameId} — none of which anything drops on its own, so
 * without this a long-running server leaks every game it has ever hosted.
 *
 * <p>All three evictions happen together and in that order: the lobby decides what is stale, and
 * the two web-side maps follow its answer, so nothing can be reachable through one and gone from
 * another.
 */
@Component
public class GameTableSweeper {

  private static final Logger log = LoggerFactory.getLogger(GameTableSweeper.class);

  private final Lobby lobby;
  private final SeatTokens seatTokens;
  private final GameEventStream eventStream;
  private final Duration retention;

  public GameTableSweeper(
      Lobby lobby,
      SeatTokens seatTokens,
      GameEventStream eventStream,
      @Value("${app.lobby.retention-minutes:30}") long retentionMinutes) {
    this.lobby = lobby;
    this.seatTokens = seatTokens;
    this.eventStream = eventStream;
    this.retention = Duration.ofMinutes(retentionMinutes);
  }

  @Scheduled(
      initialDelayString = "${app.lobby.sweep-interval-ms:300000}",
      fixedDelayString = "${app.lobby.sweep-interval-ms:300000}")
  public void sweep() {
    List<GameId> evicted = lobby.evictStaleTables(retention);
    for (GameId id : evicted) {
      seatTokens.evict(id);
      eventStream.closeStreamsFor(id);
    }
    if (!evicted.isEmpty()) {
      log.info("Evicted {} stale game table(s)", evicted.size());
    }
  }
}
