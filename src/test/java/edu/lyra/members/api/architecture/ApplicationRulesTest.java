package edu.lyra.members.api.architecture;

import java.util.List;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class ApplicationRulesTest {

    private static final String NOT_MINIMAL_MESSAGE =
            "%s must contain only the minimum code needed to start a Spring Boot application (no fields, and no " +
            "members beyond a single static main(String[] args) method)";

    /**
     * Every class named {@code *Application} must be the Spring Boot entry point, annotated with
     * {@code @SpringBootApplication}, so it is unambiguously the class that boots the service.
     *
     * <p>Compliant: {@code @SpringBootApplication class MembersApiApplication}
     *
     * <p>Violation: {@code class MembersApiApplication} (missing the annotation)
     */
    @ArchTest
    static final ArchRule applicationClassesAreAnnotatedWithSpringBootApplication =
            classes().that().haveSimpleNameEndingWith("Application")
                     .should().beAnnotatedWith(SpringBootApplication.class);

    /**
     * The inverse of the rule above: every {@code @SpringBootApplication} class must have a simple name ending in
     * "Application", so the entry point is always easy to spot by name alone.
     *
     * <p>Compliant: {@code @SpringBootApplication class MembersApiApplication}
     *
     * <p>Violation: {@code @SpringBootApplication class MembersApiBootstrap}
     */
    @ArchTest
    static final ArchRule springBootApplicationsAreNamedApplication =
            classes().that().areAnnotatedWith(SpringBootApplication.class)
                     .should().haveSimpleNameEndingWith("Application");

    /**
     * Every class named {@code *Application} is the Spring Boot entry point and must contain only the minimum code
     * needed to start the application: no fields, and no members beyond a single {@code static void
     * main(String[] args)} method. Any other logic belongs in a proper {@code @Configuration} class or vertical
     * slice, keeping the entry point trivially correct and exempt from coverage/mutation analysis (see the
     * Sonar/PIT exclusions in pom.xml).
     *
     * <p>Compliant:
     * <pre>{@code
     * @SpringBootApplication
     * public class MembersApiApplication {
     *     static void main(final String[] args) {
     *         SpringApplication.run(MembersApiApplication.class, args);
     *     }
     * }
     * }</pre>
     *
     * <p>Violation (extra field or method):
     * <pre>{@code
     * @SpringBootApplication
     * public class MembersApiApplication {
     *     private static final Logger LOG = LoggerFactory.getLogger(MembersApiApplication.class); // a field
     *     static void main(final String[] args) { ... }
     *     static void doSomethingElse() { ... } // a second method
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule applicationClassesContainOnlyTheMinimumCodeToStart =
            //@formatter:off
            classes().that().haveSimpleNameEndingWith("Application")
                     .should(new ArchCondition<>("contain only the minimum code needed to start Spring Boot") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             final boolean minimal =
                                     javaClass.getFields().isEmpty() && hasOnlyTheMainMethod(javaClass);
                             events.add(new SimpleConditionEvent(javaClass, minimal,
                                                                 NOT_MINIMAL_MESSAGE.formatted(
                                                                         javaClass.getFullName())));
                         }
                     });
    //@formatter:on

    private static boolean hasOnlyTheMainMethod(final JavaClass javaClass) {
        final List<JavaMethod> methods = List.copyOf(javaClass.getMethods());
        return methods.size() == 1 && isMainMethod(methods.get(0));
    }

    private static boolean isMainMethod(final JavaMethod method) {
        return "main".equals(method.getName()) && method.getModifiers().contains(JavaModifier.STATIC) &&
               method.getRawParameterTypes().size() == 1 &&
               method.getRawParameterTypes().get(0).isEquivalentTo(String[].class);
    }

}
