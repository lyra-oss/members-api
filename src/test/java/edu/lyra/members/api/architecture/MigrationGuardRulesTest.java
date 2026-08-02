package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class MigrationGuardRulesTest {

    /**
     * No class may depend on Spring Data REST, so it cannot creep back in through a stray import now
     * that the migration to plain Spring MVC + Spring Data JPA is complete.
     *
     * <p>Compliant: a repository is exposed via an explicit {@code @RequestMapping} controller
     *
     * <p>Violation: a class imports {@code org.springframework.data.rest.core.annotation.RepositoryEventHandler}
     */
    @ArchTest
    static final ArchRule noClassDependsOnSpringDataRest =
            //@formatter:off
            noClasses().should().dependOnClassesThat().resideInAPackage("org.springframework.data.rest..")
                       .as("no class should depend on Spring Data REST; "
                           + "it was fully replaced by plain Spring MVC + Spring Data JPA");
            //@formatter:on

}
