package edu.lyra.members.api.config.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import edu.lyra.members.api.exceptions.ClassroomHasKidsException;
import edu.lyra.members.api.exceptions.ParentHasKidsException;
import edu.lyra.members.api.exceptions.SchoolHasReferencesException;
import edu.lyra.members.api.exceptions.SchoolMismatchException;
import edu.lyra.members.api.exceptions.TeacherAssignedToClassroomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataIntegrityViolationException;
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
                         arguments("a".repeat(101), "a".repeat(100) + "…"), arguments(null, null));
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
    void testParentHasKidsErrorResponse() {
        final ResponseEntity<ProblemDetail> response =
                advice.handleParentHasKidsException(new ParentHasKidsException("parent still has 2 kid(s) linked"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        final ProblemDetail problemDetail = response.getBody();
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getType()).isEqualTo(create("https://lyra.sagittec.com/problems/parent-has-kids"));
        assertThat(problemDetail.getTitle()).isEqualTo("Parent still has kids linked");
        assertThat(problemDetail.getDetail()).isEqualTo("parent still has 2 kid(s) linked");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }

    @Test
    void testTeacherAssignedToClassroomErrorResponse() {
        final ResponseEntity<ProblemDetail> response = advice.handleTeacherAssignedToClassroomException(
                new TeacherAssignedToClassroomException("teacher still tutors a classroom"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        final ProblemDetail problemDetail = response.getBody();
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getType()).isEqualTo(
                create("https://lyra.sagittec.com/problems/teacher-assigned-to-classroom"));
        assertThat(problemDetail.getTitle()).isEqualTo("Teacher is still assigned to a classroom");
        assertThat(problemDetail.getDetail()).isEqualTo("teacher still tutors a classroom");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }

    @Test
    void testClassroomHasKidsErrorResponse() {
        final ResponseEntity<ProblemDetail> response = advice.handleClassroomHasKidsException(
                new ClassroomHasKidsException("classroom still has 3 kid(s) enrolled"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        final ProblemDetail problemDetail = response.getBody();
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getType()).isEqualTo(create("https://lyra.sagittec.com/problems/classroom-has-kids"));
        assertThat(problemDetail.getTitle()).isEqualTo("Classroom still has kids enrolled");
        assertThat(problemDetail.getDetail()).isEqualTo("classroom still has 3 kid(s) enrolled");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }

    @Test
    void testSchoolHasReferencesErrorResponse() {
        final ResponseEntity<ProblemDetail> response = advice.handleSchoolHasReferencesException(
                new SchoolHasReferencesException("school still has 1 classroom(s) and 2 teacher(s) linked"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        final ProblemDetail problemDetail = response.getBody();
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getType()).isEqualTo(
                create("https://lyra.sagittec.com/problems/school-has-references"));
        assertThat(problemDetail.getTitle()).isEqualTo("School still has classrooms or teachers linked");
        assertThat(problemDetail.getDetail()).isEqualTo("school still has 1 classroom(s) and 2 teacher(s) linked");
        assertThat(problemDetail.getProperties()).containsKey("timestamp");
    }

    @Test
    void testDataIntegrityViolationErrorResponse() {
        final ResponseEntity<ProblemDetail> response = advice.handleDataIntegrityViolationException(
                new DataIntegrityViolationException("foreign key constraint violated"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        final ProblemDetail problemDetail = response.getBody();
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getType()).isEqualTo(
                create("https://lyra.sagittec.com/problems/referential-integrity-violation"));
        assertThat(problemDetail.getTitle()).isEqualTo("Referential integrity constraint violation");
        assertThat(problemDetail.getDetail()).isEqualTo("foreign key constraint violated");
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
        errors.addError(new FieldError("Parent", "surname", "must not be blank"));
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
                               "property", "surname",
                               "message", "must not be blank")));
        //@formatter:on
    }

}
