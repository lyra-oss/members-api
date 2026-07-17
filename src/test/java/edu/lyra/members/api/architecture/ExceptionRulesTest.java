package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Keeps custom exceptions discoverable and consistently named: every {@link Throwable} the application declares lives
 * in the shared {@code exceptions} package and is named {@code *Exception}, so the single
 * {@code ProblemDetailsControllerAdvice} that maps them to responses has one obvious place to look. Deliberately says
 * nothing about checked vs. unchecked — checked exceptions may be introduced later. Scoped to main source only.
 */
@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class ExceptionRulesTest {

    @ArchTest
    static final ArchRule throwablesLiveInTheExceptionsPackage =
            classes().that().areAssignableTo(Throwable.class)
                     .should().resideInAPackage("..exceptions");

    @ArchTest
    static final ArchRule throwablesAreNamedException =
            classes().that().areAssignableTo(Throwable.class)
                     .should().haveSimpleNameEndingWith("Exception");

}
