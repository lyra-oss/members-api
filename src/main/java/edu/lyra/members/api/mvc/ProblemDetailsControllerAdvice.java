package edu.lyra.members.api.mvc;

import edu.lyra.members.api.handlers.SchoolMismatchException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static java.net.URI.create;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Map.of;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static org.springframework.http.ResponseEntity.status;

@ControllerAdvice
class ProblemDetailsControllerAdvice
        extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateKeyException(final DuplicateKeyException ex) {
        final ProblemDetail problemDetail = ProblemDetail.forStatus(CONFLICT);
        problemDetail.setType(create("https://lyra.sagittec.com/problems/constraint-violation"));
        problemDetail.setTitle("Database constraint violation");
        problemDetail.setDetail(this.humanize(ex));
        problemDetail.setProperty("timestamp", now(systemDefault()));
        //@formatter:off
        problemDetail.setProperty("errors", of("code", "UNIQUE_CONSTRAINT",
                                               "message", "There already exists a resource with the same constraints"));
        //@formatter:on
        return status(CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    @ExceptionHandler(SchoolMismatchException.class)
    public ResponseEntity<ProblemDetail> handleSchoolMismatchException(final SchoolMismatchException ex) {
        final ProblemDetail problemDetail = ProblemDetail.forStatus(UNPROCESSABLE_CONTENT);
        problemDetail.setType(create("https://lyra.sagittec.com/problems/school-mismatch"));
        problemDetail.setTitle("Teacher does not belong to classroom's school");
        problemDetail.setDetail(this.humanize(ex));
        problemDetail.setProperty("timestamp", now(systemDefault()));
        return status(UNPROCESSABLE_CONTENT).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    @ExceptionHandler(ConversionFailedException.class)
    public ResponseEntity<ProblemDetail> handleConversionFailedException(final ConversionFailedException ex) {
        final ProblemDetail problemDetail = ProblemDetail.forStatus(BAD_REQUEST);
        problemDetail.setType(create("https://lyra.sagittec.com/problems/invalid-parameter"));
        problemDetail.setTitle("Invalid request parameter");
        problemDetail.setDetail(
                "'%s' is not a valid %s".formatted(ex.getValue(), ex.getTargetType().getType().getSimpleName()));
        problemDetail.setProperty("timestamp", now(systemDefault()));
        return status(BAD_REQUEST).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
    }

    private String humanize(final Throwable ex) {
        final String msg = ex.getMessage();
        return (msg != null && msg.length() > 100) ? msg.substring(0, 100) + "…" : msg;
    }

}
