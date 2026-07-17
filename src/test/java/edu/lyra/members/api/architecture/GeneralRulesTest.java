package edu.lyra.members.api.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.context.annotation.Configuration;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

@AnalyzeClasses(packages = "edu.lyra.members.api")
class GeneralRulesTest {

    @ArchTest
    static final ArchRule springConfigurationClassName =
            classes().that().areAnnotatedWith(Configuration.class).should().haveSimpleNameEndingWith("Configuration");

    @ArchTest
    static final ArchRule noClassesAccessStandardStreams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

}
