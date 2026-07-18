package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.GeneralCodingRules.DEPRECATED_API_SHOULD_NOT_BE_USED;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class CodingRulesTest {

    /**
     * Forbids using java.util.logging anywhere; all logging must go through SLF4J.
     *
     * <p>Violation:
     * <pre>{@code
     * private static final java.util.logging.Logger LOG =
     *         java.util.logging.Logger.getLogger(PersonService.class.getName());
     * }</pre>
     */
    @ArchTest
    static final ArchRule noJavaUtilLogging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    /**
     * Forbids throwing generic Exception/RuntimeException/Throwable directly, so callers can always
     * catch a specific, meaningful failure type instead of a catch-all.
     *
     * <p>Compliant:
     * <pre>{@code
     * throw new PersonNotFoundException(id);
     * }</pre>
     *
     * <p>Violation:
     * <pre>{@code
     * throw new RuntimeException("person not found: " + id);
     * }</pre>
     */
    @ArchTest
    static final ArchRule noGenericExceptions = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    /**
     * Forbids calling any API marked {@code @Deprecated}.
     *
     * <p>Violation:
     * <pre>{@code
     * @Deprecated
     * void oldMethod() { ... }
     *
     * void caller() {
     *     oldMethod(); // calling a @Deprecated method
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule noDeprecatedApi = DEPRECATED_API_SHOULD_NOT_BE_USED;

    /**
     * Forbids using Joda-Time; the codebase standardises on {@code java.time} instead.
     *
     * <p>Compliant:
     * <pre>{@code
     * java.time.Instant now = java.time.Instant.now();
     * }</pre>
     *
     * <p>Violation:
     * <pre>{@code
     * org.joda.time.DateTime now = org.joda.time.DateTime.now();
     * }</pre>
     */
    @ArchTest
    static final ArchRule noJodaTime = NO_CLASSES_SHOULD_USE_JODATIME;

}
