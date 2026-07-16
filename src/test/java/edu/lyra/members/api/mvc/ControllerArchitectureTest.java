package edu.lyra.members.api.mvc;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.slf4j.Logger;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "edu.lyra.members.api.mvc")
class ControllerArchitectureTest {

    private static final String NOT_ANNOTATED_WITH_SLF4J_MESSAGE =
            "%s is not annotated with @Slf4j (no 'log' field of type org.slf4j.Logger was found)";

    private static final String MAPPED_METHOD_DOES_NOT_LOG_MESSAGE = "%s does not log anything";

    private static final DescribedPredicate<JavaClass> IS_SPRING_CONTROLLER =
            new DescribedPredicate<>("is a Spring controller") {

                @Override
                public boolean test(final JavaClass javaClass) {
                    return javaClass.isAnnotatedWith(RestController.class) ||
                           javaClass.isAnnotatedWith(Controller.class) ||
                           javaClass.isAnnotatedWith(RepositoryRestController.class);
                }
            };

    private static final DescribedPredicate<JavaMethod> IS_MAPPED_METHOD =
            new DescribedPredicate<>("is a request-mapped method") {

                @Override
                public boolean test(final JavaMethod method) {
                    return method.isAnnotatedWith(RequestMapping.class) || method.isAnnotatedWith(GetMapping.class) ||
                           method.isAnnotatedWith(PostMapping.class) || method.isAnnotatedWith(PutMapping.class) ||
                           method.isAnnotatedWith(PatchMapping.class) || method.isAnnotatedWith(DeleteMapping.class);
                }
            };

    @ArchTest
    static final ArchRule controllersLogTheirMappedMethods =
            //@formatter:off
            classes().that(IS_SPRING_CONTROLLER)
                     .should(new ArchCondition<>(
                             "be annotated with @Slf4j and log at least one line in every mapped method") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             final boolean hasSlf4jLogger = javaClass.tryGetField("log")
                                                                     .filter(field -> field.getRawType().isAssignableTo(
                                                                             Logger.class)).isPresent();
                             events.add(new SimpleConditionEvent(javaClass, hasSlf4jLogger,
                                                                 NOT_ANNOTATED_WITH_SLF4J_MESSAGE.formatted(
                                                                         javaClass.getFullName())));

                             javaClass.getMethods().stream().filter(IS_MAPPED_METHOD).forEach(method -> {
                                 final boolean logsAtLeastOneLine = logsWithinMethod(javaClass, method);
                                 events.add(new SimpleConditionEvent(method, logsAtLeastOneLine,
                                                                     MAPPED_METHOD_DOES_NOT_LOG_MESSAGE.formatted(
                                                                             method.getFullName())));
                             });
                         }
                     });
    //@formatter:on

    /**
     * Considers not only calls made directly from the mapped method's own body, but also calls made from lambdas
     * declared within it: the compiler lowers those to synthetic {@code lambda$<methodName>$<n>} methods on the same
     * class, which ArchUnit reports as separate {@link JavaMethod}s with their own, otherwise-uncredited call sets.
     */
    private static boolean logsWithinMethod(final JavaClass javaClass, final JavaMethod method) {
        final String lambdaPrefix = "lambda$" + method.getName() + "$";
        //@formatter:off
        return javaClass.getMethods().stream().filter(candidate ->
                                 candidate.getName().equals(method.getName()) ||
                                 candidate.getName().startsWith(lambdaPrefix))
                        .flatMap(candidate -> candidate.getMethodCallsFromSelf().stream())
                        .anyMatch(call -> call.getTarget().getOwner().isAssignableTo(Logger.class));
        //@formatter:on
    }

}
