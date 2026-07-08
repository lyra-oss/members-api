package edu.lyra.members.api.mvc;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static java.net.URI.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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

}
