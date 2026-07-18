package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class ExceptionRulesTest {

    /**
     * Every Throwable subclass must live in an "...exceptions" package, so custom exceptions are
     * always easy to find in one place rather than scattered across the codebase.
     */
    @ArchTest
    static final ArchRule throwablesLiveInTheExceptionsPackage =
            classes().that().areAssignableTo(Throwable.class)
                     .should().resideInAPackage("..exceptions");

    /**
     * Every Throwable subclass must have a simple name ending in "Exception", keeping the naming
     * convention self-explanatory at a glance.
     */
    @ArchTest
    static final ArchRule throwablesAreNamedException =
            classes().that().areAssignableTo(Throwable.class)
                     .should().haveSimpleNameEndingWith("Exception");

}
