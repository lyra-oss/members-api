package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

/**
 * Protects the build wiring, which routes classes purely by name: the Failsafe plugin runs {@code *IT} classes as
 * integration tests and PIT excludes them from mutation analysis, while everything under {@code architecture} is
 * excluded from mutation analysis too. A misnamed integration test would silently run under Surefire (or be mutation-
 * tested against a live application context) instead. This is the one rule set that deliberately analyses the test
 * classes rather than the main source.
 */
@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.OnlyIncludeTests.class)
class TestSuiteRulesTest {

    @ArchTest
    static final ArchRule integrationTestsAreNamedIT =
            classes().that().areAssignableTo("edu.lyra.members.api.BaseIT")
                     .should().haveSimpleNameEndingWith("IT")
                     .as("integration tests (subtypes of BaseIT) must be named *IT so Failsafe runs them");

    @ArchTest
    static final ArchRule archTestsLiveInTheArchitecturePackage =
            fields().that().areAnnotatedWith(ArchTest.class)
                    .should().beDeclaredInClassesThat().resideInAPackage("..architecture..")
                    .as("@ArchTest rules must live under the 'architecture' package that PIT excludes");

}
