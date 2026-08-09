package com.PRS.session.events;

/**
 * Notified of every {@link SessionEvent} in order, on the caller's thread. A listener that throws
 * is caught and logged so one broken listener can't kill a game; a listener that blocks stalls the
 * session, so don't do slow work here.
 */
public interface SessionListener {

  void onEvent(SessionEvent event);
}
