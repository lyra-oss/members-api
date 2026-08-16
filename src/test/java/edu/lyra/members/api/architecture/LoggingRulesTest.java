package edu.lyra.members.api.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class LoggingRulesTest {

    private static final String NOT_ANNOTATED_WITH_SLF4J_MESSAGE =
            "%s is not annotated with @Slf4j (no 'log' field of type org.slf4j.Logger was found)";

    private static final String DOES_NOT_LOG_ANYTHING_MESSAGE = "%s does not log anything";

    private static final String MAPPED_METHOD_DOES_NOT_LOG_MESSAGE = "%s does not log anything";

    private static final DescribedPredicate<JavaClass> IS_SPRING_CONTROLLER =
            DescribedPredicate.describe("is a Spring controller",
                                        javaClass -> javaClass.isAnnotatedWith(RestController.class) ||
                                                     javaClass.isAnnotatedWith(Controller.class));

    private static final DescribedPredicate<JavaMethod> IS_MAPPED_METHOD =
            DescribedPredicate.describe("is a request-mapped method",
                                        method -> method.isAnnotatedWith(RequestMapping.class) ||
                                                  method.isAnnotatedWith(GetMapping.class) ||
                                                  method.isAnnotatedWith(PostMapping.class) ||
                                                  method.isAnnotatedWith(PutMapping.class) ||
                                                  method.isAnnotatedWith(PatchMapping.class) ||
                                                  method.isAnnotatedWith(DeleteMapping.class));

    private static final DescribedPredicate<JavaClass> IS_AN_ADAPTER_OR_POLICY =
            DescribedPredicate.describe("has a simple name ending with Adapter or Policy",
                                        javaClass -> javaClass.getSimpleName().endsWith("Adapter") ||
                                                     javaClass.getSimpleName().endsWith("Policy"));

    /**
     * Every Spring controller ({@code @RestController} or {@code @Controller}) must have an SLF4J logger
     * ({@code @Slf4j}), and every one of its request-mapped methods ({@code @GetMapping}, {@code @PostMapping}, etc.)
     * must log at least one line, so every inbound request leaves a trace.
     *
     * <p>Compliant:
     * <pre>{@code
     * @Slf4j
     * @RestController
     * class PersonController {
     *     @GetMapping
     *     Person get(final UUID id) {
     *         log.info("fetching person {}", id);
     *         return ...;
     *     }
     * }
     * }</pre>
     *
     * <p>Violation (mapped method does not log):
     * <pre>{@code
     * @Slf4j
     * @RestController
     * class PersonController {
     *     @GetMapping
     *     Person get(final UUID id) {
     *         return ...; // no log line
     *     }
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule controllersLogTheirMappedMethods =
            //@formatter:off
            classes().that(IS_SPRING_CONTROLLER)
                     .should(new ArchCondition<>(
                             "be annotated with @Slf4j and log at least one line in every mapped method") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             events.add(new SimpleConditionEvent(javaClass, hasSlf4jLogger(javaClass),
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
     * Every {@code *Adapter} and {@code *Policy} class must have an SLF4J logger and log at least one line somewhere in
     * the class, so every data-access orchestration and every authorization decision is traceable.
     *
     * <p>Compliant:
     * <pre>{@code
     * @Slf4j
     * class SchoolPolicy {
     *     void authorizeUpdate(final School school) {
     *         log.debug("Authorizing update of school {}", school.getId());
     *         ...
     *     }
     * }
     * }</pre>
     *
     * <p>Violation (no {@code log.*(...)} call anywhere in the class):
     * <pre>{@code
     * @Slf4j
     * class SchoolPolicy {
     *     void authorizeUpdate(final School school) { ... }
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule adaptersAndPoliciesLogTheirDecisions =
            //@formatter:off
            classes().that(IS_AN_ADAPTER_OR_POLICY)
                     .should(new ArchCondition<>("be annotated with @Slf4j and log at least one line") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             events.add(new SimpleConditionEvent(javaClass, hasSlf4jLogger(javaClass),
                                                                 NOT_ANNOTATED_WITH_SLF4J_MESSAGE.formatted(
                                                                         javaClass.getFullName())));
                             final boolean logsAtLeastOneLine = javaClass.getMethodCallsFromSelf().stream()
                                                                          .anyMatch(LoggingRulesTest::isLoggerCall);
                             events.add(new SimpleConditionEvent(javaClass, logsAtLeastOneLine,
                                                                 DOES_NOT_LOG_ANYTHING_MESSAGE.formatted(
                                                                         javaClass.getFullName())));
                         }
                     });
    //@formatter:on

    private static boolean logsWithinMethod(final JavaClass javaClass, final JavaMethod method) {
        final String lambdaPrefix = "lambda$" + method.getName() + "$";
        //@formatter:off
        return javaClass.getMethods().stream().filter(candidate ->
                                 candidate.getName().equals(method.getName()) ||
                                 candidate.getName().startsWith(lambdaPrefix))
                        .flatMap(candidate -> candidate.getMethodCallsFromSelf().stream())
                        .anyMatch(LoggingRulesTest::isLoggerCall);
        //@formatter:on
    }

    private static boolean isLoggerCall(final JavaMethodCall call) {
        return call.getTarget().getOwner().isAssignableTo(Logger.class);
    }

    private static boolean hasSlf4jLogger(final JavaClass javaClass) {
        return javaClass.tryGetField("log").filter(field -> field.getRawType().isAssignableTo(Logger.class))
                        .isPresent();
    }

}
