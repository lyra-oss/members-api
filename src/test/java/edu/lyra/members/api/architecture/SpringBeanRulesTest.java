package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Pins how beans get into the context: explicitly, through package-private {@code @Bean} factory methods on
 * package-private {@code @Configuration} classes, with dependencies handed in via constructors. Vertical-slice
 * internals (handlers, controllers) are therefore never component-scanned {@code @Component}/{@code @Service} beans —
 * which is what lets them stay package-private and invisible outside their aggregate. The one legitimately
 * component-scanned bean, {@code KeycloakRoleStrategy}, lives under {@code config} and is exempted.
 * Scoped to main source only: test fixtures wire themselves with Spring's test support and field {@code @Autowired}.
 */
@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class SpringBeanRulesTest {

    @ArchTest
    static final ArchRule sliceBeansAreNotComponentScanned =
            //@formatter:off
            noClasses().that().resideOutsideOfPackage("..config..")
                       .should().beAnnotatedWith("org.springframework.stereotype.Component")
                       .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
                       .as("classes outside 'config' should be registered explicitly via @Bean, "
                           + "not component-scanned as @Component/@Service");
            //@formatter:on

    @ArchTest
    static final ArchRule configurationClassesAreNotPublic =
            noClasses().that().areAnnotatedWith(Configuration.class).should().bePublic();

    @ArchTest
    static final ArchRule beanMethodsAreNotPublic =
            methods().that().areAnnotatedWith(Bean.class).should().notBePublic();

    @ArchTest
    static final ArchRule constructorInjectionOnly = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

}
