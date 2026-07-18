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
     */
    @ArchTest
    static final ArchRule noJavaUtilLogging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    /**
     * Forbids throwing generic Exception/RuntimeException/Throwable directly, so callers can always
     * catch a specific, meaningful failure type instead of a catch-all.
     */
    @ArchTest
    static final ArchRule noGenericExceptions = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    /**
     * Forbids calling any API marked @Deprecated.
     */
    @ArchTest
    static final ArchRule noDeprecatedApi = DEPRECATED_API_SHOULD_NOT_BE_USED;

    /**
     * Forbids using Joda-Time; the codebase standardises on java.time instead.
     */
    @ArchTest
    static final ArchRule noJodaTime = NO_CLASSES_SHOULD_USE_JODATIME;

}
