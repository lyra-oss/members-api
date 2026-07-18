package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class SpringBeanRulesTest {

    /**
     * Outside of the "config" package, classes may not be annotated with @Component
     * or @Service; beans must instead be registered explicitly via @Bean methods in
     * a @Configuration class, keeping bean wiring explicit rather than relying on
     * component scanning.
     */
    @ArchTest
    static final ArchRule sliceBeansAreNotComponentScanned =
            //@formatter:off
            noClasses().that().resideOutsideOfPackage("..config..")
                       .should().beAnnotatedWith("org.springframework.stereotype.Component")
                       .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
                       .as("classes outside 'config' should be registered explicitly via @Bean, "
                           + "not component-scanned as @Component/@Service");
            //@formatter:on

    /**
     * Configuration classes (@Configuration) must not be public, since they are only
     * meant to be loaded by Spring, not referenced directly from other code.
     */
    @ArchTest
    static final ArchRule configurationClassesAreNotPublic =
            noClasses().that().areAnnotatedWith(Configuration.class).should().bePublic();

    /**
     * Bean methods (@Bean) must not be public, for the same reason: they exist for
     * Spring's container to call, not for direct external invocation.
     */
    @ArchTest
    static final ArchRule beanMethodsAreNotPublic =
            methods().that().areAnnotatedWith(Bean.class).should().notBePublic();

    /**
     * Forbids field injection (@Autowired on fields); dependencies must be injected via
     * constructors so they can be made immutable and are visible at construction time.
     */
    @ArchTest
    static final ArchRule constructorInjectionOnly = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

}
