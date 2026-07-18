package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.OnlyIncludeTests.class)
class TestSuiteRulesTest {

    /**
     * Every subclass of BaseIT must have a simple name ending in "IT", which is the naming
     * convention the Failsafe plugin uses to discover and run integration tests.
     */
    @ArchTest
    static final ArchRule integrationTestsAreNamedIT =
            classes().that().areAssignableTo("edu.lyra.members.api.BaseIT")
                     .should().haveSimpleNameEndingWith("IT")
                     .as("integration tests (subtypes of BaseIT) must be named *IT so Failsafe runs them");

    /**
     * Every @ArchTest field must be declared in a class that lives directly in the
     * "architecture" package (not in a sub-package of it), which PIT mutation testing excludes,
     * keeping architecture-rule fields out of scope of mutation coverage and all together in one
     * place.
     */
    @ArchTest
    static final ArchRule archTestsLiveInTheArchitecturePackage =
            fields().that().areAnnotatedWith(ArchTest.class)
                    .should().beDeclaredInClassesThat().resideInAPackage("..architecture")
                    .as("@ArchTest rules must live directly in the 'architecture' package that PIT excludes, "
                        + "not in a sub-package of it");

}
