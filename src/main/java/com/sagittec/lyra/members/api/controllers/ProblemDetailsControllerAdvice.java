package com.sagittec.lyra.members.api.controllers;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static java.net.URI.create;
import static java.time.OffsetDateTime.now;

import static org.springframework.http.HttpStatus.CONFLICT;

@ControllerAdvice
class ProblemDetailsControllerAdvice
        extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleIntegrity(final DataIntegrityViolationException ex) {
        final ProblemDetail pd = ProblemDetail.forStatus(CONFLICT);
        pd.setType(create("https://lyra.sagittec.com/problems/constraint-violation"));
        pd.setTitle("Database constraint violation");
        pd.setDetail(this.humanize(ex));
        //@formatter:off
        pd.setProperty("errors", Map.of("code", "UNIQUE_CONSTRAINT",
                                        "message", "There already exists a resource with the same constraints"));
        //@formatter:on
        pd.setProperty("timestamp", now());
        return ResponseEntity.status(CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }

    private String humanize(Throwable ex) {
        final String msg = ex.getMessage();
        return (msg != null && msg.length() > 100) ? msg.substring(0, 100) + "…" : msg;
    }

}
