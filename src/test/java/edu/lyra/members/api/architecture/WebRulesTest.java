package edu.lyra.members.api.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class WebRulesTest {

    private static final DescribedPredicate<JavaMethod> ARE_REQUEST_MAPPED =
            new DescribedPredicate<>("are request-mapped") {

                @Override
                public boolean test(final JavaMethod method) {
                    return method.isAnnotatedWith(RequestMapping.class) || method.isAnnotatedWith(GetMapping.class) ||
                           method.isAnnotatedWith(PostMapping.class) || method.isAnnotatedWith(PutMapping.class) ||
                           method.isAnnotatedWith(PatchMapping.class) || method.isAnnotatedWith(DeleteMapping.class);
                }
            };

    private static final DescribedPredicate<JavaMethod> ARE_REPOSITORY_EVENT_HANDLER_METHODS =
            new DescribedPredicate<>("are Spring Data REST @Handle* methods") {

                @Override
                public boolean test(final JavaMethod method) {
                    return method.getAnnotations().stream().anyMatch(
                            annotation -> annotation.getRawType().getName().startsWith(
                                    "org.springframework.data.rest.core.annotation.Handle"));
                }
            };

    // Transitional: accepts either controller stereotype while the migration off Spring Data REST moves
    // one vertical slice at a time; see NamingRulesTest.IS_A_CONTROLLER_STEREOTYPE for the same pairing.
    private static final DescribedPredicate<JavaClass> IS_A_CONTROLLER_STEREOTYPE =
            new DescribedPredicate<>("is annotated with @RepositoryRestController or @RestController") {

                @Override
                public boolean test(final JavaClass javaClass) {
                    return javaClass.isAnnotatedWith(RepositoryRestController.class) ||
                           javaClass.isAnnotatedWith(RestController.class);
                }
            };

    /**
     * Request-mapped methods ({@code @GetMapping}, {@code @PostMapping}, etc.) declared in a
     * {@code @RepositoryRestController} or {@code @RestController} must not be public, since Spring MVC
     * always invokes handler methods reflectively rather than through direct calls.
     *
     * <p>Compliant:
     * <pre>{@code
     * @RestController
     * class PersonController {
     *     @GetMapping
     *     Person get(final UUID id) { ... }
     * }
     * }</pre>
     *
     * <p>Violation:
     * <pre>{@code
     * @RestController
     * class PersonController {
     *     @GetMapping
     *     public Person get(final UUID id) { ... }
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule mappedControllerMethodsAreNotPublic =
            methods().that(ARE_REQUEST_MAPPED).and().areDeclaredInClassesThat(IS_A_CONTROLLER_STEREOTYPE)
                     .should().notBePublic();

    /**
     * Methods annotated with a Spring Data REST {@code @Handle*} annotation in a
     * {@code @RepositoryEventHandler} must be public, since Spring Data REST needs to invoke them
     * directly.
     *
     * <p>Compliant:
     * <pre>{@code
     * @RepositoryEventHandler
     * class PersonHandler {
     *     @HandleBeforeSave
     *     public void beforeSave(final Person person) { ... }
     * }
     * }</pre>
     *
     * <p>Violation:
     * <pre>{@code
     * @RepositoryEventHandler
     * class PersonHandler {
     *     @HandleBeforeSave
     *     private void beforeSave(final Person person) { ... }
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule handlerMethodsArePublic =
            methods().that(ARE_REPOSITORY_EVENT_HANDLER_METHODS)
                     .and().areDeclaredInClassesThat().areAnnotatedWith(RepositoryEventHandler.class)
                     .should().bePublic();

}
