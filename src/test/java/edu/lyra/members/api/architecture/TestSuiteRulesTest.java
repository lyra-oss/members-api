package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

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
