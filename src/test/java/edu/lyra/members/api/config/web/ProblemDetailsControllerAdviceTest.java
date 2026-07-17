package edu.lyra.members.api.config.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import edu.lyra.members.api.exceptions.SchoolMismatchException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;

import static java.net.URI.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.core.convert.TypeDescriptor.valueOf;

class ProblemDetailsControllerAdviceTest {

    private final ProblemDetailsControllerAdvice advice = new ProblemDetailsControllerAdvice();

    static Stream<Arguments> messageTruncationScenarios() {
        return Stream.of(arguments("short message", "short message"), arguments("a".repeat(100), "a".repeat(100)),
                         arguments("a".repeat(101), "a".repeat(100) + "…"));
    }

    @Test
    void testErrorResponse() {
        final ResponseEntity<ProblemDetail> response =
                advice.handleDuplicateKeyException(new DuplicateKeyException("duplicate key error"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        final ProblemDetail problemDetail = response.getBody();
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getType()).isEqualTo(
                create("https://lyra.sagittec.com/problems/constraint-violation"));
        assertThat(problemDetail.getTitle()).isEqualTo("Database constraint violation");
        assertThat(problemDetail.getProperties()).containsKey("errors");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }

    @ParameterizedTest
    @MethodSource("messageTruncationScenarios")
    void humanize_handlesMessageLength(final String message, final String expectedDetail) {
        final ResponseEntity<ProblemDetail> response =
                advice.handleDuplicateKeyException(new DuplicateKeyException(message));
        assertThat(response.getBody().getDetail()).isEqualTo(expectedDetail);
    }

    @Test
    void testSchoolMismatchErrorResponse() {
        final ResponseEntity<ProblemDetail> response =
                advice.handleSchoolMismatchException(new SchoolMismatchException("teacher does not belong to school"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        final ProblemDetail problemDetail = response.getBody();
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getType()).isEqualTo(create("https://lyra.sagittec.com/problems/school-mismatch"));
        assertThat(problemDetail.getTitle()).isEqualTo("Teacher does not belong to classroom's school");
        assertThat(problemDetail.getDetail()).isEqualTo("teacher does not belong to school");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }

    @Test
    void testConversionFailedErrorResponse() {
        //@formatter:off
        final ConversionFailedException ex = new ConversionFailedException(
                valueOf(String.class),
                valueOf(UUID.class),
                "not-a-uuid",
                new IllegalArgumentException("Invalid UUID string: not-a-uuid"));
        //@formatter:on
        final ResponseEntity<ProblemDetail> response = advice.handleConversionFailedException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        final ProblemDetail problemDetail = response.getBody();
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getType()).isEqualTo(create("https://lyra.sagittec.com/problems/invalid-parameter"));
        assertThat(problemDetail.getTitle()).isEqualTo("Invalid request parameter");
        assertThat(problemDetail.getDetail()).isEqualTo("'not-a-uuid' is not a valid UUID");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }

    @Test
    void testRepositoryConstraintViolationErrorResponse() {
        final MapBindingResult errors = new MapBindingResult(new HashMap<>(), "Parent");
        errors.addError(new FieldError("Parent", "contactInfo.surname", "must not be blank"));
        final RepositoryConstraintViolationException ex       = new RepositoryConstraintViolationException(errors);
        final ResponseEntity<ProblemDetail>          response = advice.handleRepositoryConstraintViolationException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        final ProblemDetail problemDetail = response.getBody();
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getType()).isEqualTo(create("https://lyra.sagittec.com/problems/validation-error"));
        assertThat(problemDetail.getTitle()).isEqualTo("Validation failed");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
        //@formatter:off
        assertThat(problemDetail.getProperties()).containsEntry("errors",
                List.of(Map.of("entity", "Parent",
                               "property", "contactInfo.surname",
                               "message", "must not be blank")));
        //@formatter:on
    }

}
