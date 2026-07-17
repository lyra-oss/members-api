package edu.lyra.members.api.config.web;

import java.util.List;
import java.util.Map;

import edu.lyra.members.api.exceptions.SchoolMismatchException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static java.net.URI.create;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Map.of;
import static java.util.Objects.requireNonNullElse;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static org.springframework.http.ResponseEntity.status;

@ControllerAdvice
class ProblemDetailsControllerAdvice
        extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateKeyException(final DuplicateKeyException ex) {
        //@formatter:off
        return ProblemDetailBuilder.forStatus(CONFLICT)
                .type("https://lyra.sagittec.com/problems/constraint-violation")
                .title("Database constraint violation")
                .detail(this.humanize(ex))
                .property("errors", of("code", "UNIQUE_CONSTRAINT",
                                       "message", "There already exists a resource with the same constraints"))
                .build();
        //@formatter:on
    }

    @ExceptionHandler(SchoolMismatchException.class)
    public ResponseEntity<ProblemDetail> handleSchoolMismatchException(final SchoolMismatchException ex) {
        return ProblemDetailBuilder.forStatus(UNPROCESSABLE_CONTENT)
                                   .type("https://lyra.sagittec.com/problems/school-mismatch")
                                   .title("Teacher does not belong to classroom's school").detail(this.humanize(ex))
                                   .build();
    }

    @ExceptionHandler(ConversionFailedException.class)
    public ResponseEntity<ProblemDetail> handleConversionFailedException(final ConversionFailedException ex) {
        return ProblemDetailBuilder.forStatus(BAD_REQUEST).type("https://lyra.sagittec.com/problems/invalid-parameter")
                                   .title("Invalid request parameter")
                                   .detail("'%s' is not a valid %s".formatted(ex.getValue(),
                                                                              ex.getTargetType().getType()
                                                                                .getSimpleName())).build();
    }

    @ExceptionHandler(RepositoryConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleRepositoryConstraintViolationException(final RepositoryConstraintViolationException ex) {
        //@formatter:off
        final List<Object> errors = ex.getErrors().getFieldErrors().stream()
                                       .map(this::toErrorEntry)
                                       .map(Object.class::cast)
                                       .toList();
        return ProblemDetailBuilder.forStatus(BAD_REQUEST)
                .type("https://lyra.sagittec.com/problems/validation-error")
                .title("Validation failed")
                .property("errors", errors)
                .build();
        //@formatter:on
    }

    private Map<String, String> toErrorEntry(final FieldError fieldError) {
        //@formatter:off
        return of("entity", fieldError.getObjectName(),
                  "property", fieldError.getField(),
                  "message", requireNonNullElse(fieldError.getDefaultMessage(), "invalid"));
        //@formatter:on
    }

    private String humanize(final Throwable ex) {
        final String msg = ex.getMessage();
        return (msg != null && msg.length() > 100) ? msg.substring(0, 100) + "…" : msg;
    }

    private static final class ProblemDetailBuilder {

        private final HttpStatus    status;
        private final ProblemDetail problemDetail;

        private ProblemDetailBuilder(final HttpStatus status) {
            this.status        = status;
            this.problemDetail = ProblemDetail.forStatus(status);
            this.problemDetail.setProperty("timestamp", now(systemDefault()));
        }

        private static ProblemDetailBuilder forStatus(final HttpStatus status) {
            return new ProblemDetailBuilder(status);
        }

        private ProblemDetailBuilder type(final String type) {
            problemDetail.setType(create(type));
            return this;
        }

        private ProblemDetailBuilder title(final String title) {
            problemDetail.setTitle(title);
            return this;
        }

        private ProblemDetailBuilder detail(final String detail) {
            problemDetail.setDetail(detail);
            return this;
        }

        private ProblemDetailBuilder property(final String name, final Object value) {
            problemDetail.setProperty(name, value);
            return this;
        }

        private ResponseEntity<ProblemDetail> build() {
            return status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
        }

    }

}
