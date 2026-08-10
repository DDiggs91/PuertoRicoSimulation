package com.PRS.web.api;

import org.springframework.http.HttpStatus;

/**
 * Carries enough to build a {@code Problem} response. {@code reason} is the {@code RejectionReason}
 * or {@code LobbyRejectionReason} enum name when the failure traces back to one, null otherwise
 * (e.g. a malformed seat token).
 */
public final class ApiException extends RuntimeException {

  private final HttpStatus status;
  private final String title;
  private final String reason;

  public ApiException(HttpStatus status, String title, String detail, String reason) {
    super(detail);
    this.status = status;
    this.title = title;
    this.reason = reason;
  }

  public HttpStatus status() {
    return status;
  }

  public String title() {
    return title;
  }

  public String reason() {
    return reason;
  }
}
