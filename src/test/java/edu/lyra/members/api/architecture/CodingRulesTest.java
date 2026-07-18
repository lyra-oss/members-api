package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.GeneralCodingRules.DEPRECATED_API_SHOULD_NOT_BE_USED;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME;

/**
 * ArchUnit's catalogue of common hygiene rules, adopted alongside the standard-streams rule already in
 * {@code GeneralRulesTest}: logging goes through SLF4J (never {@code java.util.logging}), exceptions thrown are
 * specific (never bare {@code RuntimeException}/{@code Exception}), no deprecated API is used — valuable on a codebase
 * tracking Spring Boot majors this closely — and dates use {@code java.time}, never Joda-Time. Scoped to main source
 * only.
 */
@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class CodingRulesTest {

    @ArchTest
    static final ArchRule noJavaUtilLogging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    @ArchTest
    static final ArchRule noGenericExceptions = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    @ArchTest
    static final ArchRule noDeprecatedApi = DEPRECATED_API_SHOULD_NOT_BE_USED;

    @ArchTest
    static final ArchRule noJodaTime = NO_CLASSES_SHOULD_USE_JODATIME;

}
