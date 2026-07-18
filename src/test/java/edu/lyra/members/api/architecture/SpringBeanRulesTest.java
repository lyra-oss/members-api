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
