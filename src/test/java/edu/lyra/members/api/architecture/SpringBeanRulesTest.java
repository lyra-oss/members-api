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
     * Outside of the "config" package, classes may not be annotated with {@code @Component} or
     * {@code @Service}; beans must instead be registered explicitly via {@code @Bean} methods in a
     * {@code @Configuration} class, keeping bean wiring explicit rather than relying on component
     * scanning.
     *
     * <p>Compliant:
     * <pre>{@code
     * @Configuration
     * class PersonSliceConfiguration {
     *     @Bean
     *     PersonService personService(final PersonRepository repository) {
     *         return new PersonService(repository);
     *     }
     * }
     * }</pre>
     *
     * <p>Violation:
     * <pre>{@code
     * @Service
     * class PersonService { ... } // component-scanned outside 'config'
     * }</pre>
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
     * {@code @Configuration} classes must not be public, since they are only meant to be loaded by
     * Spring, not referenced directly from other code.
     *
     * <p>Compliant: {@code @Configuration class WebConfiguration}
     *
     * <p>Violation: {@code @Configuration public class WebConfiguration}
     */
    @ArchTest
    static final ArchRule configurationClassesAreNotPublic =
            noClasses().that().areAnnotatedWith(Configuration.class).should().bePublic();

    /**
     * {@code @Bean} methods must not be public, for the same reason: they exist for Spring's
     * container to call, not for direct external invocation.
     *
     * <p>Compliant: {@code @Bean PersonService personService() { ... }}
     *
     * <p>Violation: {@code @Bean public PersonService personService() { ... }}
     */
    @ArchTest
    static final ArchRule beanMethodsAreNotPublic =
            methods().that().areAnnotatedWith(Bean.class).should().notBePublic();

    /**
     * Forbids field injection ({@code @Autowired} on fields); dependencies must be injected via
     * constructors so they can be made immutable and are visible at construction time.
     *
     * <p>Compliant:
     * <pre>{@code
     * class PersonService {
     *     private final PersonRepository repository;
     *     PersonService(final PersonRepository repository) { this.repository = repository; }
     * }
     * }</pre>
     *
     * <p>Violation:
     * <pre>{@code
     * class PersonService {
     *     @Autowired
     *     private PersonRepository repository;
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule constructorInjectionOnly = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

}
