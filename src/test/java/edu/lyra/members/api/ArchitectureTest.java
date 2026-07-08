package edu.lyra.members.api;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.slf4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "edu.lyra.members.api")
class ArchitectureTest {

    private static final String NOT_ANNOTATED_WITH_SLF4J_MESSAGE =
            "%s is not annotated with @Slf4j (no 'log' field of type org.slf4j.Logger was found)";

    private static final String DOES_NOT_LOG_ANYTHING_MESSAGE = "%s does not log anything";

    @ArchTest
    static final ArchRule springConfigurationClassName =
            classes().that().areAnnotatedWith(Configuration.class).should().haveSimpleNameEndingWith("Configuration");

    @ArchTest
    static final ArchRule repositoryEventHandlersLogTheirEvents =
            classes().that().areAnnotatedWith(RepositoryEventHandler.class)
                     .should(new ArchCondition<>("Be annotated with @Slf4j and log at least one line") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             final boolean hasSlf4jLogger = javaClass.tryGetField("log")
                                                                     .filter(field -> field.getRawType().isAssignableTo(
                                                                             Logger.class)).isPresent();
                             events.add(new SimpleConditionEvent(javaClass, hasSlf4jLogger,
                                                                 NOT_ANNOTATED_WITH_SLF4J_MESSAGE.formatted(
                                                                         javaClass.getFullName())));
                             final boolean logsAtLeastOneLine = javaClass.getMethodCallsFromSelf().stream().anyMatch(
                                     call -> call.getTarget().getOwner().isAssignableTo(Logger.class));
                             events.add(new SimpleConditionEvent(javaClass, logsAtLeastOneLine,
                                                                 DOES_NOT_LOG_ANYTHING_MESSAGE.formatted(
                                                                         javaClass.getFullName())));
                         }
                     });

}
