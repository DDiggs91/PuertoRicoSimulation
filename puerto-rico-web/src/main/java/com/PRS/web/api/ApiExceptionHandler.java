package com.PRS.web.api;

import com.PRS.contract.model.Problem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps {@link ApiException} to a {@code Problem} body. Rejections stay values end-to-end. */
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Problem> handle(ApiException e) {
    Problem problem =
        new Problem(e.status().value(), e.title()).detail(e.getMessage()).reason(e.reason());
    return ResponseEntity.status(e.status()).body(problem);
  }
}
