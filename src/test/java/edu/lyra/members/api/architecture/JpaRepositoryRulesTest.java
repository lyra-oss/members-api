package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class JpaRepositoryRulesTest {

    /**
     * Every Spring Data @Repository must also be annotated with @Transactional.
     */
    @ArchTest
    static final ArchRule repositoriesAreTransactional =
            classes().that().areAnnotatedWith(Repository.class).should().beAnnotatedWith(Transactional.class);

    /**
     * Forbids the Jakarta @Transactional annotation anywhere; use Spring's
     * org.springframework.transaction.annotation.Transactional instead, since only the Spring
     * annotation is proxy-aware in this codebase.
     */
    @ArchTest
    static final ArchRule noJakartaTransactional =
            noClasses().should().beAnnotatedWith("jakarta.transaction.Transactional")
                       .as("use org.springframework.transaction.annotation.Transactional, "
                           + "not jakarta.transaction.Transactional");

}
