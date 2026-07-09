package edu.lyra.members.api;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.context.annotation.Configuration;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "edu.lyra.members.api")
class ArchitectureTest {

    @ArchTest
    static final ArchRule springConfigurationClassName =
            classes().that().areAnnotatedWith(Configuration.class).should().haveSimpleNameEndingWith("Configuration");

}
